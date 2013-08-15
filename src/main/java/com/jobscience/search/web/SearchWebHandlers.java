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
    public WebResponse search(@WebParam("q_") Map searchValues,
                              @WebParam("pageIdx") Integer pageIdx, @WebParam("pageSize") Integer pageSize,
                              @WebParam("column")String column,@WebParam("order")String order,
                              @WebParam("searchColumns")String searchColumns ){
        
        
        if(pageIdx == null ){
            pageIdx = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        if(column!=null){
        	if(searchColumns.contains(column)){
        		orderCon = "\""+getOrderColumn(column)+ "\" " +(("desc".equals(order))?"desc":"asc");
        	}
        }
        
        // FIXME: needs to get the search map from request.
        // Map searchValues = MapUtil.mapIt("search",search);
        SearchResult searchResult = searchDao.search(searchColumns,searchValues, pageIdx, pageSize,orderCon);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
    
    private String getOrderColumn(String originalName){
		if("Name".equalsIgnoreCase(originalName)||
		   "Title".equalsIgnoreCase(originalName)||
		   "Company".equalsIgnoreCase(originalName)||
		   "Skill".equalsIgnoreCase(originalName)||
		   "Education".equalsIgnoreCase(originalName)||
		   "email".equalsIgnoreCase(originalName)){
			return "l"+originalName.substring(0,1).toUpperCase()+originalName.substring(1);
		}else if("createddate".equalsIgnoreCase(originalName)){
			return "CreatedDate";
		}		
		return originalName;
    }
}
