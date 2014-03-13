package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.SysDBSetupManager;

@Singleton
public class SysDBSetupWebHanlder {

    @Inject
    private SysDBSetupManager sysDbSetupManager;

    @Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebGet("/admin-sys-status")
    @RequireAdmin
    public WebResponse checkSysSchema(){
        Map status = sysDbSetupManager.getSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-start")
    @RequireAdmin
    public WebResponse start(@WebParam("force") Boolean force){
        force = force == null ? false : true;
        sysDbSetupManager.startSetup(force);
        Map status = sysDbSetupManager.getSetupStatus();
        return webResponseBuilder.success(status);
    }
    
    @WebPost("/admin-sys-pause")
    @RequireAdmin
    public WebResponse pause(){
        sysDbSetupManager.stop();
        Map status = sysDbSetupManager.getSetupStatus();
        return webResponseBuilder.success(status);
    }
    
}