package com.jobscience.search.dao;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.jasql.PQuery;
import org.jasql.Runner;

public abstract class DaoHelper {
    
    @Inject
    public DatasourceManager datasourceManager;
    
    // ---------  query method --------- //
    public List<Map> executeQuery(String orgName,String query) {
        return executeQuery(datasourceManager.newOrgRunner(orgName), query);
    }
    
    public List<Map> executeQuery(String orgName,  String query, Object... vals) {
        return executeQuery(datasourceManager.newOrgRunner(orgName), query, vals);
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
        return executeUpdate(datasourceManager.newOrgRunner(orgName), query, vals);
    }
    
    public int executeUpdate(Runner runner, String sql, Object... vals) {
        try{
            return runner.executeUpdate(sql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /update method -------- //
    
    // --------- create method -------- //
    public Object create(String orgName, String sql, Object... vals){
       return create(datasourceManager.newOrgRunner(orgName), sql, vals);
    }
    
    public Object create(Runner runner, String sql, Object... vals){
        try{
            return runner.executeWithReturn(sql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /create method -------- //
    
    // --------- insert method -------- //
    public Object insert(Runner runner, String tableName, Map objMap){
        try{
            return runner.create(tableName, objMap);
        }finally{
            runner.close();
        }
    }
    // --------- /insert method -------- //
    
	// --------- count method -------- //
    public int executeCount(String orgName, String sql, Object... vals){
        return executeCount(datasourceManager.newOrgRunner(orgName), sql, vals);
	}
    
    public int executeCount(Runner runner, String countSql, Object... vals){
        try{
            return runner.executeCount(countSql, vals);
        }finally{
            runner.close();
        }
    }
    // --------- /count method -------- //
    
	// --------- other method -------- //
    public PQuery getNewPQuery(String orgName, String sql){
        return datasourceManager.newOrgRunner(orgName).newPQuery(sql);
	}
    // --------- /other method -------- //
}
