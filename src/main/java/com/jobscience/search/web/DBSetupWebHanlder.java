package com.jobscience.search.web;

import java.sql.SQLException;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.IndexerManager;
import com.jobscience.search.dao.SchemaType;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private IndexerManager indexerManager;
    
    @WebPost("/createSysSchema")
    public WebResponse createSysSchema() throws SQLException {
        boolean result = dbSetupManager.createSysSchema();
        if(result){
        	return WebResponse.success();
        }else{
        	return WebResponse.fail();
        }
    }
    
    @WebPost("/updateZipCode")
    public WebResponse updateZipCode() {
    	boolean result = dbSetupManager.updateZipCode();
        if(result){
        	return WebResponse.success();
        }else{
        	return WebResponse.fail();
        }
    }
    
    @WebGet("/checkSetupStatus")
    public WebResponse checkSetupStatus(@WebParam("type")SchemaType type,@WebParam("orgName")String orgName) throws SQLException {
        Integer result= dbSetupManager.checkSetupStatus(type,orgName);
        return WebResponse.success(result);
    }
  
    @WebPost("/createExtraTables")
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
    	boolean result = dbSetupManager.createExtraTables(orgName);
        if(result){
        	return WebResponse.success();
        }else{
        	return WebResponse.fail();
        }
    }
    
    @WebPost("/createIndexColumns")
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName) {
    	boolean result = dbSetupManager.createIndexColumns(orgName);
        if(result){
        	return WebResponse.success();
        }else{
        	return WebResponse.fail();
        }
    }
    
    @WebPost("/createIndexResume")
    public WebResponse createIndexResume(@WebParam("orgName")String orgName) {
    	indexerManager.run(orgName);
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
    
    @WebGet("/getResumeIndexStatus")
    public WebResponse getStatus(@WebParam("orgName")String orgName) {
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
    
    @WebPost("/stopCreateIndexResume")
    public WebResponse createIndesxResume(@WebParam("orgName")String orgName) {
    	indexerManager.stop();
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
    
}