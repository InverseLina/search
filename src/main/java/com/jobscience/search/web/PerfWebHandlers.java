package com.jobscience.search.web;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import net.sf.json.JSONObject;

import com.britesnow.snow.util.MapUtil;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.codahale.metrics.Snapshot;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.dao.DatasourceManager;
import com.jobscience.search.dao.SearchDao;
import com.jobscience.search.dao.SearchResult;
import com.jobscience.search.organization.OrgContextManager;
import com.jobscience.search.perf.PerfHook;

@Singleton
public class PerfWebHandlers {

    @Inject
    private SearchDao searchDao;

    @Inject
    private OrgContextManager orgHolder;

    @Inject
    private DaoHelper daoHelper;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @Inject
    private OrgContextManager orgContextManager;
    
    @Inject
    private PerfHook perfHook;
    
    @Inject
    private DatasourceManager datasourceManager;

    /**
     * api for main search
     * 
     * @param searchValues
     * @param searchColumns
     * @return
     */
    @WebGet("/perf/search")
    @RequireAdmin
    public WebResponse search(@WebParam("searchValues") String searchValues,
                            @WebParam("searchColumns") String searchColumns,@WebParam("org") String org) {
        String orderCon = "";
        JSONObject jo = JSONObject.fromObject(searchValues);
        Map<String, String> searchMap = new HashMap<String, String>();
        // resolve the search parameters,cause all parameters begin with "q_"
        for (Object key : jo.keySet()) {
            searchMap.put(key.toString().substring(2), jo.get(key).toString());
        }

        // for contact,use id,name,title,email,CreatedDate instead
        searchColumns = searchColumns.replaceAll("contact", "id,name,title,email,CreatedDate");
        SearchResult searchResult = searchDao.search(searchColumns, searchMap, 0, 30, orderCon, searchValues, "-1", orgContextManager.getOrgContext(org));
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
    @WebGet("/perf/autocomplete")
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
        SearchResult sResult = searchDao.getGroupValuesForAdvanced(searchMap, type, queryString, orderByCount, min, 30, pageNum,orgContextManager.getOrgContext(org));

        HashMap<String, Number> resultMap = new HashMap<String, Number>();
        resultMap.put("count", sResult.getCount());
        resultMap.put("duration", sResult.getDuration());
        return webResponseBuilder.success(resultMap);
    }

    @WebGet("/perf/checkStatus")
    @RequireAdmin
    public WebResponse checkStatus() throws SQLException {
        try {
            orgHolder.getOrgName();
            return webResponseBuilder.success(true);
        } catch (Exception e) {
            return webResponseBuilder.success(false);
        }
    }

    @WebGet("/perf/get-user-pref")
    public WebResponse getUserPref (@WebUser Map user) throws SQLException {
        try {
            List<Map> prefs = daoHelper.executeQuery(orgHolder.getOrgName(), "select * from pref where name = ? and user_id = ?", "filter_order", user.get("id"));
            if (prefs.size() == 1) {
                return webResponseBuilder.success(prefs.get(0));
            } else {
                return webResponseBuilder.success(false);
            }
        } catch (Exception e) {
            return webResponseBuilder.success(false);
        }
    }

    @WebPost("/perf/save-user-pref")
    @RequireAdmin
    public WebResponse saveUserPref (@WebUser Map user, @WebParam("value") String value, RequestContext rc) throws SQLException {
        if (user != null && value.trim().length() > 0) {
            try {
                List<Map> prefs = daoHelper.executeQuery(orgHolder.getOrgName(),
                        "select * from pref where name = ? and user_id = ?", "filter_order", user.get("id"));
                if (prefs.size() == 0) {
                    daoHelper.executeUpdate(orgHolder.getOrgName(),
                            "insert into pref (user_id, name, val_text) values(?,?,?)",user.get("id"),"filter_order", value);
                } else {
                    daoHelper.executeUpdate(orgHolder.getOrgName(),
                            "update pref set val_text = ? where id = ?",value, prefs.get(0).get("id") );
                }
                return webResponseBuilder.success(true);
            } catch (Exception e) {
                return webResponseBuilder.success(false);
            }
        }else{
            return webResponseBuilder.fail();
        }

    }

    @WebGet("/perf")
    @RequireAdmin
    public WebResponse getPerf(){
        List<Map> connectionSize = daoHelper.executeQuery(datasourceManager.newRunner(),
                " select count(*) as count from pg_stat_activity;");
        Snapshot snap = perfHook.getResponseDurations().getSnapshot();
        return webResponseBuilder.success(MapUtil.mapIt("max",snap.getMax(),
                                          "min",snap.getMin(),
                                          "mean",snap.getMean(),
                                          "connections",connectionSize.get(0).get("count")));
    }
}
