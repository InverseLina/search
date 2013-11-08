package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.db.DBHelper;

@Singleton
public class SfidManager {

	/**
	 * if <code>true</code>,mean the resume index is creating
	 */
	 private volatile boolean on = false;
	 @Inject
	 private DBHelper dbHelper;
	 @Inject
	 private CurrentRequestContextHolder currentRequestContextHolder;
	 @Inject
     private OrgConfigDao orgConfigDao;
	 private IndexerStatus indexerStatus ;
	 public synchronized void run(String orgName) throws Exception{
		if(on){
			return ;
		}
	    this.on = true;
    	StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        path.append("/WEB-INF/sql");
	  	File orgFolder = new File(path + "/org");
	    File[] sqlFiles = orgFolder.listFiles();
	    String insertSql="" ,addColumnSql = "";
	    try{
		    for(File file : sqlFiles){
		    	StringBuilder temp = new StringBuilder();
	        	if(file.getName().startsWith("05_")){
	        		BufferedReader in = new BufferedReader(new FileReader(file));
	                String str;
	                while ((str = in.readLine()) != null) {
	                    temp.append(str);
	                }
	                in.close();
	                String sqls[]= temp.toString().split("-- SCRIPTS");
	                addColumnSql = sqls[1];
	                dbHelper.executeUpdate(orgName,addColumnSql);
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
	    if(indexerStatus==null){
	    	int all = getContactsCount(orgName);
	    	int perform = getContactExCount(orgName);
	    	indexerStatus = new IndexerStatus(all-perform, perform);
	    }
	    while(indexerStatus.getRemaining()>0&&on){
	    	dbHelper.executeUpdate(orgName,insertSql);
	    	int perform = getContactExCount(orgName);
	    	indexerStatus = new IndexerStatus(indexerStatus.getPerform()+indexerStatus.getRemaining()-perform, perform);
	    }
	    if(indexerStatus.getRemaining()==0){
	    	this.on = false;
	    }
	 }
	 
	 public void stop(){
		 this.on = false;
	 }
	 
	 public IndexerStatus getStatus(String orgName){
		int all = getContactsCount(orgName);
    	int perform = getContactExCount(orgName);
    	indexerStatus = new IndexerStatus(all-perform, perform);
		return indexerStatus;
	 }
	 private int getContactsCount(String orgName){
    	List<Map> list = dbHelper.executeQuery(orgName, "select count(*) as count from contact");
    	if(list.size()==1){
    		return Integer.parseInt(list.get(0).get("count").toString());
    	}
		return 0;
	 }
	    
	 private int getContactExCount(String orgName){
	    List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname="" ;
        if(orgs.size()==1){
            schemaname = orgs.get(0).get("schemaname").toString();
        }
	    if(!checkColumn("sfid", "contact_ex", schemaname)){
	        return 0;
	    }
    	List<Map> list = dbHelper.executeQuery(orgName, "select count(id) as count from contact_ex where sfid is not null");
    	if(list.size()==1){
    		return Integer.parseInt(list.get(0).get("count").toString());
    	}
		return 0;
	 }

	public boolean isOn() {
		return on;
	}
	
	private boolean checkColumn(String columnName,String table,String schemaName) {
        boolean result = false;
        
        Connection conn = dbHelper.openConnection();
        try{
            PreparedStatement st = conn.prepareStatement(" select 1 from information_schema.columns " +
                    " where table_name =? and table_schema=?  and column_name=? ");
            st.setString(1, table);
            st.setString(2, schemaName);
            st.setString(3, columnName);
            ResultSet s = st.executeQuery();
            if(s.next()){
                result =  true;
            }
            st.close();
            conn.close();
        }catch (SQLException e) {
            e.printStackTrace();
        }finally{
            try{
            conn.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
