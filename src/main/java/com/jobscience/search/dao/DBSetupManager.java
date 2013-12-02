package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.josql.Runner;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.db.DataSourceManager;

@Singleton
public class DBSetupManager {

    @Inject
    private DaoHelper daoHelper;
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
    @Inject
    private SfidManager sfidManager;
    @Inject
    private ContactTsvManager contactTsvManager;
    
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
        status.put("ex_grouped_skills",  orgExtraTableNames.contains("ex_grouped_skills,"));
        status.put("ex_grouped_educations",  orgExtraTableNames.contains("ex_grouped_educations,"));
        status.put("ex_grouped_employers",  orgExtraTableNames.contains("ex_grouped_employers,"));
        status.put("ex_grouped_locations",  orgExtraTableNames.contains("ex_grouped_locations,"));

        status.put("pgtrgm", this.checkExtension("pg_trgm"));
        Map indexMap = new HashMap();
        String indexNames = this.checkOrgIndex(orgName)+",";
        indexMap.put("contact_ex_contact_tsv_gin", indexNames.contains("contact_ex_contact_tsv_gin,"));
        indexMap.put("contact_title", indexNames.contains("contact_title_gin,"));
        indexMap.put("contact_name", indexNames.contains("contact_name_gin,"));
        indexMap.put("contact_firstname", indexNames.contains("contact_firstname_gin,"));
        indexMap.put("contact_lastname", indexNames.contains("contact_lastname_gin,"));
        indexMap.put("ts2__skill__c_name", indexNames.contains("ts2__skill__c_name,"));
        indexMap.put("ts2__skill__c_contact_c", indexNames.contains("ts2__skill__c_contact_c,"));
        indexMap.put("ts2__employment_history__c_contact_c", indexNames.contains("ts2__employment_history__c_contact_c,"));
        indexMap.put("ts2__employment_history__c_name_c", indexNames.contains("ts2__employment_history__c_name_c,"));
        indexMap.put("ts2__education_history__c_contact_c", indexNames.contains("ts2__education_history__c_contact_c,"));
        indexMap.put("ts2__education_history__c_name_c", indexNames.contains("ts2__education_history__c_name_c,"));
        
        indexMap.put("contact_ex_resume_tsv_gin", indexNames.contains("contact_ex_resume_tsv_gin,"));
        indexMap.put("ts2__skill__c_idx_sfid", indexNames.contains("ts2__skill__c_idx_sfid,"));
        indexMap.put("ts2__employment_history__c_idx_sfid", indexNames.contains("ts2__employment_history__c_idx_sfid"));
        indexMap.put("ts2__education_history__c_idx_sfid", indexNames.contains("ts2__education_history__c_idx_sfid,"));
        indexMap.put("ex_grouped_locations_name", indexNames.contains("ex_grouped_locations_name,"));
        indexMap.put("ex_grouped_skills_name", indexNames.contains("ex_grouped_skills_name,"));
        indexMap.put("ex_grouped_educations_name", indexNames.contains("ex_grouped_educations_name,"));
        indexMap.put("ex_grouped_employers_name", indexNames.contains("ex_grouped_employers_name,"));
        indexMap.put("contact_idx_sfid", indexNames.contains("contact_idx_sfid,"));
        indexMap.put("contact_ex_sfid", indexNames.contains("contact_ex_sfid,"));
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
        
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname="" ;
        if(orgs.size()==1){
            schemaname = orgs.get(0).get("schemaname").toString();
        }
        
        if(orgExtraTableNames.contains("contact_ex,")){
            if(checkColumn("sfid", "contact_ex", schemaname)){
                if(sfidManager.isOn()){
                    status.put("sfid", "running");
                }else{
                    if(sfidManager.getStatus(orgName).getRemaining()==0){
                        status.put("sfid", "done");
                    }else{
                        if(sfidManager.getStatus(orgName).getPerform()>0){
                            status.put("sfid","part");
                        }else{
                            status.put("sfid", false);
                        }
                    }
                }
            }else{
                status.put("sfid", false);
            }
        }else{
            status.put("sfid", false);
        }
        
        if(orgExtraTableNames.contains("contact_ex,")){
            if(checkColumn("contact_tsv", "contact_ex", schemaname)){
                if(contactTsvManager.isOn()){
                    status.put("contact_tsv", "running");
                }else{
                    if(contactTsvManager.getStatus(orgName).getRemaining()==0){
                        status.put("contact_tsv", "done");
                    }else{
                        if(contactTsvManager.getStatus(orgName).getPerform()>0){
                            status.put("contact_tsv","part");
                        }else{
                            status.put("contact_tsv", false);
                        }
                    }
                }
            }else{
                status.put("contact_tsv", false);
            }
        }else{
            status.put("contact_tsv", false);
        }
        
        return status;
    }
    
    private boolean checkColumn(String columnName,String table,String schemaName) throws SQLException{
        boolean result = false;
        
        List list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), " select 1 from information_schema.columns " + " where table_name =? and table_schema=?  and column_name=? ", table, schemaName, columnName);
        if (list.size() > 0) {
            result = true;
        }
        return result;
    }
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
    	
    	daoHelper.executeUpdate(daoHelper.openDefaultRunner(), "CREATE extension "+extName+"  with schema pg_catalog;");
		return result;
    }
    /**
     * create system schema,will excute all the sql files under /jss_sys
     * @return
     * @throws SQLException
     */
    public boolean createSysSchema() throws Exception{
    	boolean result = true;
    	dsMng.createSysSchemaIfNecessary();
        File sysFolder = new File(getRootSqlFolderPath() + "/jss_sys");
        File[] sqlFiles = sysFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
            List subSqlList = loadSQLFile(file);
            allSqls.addAll(subSqlList);
        }
        Runner runner = daoHelper.openNewSysRunner();
        try {
            runner.startTransaction();
            for(String sql : allSqls){
               runner.executeUpdate(sql);
            }
            runner.commit();
        } catch (Exception e) {
            try {
                runner.roolback();
            } catch (Exception e1) {
                e1.printStackTrace();
                result = false;
            }
           throw e;
        }finally{
            runner.close();
        }
        return result;
    }
    
    public boolean updateZipCode() throws Exception{
    	return doUpdateZipCode(true)!=0;
    }
    
    /**
     * check the system tables existed or not
     * @return
     */
    public  String checkSysTables(){
    	List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select string_agg(table_name,',') as names from information_schema.tables" +
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
    public boolean createExtraTables(String orgName) throws Exception{
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
        Runner runner = daoHelper.openNewOrgRunner(orgName);
        try {
            runner.startTransaction();
            for(String sql : allSqls){
        		runner.executeUpdate(sql.replaceAll("#", ";"));
            }
            runner.commit();
        } catch (Exception e) {
        	e.printStackTrace();
        	result = false;
            try {
                runner.roolback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw e;
        }finally{
            runner.close();
        }
       return result;
    }
    
    /**
     * create index for contact and contact_ex
     * @param orgName
     * @return
     * @throws SQLException
     */
    public boolean createIndexColumns(String orgName,boolean contactEx) throws Exception{
       boolean result = true;
       File orgFolder = new File(getRootSqlFolderPath() + "/org");
       File[] sqlFiles = orgFolder.listFiles();
       String indexes = new String();
       for(File file : sqlFiles){
        	if(file.getName().equals("indexes.json")){//only load the indexes.json
	            List<String> subSqlList = loadSQLFile(file);
	            indexes = subSqlList.get(0);
        	}
       }
       JSONObject indexesObj = JSONObject.fromObject(indexes);
       Map<String,JSONArray> m = new HashMap<String,JSONArray>();
       for(Object tableName:indexesObj.keySet()){
          m.put(tableName.toString(), JSONArray.fromObject(indexesObj.get(tableName)));
       }
       
       Runner runner = daoHelper.openNewOrgRunner(orgName);
       try {
           if(contactEx&&m.get("contact_ex")!=null){
               JSONArray ja = m.get("contact_ex");
               for(int i=0;i<ja.size();i++){
                   JSONObject jo =  JSONObject.fromObject(ja.get(i));
                   runner.executeUpdate(generateIndexSql("contact_ex",jo));
               }
           }else{
               for(String key:m.keySet()){
                   if(!key.equals("contact_ex")){
                       JSONArray ja = m.get(key);
                       for(int i=0;i<ja.size();i++){
                           JSONObject jo =  JSONObject.fromObject(ja.get(i));
                           runner.executeUpdate(generateIndexSql(key,jo));
                       }
                   }
               }
               
           }
           result = true;
       }catch (Exception e) {
        	result = false;
        	e.printStackTrace();
            try {
                runner.roolback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw e;
        }finally{
            runner.close();
        }
        return result;
    }
    
    private String generateIndexSql(String tabelName,JSONObject jo){
        StringBuilder sb = new StringBuilder();
        sb.append(" DO $$  ")
            .append(	"  BEGIN" )
            .append("    IF NOT EXISTS (" )
            .append("        SELECT 1" )
            .append("        FROM   pg_class c" )
            .append("        JOIN   pg_namespace n ON n.oid = c.relnamespace" )
            .append("        WHERE  c.relname = '" )
            .append(jo.get("name"))
            .append("'        AND    n.nspname =   current_schema" )
            .append("        ) THEN" )
            .append("       CREATE INDEX " )
            .append(jo.get("name"))
            .append("  ON ")
            .append(tabelName)
            .append(" USING " )
            .append(jo.get("type"))
            .append(" ( ")
            .append(jo.get("column"))
            .append(" ")
            .append(jo.get("operator"))
            .append(");    END IF;")
            .append("    END$$;");
        
        return sb.toString();
    }
    /**
     * Get the count for index count for contact and contact_ex
     * @param orgName
     * @return
     * @see {@link #createIndexColumns(String)}
     */
    public Integer getIndexStatus(String orgName,boolean contactEx){
    	StringBuilder sql = new StringBuilder();
    	sql.append( "select count(*) as count from pg_indexes " +
                "where indexname in ('contact_ex_resume_tsv_gin','contact_title_gin'," +
                "'contact_name_gin','contact_firstname_gin'," +
                "'ts2__skill__c_idx_sfid','ts2__employment_history__c_idx_sfid',"+
                "'ts2__education_history__c_idx_sfid','contact_idx_sfid',"+
                "'contact_lastname_gin','ts2__skill__c_name'," +
                "'ex_grouped_locations_name',"+
                "'ts2__skill__c_contact_c','ts2__employment_history__c_contact_c'," +
                "'ts2__employment_history__c_name_c','ts2__education_history__c_contact_c'," +
                "'ts2__education_history__c_name_c','contact_ex_sfid'," +
                "'contact_ex_contact_tsv_gin','ex_grouped_skills_name','ex_grouped_educations_name'," +
                "'ex_grouped_employers_name') and schemaname=current_schema ")
                .append(contactEx?" and tablename='contact_ex' ":" and tablename<>'contact_ex' ");
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
    	if(list.size()==1){
    			return Integer.parseInt(list.get(0).get("count").toString());
    	}
    	return 0;
    }
    
    public String getWrongIndex(String orgName){
        StringBuilder sql = new StringBuilder();
        sql.append( "select string_agg(tablename||'.'||indexname,', ') as indexes from pg_indexes " +
                "where indexname not in ('contact_ex_resume_tsv_gin','contact_title_gin'," +
                "'contact_name_gin','contact_firstname_gin'," +
                "'ts2__skill__c_idx_sfid','ts2__employment_history__c_idx_sfid',"+
                "'ts2__education_history__c_idx_sfid','contact_idx_sfid',"+
                "'contact_lastname_gin','ts2__skill__c_name'," +
                "'ex_grouped_locations_name',"+
                "'ts2__skill__c_contact_c','ts2__employment_history__c_contact_c'," +
                "'ts2__employment_history__c_name_c','ts2__education_history__c_contact_c'," +
                "'ts2__education_history__c_name_c','contact_ex_sfid'," +
                "'contact_ex_contact_tsv_gin','ex_grouped_skills_name','ex_grouped_educations_name'," +
                "'ex_grouped_employers_name') and tablename in(" +
                "'contact_ex','contact','ts2__skill__c','ts2__employment_history__c'," +
                "'ts2__education_history__c','ex_grouped_skills','ex_grouped_educations','ex_grouped_employers')" +
                " and indexname not ilike '%pkey%' and schemaname=current_schema ");
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
        if(list.size()==1){
                return list.get(0).get("indexes")==null?"":list.get(0).get("indexes").toString();
        }
        return "";
    }
    
    public String removeWrongIndex(String orgName) throws Exception{
        StringBuilder sql = new StringBuilder();
        sql.append( "select indexname from pg_indexes " +
                "where indexname not in ('contact_ex_resume_tsv_gin','contact_title_gin'," +
                "'contact_name_gin','contact_firstname_gin'," +
                "'ts2__skill__c_idx_sfid','ts2__employment_history__c_idx_sfid',"+
                "'ts2__education_history__c_idx_sfid','contact_idx_sfid',"+
                "'contact_lastname_gin','ts2__skill__c_name'," +
                "'ex_grouped_locations_name',"+
                "'ts2__skill__c_contact_c','ts2__employment_history__c_contact_c'," +
                "'ts2__employment_history__c_name_c','ts2__education_history__c_contact_c'," +
                "'ts2__education_history__c_name_c','contact_ex_sfid'," +
                "'contact_ex_contact_tsv_gin','ex_grouped_skills_name','ex_grouped_educations_name'," +
                "'ex_grouped_employers_name') and tablename in(" +
                "'contact_ex','contact','ts2__skill__c','ts2__employment_history__c'," +
                "'ts2__education_history__c','ex_grouped_skills','ex_grouped_educations','ex_grouped_employers')" +
                " and indexname not ilike '%pkey%' and schemaname=current_schema ");
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
        
        Runner runner = daoHelper.openNewOrgRunner(orgName);
        try{
            for(Map m:list){
                runner.executeUpdate(" drop index "+m.get("indexname")+" ;");
            }
        }catch (Exception e) {
            e.printStackTrace();
            try {
                runner.roolback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            throw e;
        }finally{
            runner.close();
        }
        return "";
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
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select count(*) as count from zipcode_us");
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
    private int doUpdateZipCode(boolean updateDb) throws Exception{
    	try{
    		int rowCount = 0;
			URL url = new URL(zipcodePath);
			HttpURLConnection con =  (HttpURLConnection) url.openConnection();
			ZipInputStream in = new ZipInputStream(con.getInputStream());
			in.getNextEntry();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = br.readLine();
			String prefix = "INSERT INTO jss_sys.zipcode_us ("+line+") values ";
			Runner runner = daoHelper.openDefaultRunner();
			try{
			    runner.startTransaction();
				line = br.readLine();
				while(line!=null){
					if(!line.trim().equals("")){
						if(updateDb){
						    runner.executeUpdate(prefix+"("+line.replaceAll("\'", "\'\'").replaceAll("\"", "\'")+");");
						}
						rowCount++;
					}
					line = br.readLine();
				}
				if(updateDb){
				    runner.commit();
				}
			}catch (Exception e) {
				e.printStackTrace();
				try {
				    runner.roolback();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				throw e;
			}finally{
			    runner.close();
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
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), 
    	            "select string_agg(indexname,',') as names from pg_indexes " +
    	            "where indexname in ('contact_ex_resume_tsv_gin','contact_title_gin'," +
                    "'contact_name_gin','contact_firstname_gin'," +
                    "'ts2__skill__c_idx_sfid','ts2__employment_history__c_idx_sfid',"+
                    "'ts2__education_history__c_idx_sfid','contact_idx_sfid',"+
                    "'contact_lastname_gin','ts2__skill__c_name'," +
                    "'ex_grouped_locations_name',"+
                    "'ts2__skill__c_contact_c','ts2__employment_history__c_contact_c'," +
                    "'ts2__employment_history__c_name_c','ts2__education_history__c_contact_c'," +
                    "'ts2__education_history__c_name_c','contact_ex_sfid'," +
                    "'contact_ex_contact_tsv_gin','ex_grouped_skills_name','ex_grouped_educations_name'," +
                    "'ex_grouped_employers_name') and schemaname=current_schema ");
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
    	List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), "select count(*) as count from pg_catalog.pg_extension" +
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
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select string_agg(table_name,',') as names from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name in ('label_contact','label','contact_ex','savedsearches','user','ex_grouped_skills','ex_grouped_educations','ex_grouped_employers','ex_grouped_locations')");
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
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select count(*) as count from information_schema.schemata" +
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
    	List<Map> list = daoHelper.executeQuery(dsMng.getSysDataSource(),  "select count(*) as count from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name ='"+table+"'");
    	if(list.size()==1){
    		if("1".equals(list.get(0).get("count").toString())){
    			return true;
    		}
    	}
    	return false;
    }
    */
   public List<String> getSqlCommandForOrg(String fileName){
       return loadSQLFile(new File(getRootSqlFolderPath()+"/org/"+fileName));
   }
   
   public boolean createExtraGroup(String orgName,String tableName) throws Exception{
       boolean result = true;
       File orgFolder = new File(getRootSqlFolderPath() + "/org");
       File[] sqlFiles = orgFolder.listFiles();
       List<String> allSqls = new ArrayList();
       String filePrexName = "";
       if(tableName.contains("skills")){
           filePrexName = "06_";
       }else if(tableName.contains("educations")){
           filePrexName = "07_";
       }else if(tableName.contains("employers")){
           filePrexName = "08_";
       }else {
           filePrexName = "09_";
       }
       for(File file : sqlFiles){
           if(file.getName().startsWith(filePrexName)){
               List<String> subSqlList = loadSQLFile(file);
               allSqls.addAll(subSqlList);
           }
       }
       Runner runner = daoHelper.openNewOrgRunner(orgName);
       try {
           runner.startTransaction();
           for(String sql : allSqls){
               runner.executeUpdate(sql.replaceAll("#", ";"));
           }
           runner.commit();
       } catch (Exception e) {
           e.printStackTrace();
           result = false;
           try {
               runner.roolback();
           } catch (Exception e1) {
               e1.printStackTrace();
           }
           throw e;
       }finally{
           runner.close();
       }
      return result;
   }
}    