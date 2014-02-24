package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.Map;

import com.britesnow.snow.util.JsonUtil;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebPath;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.ConfigManager;

@Singleton
public class ConfigWebHandlers {

    @Inject
    private ConfigManager configManager;
    @Inject
    private CurrentOrgHolder orgHolder;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebPost("/config/save")
    public WebResponse saveConfig(@WebParam("configsJson") String configsJson,
                            @WebParam("orgId") Integer orgId) throws SQLException {
        Map paramConfigs = JsonUtil.toMapAndList(configsJson);
        Integer id = -1;
        try {
            if(orgId != null){
                id = orgId;
            }else{
                id = orgHolder.getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        configManager.saveOrUpdateConfig(paramConfigs,id);
        return webResponseBuilder.success();
    }

    @WebGet("/config/get/{name}")
    public WebResponse getConfig(@WebPath(2) String name,@WebParam("orgId") Integer orgId) throws SQLException {
        try {
            if(name == null){
                return webResponseBuilder.success(configManager.getConfigMap(orgId));
            }
            return webResponseBuilder.success(configManager.getConfig(name,orgId));
        } catch (NullPointerException e) {
            //this exception just occur at first time when admin not finish config db
            //e.printStackTrace();
            return webResponseBuilder.fail();
        }
    }
    @WebGet("/config/getByName/{name}")
    public WebResponse getConfigByName(@WebPath(2) String name) throws Exception {
        return webResponseBuilder.success(configManager.getConfig(name,orgHolder.getId()));
    }

    @WebModelHandler(startsWith = "/admin")
    public void admin(@WebModel Map m, RequestContext rc) {
        m.put("login", rc.getCookie("login") == null ? false : Boolean.parseBoolean(rc.getCookie("login")));
    }

    @WebPost("/admin/validate")
    public WebResponse doValidate(@Named("jss.sysadmin.pwd") String configPassword, RequestContext rc,
                            @WebParam("password") String password) throws SQLException {
        if (configPassword.equals(password)) {
            rc.setCookie("login", true);
            rc.setCookie("passCode", true);
            return webResponseBuilder.success();
        } else {
            rc.setCookie("login", false);
            return webResponseBuilder.fail();
        }
    }

}
