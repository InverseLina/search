package com.jobscience.search.web;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class SearchWebHandlers {


    @Inject
    private SearchDao searchDao;
    
    @WebGet("/search")
    public WebResponse search(@WebParam("q_") Map searchValues,@WebParam("searchMode") String searchMode){
        
    	if(searchMode==null){
    		searchMode = "simple";
    	}
        // FIXME: needs to get the search map from request.
       // Map searchValues = MapUtil.mapIt("search",search);
        SearchResult searchResult = searchDao.search(searchValues,searchMode);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
}
