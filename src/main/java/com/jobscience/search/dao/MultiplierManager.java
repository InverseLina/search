package com.jobscience.search.dao;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

@Singleton
public class MultiplierManager {

    @Inject
    private ConfigManager configManager;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private DBHelper dbHelper;
    @Inject
    private DBSetupManager dbSetupManager;
    @Inject
    private CurrentOrgHolder currentOrgHolder;
    
    public void multiplyData(int times,String orgName) throws SQLException{
       List<String> sqlCommands= dbSetupManager.getSqlCommandForOrg("04_multiply_data.sql");
       for(String sql:sqlCommands){
           if(!sql.trim().equals("")){
               dbHelper.executeUpdate(orgName, sql, new Object[0]);
           }
       }
       
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        Map org = new HashMap();
        if(orgs.size()>0){
            org = orgs.get(0);
        }
        Integer orgId = Integer.parseInt(org.get("id").toString());
        List<Map> configs = configManager.getConfig(null, orgId);
        Long current_iteration_number =1L;
        Long origin_count = null;
        for(Map m:configs){
            if("current_iteration_number".equals(m.get("name").toString())){
                current_iteration_number = Long.parseLong((String)m.get("value"));
            }
            if("origin_count".equals(m.get("name").toString())){
                origin_count = Long.parseLong((String)m.get("value"));
            }
        }
        Map newConfig = new HashMap();
        newConfig.put("current_iteration_number", current_iteration_number+times);
        if(origin_count==null){
            List<Map> counts = dbHelper.executeQuery(orgName, "select count(*) as count from contact;");
            if(counts.size()==1){
                origin_count = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("origin_count", origin_count);
            }
        }
       
        configManager.saveOrUpdateConfig(newConfig, orgId);
        while(times>0){
            Long perform = 0L;
            while(origin_count-perform>1000){
                dbHelper.executeQuery(orgName,"select multiplydata("+perform+",1000,"+current_iteration_number+")");
                perform+=1000;
            }
            if(origin_count-perform>0){
                dbHelper.executeQuery(orgName,"select multiplydata("+perform+","+(origin_count-perform)+","+current_iteration_number+")");
                perform = origin_count;
            }
            times--;
        }
       
    }
    
    
}



