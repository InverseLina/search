package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.SearchConfigurationDao;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.britesnow.snow.util.MapUtil.mapIt;

@Singleton
public class SearchConfigurationWebHandlers {
    public static final String CONFIG_PATH = "/WEB-INF/config/sys/searchconfig.val";


    @Inject
    private SearchConfigurationManager searchConfigurationManager;

    @Inject
    private SearchConfigurationDao searchConfigurationDao;

    @WebGet("/searchuiconfig")
    public WebResponse searchuiconfig(@WebParam("org") String orgName){
        return WebResponse.success(searchConfigurationManager.getFilters(orgName));
    }

    @WebGet("/getSearchConfig")
    public WebResponse getSearchConfig(RequestContext rc) throws IOException {
        List<Map> result = searchConfigurationDao.getSearchConfig();
        if (result.size() == 0) {
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return WebResponse.success(Resources.toString(url, Charsets.UTF_8));

        }else{
            return WebResponse.success(result.get(0).get("val_text"));
        }

    }

    @WebGet("/resetSearchConfig")
    public WebResponse resetSearchConfig(RequestContext rc) throws IOException {
        searchConfigurationDao.resetSearchConfig();
        URL url = rc.getServletContext().getResource(CONFIG_PATH);
        return WebResponse.success(Resources.toString(url, Charsets.UTF_8));
    }

    @WebPost("/saveSearchConfig")
    public WebResponse saveSearchConfig(@WebParam("content") String content, RequestContext rc) throws IOException {
        if(!searchConfigurationManager.isValid(content)){
            return WebResponse.success(mapIt("valid",false));
        }
        searchConfigurationDao.saveSearchConfig(content);
        return WebResponse.success(mapIt("valid", true));
    }

    @WebGet("/getOrgSearchConfig")
    public WebResponse getOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {
        return WebResponse.success(searchConfigurationManager.getOrgConfig(orgName));

    }

    @WebGet("/resetOrgSearchConfig")
    public WebResponse resetOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {

        searchConfigurationDao.resetOrgSearchConfig(orgName);
        return WebResponse.success(searchConfigurationManager.getOrgConfig(orgName));
    }

    @WebPost("/saveOrgSearchConfig")
    public WebResponse saveOrgSearchConfig(@WebParam("orgName") String orgName,
                                           @WebParam("content") String content, RequestContext rc) throws Exception {
        if(!searchConfigurationManager.isValid(content)){
            return WebResponse.success(mapIt("valid",false));
        }
        searchConfigurationDao.saveOrgSearchConfig(orgName, content);

        return WebResponse.success(mapIt("valid", true, "config", searchConfigurationManager.getOrgConfig(orgName)));
    }
    
}
