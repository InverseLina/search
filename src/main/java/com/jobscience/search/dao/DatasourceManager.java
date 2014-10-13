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
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

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
    private HikariDataSource  rwdataSource;
    private HikariDataSource  rodataSource;
    private volatile DB rwdb;
    private volatile DB rodb;
    private ConcurrentHashMap<String, String> orgs = new ConcurrentHashMap<String, String>();
    private ConcurrentHashMap<String, String> roorgs = new ConcurrentHashMap<String, String>();
    private Logger logger = LoggerFactory.getLogger(getClass());
    private boolean hasRODataSource;
    
    @Inject
    public void init(@Named("jss.db.url") String url,
                     @Named("jss.db.user") String user,
                     @Named("jss.db.pwd") String pwd,
                     @Named("jss.db.pool.size") String poolSize) {
        this.url = url;
        this.user = user;
        this.pwd = pwd;
        this.poolSize = poolSize;
        setReadWriteDataSource();
    }
    
    @Inject(optional=true)
    public void initRO(@Named("jss.db.ro.url") String ro_url,
                     @Named("jss.db.ro.user") String ro_user,
                     @Named("jss.db.ro.pwd") String ro_pwd,
                     @Named("jss.db.ro.pool.size") String ro_poolSize) {
        this.ro_url = ro_url;
        this.ro_user = ro_user;
        this.ro_pwd = ro_pwd;
        this.ro_poolSize = ro_poolSize;
        setReadOnlyDataSource();
    }
    
    private void setReadWriteDataSource (){
        this.rwdataSource = buildDs(url, user, pwd, Integer.valueOf(poolSize));
        this.rwdb = new DBBuilder().newDB(rwdataSource);
    }
    
    private void setReadOnlyDataSource (){
    	if (!Strings.isNullOrEmpty(ro_url) && !Strings.isNullOrEmpty(ro_user) && !Strings.isNullOrEmpty(ro_pwd) && !Strings.isNullOrEmpty(ro_poolSize)){
    		this.rodataSource = buildDs(ro_url, ro_user, ro_pwd, Integer.valueOf(ro_poolSize));
    		hasRODataSource = true;
    	} else {
    		this.rodataSource = buildDs(url, user, pwd, Integer.valueOf(poolSize));
    		hasRODataSource = false;
    	}
        this.rodb = new DBBuilder().newDB(rodataSource);
    }

    public void updateDB(String orgName, boolean isRODB){
    	if(isRODB) {
    		if(!Strings.isNullOrEmpty(orgName)){
    			roorgs.remove(orgName);
            }
    		this.rodb = new DBBuilder().newDB(rodataSource);
    	}else{
    		if(!Strings.isNullOrEmpty(orgName)){
                orgs.remove(orgName);
            }
    		this.rwdb = new DBBuilder().newDB(rwdataSource);
    	}
    }
    

    public Runner newSysRunner(boolean isRODB){
    	Runner runner ;
    	if(isRODB) {
    		runner = rodb.newRunner();
    	}else{
    		runner = rwdb.newRunner();
    	}
        setSearchPath(runner,sysSchema);
        return runner;
    }
    
    public Runner newRunner(boolean isRODB){
    	Runner runner ;
    	if(isRODB) {
    		runner = rodb.newRunner();
    	}else{
    		runner = rwdb.newRunner();
    	}
        return runner;
    }
    
    public Runner newOrgRunner(String orgName, boolean isRODB){
    	if(isRODB) {
    		return newRODBOrgRunner(orgName, isRODB);
    	}else{
    		return newDBOrgRunner(orgName, isRODB);
    	}
    }
    
    public Map getPoolInfo(){
        Map poolInfo = new HashMap();
        poolInfo.put("maxLifetime",rwdataSource.getMaxLifetime());
        poolInfo.put("minimumIdle",rwdataSource.getMinimumIdle());
        poolInfo.put("maximumPoolSize",rwdataSource.getMaximumPoolSize());
        return poolInfo;
    }
    
    public Map getROPoolInfo(){
    	if(!hasRODataSource){
    		return null;
    	}
        Map poolInfo = new HashMap();
        poolInfo.put("maxLifetime",rodataSource.getMaxLifetime());
        poolInfo.put("minimumIdle",rodataSource.getMinimumIdle());
        poolInfo.put("maximumPoolSize",rodataSource.getMaximumPoolSize());
        return poolInfo;
    }
    private HikariDataSource  buildDs( String url, String user, String pwd, int pooSize) {
    	HikariConfig config = new HikariConfig();
    	PGSimpleDataSource pg = new PGSimpleDataSource();
    	try {
    	    pg.setUrl(url);
    	    pg.setUser(user);
    	    pg.setPassword(pwd);
    	} catch (SQLException e) {
    	    throw new RuntimeException(e);
    	}
    	config.setDataSource(pg);
    	config.setMaximumPoolSize(pooSize);
        return new HikariDataSource(config);
    }
    
    private void setSearchPath(Runner runner,String searchPath){
        runner.executeUpdate("set search_path to \""+searchPath+"\"");
    }

    public Runner newDBOrgRunner(String orgName, boolean isRODB){
        Runner runner = rwdb.newRunner();
        if(orgs.containsKey(orgName)){
            setSearchPath(runner, orgs.get(orgName));
        }else{
            Runner sysRunner = newSysRunner(isRODB);
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

    public Runner newRODBOrgRunner(String orgName, boolean isRODB){
        Runner runner = rodb.newRunner();
        if(roorgs.containsKey(orgName)){
            setSearchPath(runner, roorgs.get(orgName));
        }else{
            Runner sysRunner = newSysRunner(isRODB);
            List<Map> list = sysRunner.executeQuery("select * from org where name =?", orgName);
            if (list.size() > 0) {
                String schema = (String) list.get(0).get("schemaname");
                setSearchPath(runner, schema);
                roorgs.put(orgName, schema);
            }else{
                logger.warn("There has no schema for organization named "+orgName);
            }
            sysRunner.close();
        }
        return runner;
    }

}
