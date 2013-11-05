package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;
import com.jobscience.search.db.DBHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class PerfWebHandlers {


    @Inject
    private SearchDao searchDao;
    
    /**
     * api for main perfSearch
     * @param searchValues
     * @param pageIndex
     * @param pageSize
     * @param orderBy
     * @param orderType
     * @param searchColumns
     * @return
     */
    @WebGet("/perfSearch")
    public WebResponse perfSearch(@WebParam("searchValues") String searchValues,@WebParam("searchColumns")String searchColumns ){
        String orderCon = "";
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map searchMap = new HashMap();
        // resolve the search parameters,cause all parameters begin with "q_"
        for(Object key:jo.keySet()){
            searchMap.put(key.toString().substring(2),jo.get(key).toString());
        }
        
        //for contact,use id,name,title,email,CreatedDate instead
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns,searchMap, 0, 30,orderCon);
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
    @WebGet("/getPerfGroupValuesForAdvanced")
    public WebResponse getPerfGroupValuesForAdvanced(@WebParam("searchValues") String searchValues,@WebParam("type")String type,
                                                 @WebParam("queryString")String queryString,@WebParam("orderByCount")Boolean orderByCount,
                                                 @WebParam("min")String min, @WebParam("pageSize")Integer pageSize, 
                                                 @WebParam("pageNum")Integer pageNum) throws SQLException{
        Map result = new HashMap();
        long start = System.currentTimeMillis();
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
        List<Map> list = searchDao.getGroupValuesForAdvanced(searchMap,type,queryString,orderByCount,min,30,pageNum);
        result.put("list", list);
        long end = System.currentTimeMillis();
        result.put("duration", end - start);
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
