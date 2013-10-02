package com.jobscience.search.dao;


import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Singleton
public class SavedSearchesDao {
    public static final String getSql = "select id, search, name, " +
            "   case when create_date > COALESCE(update_date, timestamp '1970-1-1 00:00:01') then create_date " +
            "   else update_date " +
            "   end as new_date from savedsearches order by new_date desc offset ? limit ? ";
    public static final String getByNameSql = "select 1 from savedsearches where name = ?";
    public static final String getByIDSql = "select * from savedsearches where id = ?";
    public static final String deleteSql = "delete from savedsearches where id = ?";
    public static final String insertSql = "INSERT INTO savedsearches(create_date,name, search)  VALUES (?,?, ?);";
    public static final String updateSql = "UPDATE savedsearches SET   update_date=?, search=?  WHERE name = ?";

    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentOrgHolder orgHolder;

    public List<Map> list(int offset, int limit) {
        return dbHelper.executeQuery(orgHolder.getOrgName(), getSql, offset, limit);
    }

    public void save(String name, String content){
        List<Map> list = dbHelper.executeQuery(orgHolder.getOrgName(), getByNameSql, name);
        if(list.size()==0){
            dbHelper.executeUpdate(orgHolder.getOrgName(), insertSql, new Timestamp(System.currentTimeMillis()), name, content);
        }else{
            dbHelper.executeUpdate(orgHolder.getOrgName(),updateSql, new Timestamp(System.currentTimeMillis()), content, name);
        }
    }

    public void delete(Long id){
        dbHelper.executeUpdate(orgHolder.getOrgName(),deleteSql, id);
    }

    public int count(String name) {
        return dbHelper.executeQuery(orgHolder.getOrgName(), getByNameSql, name).size();
    }

    public Map get(Long id) {
       List<Map> list = dbHelper.executeQuery(orgHolder.getOrgName(), getByIDSql, id);
        if (list.size() > 0) {
            return list.get(0);
        }else{
            return null;
        }
    }
}
