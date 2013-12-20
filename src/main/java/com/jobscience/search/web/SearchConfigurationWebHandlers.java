package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Singleton
public class SearchConfigurationWebHandlers {
    public static final String CONFIG_PATH = "/WEB-INF/config/sys/searchconfig.val";
    public static final String COL_NAME = "searchconfig";

    @javax.inject.Inject
    private DaoHelper daoHelper;

    @Inject
    private SearchConfigurationManager searchConfigurationManager;

    @WebGet("/searchuiconfig")
    public WebResponse searchuiconfig(@WebParam("org") String orgName){
        return WebResponse.success(searchConfigurationManager.getFilters(orgName));
    }

    @WebGet("/getSearchConfig")
    public WebResponse getSearchConfig(RequestContext rc) throws IOException {
        List<Map> result = daoHelper.executeQuery(daoHelper.openNewSysRunner(),
                "select val_text from config where name = ? and org_id is null", COL_NAME);
        if (result.size() == 0) {
            URL url = rc.getServletContext().getResource(CONFIG_PATH);
            return WebResponse.success(Resources.toString(url, Charsets.UTF_8));

        }else{
            return WebResponse.success(result.get(0).get("val_text"));
        }

    }

    @WebGet("/resetSearchConfig")
    public WebResponse resetSearchConfig(RequestContext rc) throws IOException {
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(),
                "delete  from config where name = ? and org_id is null", COL_NAME);
        URL url = rc.getServletContext().getResource(CONFIG_PATH);
        return WebResponse.success(Resources.toString(url, Charsets.UTF_8));
    }

    @WebPost("/saveSearchConfig")
    public WebResponse saveSearchConfig(@WebParam("content") String content, RequestContext rc) throws IOException {
        daoHelper.insert(daoHelper.openNewSysRunner(),
                "insert into config (name, val_text) values(?,?)", COL_NAME, content);
        return WebResponse.success();
    }
    
}
