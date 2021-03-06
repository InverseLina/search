package com.jobscience.search.web;

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
    
    @WebPost("/admin-org-recreate-cityscore")
    @RequireAdmin
    public WebResponse orgRecreateCityScore(@WebParam("org")String orgName){
        try{
            dbSetupManager.orgRecreateCityScore(orgName);
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
    public WebResponse start(){
        dbSetupManager.systemSetup();
        Map status = dbSetupManager.getSystemSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-recreate-cityworld")
    @RequireAdmin
    public WebResponse recreateCityWorld (){
        dbSetupManager.systemRecreateCityWorld();
        Map status = dbSetupManager.getSystemSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-reset")
    @RequireAdmin
    public WebResponse reset(){
        dbSetupManager.resetSysSetup();
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
    
    @WebPost("/rebuildResume")
    @RequireAdmin
    public WebResponse rebuildResume(@WebParam("orgName")String orgName){
        dbSetupManager.rebuildResume(orgName);
        return webResponseBuilder.success();
    }
    
}