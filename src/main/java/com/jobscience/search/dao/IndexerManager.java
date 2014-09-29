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
public class IndexerManager {

	/**
	 * if <code>true</code>,mean the resume index is creating
	 */
	 private volatile boolean on = false;
	 @Inject
	 private DaoRwHelper daoRwHelper;

	 private IndexerStatus indexerStatus;

	 public synchronized void run(String orgName,String webPath) throws Exception{
		if(on){
			return ;
		}
	    this.on = true;
        webPath += "/WEB-INF/sql";
	  	File orgFolder = new File(webPath + "/org");
	    File[] sqlFiles = orgFolder.listFiles();
	    String insertSql="" ;
	    String updateSql = "";
	    try{
		    for(File file : sqlFiles){
		    	StringBuilder temp = new StringBuilder();
	        	if (file.getName().startsWith("03_")) {
	        		BufferedReader in = new BufferedReader(new FileReader(file));
	                String str;
	                while ((str = in.readLine()) != null) {
	                    temp.append(str);
	                }
	                in.close();
	                insertSql = temp.toString().split("-- SCRIPTS")[1].trim();
	                if(insertSql.endsWith(";")){
	                	insertSql = insertSql.substring(0,insertSql.length()-1);
	                }
	                updateSql = temp.toString().split("-- SCRIPTS")[2].trim();
	        	}
	        }
	    }catch (Exception e) {
	    	e.printStackTrace();
			throw e;
		}
	    executeResumeSql(orgName, insertSql, updateSql);
	}

	public void stop(){
		this.on = false;
		indexerStatus = null;
	}

	public boolean isOn() {
		return on;
	}	 

	public IndexerStatus getStatus(String orgName,boolean quick){
		if(quick){
			return getQuickStatus(orgName);
	    }

		int all = getContactsCount(orgName);
		int perform = getContactExCount(orgName);
		indexerStatus = new IndexerStatus(all-perform, perform);
		return indexerStatus;
	}

	public IndexerStatus getQuickStatus(String orgName){
		int all = 0;
		List<Map> list = daoRwHelper.executeQuery(orgName, "select max(id) as count from contact");
	    if(list.size() == 1){
	    	all =  Integer.parseInt(list.get(0).get("count").toString());
	    }
	    indexerStatus = new IndexerStatus(0,all);
	    return indexerStatus;
	}

	private void executeResumeSql(String orgName, String insertSql, String updateSql){
		indexerStatus = getStatus(orgName, false);
		Runner runner = daoRwHelper.datasourceManager.newOrgRunner(orgName);
	    PQuery insertPq = runner.newPQuery(insertSql + " limit ?");
	    PQuery updatePq = runner.newPQuery(updateSql);
	    try{
		    while(getJssContactCount(orgName) != getContactsCount(orgName) && on){//insert
		    	insertPq.executeUpdate(new Object[]{1000});
		        if(getContactExCount(orgName) < getContactsCount(orgName)){
		        	updatePq.executeUpdate();
		        }
		    }
		    indexerStatus = getStatus(orgName, false);
		    while(indexerStatus.getRemaining() > 0 && on){//update
		    	updatePq.executeUpdate();
		    	indexerStatus = getStatus(orgName, false);
		    }
	    }finally{
	    	on = false;
	    	insertPq.close();
	    	updatePq.close();
	    	runner.close();
	    }
	   
	    if(indexerStatus != null && indexerStatus.getRemaining() == 0){
            this.on = false;
        }
	   
	}

	private int getContactsCount(String orgName){
		List<Map> list = daoRwHelper.executeQuery(orgName, "select count(*) as count from contact");
		if(list.size() == 1){
			return Integer.parseInt(list.get(0).get("count").toString());
		}
		return 0;
	}

	private int getJssContactCount(String orgName){
		List<Map> list = daoRwHelper.executeQuery(orgName, "select count(id) as count from jss_contact ");
		if(list.size() == 1){
			return Integer.parseInt(list.get(0).get("count").toString());
		}
		return 0;
	}

	private int getContactExCount(String orgName){
		List<Map> list = daoRwHelper.executeQuery(orgName, "select count(id) as count from jss_contact where resume_tsv is not null");
		if(list.size() == 1){
			return Integer.parseInt(list.get(0).get("count").toString());
		}
		return 0;
	}
}
