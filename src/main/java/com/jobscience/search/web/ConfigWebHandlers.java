package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
import com.jobscience.search.oauth.ForceAuthService;

@Singleton
public class ConfigWebHandlers {

    @Inject
    private ConfigManager configManager;

    @Inject
    private ForceAuthService forceAuthService;

    @WebPost("/config/save")
    public WebResponse saveConfig(@WebParam("local_distance") String distance, @WebParam("local_date") String date,
                            @WebParam("action_add_to_sourcing") String addToSourcing,
                            @WebParam("action_favorite") String favorite,
                            @WebParam("config_canvasapp_secret") String canvasappSecret,
                            @WebParam("config_apiKey") String apiKey, @WebParam("config_apiSecret") String apiSecret,
                            @WebParam("config_callBackUrl") String callBackUrl,@WebParam("needAdmin") String needAdmin,@WebParam("instance_url") String instanceUrl,
                            @WebParam("orgId") Integer orgId) throws SQLException {
        Map<String, String> params = new HashMap<String, String>();
        if(!"false".equals(needAdmin)){
            params.put("local_distance", distance);
            params.put("local_date", date);
            params.put("action_add_to_sourcing", addToSourcing);
            params.put("action_favorite", favorite);
            params.put("instance_url", instanceUrl);
        }else{
	        params.put("config_canvasapp_secret", canvasappSecret);
	        params.put("config_apiKey", apiKey);
	        params.put("config_apiSecret", apiSecret);
	        params.put("config_callBackUrl", callBackUrl);
        }
        configManager.saveOrUpdateConfig(params,orgId);
        forceAuthService.reloadService();
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
            return WebResponse.success();
        } else {
            rc.setCookie("login", false);
            return WebResponse.fail();
        }
    }

}
