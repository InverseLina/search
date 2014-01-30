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
import com.jobscience.search.dao.ConfigManager;

@Singleton
public class ConfigWebHandlers {

    @Inject
    private ConfigManager configManager;

    @WebPost("/config/save")
    public WebResponse saveConfig(@WebParam("configsJson") String configsJson,
                            @WebParam("orgId") Integer orgId) throws SQLException {
        Map paramConfigs = JsonUtil.toMapAndList(configsJson);
        configManager.saveOrUpdateConfig(paramConfigs,orgId);
        return WebResponse.success();
    }

    @WebGet("/config/get/{name}")
    public WebResponse getConfig(@WebPath(2) String name,@WebParam("orgId") Integer orgId) throws SQLException {
        return WebResponse.success(configManager.getConfig(name,orgId));
    }
    @WebGet("/config/getByName/{name}")
    public WebResponse getConfigByName(@WebPath(2) String name) throws Exception {
        return WebResponse.success(configManager.getConfig(name));
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
            return WebResponse.success();
        } else {
            rc.setCookie("login", false);
            return WebResponse.fail();
        }
    }

}
