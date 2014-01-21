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


@Singleton
public class ConfigManager {

    @Inject
    private DaoHelper daoHelper;
    @Inject
    private CurrentOrgHolder  orgHolder;

    @Inject
    @Named("saleforce.apiKey")
    private String apiKey;
    @Inject
    @Named("saleforce.apiSecret")
    private String apiSecret;
    @Inject
    @Named("saleforce.callBackUrl")
    private String callBackUrl;
    
    @Inject(optional=true)
    @Named("jss.feature.userlist")
    private String userlistFeature;

    /**
     * Save or Update org config
     * @param params 
     * @param orgId if <code>null</code>,use current org id,else use the orgId
     * @throws SQLException
     */
    public void saveOrUpdateConfig(Map<String, String> params,Integer orgId) throws SQLException {
        StringBuilder names = new StringBuilder("(");
        StringBuilder sql = new StringBuilder();
        Integer id = 0;
        try {
           id = orgHolder.getId();
        } catch (Exception e) {
           e.printStackTrace();
        }
        if(orgId!=null){
        	id=orgId;
        }
        if(id==null){
        	id = -1;
        }
        for (String key : params.keySet()) {
            names.append("'" + key + "'");
            names.append(",");
            sql.append(format("(%s, '%s', '%s'),", id, key, params.get(key)));
        }
        names.append("'-1')");
        sql.deleteCharAt(sql.length() - 1);
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(), format("delete from config where org_id = %s and  name in %s", id, names));
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(), format("insert into  config(org_id, name,value) values %s ", sql));
    }

    /**
     * Get org configs,will always get the global configs
     * @param name
     * @param orgId the special org config need query
     * @return
     */
    public List<Map> getConfig(String name,Integer orgId) {
        String sql = "select * from config where 1=1 ";
        if (name != null) {
            sql += " and name='" + name + "'";
        }
        Integer it = -1;
        try {
            it = orgHolder.getId();
        } catch (Exception e) {
        	//e.printStackTrace();
        }
        if(orgId!=null){
          it = orgId;
        }
        sql += " and (org_id = " + it;
         
        if(it!=-1){
          sql += " or org_id = -1" ;
        }
        sql+=")";
        List<Map> configList = new ArrayList();
        try{
          configList = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql);
        }catch (Exception e) {
			
		}
        List list = new ArrayList();
        if (configList != null && configList.size() > 0) {
           list = configList;
        }
        list = checkSaleforceInfo(list);
        return list;
    }
    
    /**
     * only get current org config
     * @param name
     * @return
     */
    public Map getConfig(String name) {
        String sql = "select * from config where name = ? and org_id = ? ";
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql, name, orgHolder.getId());
        if (list.size() == 1) {
            return list.get(0);
        }else{
            return null;
        }
    }

    /**
     * get salesforce auth app configs,
     * if org app auth configs not existed,use global as default
     * @param list
     * @return
     */
    public List checkSaleforceInfo(List<Map> list) {
        if (list.size() > 0) {
            boolean isApiKey = false;
            boolean isApiSecret = false;
            boolean isCallBackUrl = false;
            boolean isUserlistFeature = false;
            for (Map map : list) {
                if ("config_apiKey".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isApiKey = true;
                } else if ("config_apiSecret".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isApiSecret = true;
                } else if ("config_callBackUrl".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                    isCallBackUrl = true;
                } else if ("config_userlistFeature".equals((String)map.get("name")) && !"".equals(map.get("value"))) {
                	isUserlistFeature = true;
                }
            }
            Map map1 = new HashMap();
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
            if (!isUserlistFeature) {
            	map1 = new HashMap();
            	map1.put("name", "config_userlistFeature");
            	map1.put("value", userlistFeature);
            	list.add(map1);
            }
        } else {
            Map map = new HashMap();
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
            map = new HashMap();
            map.put("name", "config_userlistFeature");
            map.put("value", userlistFeature);
            list.add(map);
        }
        return list;
    }
}
