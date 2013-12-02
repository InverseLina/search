package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.josql.DBHelperBuilder;
import org.josql.Runner;

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
    
}
