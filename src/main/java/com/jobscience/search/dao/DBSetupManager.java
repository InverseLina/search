package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.db.DBHelper;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class DBSetupManager {

    @Inject
    private DBHelper dbHelper;
    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Named("zipcode.path")
    @Inject
    private String zipcodePath;
    @Named("org.path")
    @Inject
    private String orgPath;
    @Inject
    private DataSourceManager dsMng;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private IndexerManager indexerManager;
    private Cache<String, Object> cache= CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
    .maximumSize(100).build(new CacheLoader<String,Object >() {
		@Override
		public Object load(String key) throws Exception {
			return key;
	}});
    
    public Integer checkSetupStatus(SchemaType type,String orgName) throws SQLException, IOException{
    	Integer status =0;
    	if(checkSysTables()){
    		status=SetupStatus.SYS_CREATE_SCHEMA.getValue();
        }
    	if(status==SetupStatus.SYS_CREATE_SCHEMA.getValue()){
    		if(checkZipcodeImported()){
	        	status = SetupStatus.SYS_IMPORT_ZIPCODE_DATA.getValue();
	        }
    	}
    	if(type.equals(SchemaType.ORG)){
	    	if(status==SetupStatus.SYS_IMPORT_ZIPCODE_DATA.getValue()){
	    		if(checkOrgExtra(orgName)){
		        	status = SetupStatus.ORG_CREATE_EXTRA.getValue();
		        }
	    	}
	    	
	    	if(status==SetupStatus.ORG_CREATE_EXTRA.getValue()){
	    		boolean orgIndex = false;
	    		if(checkOrgIndex()){
		        	status = SetupStatus.ORG_CREATE_INDEX_COLUMNS.getValue();
		        	orgIndex = true;
		        }
	    		if(indexerManager.isOn()){
	    	        status=SetupStatus.ORG_CREATE_INDEX_RESUME_RUNNING.getValue()*(orgIndex?SetupStatus.ORG_CREATE_INDEX_COLUMNS.getValue():1);
	        	}
		        if(indexerManager.getStatus(orgName).getRemaining()==0){
		        	status=SetupStatus.ORG_CREATE_INDEX_RESUME.getValue()*(orgIndex?SetupStatus.ORG_CREATE_INDEX_COLUMNS.getValue():1);
		        }
	    	}
    	}
        return status;
    }
    
    public boolean createSysSchema() throws SQLException{
    	boolean result = true;
    	dsMng.createSysSchemaIfNecessary();
        File sysFolder = new File(getRootSqlFolderPath() + "/jss_sys");
        File[] sqlFiles = sysFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
            List subSqlList = loadSQLFile(file);
            allSqls.addAll(subSqlList);
        }
        Connection conn = dbHelper.getSysConnection();
        try {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            for(String sql : allSqls){
                st.addBatch(sql);
            }
            st.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
                result = false;
            }
           throw e;
        }
        return result;
    }
    
    public boolean updateZipCode() throws SQLException, IOException{
    	return doUpdateZipCode(true)!=0;
    }
    
   
    public void importOrgData(){
    	try{
			URL url = new URL(orgPath);
			HttpURLConnection con =  (HttpURLConnection) url.openConnection();
			ZipInputStream in = new ZipInputStream(con.getInputStream());
			in.getNextEntry();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			StringBuilder sqlContent = new StringBuilder();
			List<String> sqlList = new ArrayList<String>();
			while(line!=null){
				sqlContent.append(line);
				line = br.readLine();
			}
			 String sqls[] = sqlContent.toString().split(";");
            for (String sql : sqls) {
                sqlList.add(sql);
            }
            Connection conn = dbHelper.getConnection();
            try {
                conn.setAutoCommit(false);
                Statement st = conn.createStatement();
                for(String sql : sqlList){
                    st.addBatch(sql);
                }
                st.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                }
                e.printStackTrace();
            }
			in.close();
    	}catch (IOException e) {
			e.printStackTrace();
		}
    
    }
    
    public  boolean checkSysTables(){
    	List<Map> list = dbHelper.executeQuery(dsMng.getDefaultDataSource(), "select count(*) as count from information_schema.tables" +
        		" where table_schema='jss_sys' and table_type='BASE TABLE' and table_name in ('zipcode_us','org','config')");
    	if(list.size()==1){
    		if("3".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    private  boolean checkOrgExtra(String orgName){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name in ('contact_ex','savedsearches','user')");
    	if(list.size()==1){
    		if("3".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean createExtraTables(String orgName) throws SQLException{
    	boolean result = true;
        File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
        	if(file.getName().startsWith("01_")){//only load the 01_create_extra.sql
	            List<String> subSqlList = loadSQLFile(file);
	            allSqls.addAll(subSqlList);
        	}
        }
        Connection conn = dbHelper.getConnection(orgName);
        try {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            for(String sql : allSqls){
        		st.addBatch(sql.replaceAll("#", ";"));
            }
            st.executeBatch();
            conn.commit();
        } catch (SQLException e) {
        	e.printStackTrace();
        	result = false;
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw e;
        }
       return result;
    }
    
    public boolean createIndexColumns(String orgName) throws SQLException{
    	boolean result = true;
        File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
        	if(file.getName().startsWith("02_")){//only load the 01_create_extra.sql
	            List<String> subSqlList = loadSQLFile(file);
	            allSqls.addAll(subSqlList);
        	}
        }
        Connection conn = dbHelper.getConnection(orgName);
        try {
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            for(String sql : allSqls){
        		st.addBatch(sql);
            }
            st.executeBatch();
            conn.commit();
        } catch (SQLException e) {
        	result = false;
        	e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw e;
        }
        return result;
    }
    
    private String getRootSqlFolderPath(){
        StringBuilder path = new StringBuilder(currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/"));
        path.append("/WEB-INF/sql");
        return path.toString();
    }
    
    private List<String> loadSQLFile(File file){
        List<String> sqlList = new ArrayList<String>();
        StringBuffer temp = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                temp.append(str);
            }
            in.close();
            String sqls[] = temp.toString().split(";");
            for (String sql : sqls) {
                sqlList.add(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlList;
    }
    
    private boolean checkZipcodeImported() throws SQLException, IOException{
    	Integer zipcodeLoadCount = (Integer)cache.getIfPresent("zipcodeLoadCount");
    	if(zipcodeLoadCount==null){
    		zipcodeLoadCount = doUpdateZipCode(false);
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from zipcode_us");
    	if(list.size()==1){
    		if(list.get(0).get("count").toString().equals(zipcodeLoadCount+"")){
    			return true;
    		}
    	}
    	return false;
    }
    
    private int doUpdateZipCode(boolean updateDb) throws SQLException, IOException{
    	try{
    		int rowCount = 0;
			URL url = new URL(zipcodePath);
			HttpURLConnection con =  (HttpURLConnection) url.openConnection();
			ZipInputStream in = new ZipInputStream(con.getInputStream());
			in.getNextEntry();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			String prefix = "INSERT INTO jss_sys.zipcode_us ("+line+") values ";
			Connection conn = dbHelper.getConnection();
			try{
				Statement st = conn.createStatement();
				conn.setAutoCommit(false);
				line = br.readLine();
				while(line!=null){
					if(!line.trim().equals("")){
						if(updateDb){
							st.addBatch(prefix+"("+line.replaceAll("\'", "\'\'").replaceAll("\"", "\'")+");");
						}
						rowCount++;
					}
					line = br.readLine();
				}
				if(updateDb){
					st.executeBatch();
			        conn.commit();
				}
			}catch (SQLException e) {
				e.printStackTrace();
				try {
					conn.rollback();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
				throw e;
			}
			in.close();
			cache.put("zipcodeLoadCount", rowCount);
			return rowCount;
    	}catch (IOException e) {
			throw e;
		}
    }
    
    private boolean checkOrgIndex(){
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from pg_indexes where indexname='contact_ex_idx_resume_gin'");
    	if(list.size()==1){
    		if(Integer.parseInt(list.get(0).get("count").toString())>0){
    			return true;
    		}
    	}
    	return false;
    }
}    