package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
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
 
    public Map getSysConfig() throws SQLException, IOException{
        Map status = new HashMap();
        status.put("schema_create", dsMng.checkSysSchema());
        
        String sysTableNames = this.checkSysTables();
        Map tableMap = new HashMap();
        tableMap.put("org", sysTableNames.contains("org"));
        tableMap.put("config", sysTableNames.contains("config"));
        tableMap.put("zipcode_us", sysTableNames.contains("zipcode_us"));
        status.put("tables", tableMap);
        
        if(sysTableNames.contains("zipcode_us")){
            status.put("zipcode_import", this.checkZipcodeImported());
        }else{
            status.put("zipcode_import", false);
        }
        status.put("pgtrgm", this.checkExtension("pg_trgm"));
        return status;
    }
    
    public Map getOrgConfig(String orgName) throws SQLException, IOException{
        Map status = new HashMap();
        status.put("schema_create", this.checkSchema(orgName));
        String orgExtraTableNames = this.checkOrgExtra(orgName)+",";
        Map tableMap = new HashMap();
        tableMap.put("label", orgExtraTableNames.contains("label,"));
        tableMap.put("label_contact", orgExtraTableNames.contains("label_contact,"));
        tableMap.put("contact_ex", orgExtraTableNames.contains("contact_ex,"));
        tableMap.put("savedsearches", orgExtraTableNames.contains("savedsearches,"));
        tableMap.put("user", orgExtraTableNames.contains("user,"));
        status.put("tables", tableMap);
        
        Map indexMap = new HashMap();
        String indexNames = this.checkOrgIndex(orgName)+",";
        indexMap.put("contact_ex_resume", indexNames.contains("contact_ex_idx_resume_gin,"));
        indexMap.put("contact_title", indexNames.contains("contact_title_trgm_gin,"));
        indexMap.put("contact_name", indexNames.contains("contact_name_trgm_gin,"));
        indexMap.put("contact_firstname", indexNames.contains("contact_firstname_trgm_gin,"));
        indexMap.put("contact_lastname", indexNames.contains("contact_lastname_trgm_gin,"));
        indexMap.put("ts2__skill__c_name", indexNames.contains("ts2__skill__c_name,"));
        indexMap.put("ts2__skill__c_contact_c", indexNames.contains("ts2__skill__c_contact_c,"));
        indexMap.put("ts2__employment_history__c_contact_c", indexNames.contains("ts2__employment_history__c_contact_c,"));
        indexMap.put("ts2__employment_history__c_name_c", indexNames.contains("ts2__employment_history__c_name_c,"));
        indexMap.put("ts2__education_history__c_contact_c", indexNames.contains("ts2__education_history__c_contact_c,"));
        indexMap.put("ts2__education_history__c_name_c", indexNames.contains("ts2__education_history__c_name_c,"));
        status.put("indexes", indexMap);
        
        if(orgExtraTableNames.contains("contact_ex,")){
            if(indexerManager.isOn()){
                status.put("resume", "running");
            }else{
                if(indexerManager.getStatus(orgName).getRemaining()==0){
                    status.put("resume", "done");
                }else{
                    if(indexerManager.getStatus(orgName).getPerform()>0){
                        status.put("resume","part");
                    }else{
                        status.put("resume", false);
                    }
                }
            }
        }else{
            status.put("resume", false);
        }
        return status;
    }
    /*public Integer checkSetupStatus(SchemaType type,String orgName) throws SQLException, IOException{
    	Integer status =0;
    	if(checkSysTables()){
    		status=SetupStatus.SYS_SCHEMA_CREATED.getValue();
        }
    	if(status==SetupStatus.SYS_SCHEMA_CREATED.getValue()){
    		if(checkZipcodeImported()){
	        	status = status|SetupStatus.ZIPCODE_DATA_IMPORTED.getValue();
	        }
    	}
    	if(type.equals(SchemaType.ORG)){
    		
    		List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        	String schemaname="" ;
        	if(orgs.size()==1){
        		schemaname = orgs.get(0).get("schemaname").toString();
        	}
        	
	    	if(status>=SetupStatus.SYS_SCHEMA_CREATED.getValue()){
	    		if(checkSchema(orgName)&&checkTable(orgName, "contact")){
	    		    status=status|SetupStatus.ORG_SCHEMA_CREATED.getValue();
	    			if(checkOrgExtra(orgName)){
			        	status = status|SetupStatus.ORG_EXTRA_CREATED.getValue();
			        }
	    		}
	    	}
	    	
	    	if(status>=SetupStatus.ORG_EXTRA_CREATED.getValue()){
	    		if(checkExtension("pg_trgm")){
		        	status = status|SetupStatus.PG_TRGM_CREATED.getValue();
		        	if(checkOrgIndex(schemaname)){
			        	status = status|SetupStatus.ORG_INDEX_COLUMNS_CREATED.getValue();
			        }
		        }
	    		
	    		if(indexerManager.isOn()){
	    	        status = status|SetupStatus.ORG_RESUME_RUNNING.getValue();
	        	}
		        if(indexerManager.getStatus(orgName).getRemaining()==0){
		            status = status|SetupStatus.ORG_INDEX_RESUME_CREATED.getValue();
		        }
	    	}
    	}else{
    		if(checkExtension("pg_trgm")){
        		status = status|SetupStatus.PG_TRGM_CREATED.getValue();
        	}
    	}
        return status;
    }*/
    
    /**
     * create extension for public schema
     * @param extName
     * @return
     * @throws SQLException
     */
    public boolean createExtension(String extName) throws SQLException{
    	boolean result = false;
    	if(checkExtension(extName)){
    		return true;
    	}
    	  Connection conn = dsMng.getDefaultConnection();
    	try{
	        PreparedStatement st = conn.prepareStatement("CREATE extension "+extName+";");
	        result = st.execute();
	        st.close();
	        conn.close();
    	}catch (SQLException e) {
			throw e;
		}finally{
            conn.close();
        }
		return result;
    }
    /**
     * create system schema,will excute all the sql files under /jss_sys
     * @return
     * @throws SQLException
     */
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
        Connection conn = dbHelper.openSysConnection();
        try {
            conn.setAutoCommit(false);
            for(String sql : allSqls){
                Statement st = conn.createStatement();
                st.execute(sql);
                st.close();
            }
            conn.commit();
        } catch (SQLException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
                result = false;
            }
           throw e;
        }finally{
            conn.close();
        }
        return result;
    }
    
    public boolean updateZipCode() throws SQLException, IOException{
    	return doUpdateZipCode(true)!=0;
    }
    
    /**
     * check the system tables existed or not
     * @return
     */
    public  String checkSysTables(){
    	List<Map> list = dbHelper.executeQuery(dsMng.getDefaultDataSource(), "select string_agg(table_name,',') as names from information_schema.tables" +
        		" where table_schema='jss_sys' and table_type='BASE TABLE' and table_name in ('zipcode_us','org','config')");
    	if(list.size()==1){
    	    String names = (String)list.get(0).get("names");
    	    if(names==null){
    	        return "";
    	    }
    	    return names;
    	}
    	return "";
    }
    
    /**
     * create extra table for given ORG,will excute 01_create_extra.sql under /org folder
     * @param orgName
     * @return
     * @throws SQLException
     */
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
        Connection conn = dbHelper.openConnection(orgName);
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
        }finally{
            conn.close();
        }
       return result;
    }
    
    /**
     * create index for contact and contact_ex
     * @param orgName
     * @return
     * @throws SQLException
     */
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
        Connection conn = dbHelper.openConnection(orgName);
        try {
            Statement st = conn.createStatement();
            for(String sql : allSqls){
        		st.addBatch(sql);
        		st.executeBatch();
            }
            st.executeBatch();
        } catch (SQLException e) {
        	result = false;
        	e.printStackTrace();
            try {
                conn.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            throw e;
        }finally{
            conn.close();
        }
        return result;
    }
    
    /**
     * Get the count for index count for contact and contact_ex
     * @param orgName
     * @return
     * @see {@link #createIndexColumns(String)}
     */
    public int getIndexCount(String orgName){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	StringBuilder sql = new StringBuilder();
    	sql.append(" select count(*) as count from pg_indexes ")
    	   .append(" where indexname in ('contact_ex_idx_resume_gin',")
    	   .append("'contact_title_trgm_gin','contact_name_trgm_gin',")
    	   .append("'contact_firstname_trgm_gin','contact_lastname_trgm_gin')")
    	   .append(" and schemaname='").append(schemaname)
    	   .append("' ");
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), sql.toString());
    	if(list.size()==1){
    			return Integer.parseInt(list.get(0).get("count").toString());
    	}
    	return 0;
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
            String sqls[] = temp.toString().split("-- SCRIPTS");
            for (String sql : sqls) {
                sqlList.add(sql);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sqlList;
    }
    
    /**
     * check the zipcode data imported completed or not
     * first it would get from cache,if not found,will get from the sql file on dropbox
     * @return
     * @throws SQLException
     * @throws IOException
     */
    private boolean checkZipcodeImported() throws SQLException, IOException{
    	Integer zipcodeLoadCount = (Integer)cache.getIfPresent("zipcodeLoadCount");
    	if(zipcodeLoadCount==null){
    		zipcodeLoadCount = 43191;//doUpdateZipCode(false);
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from zipcode_us");
    	if(list.size()==1){
    		if(list.get(0).get("count").toString().equals(zipcodeLoadCount+"")){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * update zipcode data from bropbox
     * @param updateDb if<code>true</code> will insert into table,else just get the recordes count
     * @return
     * @throws SQLException
     * @throws IOException
     */
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
			Connection conn = dbHelper.openConnection();
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
			}finally{
	            conn.close();
	        }
			in.close();
			cache.put("zipcodeLoadCount", rowCount);
			return rowCount;
    	}catch (IOException e) {
			throw e;
		}
    }
    /**
     * check the contact and contact_ex index for org
     * @param schemaname
     * @return
     */
    private String checkOrgIndex(String orgName){
    	List<Map> list = dbHelper.executeQuery(dsMng.getOrgDataSource(orgName), 
    	            "select string_agg(indexname,',') as names from pg_indexes " +
    	            "where indexname in ('contact_ex_idx_resume_gin','contact_title_trgm_gin'," +
    	            "'contact_name_trgm_gin','contact_firstname_trgm_gin'," +
    	            "'contact_lastname_trgm_gin','ts2__skill__c_name'," +
    	            "'ts2__skill__c_contact_c','ts2__employment_history__c_contact_c'," +
    	            "'ts2__employment_history__c_name_c','ts2__education_history__c_contact_c'," +
    	            "'ts2__education_history__c_name_c') and schemaname=current_schema ");
    	if(list.size()==1){
            String names = (String)list.get(0).get("names");
            if(names==null){
                return "";
            }
            return names;
        }
    	return "";
    }
    
    /**
     * check the extension by extension name
     * @param extName
     * @return
     */
    private boolean checkExtension(String extName){
    	List<Map> list = dbHelper.executeQuery(dsMng.getDefaultDataSource(), "select count(*) as count from pg_catalog.pg_extension" +
        		" where extname='"+extName+"' ");
    	if(list.size()==1){
    		if("1".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * check the org extra tables are existed or not(main for 'contact_ex','savedsearches','user')
     * @param orgName
     * @return
     */
    private  String checkOrgExtra(String orgName){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select string_agg(table_name,',') as names from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name in ('label_contact','label','contact_ex','savedsearches','user')");
    	if(list.size()==1){
            String names = (String)list.get(0).get("names");
            if(names==null){
                return "";
            }
            return names;
        }
    	return "";
    }
    
    /**
     * check the schema is existed or not for org
     * @param orgName
     * @return
     */
    private  boolean checkSchema(String orgName){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(), "select count(*) as count from information_schema.schemata" +
        		" where schema_name='"+schemaname+"'");
    	if(list.size()==1){
    		if("1".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * check the org schema has the special table or not
     * @param orgName
     * @param table
     * @return
     *//*
    private  boolean checkTable(String orgName,String table){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	List<Map> list = dbHelper.executeQuery(dsMng.getSysDataSource(),  "select count(*) as count from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name ='"+table+"'");
    	if(list.size()==1){
    		if("1".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    */
    public static void main(String[] args) {
        String a="-- SCRIPTS create-table:org"+System.getProperty("line.separator")+"CREATE TABLE org";
        for(String b:a.split("-- SCRIPTS")){
            if(!b.trim().equals(""))
                System.out.println(b.substring(0,b.indexOf(System.getProperty("line.separator"))));
        }
    }
}    