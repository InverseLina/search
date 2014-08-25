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
import com.google.common.base.Strings;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.SearchConfigurationDao;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class SearchConfigurationWebHandlers {
    public static final String CONFIG_PATH = "/WEB-INF/config/sys/searchconfig.val";

    private static String customFieldsWarnMsg  = "there has more than 10 customField configed and will only show first 10!";
    		
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
    @RequireAdmin
    public WebResponse getSearchConfig(RequestContext rc) throws IOException {
        boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
        if(!isSysSchemaExist){
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return webResponseBuilder.success(mapIt("content",Resources.toString(url, Charsets.UTF_8)));
        }
        List<Map> result = searchConfigurationDao.getSearchConfig();
        if (result.size() == 0) {
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return webResponseBuilder.success(mapIt("content",Resources.toString(url, Charsets.UTF_8)));

        }else{
            return webResponseBuilder.success(mapIt("content",result.get(0).get("val_text"),
                    "errorMsg",searchConfigurationManager.getErrorMsg((String)result.get(0).get("val_text"),false,null)));
        }

    }

    @WebGet("/resetSearchConfig")
    @RequireAdmin
    public WebResponse resetSearchConfig(RequestContext rc) throws IOException {
        searchConfigurationDao.resetSearchConfig();
        URL url = rc.getServletContext().getResource(CONFIG_PATH);
        return webResponseBuilder.success(Resources.toString(url, Charsets.UTF_8));
    }

    @WebPost("/saveSearchConfig")
    @RequireAdmin
    public WebResponse saveSearchConfig(@WebParam("content") String content, RequestContext rc) throws IOException {
        String errorMsg = searchConfigurationManager.getErrorMsg(content,false,null);
        if(!Strings.isNullOrEmpty(errorMsg)){
            return webResponseBuilder.success(mapIt("valid",false,"errorMsg",errorMsg));
        }
        searchConfigurationDao.saveSearchConfig(content);
        if(searchConfigurationManager.getcustomFieldsSize(null) != null && searchConfigurationManager.getcustomFieldsSize(null) > 10){
        	return webResponseBuilder.success(mapIt("valid",true,"warnMsg",customFieldsWarnMsg));
        }else{
            return webResponseBuilder.success(mapIt("valid", true));
        }
    }

    @WebGet("/getOrgSearchConfig")
    @RequireAdmin
    public WebResponse getOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {
        String content = searchConfigurationManager.getOrgConfig(orgName);
        String errorMsg = searchConfigurationManager.getErrorMsg(content,true,orgName);
        return webResponseBuilder.success(mapIt("content",content,"errorMsg",errorMsg));
    }

    @WebGet("/resetOrgSearchConfig")
    @RequireAdmin
    public WebResponse resetOrgSearchConfig(RequestContext rc, @WebParam("orgName") String orgName) throws Exception {

        searchConfigurationDao.resetOrgSearchConfig(orgName);
        return webResponseBuilder.success(searchConfigurationManager.getOrgConfig(orgName));
    }

    @WebPost("/saveOrgSearchConfig")
    @RequireAdmin
    public WebResponse saveOrgSearchConfig(@WebParam("orgName") String orgName,
                                           @WebParam("content") String content, RequestContext rc) throws Exception {
        String errorMsg = searchConfigurationManager.getErrorMsg(content,true,orgName);
        if(!Strings.isNullOrEmpty(errorMsg)){
            return webResponseBuilder.success(mapIt("valid",false,"errorMsg",errorMsg));
        }
        searchConfigurationDao.saveOrgSearchConfig(orgName, content);
        if(searchConfigurationManager.getcustomFieldsSize(orgName) != null && searchConfigurationManager.getcustomFieldsSize(orgName) > 10){
        	return webResponseBuilder.success(mapIt("valid",true,"config", searchConfigurationManager.getOrgConfig(orgName),"warnMsg",customFieldsWarnMsg));
        }else{
        	return webResponseBuilder.success(mapIt("valid", true, "config", searchConfigurationManager.getOrgConfig(orgName)));
        }
    }
    
}
