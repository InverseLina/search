package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.List;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.IndexerManager;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private IndexerManager indexerManager;
    
    @WebPost("/createSysSchema")
    public WebResponse createSysSchema() throws SQLException {
        dbSetupManager.createSysSchema();
        return WebResponse.success();
    }
    
    @WebPost("/updateZipCode")
    public WebResponse updateZipCode() {
        dbSetupManager.updateZipCode();
        return WebResponse.success();
    }
    
    @WebGet("/checkSetupStatus")
    public WebResponse checkSetupStatus(@WebParam("types")String types,@WebParam("orgName")String orgName) throws SQLException {
        List list = dbSetupManager.checkSetupStatus(types,orgName);
        return WebResponse.success(list);
    }
  
    @WebPost("/createExtraTables")
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
        dbSetupManager.createExtraTables(orgName);
        return WebResponse.success();
    }
    
    @WebPost("/createIndexColumns")
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName) {
        dbSetupManager.createIndexColumns(orgName);
        return WebResponse.success();
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