package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class SearchWebHandlers {


    @Inject
    private SearchDao searchDao;
    
    @WebGet("/search")
    public WebResponse search(@WebParam("searchValues") String searchValues,
                              @WebParam("pageIndex") Integer pageIndex, @WebParam("pageSize") Integer pageSize,
                              @WebParam("orderBy")String orderBy,@WebParam("orderType")Boolean orderType,
                              @WebParam("searchColumns")String searchColumns ){
        
        
        if(pageIndex == null ){
        	pageIndex = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map searchMap = new HashMap();
        for(Object key:jo.keySet()){
        	searchMap.put(key.toString().substring(2),jo.get(key).toString());
        }
        
        if(orderBy!=null){
        	if(searchColumns.contains(orderBy)){
        		if(orderType==null){
        			orderType = true;
        		}
        		if(orderBy.equals("contact")){
        			orderBy = "name";
        		}
        		orderCon = " \""+getOrderColumn(orderBy)+ "\" " +(orderType?"asc":"desc");
        	}
        }
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns,searchMap, pageIndex, pageSize,orderCon);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
    
    @WebGet("/getAutoCompleteData")
    public WebResponse getTopCompanies(@WebParam("type") String type, @WebParam("offset") Integer offset,
                            @WebParam("limit") Integer limit,@WebParam("min")String min,@WebParam("keyword") String keyword) throws SQLException {
    	Map result = new HashMap();
        if(offset==null){
        	offset = 0;
        }
        if(limit==null){
        	limit=5;
        }
        
        long start = System.currentTimeMillis();
        if(type==null || "".equals(type) || "company".equals(type)){
	        List companies = searchDao.getTopAdvancedType(offset,limit,"company",keyword,min);
	        result.put("companies", companies);
        }
       
        if(type==null || "".equals(type) || "education".equals(type)){
	        List educations = searchDao.getTopAdvancedType(offset,limit,"education",keyword,min);
	        result.put("educations", educations);
        }
      
        if(type==null || "".equals(type) || "skill".equals(type)){
	        List skills = searchDao.getTopAdvancedType(offset,limit,"skill",keyword,min);
	        result.put("skills", skills);
        }
        
        if(type==null || "".equals(type) || "location".equals(type)){
	        List locations = searchDao.getTopAdvancedType(offset,limit,"location",keyword,min);
	        result.put("locations", locations);
        }
        
        long end = System.currentTimeMillis();
        result.put("duration", end - start);
        WebResponse wr = WebResponse.success(result);
        return wr;
    }
    
    @WebGet("/getGroupValuesForAdvanced")
    public WebResponse getGroupValuesForAdvanced(@WebParam("searchValues") String searchValues,@WebParam("type")String type,
    											 @WebParam("queryString")String queryString,@WebParam("orderByCount")Boolean orderByCount) throws SQLException{
        Map result = new HashMap();
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map searchMap = new HashMap();
        for(Object key:jo.keySet()){
        	searchMap.put(key.toString().substring(2),jo.get(key).toString());
        }
        if(orderByCount==null){
        	orderByCount = false;
        }
        List<Map> list = searchDao.getGroupValuesForAdvanced(searchMap,type,queryString,orderByCount);
        
        result.put("list", list);
        WebResponse wr = WebResponse.success(result);
        return wr;
    }

    
    private String getOrderColumn(String originalName){
		if("name".equalsIgnoreCase(originalName)||
		   "title".equalsIgnoreCase(originalName)||
		   "company".equalsIgnoreCase(originalName)||
		   "skill".equalsIgnoreCase(originalName)||
		   "education".equalsIgnoreCase(originalName)||
		   "email".equalsIgnoreCase(originalName)){
			return "l"+originalName;
		}else if("createddate".equalsIgnoreCase(originalName)){
			return "createddate";
		}else if( "location".equalsIgnoreCase(originalName)){
			return "location";
		}
		return originalName;
    }
}
