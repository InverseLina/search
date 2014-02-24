package com.jobscience.search.web;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.SearchConfigurationDao;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class SearchConfigurationWebHandlers {
    public static final String CONFIG_PATH = "/WEB-INF/config/sys/searchconfig.val";


    @Inject
    private SearchConfigurationManager searchConfigurationManager;

    @Inject
    private SearchConfigurationDao searchConfigurationDao;
    
    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebGet("/searchuiconfig")
    public WebResponse searchuiconfig(@WebParam("org") String orgName){
        return webResponseBuilder.success(searchConfigurationManager.getFilters(orgName));
    }

    @WebGet("/getSearchConfig")
    public WebResponse getSearchConfig(RequestContext rc) throws IOException {
        boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
        if(!isSysSchemaExist){
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return webResponseBuilder.success(Resources.toString(url, Charsets.UTF_8));
        }
        List<Map> result = searchConfigurationDao.getSearchConfig();
        if (result.size() == 0) {
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return webResponseBuilder.success(Resources.toString(url, Charsets.UTF_8));

        }else{
            return webResponseBuilder.success(result.get(0).get("val_text"));
        }

    }

    @WebGet("/resetSearchConfig")
    public WebResponse resetSearchConfig(RequestContext rc) throws IOException {
        searchConfigurationDao.resetSearchConfig();
        URL url = rc.getServletContext().getResource(CONFIG_PATH);
        return webResponseBuilder.success(Resources.toString(url, Charsets.UTF_8));
    }

    @WebPost("/saveSearchConfig")
    public WebResponse saveSearchConfig(@WebParam("content") String content, RequestContext rc) throws IOException {
        if(!searchConfigurationManager.isValid(content)){
            return webResponseBuilder.success(mapIt("valid",false));
        }
        searchConfigurationDao.saveSearchConfig(content);
        return webResponseBuilder.success(mapIt("valid", true));
    }

    @WebGet("/getOrgSearchConfig")
    public WebResponse getOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {
        return webResponseBuilder.success(searchConfigurationManager.getOrgConfig(orgName));

    }

    @WebGet("/resetOrgSearchConfig")
    public WebResponse resetOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {

        searchConfigurationDao.resetOrgSearchConfig(orgName);
        return webResponseBuilder.success(searchConfigurationManager.getOrgConfig(orgName));
    }

    @WebPost("/saveOrgSearchConfig")
    public WebResponse saveOrgSearchConfig(@WebParam("orgName") String orgName,
                                           @WebParam("content") String content, RequestContext rc) throws Exception {
        if(!searchConfigurationManager.isValid(content)){
            return webResponseBuilder.success(mapIt("valid",false));
        }
        searchConfigurationDao.saveOrgSearchConfig(orgName, content);

        return webResponseBuilder.success(mapIt("valid", true, "config", searchConfigurationManager.getOrgConfig(orgName)));
    }
    
}
