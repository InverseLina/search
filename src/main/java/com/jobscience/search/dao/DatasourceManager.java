package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.jasql.DB;
import org.jasql.DBBuilder;
import org.jasql.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.mchange.v2.c3p0.ComboPooledDataSource;

@Singleton
public class DatasourceManager {
    private String url;
    private String user;
    private String pwd;
    private String poolSize;
    private String ro_url;
    private String ro_user;
    private String ro_pwd;
    private String ro_poolSize;
    private String sysSchema = "jss_sys";
    private ComboPooledDataSource dataSource;
    private volatile DB db;
    private ConcurrentHashMap<String, String> orgs = new ConcurrentHashMap<String, String>();
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Inject
    public void init(@Named("jss.db.url") String url,
                     @Named("jss.db.user") String user,
                     @Named("jss.db.pwd") String pwd,
                     @Named("db.pool.size") String poolSize) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        this.poolSize = poolSize;
    }
    
    @Inject(optional=true)
    public void initRO(@Named("db.ro.url") String ro_url,
                     @Named("db.ro.user") String ro_user,
                     @Named("db.ro.pwd") String ro_pwd,
                     @Named("db.ro.pool.size") String ro_poolSize) {
        this.ro_url = ro_url;
        this.ro_user = ro_user;
        this.ro_pwd = ro_pwd;
        this.ro_poolSize = ro_poolSize;
    }
    
    public void setDefaultDataSource (){
        this.dataSource = buildDs(url, user, pwd, Integer.valueOf(poolSize));
        this.db = new DBBuilder().newDB(dataSource);
    }
    
    public void setReadOnlyDataSource (){
    	if (!Strings.isNullOrEmpty(ro_url) && !Strings.isNullOrEmpty(ro_user) && !Strings.isNullOrEmpty(ro_pwd) && !Strings.isNullOrEmpty(ro_poolSize)){
    		this.dataSource = buildDs(ro_url, ro_user, ro_pwd, Integer.valueOf(ro_poolSize));
    	} else {
    		this.dataSource = buildDs(url, user, pwd, Integer.valueOf(poolSize));
    	}
        this.db = new DBBuilder().newDB(dataSource);
    }
    
    public void updateDB(String orgName){
        if(!Strings.isNullOrEmpty(orgName)){
            orgs.remove(orgName);
        }
        this.db = new DBBuilder().newDB(dataSource);
    }
    

    public Runner newSysRunner(){
        Runner runner = db.newRunner();
        setSearchPath(runner,sysSchema);
        return runner;
    }
    
    public Runner newRunner(){
        return db.newRunner();
    }
    
    public Runner newOrgRunner(String orgName){
        Runner runner = db.newRunner();
        if(orgs.containsKey(orgName)){
            setSearchPath(runner, orgs.get(orgName));
        }else{
            Runner sysRunner = newSysRunner();
            List<Map> list = sysRunner.executeQuery("select * from org where name =?", orgName);
            if (list.size() > 0) {
                String schema = (String) list.get(0).get("schemaname");
                setSearchPath(runner, schema);
                orgs.put(orgName, schema);
            }else{
                logger.warn("There has no schema for organization named "+orgName);
            }
            sysRunner.close();
        }
        return runner;
    }
    
    public Map getPoolInfo(){
        Map poolInfo = new HashMap();
        try {
            poolInfo.put("numConnections",dataSource.getNumConnectionsDefaultUser());
            poolInfo.put("numBusyConnections",dataSource.getNumBusyConnectionsDefaultUser());
            poolInfo.put("numIdleConnections",dataSource.getNumIdleConnections());
        } catch (SQLException e) {
        }
        return poolInfo;
    }
    
    private void setSearchPath(Runner runner,String searchPath){
        runner.executeUpdate("set search_path to \""+searchPath+"\"");
    }
    
    private ComboPooledDataSource buildDs( String url, String user, String pwd, int pooSize) {
        ComboPooledDataSource ds = new ComboPooledDataSource();
        ds.setJdbcUrl(url);
        ds.setUser(user);
        ds.setPassword(pwd);
        ds.setMaxPoolSize(Integer.valueOf(poolSize));
        ds.setUnreturnedConnectionTimeout(0);
        return ds;
    }
}
