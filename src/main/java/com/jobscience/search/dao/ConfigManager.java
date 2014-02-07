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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


@Singleton
public class ConfigManager {

    @Inject
    private DaoHelper daoHelper;
    
    @Inject(optional=true)
    @Named("jss.feature.userlist")
    private String userlistFeature;
    
    private static final String[] snowPropertiesInheritedArray = new String[]{"jss.feature.userlist"};
    private static final Set<String> snowPropertiesInherited = new HashSet<String>(Arrays.asList(snowPropertiesInheritedArray));
    private static final String[] orgInfoKeysArray = new String[]{"jss.feature.userlist", "apex_resume_url"};
    private static final Set<String> orgInfoKeys = new HashSet<String>(Arrays.asList(orgInfoKeysArray));
    
    /**
     * Get org configs,will always get the global configs
     * @param name
     * @param orgId the special org config need query
     * @return
     */
    public String getConfig(String name, Integer orgId){
        List params = new ArrayList();
        Integer id = orgId;
        String sql = "select * from config where name = ?";
        params.add(name);
        
        if (id == null) {
            id = -1;
        }
        
        if(id == -1){
            sql += " and org_id= -1 ";
        }else{
            sql += " and ( org_id = ? or org_id = -1 )";
            params.add(orgId);
        }
        
        List valueList = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql, params.toArray());
        String value = getConfigValueFromValueList(valueList, orgId);
        
        // if null, we try to get it from the sys database (which we already have)
        if (value == null) {
            value = getConfigValueFromValueList(valueList, -1);
        }
        
        // if still null, and it is a snowPropertiesInherited, we get it from the properties.
        if (value == null && snowPropertiesInherited.contains(name)) {
            value = getValueFromProperties(name);
        }
        
        return value;
    }
    
    public Map<String, Object> getConfigMap(Integer orgId){
        
        List params = new ArrayList();
        Integer id = orgId;
        String sql = "select * from config where 1=1 ";
        
        if (id == null) {
            id = -1;
        }
        
        if(id == -1){
            sql += " and org_id= -1 ";
        }else{
            sql += " and ( org_id = ? or org_id = -1 )";
            params.add(orgId);
        }
        
        List valueList = daoHelper.executeQuery(daoHelper.openNewSysRunner(), sql, params.toArray());
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
    
    public Map<String, Object> getOrgInfo(Integer orgId){
        Map map = getConfigMap(orgId);
        Map orgInfo = new HashMap();
        for(Iterator i = orgInfoKeys.iterator(); i.hasNext();){
            String key = (String) i.next();
            orgInfo.put(key, map.get(key));
        }
        return orgInfo;
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
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(), format("delete from config where org_id = %s and  name in %s", orgId, names));
        daoHelper.executeUpdate(daoHelper.openNewSysRunner(), format("insert into  config(org_id, name,value) values %s ", sql));
    }

    private String getConfigValueFromValueList(List<Map> valueList, Integer orgId) {
        String value = null;
        for (Map valMap : valueList) {
            Integer valOrgId = (Integer) valMap.get("org_id");
            if (orgId.equals(valOrgId)) {
                value = (String) valMap.get("value");
                if (value == null) {
                    value = (String) valMap.get("val_text");
                }
                break;
            }
        }
        return value;
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
        if("jss.feature.userlist".equals(key)){
            return userlistFeature;
        }
        return null;
    }
}
