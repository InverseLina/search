package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

@Singleton
public class SearchConfigurationWebHandlers {

    @Inject
    private SearchConfigurationManager searchConfigurationManager;

    @WebGet("/searchuiconfig")
    public WebResponse searchuiconfig(@WebParam("org") String orgName){
        return WebResponse.success(searchConfigurationManager.getFilters(orgName));
    }
    
}
