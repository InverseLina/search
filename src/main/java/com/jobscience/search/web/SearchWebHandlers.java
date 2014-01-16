package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
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
                              @WebParam("searchColumns")String searchColumns,@WebUser Map token ){
        if(pageIndex == null ){
        	pageIndex = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        String orderCon = "";
        searchValues = searchValues.replaceFirst("#", "").replaceAll("\\\\\"", "#");
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map searchMap = new HashMap();
        
        // resolve the search parameters,cause all parameters begin with "q_"
        for(Object key:jo.keySet()){
        	searchMap.put(key.toString().substring(2),jo.get(key).toString().replaceAll("#", "\\\""));
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
        SearchResult searchResult = searchDao.search(searchColumns,searchMap, pageIndex, pageSize,orderCon,searchValues,(String)token.get("ctoken"));
        WebResponse wr = WebResponse.success(searchResult);
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
    @WebGet("/getAutoCompleteData")
    public WebResponse getAutoCompleteData(@WebParam("searchValues") String searchValues,@WebParam("type")String type,
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
    
    public static void main(String[] args) {
        String a = "{asdsd:\"\"asdasdsadsad\"\"}".replaceAll("\\\"\\\"", "\"#");
        System.out.println(a);
        JSONObject jo = JSONObject.fromObject(a);
        System.out.println(jo);
    }
}
