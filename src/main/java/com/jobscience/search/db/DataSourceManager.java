package com.jobscience.search.db;

import java.io.PrintWriter;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.sql.DataSource;

import org.josql.DBHelperBuilder;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jobscience.search.dao.DaoHelper;
import com.mchange.v2.c3p0.ComboPooledDataSource;

@Singleton
public class DataSourceManager {
    private DataSource defaultDs;
    private DataSource sysDs;
    private Map<String, DataSource> dsMap = new ConcurrentHashMap<String, DataSource>();
    private String url;
    private String user;
    private String pwd;
    private String sysSchema = "jss_sys";
    @Inject
    private DaoHelper daoHelper;
    private  DataSource publicDataSource;
    @Inject
    public void init(@Named("jss.db.url") String url,
                     @Named("jss.db.user") String user,
                     @Named("jss.db.pwd") String pwd) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        defaultDs = buildDs(url,null);
        if(checkSysSchema()){
        	sysDs = buildDs(url, sysSchema);
        }
        publicDataSource = buildDs(url,"public");
        
        new DBHelperBuilder().newDBHelper(defaultDs);
    }

    public DataSource getSysDataSource() {
        return sysDs;
    }
    
    public DataSource getPublicDataSource(){
    	return publicDataSource;
    }
    
    /**
     * if system table not existed,need create it 
     * @return
     */
    public DataSource createSysSchemaIfNecessary() {
    	if(!checkSysSchema()){
    		daoHelper.executeUpdate(daoHelper.openDefaultRunner(), "CREATE SCHEMA "+sysSchema+" AUTHORIZATION "+user,new Object[0]);
    		if(sysDs==null){
    			sysDs = buildDs(url, sysSchema);
    		}
    	}
        return sysDs;
    }
    
    public DataSource getOrgDataSource(String orgName) {
        DataSource ds = dsMap.get(orgName);
        if (ds == null) {
            List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select * from org where name =?", orgName);
            if (list.size() > 0) {
                String schema = (String) list.get(0).get("schemaname");
                ds = buildDs(url, schema);
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
    
    private DataSource buildDs(String url, String schema) {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(url);
        ds.setUser(user);
        ds.setPassword(pwd);
        ds.setUnreturnedConnectionTimeout(0);
        if(schema == null || "".equals(schema)){
            return ds;
        }
        return new DataSourceWrapper(ds, schema);
    }

    public  boolean checkSysSchema(){
        String sql = "select count(*) as count from information_schema.schemata where schema_name='"+sysSchema+"'";
        Connection conn = null;
        PreparedStatement ppst = null;
        try {
            conn = defaultDs.getConnection();
            ppst = conn.prepareStatement(sql);
            ResultSet rs = ppst.executeQuery();
            if (rs.next()) {
               if(rs.getInt("count")==1){
                   return true;
               }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (ppst != null) {
                try {
                    ppst.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    //
                }

            }
        }

    	return false;
    }
    
    public DataSource updateDataSource(String orgName,String schemaName){
    	 DataSourceWrapper ds = (DataSourceWrapper) dsMap.get(orgName);
    	 if(ds!=null){
    		 ds.update(schemaName);
    	 }else{
    		 ds = (DataSourceWrapper) buildDs(url, schemaName);
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
        return initConnection(con);
    }

    private Connection initConnection(Connection con) {
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
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection con = ds.getConnection(username, password);
        return initConnection(con);
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


