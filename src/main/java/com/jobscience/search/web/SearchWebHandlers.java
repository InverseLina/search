package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class SearchWebHandlers {
    @Inject
    private SearchDao searchDao;
    
    /**
     * api for main search
     * @param searchValues
     * @param pageIndex
     * @param pageSize
     * @param orderBy
     * @param orderType
     * @param searchColumns
     * @return
     */
    @WebGet("/search")
    public WebResponse search(@WebParam("searchValues") String searchValues,
                              @WebParam("pageIndex") Integer pageIndex, @WebParam("pageSize") Integer pageSize,
                              @WebParam("orderBy")String orderBy,@WebParam("orderType")Boolean orderType,
                              @WebParam("searchColumns")String searchColumns,@WebUser String token ){
        if(pageIndex == null ){
        	pageIndex = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map searchMap = new HashMap();
        // resolve the search parameters,cause all parameters begin with "q_"
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
        }else{
            orderCon = " \"id\" asc";
        }
        //for contact,use id,name,title,email,CreatedDate instead
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns,searchMap, pageIndex, pageSize,orderCon,searchValues,token);
        WebResponse wr = WebResponse.success(searchResult);
        return wr;
    }
    
    /**
     * use {@link #getGroupValuesForAdvanced(String, String, String, Boolean, String, Integer, Integer) instead
     * @param type
     * @param offset
     * @param limit
     * @param min
     * @param keyword
     * @return
     * @throws SQLException
     */
    @WebGet("/getAutoCompleteData")
    @Deprecated
    public WebResponse getAutoCompleteData(@WebParam("type") String type, @WebParam("offset") Integer offset,
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
    
    /**
     * Get auto complete data order by count
     * @param searchValues
     * @param type
     * @param queryString
     * @param orderByCount
     * @param min
     * @param pageSize
     * @param pageNum
     * @return
     * @throws SQLException
     */
    @WebGet("/getGroupValuesForAdvanced")
    public WebResponse getGroupValuesForAdvanced(@WebParam("searchValues") String searchValues,@WebParam("type")String type,
    											 @WebParam("queryString")String queryString,@WebParam("orderByCount")Boolean orderByCount,
    											 @WebParam("min")String min, @WebParam("pageSize")Integer pageSize, 
    											 @WebParam("pageNum")Integer pageNum) throws SQLException{
        Map result = new HashMap();
        JSONObject jo = JSONObject.fromObject(searchValues);
        if (queryString == null) {
            queryString = "";
        }
        result.put("queryString", queryString);
        Map searchMap = new HashMap();
        for(Object key:jo.keySet()){
        	searchMap.put(key.toString().substring(2),jo.get(key).toString());
        }
        if(orderByCount==null){
        	orderByCount = true;
        }
        if(pageNum==null||pageNum<1){
        	pageNum=1;
        }
        if(pageSize==null||pageSize<1){
        	pageSize=7;
        }
        SearchResult  sResult = searchDao.getGroupValuesForAdvanced(searchMap,type,queryString,orderByCount,min,pageSize,pageNum);
        result.put("list", sResult.getResult());
        result.put("selectDuration", sResult.getSelectDuration());
        result.put("duration", sResult.getDuration());
        WebResponse wr = WebResponse.success(result);
        return wr;
    }

    /**
     * get the order column name by original column
     * @param originalName
     * @return
     */
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
