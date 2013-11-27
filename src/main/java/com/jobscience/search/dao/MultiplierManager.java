package com.jobscience.search.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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

    private volatile int currentTime;
    private volatile Long performCounts;
    private volatile Long contactCounts;
    
    public synchronized void multiplyData(Integer times,String orgName,String tableName) throws SQLException{
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
        Long origin_count = null,companyCount = null,skillCount=null,educationCount=null;
        for(Map m:configs){
            if("current_iteration_number".equals(m.get("name").toString())){
                current_iteration_number = Long.parseLong((String)m.get("value"));
            }
            if((tableName+"_origin_count").equals(m.get("name").toString())){
                origin_count = Long.parseLong((String)m.get("value"));
            }
            if(("ts2__employment_history__c_origin_count").equals(m.get("name").toString())){
                companyCount = Long.parseLong((String)m.get("value"));
            }
            if(("ts2__skill__c_origin_count").equals(m.get("name").toString())){
                skillCount = Long.parseLong((String)m.get("value"));
            }
            if(("ts2__education_history__c_origin_count").equals(m.get("name").toString())){
                educationCount = Long.parseLong((String)m.get("value"));
            }
        }
        Map newConfig = new HashMap();
        newConfig.put("current_iteration_number", current_iteration_number+times);
        if(origin_count==null){
            List<Map> counts = dbHelper.executeQuery(orgName, "select count(*) as count from "+tableName);
            if(counts.size()==1){
                origin_count = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put(tableName+"_origin_count", origin_count);
            }
        }
        if(companyCount==null){
            List<Map> counts = dbHelper.executeQuery(orgName, "select count(*) as count from ts2__employment_history__c");
            if(counts.size()==1){
                companyCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__employment_history__c_origin_count", companyCount);
            }
        }
        if(skillCount==null){
            List<Map> counts = dbHelper.executeQuery(orgName, "select count(*) as count from ts2__skill__c");
            if(counts.size()==1){
                skillCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__skill__c_origin_count", skillCount);
            }
        }
        if(educationCount==null){
            List<Map> counts = dbHelper.executeQuery(orgName, "select count(*) as count from ts2__education_history__c");
            if(counts.size()==1){
                educationCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__education_history__c_origin_count", educationCount);
            }
        }
        
        
        
        
        contactCounts = origin_count;
        configManager.saveOrUpdateConfig(newConfig, orgId);
        currentTime = 0;
        PreparedStatement statement;
        Connection con = dbHelper.openConnection(orgName);
        while(times>0){
            Long perform = 0L;
            performCounts = perform;
            currentTime++;
            while(origin_count-perform>10000){
                statement = con.prepareStatement("select multiplydata("+perform+",10000,"+current_iteration_number+",'"+tableName+"')");
                statement.executeQuery();
                statement.close();
                perform+=10000;
                performCounts = perform;
            }
           
            if(origin_count-perform>0){
                statement = con.prepareStatement("select multiplydata("+perform+","+(origin_count-perform)+","+current_iteration_number+",'"+tableName+"')");
                statement.executeQuery();
                statement.close();
                perform = origin_count;
                performCounts = perform;
                
            }
            if("contact".equals(tableName)){
                perform = 0L;
                while(companyCount-perform>10000){
                    statement = con.prepareStatement("select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__employment_history__c')");
                    statement.executeQuery();
                    statement.close();
                    perform+=10000;
                }
               
                if(companyCount-perform>0){
                    statement = con.prepareStatement("select multiplydata("+perform+","+(companyCount-perform)+","+current_iteration_number+",'ts2__employment_history__c')");
                    statement.executeQuery();
                    statement.close();
                    perform = origin_count;
                    
                }
                
                perform = 0L;
                while(skillCount-perform>10000){
                    statement = con.prepareStatement("select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__skill__c')");
                    statement.executeQuery();
                    statement.close();
                    perform+=10000;
                }
               
                if(origin_count-perform>0){
                    statement = con.prepareStatement("select multiplydata("+perform+","+(skillCount-perform)+","+current_iteration_number+",'ts2__skill__c')");
                    statement.executeQuery();
                    statement.close();
                    perform = skillCount;
                    
                }
                
                perform = 0L;
                while(educationCount-perform>10000){
                    statement = con.prepareStatement("select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__education_history__c')");
                    statement.executeQuery();
                    statement.close();
                    perform+=10000;
                }
               
                if(origin_count-perform>0){
                    statement = con.prepareStatement("select multiplydata("+perform+","+(educationCount-perform)+","+current_iteration_number+",'ts2__education_history__c')");
                    statement.executeQuery();
                    statement.close();
                }
            }
            times--;
            current_iteration_number++;
        }
       
        con.close();
    }
    
    public Map<String,Object> getStatus(){
        Map<String,Object> m = new HashMap<String, Object>();
        m.put("currentTime", currentTime);
        m.put("performCounts", performCounts);
        m.put("contactCounts",contactCounts );
        return m;
    }
}



