package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchRequest;
import com.jobscience.search.dao.SearchResult;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class WarmupWebHandlers {

    @Inject
    private SearchDao searchDao;

    @Inject
    private OrgContextManager orgHolder;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    /**
     * api for main search
     * 
     * @param searchValues
     * @param searchColumns
     * @return
     */
    @WebGet("/warmup/search")
    @RequireAdmin
    public WebResponse search(@WebParam("searchValues") String searchValues,
                            @WebParam("searchColumns") String searchColumns,@WebParam("org") String org) {
        Map searchMap = new HashMap();
        searchMap.put("columns", searchColumns);
        searchMap.put("pageIndex", 0);
        searchMap.put("orderBy", "name");
        searchMap.put("pageSize", 30);
        searchMap.put("searchValues", searchValues);
        searchMap.put("orderType", "true");
        
        
        SearchResult searchResult = searchDao.search(new SearchRequest(searchMap),"-1",
            orgHolder.getOrgContext(org));
        Map<String, Number> resultMap = new HashMap<String, Number>();
        resultMap.put("count", searchResult.getCount());
        resultMap.put("duration", searchResult.getSelectDuration());
        resultMap.put("countDuration", searchResult.getCountDuration());
        return webResponseBuilder.success(resultMap);
    }

    /**
     * Get auto complete data order by count
     * 
     * @param type
     * @param queryString
     * @param orderByCount
     * @param min
     * @param pageSize
     * @param pageNum
     * @return
     * @throws SQLException
     */
    @WebGet("/warmup/autocomplete")
    @RequireAdmin
    public WebResponse autocomplete(@WebParam("searchValues") String searchValues, @WebParam("type") String type,
                            @WebParam("queryString") String queryString,
                            @WebParam("orderByCount") Boolean orderByCount, @WebParam("min") String min,
                            @WebParam("pageSize") Integer pageSize, @WebParam("pageNum") Integer pageNum,
                            @WebParam("org") String org)
                            throws SQLException {
        Map<String, String> result = new HashMap<String, String>();
        JSONObject jo = JSONObject.fromObject(searchValues);
        if (queryString == null) {
            queryString = "";
        }
        result.put("queryString", queryString);
        Map<String, String> searchMap = new HashMap<String, String>();
        for (Object key : jo.keySet()) {
            searchMap.put(key.toString().substring(2), jo.get(key).toString());
        }
        if (orderByCount == null) {
            orderByCount = true;
        }
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        SearchResult sResult = searchDao.getGroupValuesForAdvanced(searchMap, type, queryString, orderByCount, min, 30, pageNum, orgHolder.getOrgContext(org));

        HashMap<String, Number> resultMap = new HashMap<String, Number>();
        resultMap.put("count", sResult.getCount());
        resultMap.put("duration", sResult.getDuration());
        return webResponseBuilder.success(resultMap);
    }

    @WebGet("/warmup/checkStatus")
    @RequireAdmin
    public WebResponse checkStatus() throws SQLException {
        try {
            orgHolder.getOrgName();
            return webResponseBuilder.success(true);
        } catch (Exception e) {
            return webResponseBuilder.success(false);
        }
    }
}
