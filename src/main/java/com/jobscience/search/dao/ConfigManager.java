package com.jobscience.search.dao;

import static java.lang.String.format;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class ConfigManager {

    @Inject
    private DaoRwHelper daoRwHelper;
    
    private static final String[] snowPropertiesInheritedArray = new String[]{};
    private static final Set<String> snowPropertiesInherited = new HashSet<String>(Arrays.asList(snowPropertiesInheritedArray));
    private static final String[] orgInfoKeysArray = new String[]{"apex_resume_url","local_distance","local_date","instance_url","contact_resume_behave"};
    private static final Set<String> orgInfoKeys = new HashSet<String>(Arrays.asList(orgInfoKeysArray));
    
    private volatile LoadingCache<Integer, Map<String, Object>> configCache;
    
    public ConfigManager() {
        configCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build(new CacheLoader<Integer, Map<String, Object>>() {
            @Override
            public Map<String, Object> load(Integer orgId){
                return loadConfigMap(orgId);
            }
        });
    }
    
    /**
     * Get org configs,will always get the global configs
     * @param name
     * @param orgId the special org config need query
     * @return
     */
    public String getConfig(String name, Integer orgId){
        return (String) getConfigMap(orgId).get(name);
    }
    
    public Map<String, Object> getOrgInfo(Integer orgId){
        Map map = getConfigMap(orgId);
        Map orgInfo = new HashMap();
        for(Iterator i = orgInfoKeys.iterator(); i.hasNext();){
            String key = (String) i.next();
            orgInfo.put(key, map.get(key));
        }
        return orgInfo;
    }

    public Map<String, Object> getConfigMap(Integer orgId){
        if (orgId == null) {
            orgId = -1;
        }
        try {
            Map<String, Object> cacheConfigMap = configCache.get(orgId);
            return cacheConfigMap;
        } catch (ExecutionException e) {
        }
        return null;
    }
    
    public void updateCache(Integer orgId){
        if(orgId == null || orgId == -1){
            configCache.invalidateAll();
        }else{
            configCache.invalidate(orgId);
        }
    }
    
    /**
     * Save or Update org config
     * @param params 
     * @param orgId if <code>null</code>,use current org id,else use the orgId
     * @throws SQLException
     */
    public void saveOrUpdateConfig(Map<String, String> params,Integer orgId) throws SQLException {
        StringBuilder names = new StringBuilder("(");
        StringBuilder sql = new StringBuilder();
        for (String key : params.keySet()) {
            names.append("'" + key + "'");
            names.append(",");
            sql.append(format("(%s, '%s', '%s'),", orgId, key, params.get(key)));
        }
        names.append("'-1')");
        sql.deleteCharAt(sql.length() - 1);
        daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), format("delete from config where org_id = %s and  name in %s", orgId, names));
        daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), format("insert into  config(org_id, name,value) values %s ", sql));
        
        //clear cache
        updateCache(orgId);
    }
    
    public void updateConfigByOrgID(Integer orgId){
    	daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), format("update config set org_id = %s where org_id = '-1'", orgId));
    }
    
    public void deleteConfigByOrgID(Integer orgId){
    	daoRwHelper.executeUpdate(daoRwHelper.newSysRunner(), format("delete from config where org_id = %s", orgId));
    }
    
    protected Map<String, Object> loadConfigMap(Integer orgId){
        
        List params = new ArrayList();
        Integer id = orgId;
        String sql = "select * from config where 1=1 ";
        
        if(id == -1){
            sql += " and org_id= -1 ";
        }else{
            sql += " and ( org_id = ? or org_id = -1 )";
            params.add(orgId);
        }
        
        List valueList = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), sql, params.toArray());
        Map m = getConfigValuesFromValueList(valueList, -1);
        if(orgId != -1){
            Map m1 = getConfigValuesFromValueList(valueList, orgId);
            m.putAll(m1);
        }
        
        for(Iterator i = snowPropertiesInherited.iterator(); i.hasNext(); ){
            String key = (String) i.next();
            if(!m.containsKey(key)){
                m.put(key, getValueFromProperties(key));
            }
        }
        
        return m;
    }


    private Map getConfigValuesFromValueList(List<Map> valueList, Integer orgId) {
        Map result = new HashMap();
        for (Map valMap : valueList) {
            String value = null;
            Integer valOrgId = (Integer) valMap.get("org_id");
            if (orgId.equals(valOrgId)) {
                String key = (String) valMap.get("name");
                value = (String) valMap.get("value");
                if (value == null) {
                    value = (String) valMap.get("val_text");
                }
                result.put(key, value);
            }
        }
        return result;
    }
    
    private String getValueFromProperties(String key){
        return null;
    }

    public String getDateFormat(Integer id){
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), "select value from config where name=? AND org_id=?", "local_date", id);
        if(list.size() > 0){
            return String.valueOf(list.get(0).get("value"));
        }else{
            return null;
        }
    }
}
