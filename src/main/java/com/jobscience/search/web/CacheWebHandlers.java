package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.OrgConfigDao;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class CacheWebHandlers {

    @Inject
    private ConfigManager configManager;
    @Inject
    private SearchConfigurationManager searchConfigurationManager;
    @Inject
    private OrgConfigDao orgConfigDao;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebGet("/cache-refresh")
    @RequireAdmin
    public WebResponse saveConfig(@WebParam("orgName") String orgName) throws SQLException {
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        Integer orgId = null;
        if(orgs.size()>0){
            orgId = Integer.parseInt(orgs.get(0).get("id").toString());
        }
        
        if(orgId != null){
            configManager.updateCache(orgId);
            searchConfigurationManager.updateCache(orgName);
        }
        
        return webResponseBuilder.success();
    }

    @WebGet("/cache-refresh-all")
    @RequireAdmin
    public WebResponse getConfig() throws SQLException {
        configManager.updateCache(null);
        searchConfigurationManager.updateCache(null);
        return webResponseBuilder.success();
    }

}
