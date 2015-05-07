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
import com.jobscience.search.dao.SearchRequest;
import com.jobscience.search.dao.SearchResult;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class SearchWebHandlers {
    @Inject
    private SearchDao searchDao;
    
    @Inject
    private OrgContextManager orgHolder;

	@Inject
	private WebResponseBuilder webResponseBuilder;
    
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
                              @WebParam("searchColumns")String searchColumns,@WebUser Map token,@WebParam("searchMode")String searchMode,
                              @WebParam("skillOperator")String skillOperator,
                              @WebParam("companyOperator")String companyOperator,
                              @WebParam("searchModeChange")String searchModeChange){
        if(pageIndex == null ){
        	pageIndex = 0;
        }
        if(pageSize == null ){
            pageSize = 30;
        }
        Map searchMap = new HashMap();
        searchMap.put("columns", searchColumns);
        searchMap.put("pageIndex", pageIndex);
        searchMap.put("orderBy", orderBy);
        searchMap.put("pageSize", pageSize);
        searchMap.put("searchValues", searchValues);
        searchMap.put("orderType", orderType);
        searchMap.put("searchMode", searchMode);
        searchMap.put("skillOperator", skillOperator);
        searchMap.put("companyOperator", companyOperator);

        SearchResult searchResult = searchDao.search(new SearchRequest(searchMap),(String)token.get("ctoken"),orgHolder.getCurrentOrg());
        return webResponseBuilder.success(searchResult);
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
        for(Object key : jo.keySet()){
        	searchMap.put(key.toString().substring(2),jo.get(key).toString());
        }
        if(orderByCount == null){
        	orderByCount = true;
        }
        if(pageNum == null || pageNum < 1){
        	pageNum = 1;
        }
        if(pageSize == null || pageSize < 1){
        	pageSize = 7;
        }
        SearchResult  sResult = searchDao.getGroupValuesForAdvanced(searchMap,type,queryString,orderByCount,min,pageSize,pageNum,orgHolder.getCurrentOrg());
        result.put("list", sResult.getResult());
        result.put("selectDuration", sResult.getSelectDuration());
        result.put("duration", sResult.getDuration());
        WebResponse wr = webResponseBuilder.success(result);
        return wr;
    }
}
