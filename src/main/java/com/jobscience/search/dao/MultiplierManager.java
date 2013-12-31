package com.jobscience.search.dao;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MultiplierManager {

    @Inject
    private ConfigManager configManager;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private DaoHelper daoHelper;
    @Inject
    private DBSetupManager dbSetupManager;

    private volatile int currentTime;
    private volatile Long performCounts;
    private volatile Long contactCounts;
    private volatile boolean stop = false;
    
    public synchronized Map multiplyData(Integer times,String orgName,String tableName) throws SQLException{
       stop = false;
       Long currentDbSize = 0L;
       Long start = System.currentTimeMillis();
       List<String> sqlCommands= dbSetupManager.getSqlCommandForOrg("04_multiply_data.sql");
       for(String sql:sqlCommands){
           if(!sql.trim().equals("")){
               daoHelper.executeUpdate(orgName, sql, new Object[0]);
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
            List<Map> counts = daoHelper.executeQuery(orgName, "select count(*) as count from "+tableName);
            if(counts.size()==1){
                origin_count = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put(tableName+"_origin_count", origin_count);
            }
        }
        if(companyCount==null){
            List<Map> counts = daoHelper.executeQuery(orgName, "select count(*) as count from ts2__employment_history__c");
            if(counts.size()==1){
                companyCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__employment_history__c_origin_count", companyCount);
            }
        }
        if(skillCount==null){
            List<Map> counts = daoHelper.executeQuery(orgName, "select count(*) as count from ts2__skill__c");
            if(counts.size()==1){
                skillCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__skill__c_origin_count", skillCount);
            }
        }
        if(educationCount==null){
            List<Map> counts = daoHelper.executeQuery(orgName, "select count(*) as count from ts2__education_history__c");
            if(counts.size()==1){
                educationCount = Long.parseLong( counts.get(0).get("count").toString());
                newConfig.put("ts2__education_history__c_origin_count", educationCount);
            }
        }
        
        contactCounts = origin_count;
        configManager.saveOrUpdateConfig(newConfig, orgId);
        currentTime = 0;
        
        while(times>0&&!stop){
            Long perform = 0L;
            performCounts = perform;
            currentTime++;
            
            while(origin_count-perform>10000&&!stop){
                daoHelper.executeQuery(orgName,"select multiplydata("+perform+",10000,"+current_iteration_number+",'"+tableName+"')");
                perform+=10000;
                performCounts = perform;
            }
           
            if(origin_count-perform>0&&!stop){
                daoHelper.executeQuery(orgName,"select multiplydata("+perform+","+(origin_count-perform)+","+current_iteration_number+",'"+tableName+"')");
                perform = origin_count;
                performCounts = perform;
                
            }
            if("contact".equals(tableName)){
                perform = 0L;
                while(companyCount-perform>10000&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__employment_history__c')");
                    perform+=10000;
                }
               
                if(companyCount-perform>0&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+","+(companyCount-perform)+","+current_iteration_number+",'ts2__employment_history__c')");
                    perform = origin_count;
                    
                }
                
                perform = 0L;
                while(skillCount-perform>10000&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__skill__c')");
                    perform+=10000;
                }
               
                if(origin_count-perform>0&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+","+(skillCount-perform)+","+current_iteration_number+",'ts2__skill__c')");
                    perform = skillCount;
                    
                }
                
                perform = 0L;
                while(educationCount-perform>10000&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+",10000,"+current_iteration_number+",'ts2__education_history__c')");
                    perform+=10000;
                }
               
                if(origin_count-perform>0&&!stop){
                    daoHelper.executeQuery(orgName,"select multiplydata("+perform+","+(educationCount-perform)+","+current_iteration_number+",'ts2__education_history__c')");
                }
            }
            times--;
            current_iteration_number++;
        }
        List<Map> dbSizes= daoHelper.executeQuery(orgName,"select pg_database_size(current_database())/1024/1024 as dbsize;");
        if(dbSizes.size()==1){
            currentDbSize = Long.valueOf(dbSizes.get(0).get("dbsize").toString());
        }
        return mapIt("currentDbSize",currentDbSize,"cost",(System.currentTimeMillis()-start)/1000);
    }
    
    public void stop(){
        this.stop = true;
    }
    public Map<String,Object> getStatus(){
        Map<String,Object> m = new HashMap<String, Object>();
        m.put("currentTime", currentTime);
        m.put("performCounts", performCounts);
        m.put("contactCounts",contactCounts );
        return m;
    }
}



