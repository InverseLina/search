package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.josql.DBHelperBuilder;
import org.josql.Runner;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class DaoHelper {

    @Inject
    private DataSourceManager dsMng;

    public Runner openDefaultRunner(){
        return new DBHelperBuilder().newDBHelper(dsMng.getDefaultDataSource()).newRunner();
    }
    
    public Runner openNewSysRunner(){
        return new DBHelperBuilder().newDBHelper(dsMng.getSysDataSource()).newRunner();
    }
    
    public Runner openNewOrgRunner(String orgName){
        return new DBHelperBuilder().newDBHelper(dsMng.getOrgDataSource(orgName)).newRunner();
    }
    
    // ---------  query method --------- //
    public List<Map> executeQuery(String orgName,String query) {
        return executeQuery(openNewOrgRunner(orgName), query);
    }
    
    public List<Map> executeQuery(String orgName,  String query, Object... vals) {
        return executeQuery(openNewOrgRunner(orgName), query, vals);
    }
    
    public List<Map> executeQuery(Runner runner,String sql,Object... vals){
        try{
            return runner.executeQuery(sql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /query method --------- //
    
    // --------- update method -------- //
    public int executeUpdate(String orgName, String query, Object... vals) {
        return executeUpdate(openNewOrgRunner(orgName), query, vals);
    }
    
    public int executeUpdate(Runner runner, String sql, Object... vals) {
        try{
            return runner.executeUpdate(sql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /update method -------- //
    
    // --------- insert method -------- //
    public Object insert(String orgName, String sql, Object... vals){
       return insert(openNewOrgRunner(orgName), sql, vals);
    }
    
    public Object insert(Runner runner, String sql, Object... vals){
        try{
            return runner.executeInsert(sql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /insert method -------- //
    
    // --------- Old methods --------- //
    public Connection openConnection(String orgName) {
        return openConnection(dsMng.getOrgDataSource(orgName));
    }
    
    public Connection openPublicConnection() {
        try {
			return dsMng.getPublicDataSource().getConnection();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        return null;
    }
    
    public Connection openConnection() {
        try {
            return dsMng.getDefaultConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Connection openSysConnection() {
        try {
            return dsMng.getSysDataSource().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Connection openConnection(DataSource dataSource) {
        try {
            Connection con = dataSource.getConnection();
            return con;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public PreparedStatement prepareStatement(Connection con, String stmtStr) {
        try {
            return con.prepareStatement(stmtStr);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

   
    
    public List<Map> executeQuery(DataSource ds, String query) {
    	if(ds==null||query==null){
    		return null;
    	}
        List<Map> results = null;
        Statement stmt = null;
        try {
            Connection con = openConnection(ds);
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            results = buildResults(rs);
            con.close();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    throw Throwables.propagate(e);
                }
            }
        }
        return results;
    }


    public List<Map> executeQuery(DataSource ds, String query, Object... vals) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = openConnection(ds);
            pstmt = con.prepareStatement(query);
            return preparedStatementExecuteQuery(pstmt, vals);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
   

    public int executeUpdate(DataSource ds, String query, Object... vals) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = openConnection(ds);
            pstmt = con.prepareStatement(query);
            return preparedStatementExecuteUpdate(pstmt, vals);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
      public <T extends Number>  T executeInsertReturnId(String orgName, String query, Object... vals) {
          return executeInsertReturnId(dsMng.getOrgDataSource(orgName), query, vals);
      }
      public <T extends Number>  T executeInsertReturnId(DataSource ds, String query, Object... vals) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = openConnection(ds);
            pstmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            preparedStatementExecuteUpdate(pstmt, vals);
            ResultSet rs = pstmt.getGeneratedKeys();
            Object id= null;
            if (rs != null&&rs.next()) {
                id = rs.getObject(1);
            }
            return (T)id;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // --------- PreparedStatement Executes --------- //
    public List<Map> preparedStatementExecuteQuery(PreparedStatement pstmt, Object... vals) {
        try {
            ResultSet rs = setValues(pstmt, vals).executeQuery();
            List<Map> results = buildResults(rs);
            return results;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    public int preparedStatementExecuteCount(PreparedStatement pstmt, Object... vals) {
        try {
            ResultSet rs = setValues(pstmt, vals).executeQuery();
            rs.next();
            int count = rs.getInt(1);
            return count;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    private int preparedStatementExecuteUpdate(PreparedStatement pstmt, Object... vals) {
        try {
            return setValues(pstmt, vals).executeUpdate();
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    // --------- /PreparedStatement Executes --------- //

    private PreparedStatement setValues(PreparedStatement pStmt, Object[] vals) {
        try {
            for (int i = 0; i < vals.length; i++) {
                int cidx = i + 1;
                Object val = vals[i];
                pStmt.setObject(cidx, val);
            }
            return pStmt;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    private List<Map> buildResults(ResultSet rs) {
        List<Map> results = new ArrayList<Map>();
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int c = rsmd.getColumnCount();

            String[] params = new String[c];
            for (int i = 0; i < c; i++) {
                int cidx = i + 1;
                params[i] = rsmd.getColumnName(cidx);
            }

            while (rs.next()) {
                Map map = new HashMap<String, Object>();
                for (int i = 0; i < c; i++) {
                    int cidx = i + 1;
                    String name = params[i];
                    Object val = rs.getObject(cidx);
                    if (val != null) {
                        map.put(name, val);
                    }
                }
                results.add(map);
            }

        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }

        return results;
    }
    
    // --------- /Old methods --------- //
}