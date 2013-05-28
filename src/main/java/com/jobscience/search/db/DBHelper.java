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
import com.mchange.v2.c3p0.ComboPooledDataSource;

@Singleton
public class DBHelper {

    ComboPooledDataSource cpds;

    public DBHelper() {
        cpds = new ComboPooledDataSource();
        cpds.setJdbcUrl("jdbc:postgresql://localhost:5432/jobscience_db");
        cpds.setUser("postgres");
        cpds.setPassword("welcome");
        cpds.setUnreturnedConnectionTimeout(0);
    }

    public Connection getConnection() {
        try {
            // Class.forName("org.postgresql.Driver");
            // Connection con = null;
            // con = DriverManager.getConnection(
            // "jdbc:postgresql://localhost:5432/jobscience_db","postgres", "welcome");

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
        try {
            Connection con = getConnection();
            PreparedStatement pstmt = con.prepareStatement(query);
            return preparedStatementExecuteQuery(pstmt, vals);
        } catch (SQLException e) {
            throw Throwables.propagate(e);
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
