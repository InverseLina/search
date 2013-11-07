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
        Map searchMap = new HashMap();
        // resolve the search parameters,cause all parameters begin with "q_"
        for (Object key : jo.keySet()) {
            searchMap.put(key.toString().substring(2), jo.get(key).toString());
        }

        // for contact,use id,name,title,email,CreatedDate instead
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns, searchMap, 0, 30, orderCon);
        Map resultMap = new HashMap();
        resultMap.put("count", searchResult.getCount());
        resultMap.put("duration", searchResult.getDuration());
        resultMap.put("countDuration", searchResult.getCountDuration());
        WebResponse wr = WebResponse.success(resultMap);
        return wr;
    }

    /**
     * Get auto complete data order by count
     * 
     * @param autocomplete
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
        Map result = new HashMap();
        long start = System.currentTimeMillis();
        JSONObject jo = JSONObject.fromObject(searchValues);
        if (queryString == null) {
            queryString = "";
        }
        result.put("queryString", queryString);
        Map searchMap = new HashMap();
        for (Object key : jo.keySet()) {
            searchMap.put(key.toString().substring(2), jo.get(key).toString());
        }
        if (orderByCount == null) {
            orderByCount = true;
        }
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        List<Map> list = searchDao.getGroupValuesForAdvanced(searchMap, type, queryString, orderByCount, min, 30, pageNum);
        long end = System.currentTimeMillis();

        Map resultMap = new HashMap();
        resultMap.put("count", list.size());
        resultMap.put("duration", end - start);
        WebResponse wr = WebResponse.success(resultMap);
        return wr;
    }

}
