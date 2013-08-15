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
    
    @WebGet("/getTopCompaniesAndEducations")
    public WebResponse getTopCompanies(@WebParam("type") String type, @WebParam("offset") Integer offset,
                            @WebParam("limit") Integer limit,@WebParam("match")String match) throws SQLException {
    	Map result = new HashMap();
        if(offset==null){
        	offset = 0;
        }
        if(limit==null){
        	limit=5;
        }
        
        long start = System.currentTimeMillis();
        if(type==null || "".equals(type) || "company".equals(type)){
	        List companies = searchDao.getTopAdvancedType(offset,limit,"company",match);
	        result.put("companies", companies);
        }
       
        if(type==null || "".equals(type) || "education".equals(type)){
	        List educations = searchDao.getTopAdvancedType(offset,limit,"education",match);
	        result.put("educations", educations);
        }
      
        if(type==null || "".equals(type) || "skill".equals(type)){
	        List skills = searchDao.getTopAdvancedType(offset,limit,"skill",match);
	        result.put("skills", skills);
        }
        
        if(type==null || "".equals(type) || "location".equals(type)){
	        List locations = searchDao.getTopAdvancedType(offset,limit,"location",match);
	        result.put("locations", locations);
        }
        
        long end = System.currentTimeMillis();
        result.put("duration", end - start);
        WebResponse wr = WebResponse.success(result);
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
