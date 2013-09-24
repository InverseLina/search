package com.jobscience.search.dao;

import static java.lang.String.format;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class ConfigManager {

    @Inject
    private DBHelper          dbHelper;
    @Inject
    private CurrentOrgHolder  orgHolder;

    @Inject
    private DataSourceManager dsMng;
    @Inject
    @Named("salesforce.canvasapp.key")
    private String            canvasappKey;
    @Inject
    @Named("saleforce.apiKey")
    private String            apiKey;
    @Inject
    @Named("saleforce.apiSecret")
    private String            apiSecret;
    @Inject
    @Named("saleforce.callBackUrl")
    private String            callBackUrl;

    public void saveOrUpdateConfig(Map<String, String> params) throws SQLException {
        StringBuilder names = new StringBuilder("(");
        StringBuilder sql = new StringBuilder();
        Integer it = 0;
           try {
               it = orgHolder.getId();
           } catch (Exception e) {
               e.printStackTrace();
           }
        for (String key : params.keySet()) {
            names.append("'" + key + "'");
            names.append(",");
            // sql.append("('"+key+"','"+params.get(key)+"'),");
            if (it == 0) {
                      sql.append(format("(%s, '%s', '%s'),", null, key, params.get(key)));
                  } else {
                      sql.append(format("(%s, '%s', '%s'),", orgHolder.getId(), key, params.get(key)));
                  }
        }
        names.append("'-1')");
        sql.deleteCharAt(sql.length() - 1);
        if (it != 0) {
                 dbHelper.executeUpdate(dsMng.getSysDataSource(), format("delete from config where org_id = %s and  name in %s", orgHolder.getId(), names));
             } else {
                 dbHelper.executeUpdate(dsMng.getSysDataSource(), format("delete from config where org_id is null and  name in %s", names));
             }
        dbHelper.executeUpdate(dsMng.getSysDataSource(), format("insert into  config(org_id, name,value) values %s ", sql));

    }

    public List<Map> getConfig(String name) {
        String sql = "select * from config where 1=1 ";
        if (name != null) {
            sql += " and name='" + name + "'";
        }
        Integer it = null;
         try {
             it = orgHolder.getId();
         } catch (Exception e) {
             // TODO: handle exception
         }
         if (it == null) {
             sql += " and org_id is null ";
         } else {
             sql += " and org_id = " + it;
         }
         List<Map> configList = new ArrayList();
         configList = dbHelper.executeQuery(dsMng.getSysDataSource(), sql);
        List list = new ArrayList();
        if (configList != null && configList.size() > 0) {
            list = configList;
        }
        list = checkSaleforceInfo(list);
        return list;

    }

    public List checkSaleforceInfo(List<Map> list) {
        if (list.size() > 0) {
            boolean isCanvasappKey = false;
            boolean isApiKey = false;
            boolean isApiSecret = false;
            boolean isCallBackUrl = false;
            for (Map<String,String> map : list) {
                if ("config_canvasapp_key".equals(map.get("name")) && !"".equals(map.get("value"))) {
                    isCanvasappKey = true;
                } else if ("config_apiKey".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isApiKey = true;
                } else if ("config_apiSecret".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isApiSecret = true;
                } else if ("config_callBackUrl".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isCallBackUrl = true;
                }
            }
            Map map1 = new HashMap();
            if (!isCanvasappKey) {
                map1.put("name", "config_canvasapp_key");
                map1.put("value", canvasappKey);
                list.add(map1);
            }
            if (!isApiKey) {
                map1 = new HashMap();
                map1.put("name", "config_apiKey");
                map1.put("value", apiKey);
                list.add(map1);
            }
            if (!isApiSecret) {
                map1 = new HashMap();
                map1.put("name", "config_apiSecret");
                map1.put("value", apiSecret);
                list.add(map1);
            }
            if (!isCallBackUrl) {
                map1 = new HashMap();
                map1.put("name", "config_callBackUrl");
                map1.put("value", callBackUrl);
                list.add(map1);
            }
        } else {
            Map map = new HashMap();
            map.put("name", "config_canvasapp_key");
            map.put("value", canvasappKey);
            list.add(map);
            map = new HashMap();
            map.put("name", "config_apiKey");
            map.put("value", apiKey);
            list.add(map);
            map = new HashMap();
            map.put("name", "config_apiSecret");
            map.put("value", apiSecret);
            list.add(map);
            map = new HashMap();
            map.put("name", "config_callBackUrl");
            map.put("value", callBackUrl);
            list.add(map);
        }
        return list;
    }
}
