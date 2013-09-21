package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.List;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

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
    public WebResponse checkSetupStatus(@WebParam("types")String types) throws SQLException {
        List list = dbSetupManager.checkSetupStatus(types);
        return WebResponse.success(list);
    }
  
    @WebPost("/createExtraTables")
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
        dbSetupManager.createExtraTables(orgName);
        return WebResponse.success();
    }
    
}