package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.josql.DBHelper;
import org.josql.DBHelperBuilder;
import org.josql.Runner;

import com.britesnow.snow.web.hook.AppPhase;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.google.inject.Inject;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class DaoHelper {
    
    @Inject
    private DataSourceManager dsMng;
    
    private DBHelper defaultDBHelper;
    private DBHelper sysDBHelper;
    private Map<String,DBHelper> orgDBHelperByName = new ConcurrentHashMap<String, DBHelper>();
    
    
    // --------- DaoHelper Initialization --------- //
    @WebApplicationHook(phase = AppPhase.INIT)
    public void initDBHelpers(){
        defaultDBHelper = new DBHelperBuilder().newDBHelper(dsMng.getDefaultDataSource());
        sysDBHelper = new DBHelperBuilder().newDBHelper(dsMng.getSysDataSource());
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
                    orgDBHelper = new DBHelperBuilder().newDBHelper(dsMng.getOrgDataSource(orgName));
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
    
}
