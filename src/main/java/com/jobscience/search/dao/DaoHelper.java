package com.jobscience.search.dao;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.inject.Singleton;
import javax.sql.DataSource;

import com.google.common.base.Throwables;
import com.google.inject.name.Named;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.josql.DBHelper;
import org.josql.DBHelperBuilder;
import org.josql.Runner;

import com.britesnow.snow.web.hook.AppPhase;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.google.inject.Inject;

@Singleton
public class DaoHelper {
    

    private DBHelper defaultDBHelper;
    private DBHelper sysDBHelper;
    private Map<String,DBHelper> orgDBHelperByName = new ConcurrentHashMap<String, DBHelper>();


    private DataSource defaultDs;
    private DataSource sysDs = null;
    private String url;
    private String user;
    private String pwd;
    private String sysSchema = "jss_sys";


    @Inject
    public void init(@Named("jss.db.url") String url,
                     @Named("jss.db.user") String user,
                     @Named("jss.db.pwd") String pwd) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        defaultDs = buildDs(url,null);
        //should do in app init,otherwise will cause recursion exception
/*        if(checkSysSchema()){
            sysDs = buildDs(url, sysSchema);
        }*/
    }
    
    
    // --------- DaoHelper Initialization --------- //
    @WebApplicationHook(phase = AppPhase.INIT)
    public void initDBHelpers(){
        defaultDBHelper = new DBHelperBuilder().newDBHelper(defaultDs);
        if (checkSysSchema()) {
            sysDs = buildDs(url, sysSchema);
        }
        sysDBHelper = new DBHelperBuilder().newDBHelper(getSysDataSource());
    }
    // --------- /DaoHelper Initialization --------- //

    public Runner openDefaultRunner(){
        return defaultDBHelper.newRunner();
    }
    
    public Runner openNewSysRunner(){
        return sysDBHelper.newRunner();
    }
    
    public Runner openNewOrgRunner(String orgName){
        
        return getOrgDBHelper(orgName).newRunner();
    }
    
    public DBHelper getOrgDBHelper(String orgName){
        DBHelper orgDBHelper = orgDBHelperByName.get(orgName);
        
        // if null, we create it.
        if(orgDBHelper == null){
            synchronized(this){
                // since we are in a synchronize queue now we double check again
                orgDBHelper = orgDBHelperByName.get(orgName);
                if (orgDBHelper == null){
                    orgDBHelper = new DBHelperBuilder().newDBHelper(getOrgDataSource(orgName));
                    orgDBHelperByName.put(orgName, orgDBHelper);
                }
            }
        }
        return orgDBHelper;
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

    // --------- DataSource method -------- //
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

    public DataSource getOrgDataSource(String orgName) {
        DataSource ds = null;
        List<Map> list = executeQuery(openNewSysRunner(), "select * from org where name =?", orgName);
        if (list.size() > 0) {
            String schema = (String) list.get(0).get("schemaname");
            ds = buildDs(url, schema);
        }

        return ds;
    }

    private DataSource getSysDataSource() {
        if (sysDs == null) {
            throw new IllegalArgumentException("Sys DataSource is not Initialization");
        }
        return sysDs;
    }

    public  boolean checkSysSchema(){
        List<Map> list = executeQuery(openDefaultRunner(), "select count(*) as count from information_schema.schemata" +
                " where schema_name='"+sysSchema+"'");
        if(list.size()==1){
            if("1".equals(list.get(0).get("count").toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * if system table not existed,need create it
     * @return
     */
    public DataSource createSysSchemaIfNecessary() {
        if(!checkSysSchema()){
            executeUpdate(openDefaultRunner(), "CREATE SCHEMA " + sysSchema + " AUTHORIZATION " + user, new Object[0]);
        }
        if(sysDs==null){
            sysDs = buildDs(url, sysSchema);
        }
        return sysDs;
    }

    public void updateDataSource(String orgName) {
        orgDBHelperByName.remove(orgName);
    }

    // --------- /DataSource method -------- //
    
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
