package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

import org.jasql.PQuery;
import org.jasql.Runner;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ResumeLowerManager {

	/**
	 * if <code>true</code>,mean the resume index is creating
	 */
	 private volatile boolean on = false;
	 @Inject
	 private DaoHelper daoHelper;
	 @Inject
     private OrgConfigDao orgConfigDao;
	 private IndexerStatus indexerStatus ;
	 @Inject
     private DatasourceManager datasourceManager;

	 public synchronized void run(String orgName,String webPath) throws Exception{
		if(on){
			return ;
		}
	    this.on = true;
        webPath+="/WEB-INF/sql";
	  	File orgFolder = new File(webPath + "/org");
	    File[] sqlFiles = orgFolder.listFiles();
	    String insertSql="" ,addColumnSql = "";
	    try{
		    for(File file : sqlFiles){
		    	StringBuilder temp = new StringBuilder();
	        	if(file.getName().startsWith("14_")){
	        		BufferedReader in = new BufferedReader(new FileReader(file));
	                String str;
	                while ((str = in.readLine()) != null) {
	                    temp.append(str);
	                }
	                in.close();
	                String sqls[]= temp.toString().split("-- SCRIPTS");
	                addColumnSql = sqls[1];
	                daoHelper.executeUpdate(orgName,addColumnSql);
	                insertSql = sqls[2];
	                if(insertSql.endsWith(";")){
	                	insertSql=insertSql.substring(0,insertSql.length()-1);
	                }
	        	}
	        }
	    }catch (Exception e) {
	    	e.printStackTrace();
			throw e;
		}
	    indexerStatus = getStatus(orgName, false);
	    
	    Runner runner = datasourceManager.newOrgRunner(orgName);
        PQuery pq = runner.newPQuery(insertSql);
        try{
    	    while(indexerStatus.getRemaining() > 0 && on){
    	        pq.executeUpdate(new Object[0]);
    	    	int perform = getJssContactResumeCount(orgName);
    	    	indexerStatus = new IndexerStatus(indexerStatus.getPerform()+indexerStatus.getRemaining()-perform, perform);
    	    }
        }catch(Exception e){
            on = false;
        }
	    if(indexerStatus != null && indexerStatus.getRemaining() == 0){
	    	this.on = false;
	    }
	    pq.close();
        runner.close();
	 }
	 
	 public void stop(){
		 this.on = false;
		 indexerStatus = null;
	 }
	 
	 public IndexerStatus getStatus(String orgName,boolean quick){
        if(quick){
            return getQuickStatus(orgName);
        }
		int all = getContactResumeCount(orgName);
    	int perform = getJssContactResumeCount(orgName);
    	indexerStatus = new IndexerStatus(all-perform, perform);
		return indexerStatus;
	 }
	 
	 public IndexerStatus getQuickStatus(String orgName){
	     int all = 0;
	     List<Map> list = daoHelper.executeQuery(orgName, "select max(id) as count from contact");
	        if(list.size()==1){
	            all =  Integer.parseInt(list.get(0).get("count").toString());
	        }
	     indexerStatus = new IndexerStatus(0,all);
	     return indexerStatus;
	 }
	 
	 public boolean isOn() {
    	return on;
    }

    private int getContactResumeCount(String orgName){
    	List<Map> list = daoHelper.executeQuery(orgName, "select count(*) as count from contact");
    	if(list.size()==1){
    		return Integer.parseInt(list.get(0).get("count").toString());
    	}
		return 0;
	 }
	    
	 private int getJssContactResumeCount(String orgName){
	    List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname="" ;
        if(orgs.size()==1){
            schemaname = orgs.get(0).get("schemaname").toString();
        }
	    if(!checkColumn("resume_lower", "jss_contact", schemaname)){
	        return 0;
	    }
    	List<Map> list = daoHelper.executeQuery(orgName, "select count(id) as count from jss_contact where resume_lower is not null");
    	if(list.size()==1){
    		return Integer.parseInt(list.get(0).get("count").toString());
    	}
		return 0;
	 }

	private boolean checkColumn(String columnName,String table,String schemaName) {
        boolean result = false;
        
        List list = daoHelper.executeQuery(datasourceManager.newRunner(), " select 1 from information_schema.columns " +
                                " where table_name =? and table_schema=?  and column_name=? ", table, schemaName, columnName);
        if(list.size() > 0){
            result = true;
        }
        return result;
    }
}
