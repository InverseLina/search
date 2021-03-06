package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.britesnow.snow.util.JsonUtil;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebPath;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class ConfigWebHandlers {

    @Inject
    private OrgConfigDao orgConfigDao;
    
    @Inject
    private ConfigManager configManager;
    
    @Inject
    private OrgContextManager orgHolder;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebPost("/config/save")
    @RequireAdmin
    public WebResponse saveConfig(@WebParam("configsJson") String configsJson,
                            @WebParam("orgId") Integer orgId) throws SQLException {
    	Map result = new HashMap();
        List<Map> orgNames = orgConfigDao.getOrgNameById(orgId);
        if(orgId != -1 && (orgNames.size() != 1 || orgNames.get(0).get("name") == null)){
    		result.put("msg", "The CurrentOrg is lost!");
    		return webResponseBuilder.success(result);
    	}
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
    @RequireAdmin
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

    @WebGet("/config/getDateFormat")
    public WebResponse getDateFormat() throws Exception {
        return webResponseBuilder.success(configManager.getDateFormat(orgHolder.getId()));
    }
}
