package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.Map;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.exception.JSSSqlException;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebPost("/admin-org-setup")
    @RequireAdmin
    public WebResponse orgSetup(@WebParam("org")String orgName){
        try{
            dbSetupManager.orgSetup(orgName);
        }catch(Exception e){
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/stop-org-setup")
    @RequireAdmin
    public WebResponse stopOrgSetup(@WebParam("org")String orgName){
        try{
            dbSetupManager.stopOrgSetup(orgName);
        }catch(Exception e){
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebGet("/admin-org-status")
    @RequireAdmin
    public WebResponse getOrgSetupStatus(@WebParam("org")String orgName){
        try{
            return webResponseBuilder.success(dbSetupManager.orgStatus(orgName));
        }catch(Exception e){
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
    }
    
    @WebPost("/reset-org-setup")
    @RequireAdmin
    public WebResponse resetOrgSetup(@WebParam("org")String orgName){
        try{
            dbSetupManager.resetOrgSetup(orgName);
        }catch(Exception e){
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebGet("/admin-sys-status")
    @RequireAdmin
    public WebResponse checkSysSchema(){
        Map status = dbSetupManager.getSystemSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-start")
    @RequireAdmin
    public WebResponse start(@WebParam("force") Boolean force){
        force = force == null ? false : true;
        dbSetupManager.systemSetup(force);
        Map status = dbSetupManager.getSystemSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-pause")
    @RequireAdmin
    public WebResponse pause(){
        dbSetupManager.stopSystemSetup();
        Map status = dbSetupManager.getSystemSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/removeAllIndexes")
    @RequireAdmin
    public WebResponse removeAllIndexes(@WebParam("orgName")String orgName){
        return webResponseBuilder.success(dbSetupManager.dropIndexes(orgName));
    }
    
    @WebPost("/dropExTables")
    @RequireAdmin
    public WebResponse dropExTables(@WebParam("orgName")String orgName){
        dbSetupManager.dropExTables(orgName);
        return webResponseBuilder.success();
    }
    
    @WebGet("/getWrongIndexes")
    @RequireAdmin
    public WebResponse getWrongIndexes(@WebParam("orgName")String orgName){
        return webResponseBuilder.success(dbSetupManager.getWrongIndex(orgName));
    }
    
    @WebPost("/removeWrongIndexes")
    @RequireAdmin
    public WebResponse removeWrongIndex(@WebParam("orgName")String orgName){
        try {
            return webResponseBuilder.success(dbSetupManager.removeWrongIndex(orgName));
        } catch (SQLException e) {
            return webResponseBuilder.success(new JSSSqlException(e));
        }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
    }
    
}