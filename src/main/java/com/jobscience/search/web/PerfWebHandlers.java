package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;

@Singleton
public class PerfWebHandlers {

    @Inject
    private SearchDao searchDao;

    /**
     * api for main search
     * 
     * @param searchValues
     * @param searchColumns
     * @return
     */
    @WebGet("/perf/search")
    public WebResponse search(@WebParam("searchValues") String searchValues,
                            @WebParam("searchColumns") String searchColumns) {
        String orderCon = "";
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map<String, String> searchMap = new HashMap<String, String>();
        // resolve the search parameters,cause all parameters begin with "q_"
        for (Object key : jo.keySet()) {
            searchMap.put(key.toString().substring(2), jo.get(key).toString());
        }

        // for contact,use id,name,title,email,CreatedDate instead
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns, searchMap, 0, 30, orderCon);
        Map<String, Number> resultMap = new HashMap<String, Number>();
        resultMap.put("count", searchResult.getCount());
        resultMap.put("duration", searchResult.getSelectDuration());
        resultMap.put("countDuration", searchResult.getCountDuration());
        WebResponse wr = WebResponse.success(resultMap);
        return wr;
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
    @WebGet("/perf/autocomplete")
    public WebResponse autocomplete(@WebParam("searchValues") String searchValues, @WebParam("type") String type,
                            @WebParam("queryString") String queryString,
                            @WebParam("orderByCount") Boolean orderByCount, @WebParam("min") String min,
                            @WebParam("pageSize") Integer pageSize, @WebParam("pageNum") Integer pageNum)
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
        SearchResult sResult = searchDao.getGroupValuesForAdvanced(searchMap, type, queryString, orderByCount, min, 30, pageNum);

        HashMap<String, Number> resultMap = new HashMap<String, Number>();
        resultMap.put("count", sResult.getCount());
        resultMap.put("duration", sResult.getDuration());
        WebResponse wr = WebResponse.success(resultMap);
        return wr;
    }

}