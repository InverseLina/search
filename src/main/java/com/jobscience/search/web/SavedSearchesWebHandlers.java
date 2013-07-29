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

    public static final String getSql = "select id, search, name, " +
            "   case when create_date > COALESCE(update_date, timestamp '1970-1-1 00:00:01') then create_date " +
            "   else update_date " +
            "   end as new_date from savedsearches order by new_date desc offset ? limit ? ";
    public static final String getByNameSql = "select 1 from savedsearches where name = ?";
    public static final String deleteSql = "delete from savedsearches where id = ?";
    public static final String insertSql = "INSERT INTO savedsearches(create_date,name, search)  VALUES (?,?, ?);";
    public static final String updateSql = "UPDATE savedsearches SET   update_date=?, search=?  WHERE name = ?";


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
            List<Map> list = dbHelper.executeQuery(getByNameSql, name);
            if(list.size()==0){
                dbHelper.executeUpdate(insertSql, new Timestamp(System.currentTimeMillis()), name, content);
            }else{
                dbHelper.executeUpdate(updateSql, new Timestamp(System.currentTimeMillis()), content, name);
            }

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
}
