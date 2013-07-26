package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.db.DBHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Singleton
public class SavedSearchesWebHandlers {


    @Inject
    private DBHelper dbHelper;

    public static final String getSql = "select * from savedsearches offset ? limit ?";
    public static final String deleteSql = "delete from savedsearches where id = ?";
    public static final String insertSql = "INSERT INTO savedsearches(create_date,name, search)  VALUES (?,?, ?);";
    public static final String updateSql = "UPDATE savedsearches SET   update_date=?, search=?  WHERE id = ?";


    @WebGet("/getSavedSearches")
    public WebResponse search(@WebParam("offset") Integer offset, @WebParam("limit") Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 6;
        }
        List<Map> map = dbHelper.executeQuery(getSql, offset, limit);
        return WebResponse.success(map);
    }

    @WebPost("/saveSavedSearches")
    public WebResponse save(@WebParam("name") String name, @WebParam("content") String content) {
        try {
            dbHelper.executeUpdate(insertSql, new Timestamp(System.currentTimeMillis()),name, content);
            return WebResponse.success();
        } catch (Exception e) {
            return WebResponse.fail(e.getMessage());
        }
    }

    @WebPost("/deleteSavedSearches")
    public WebResponse delete(@WebParam("id") Long id) {
         dbHelper.executeUpdate(deleteSql, id);
        return WebResponse.success();
    }

    @WebPost("/updateSavedSearches")
    public WebResponse update(@WebParam("id") Long id, @WebParam("content") String content) {
         dbHelper.executeUpdate(updateSql, new Timestamp(System.currentTimeMillis()), content, id);
        return WebResponse.success();
    }
}
