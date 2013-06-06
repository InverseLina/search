package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchMode;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class SearchWebHandlers {


    @Inject
    private SearchDao searchDao;
    
    @WebGet("/search")
    public WebResponse search(@WebParam("q_") Map searchValues,@WebParam("searchMode") String searchModeStr,
                              @WebParam("pageIdx") Integer pageIdx, @WebParam("pageSize") Integer pageSize,
                              @WebParam("column")String column,@WebParam("order")String order){
        
        SearchMode searchMode = SearchMode.SIMPLE;
        if (searchModeStr != null) {
            searchMode = SearchMode.valueOf(searchModeStr.toUpperCase());
        }
        
        if(pageIdx == null ){
            pageIdx = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        if(column!=null){
        	orderCon = " order by  " +getOrderColumn(column)+ " " +(("desc".equals(order))?"desc":"asc");
        }
        // FIXME: needs to get the search map from request.
       // Map searchValues = MapUtil.mapIt("search",search);
        SearchResult searchResult = searchDao.search(searchValues,searchMode, pageIdx, pageSize,orderCon);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
    
    @WebGet("/getTopCompaniesAndEducations")
    public WebResponse getTopCompanies(@WebParam("type")String type,@WebParam("offset")Integer offset,@WebParam("limit")Integer limit) throws SQLException{
    	Map result = new HashMap();
        if(offset==null){
        	offset = 0;
        }
        if(limit==null){
        	limit=5;
        }
        if(type==null||"company".equals(type)){
	        List companies = searchDao.getTopMostCompanies(offset,limit);
	        result.put("companies", companies);
        }
        if(type==null||"education".equals(type)){
	        List educations = searchDao.getTopMostEducation(offset,limit);
	        result.put("educations", educations);
        }
        WebResponse wr = WebResponse.success(result);
        return wr;
    }
    
    private String getOrderColumn(String originalName){
    	if("Name".equalsIgnoreCase(originalName)||"Title".equalsIgnoreCase(originalName)){
    		return "l"+originalName;
    	}
    	
    	return originalName;
    }
}
