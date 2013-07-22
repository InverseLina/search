package com.jobscience.search.db;

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

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mchange.v2.c3p0.ComboPooledDataSource;

@Singleton
public class DBHelper {

    private ComboPooledDataSource cpds;

    @Inject
    public void init(@Named("db.url") String url,@Named("db.user") String user, @Named("db.password") String password){
        cpds = new ComboPooledDataSource();
        cpds.setJdbcUrl(url);
        cpds.setUser(user);
        cpds.setPassword(password);
        cpds.setUnreturnedConnectionTimeout(0);
    }

    public Connection getConnection() {
        try {
            Connection con = cpds.getConnection();
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

    public List<Map> executeQuery(String query) {
        List<Map> results = null;
        Statement stmt = null;
        try {
            Connection con = getConnection();
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

    public List<Map> executeQuery(String query, Object... vals) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {

            con = getConnection();

            pstmt = con.prepareStatement(query);
            return preparedStatementExecuteQuery(pstmt, vals);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    //
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    //
                }
            }
        }
    }
    public int executeUpdate(String query, Object... vals) {
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            pstmt = con.prepareStatement(query);
            return preparedStatementExecuteUpdate(pstmt, vals);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    //
                }
            }
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    //
                }
            }
        }
    }

    // --------- PreparedStatement Executes --------- //
    public List<Map> preparedStatementExecuteQuery(PreparedStatement pstmt, Object... vals) {
        try {
            ResultSet rs = setValues(pstmt,vals).executeQuery();
            List<Map> results = buildResults(rs);
            return results;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    public int preparedStatementExecuteCount(PreparedStatement pstmt, Object... vals) {
        try {
            ResultSet rs = setValues(pstmt,vals).executeQuery();
            rs.next();
            int count = rs.getInt(1);  
            return count;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    public int preparedStatementExecuteUpdate(PreparedStatement pstmt, Object... vals) {
        try {
            return setValues(pstmt,vals).executeUpdate();
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
                // String paramType = rsmd.getColumnTypeName(cidx);
                // System.out.println("param: " + params[i] + " " + paramType);
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

}
