package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.List;

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
    
    @WebGet("/checkSetupStatus")
    public WebResponse checkSetupStatus() throws SQLException {
        List list = dbSetupManager.checkSetupStatus();
        return WebResponse.success(list);
    }
  
}