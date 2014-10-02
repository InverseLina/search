package com.jobscience.search.dao;


import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.inject.Singleton;

@Singleton
public class SavedSearchesDao {
    public static final String getSql = "select id, search, name, " +
            "   case when create_date > COALESCE(update_date, timestamp '1970-1-1 00:00:01') then create_date " +
            "   else update_date " +
            "   end as new_date from jss_savedsearches order by new_date desc offset ? limit ? ";
    public static final String getByNameSql = "select 1 from jss_savedsearches where name = ?";
    public static final String getByIDSql = "select * from jss_savedsearches where id = ?";
    public static final String deleteSql = "delete from jss_savedsearches where id = ?";
    public static final String insertSql = "INSERT INTO jss_savedsearches(create_date,name, search)  VALUES (?,?, ?);";
    public static final String updateSql = "UPDATE jss_savedsearches SET   update_date=?, search=?  WHERE name = ?";

    @Inject
    private DaoRwHelper daoRwHelper;

    public List<Map> list(int offset, int limit, Map org) {
        return daoRwHelper.executeQuery((String)org.get("name"), getSql, offset, limit);
    }

    public void save(String name, String content, Map org){
        List<Map> list = daoRwHelper.executeQuery((String)org.get("name"), getByNameSql, name);
        if(list.size() == 0){
            daoRwHelper.executeUpdate((String)org.get("name"), insertSql, new Timestamp(System.currentTimeMillis()), name, content);
        }else{
            daoRwHelper.executeUpdate((String)org.get("name"), updateSql, new Timestamp(System.currentTimeMillis()), content, name);
        }
    }

    public void delete(Long id, Map org){
        daoRwHelper.executeUpdate((String)org.get("name"), deleteSql, id);
    }

    public int count(String name, Map org) {
        return daoRwHelper.executeQuery((String)org.get("name"), getByNameSql, name).size();
    }

    public Map get(Long id, Map org) {
       List<Map> list = daoRwHelper.executeQuery((String)org.get("name"), getByIDSql, id);
        if (list.size() > 0) {
            return list.get(0);
        }else{
            return null;
        }
    }
}
