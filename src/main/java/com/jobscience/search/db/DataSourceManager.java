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
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Singleton
public class DataSourceManager {
    private DataSource defaultDs;
    private DataSource sysDs;
    private Map<String, DataSource> dsMap = new ConcurrentHashMap<String, DataSource>();
    private String url;
    private String orgUser;
    private String orgPwd;
    private String sysUser;
    private String sysPwd;
    private String sysSchema;
    @Inject
    private DBHelper dbHelper;
    private  DataSource publicDataSource;
    @Inject
    public void init(@Named("jss.db_url") String url,
                     @Named("jss.sys_db.schema") String sysSchema,
                     @Named("jss.sys_db.user") String sysUser,
                     @Named("jss.sys_db.pwd") String sysPwd,
                     @Named("jss.org_db.user") String user,
                     @Named("jss.org_db.pwd") String pwd) {
        defaultDs = buildDs(url, sysUser, sysPwd,null);
        this.url = url;
        this.orgUser = user;
        this.orgPwd = pwd;
        this.sysPwd = sysPwd;
        this.sysUser = sysUser;
        this.sysSchema = sysSchema;
        if(checkSysSchema()){
        	sysDs = buildDs(url, sysUser, sysPwd, sysSchema);
        }
        publicDataSource = buildDs(url, sysUser, sysPwd,"public");
    }

    public DataSource getSysDataSource() {
        return sysDs;
    }
    
    public DataSource getPublicDataSource(){
    	return publicDataSource;
    }
    public DataSource createSysSchemaIfNecessary() {
    	if(!checkSysSchema()){
    		dbHelper.executeUpdate(defaultDs, "CREATE SCHEMA jss_sys AUTHORIZATION "+sysUser,new Object[0]);
    		if(sysDs==null){
    			sysDs = buildDs(url, sysUser, sysPwd, sysSchema);
    		}
    	}
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

    public Connection getDefaultConnection() throws SQLException {
        return defaultDs.getConnection();
    }

    public DataSource getDefaultDataSource() {
        return defaultDs;
    }
    
    private DataSource buildDs(String url, String user, String pwd, String schema) {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(url);
        ds.setUser(user);
        ds.setPassword(pwd);
        ds.setUnreturnedConnectionTimeout(0);
        //System.out.println("buildDS: " + url + " " + user + "/" + pwd + " schema: " + schema);
        if(schema == null || "".equals(schema)){
            return ds;
        }
        return new DataSourceWrapper(ds, schema);
    }

    private  boolean checkSysSchema(){
    	List<Map> list = dbHelper.executeQuery(getDefaultDataSource(), "select count(*) as count from information_schema.schemata" +
        		" where schema_name='"+sysSchema+"'");
    	if(list.size()==1){
    		if("1".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    public DataSource updateDataSource(String orgName,String schemaName){
    	 DataSourceWrapper ds = (DataSourceWrapper) dsMap.get(orgName);
    	 if(ds!=null){
    		 ds.update(schemaName);
    	 }else{
    		 ds = (DataSourceWrapper) buildDs(url, orgUser, orgPwd, schemaName);
    	 }
    	 dsMap.put(orgName.trim(), ds);
         return ds;
    }
}

class DataSourceWrapper implements DataSource {
    private final DataSource ds;
    private  String schema;

    DataSourceWrapper(DataSource ds, String schema) {
        this.ds = ds;
        this.schema = schema;
    }
    
    public void update(String schema) {
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
        	if(schema!=null){
	            pstmt = con.prepareStatement("SET search_path = " + schema);
	            pstmt.execute();
        	}
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


    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}


