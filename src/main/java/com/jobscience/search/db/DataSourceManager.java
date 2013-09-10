package com.jobscience.search.db;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import javax.inject.Singleton;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class DataSourceManager {
    private DataSource sysDs;
    private Map<String, DataSource> dsMap = new HashMap<String, DataSource>();
    private String url;
    private String orgUser;
    private String orgPwd;
    @Inject
    private DBHelper dbHelper;

    @Inject
    public void init(@Named("jss.db_url") String url,
                     @Named("jss.sys_db.schema") String sysSchema,
                     @Named("jss.sys_db.user") String sysUser,
                     @Named("jss.sys_db.pwd") String sysPwd,
                     @Named("jss.org_db.user") String user,
                     @Named("jss.org_db.pwd") String pwd) {
        sysDs = buildDs(url, sysUser, sysPwd, sysSchema);
        this.url = url;
        this.orgUser = user;
        this.orgPwd = pwd;
    }

    public DataSource getSysDataSource() {
        return sysDs;
    }

    public DataSource getOrgDataSource(String orgName) {
        DataSource ds = dsMap.get(orgName);
        if (ds == null) {
            List<Map> list = dbHelper.executeQuery(sysDs, "select * from org where name =?", orgName);
            if (list.size() > 0) {
                String schema = (String) list.get(0).get("schemaname");
                ds = buildDs(url, orgUser, orgPwd, schema);
                dsMap.put(orgName.trim(), ds);
            }
        }
        return ds;
    }

    private DataSource buildDs(String url, String user, String pwd, String schema) {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(url);
        ds.setUser(user);
        ds.setPassword(pwd);
        ds.setUnreturnedConnectionTimeout(0);
        return new DataSourceWrapper(ds, schema);
    }

}

class DataSourceWrapper implements DataSource {
    private final DataSource ds;
    private final String schema;

    DataSourceWrapper(DataSource ds, String schema) {
        this.ds = ds;
        this.schema = schema;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = ds.getConnection();
        return intConnection(con);
    }

    private Connection intConnection(Connection con) {
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement("SET search_path = " + schema);
            pstmt.execute();
            return con;
        } catch (SQLException e) {
            throw Throwables.propagate(e);
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    //
                }
            }
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection con = ds.getConnection(username, password);
        return intConnection(con);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return ds.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        ds.setLogWriter(out);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return ds.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        ds.setLoginTimeout(seconds);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return ds.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return ds.isWrapperFor(iface);
    }
}

