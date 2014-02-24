package com.jobscience.search.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.jasql.PQuery;
import org.jasql.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.searchconfig.Filter;
import com.jobscience.search.searchconfig.FilterField;
import com.jobscience.search.searchconfig.SearchConfiguration;
import com.jobscience.search.searchconfig.SearchConfigurationManager;

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
    @Named("city.path")
    @Inject
    private String cityPath;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private IndexerManager indexerManager;
    @Inject
    private SfidManager sfidManager;
    @Inject
    private ContactTsvManager contactTsvManager;
    @Inject
    private SearchConfigurationManager scm;
    
    private volatile ConcurrentMap<String,JSONArray> indexesMap;
    
    private Logger log = LoggerFactory.getLogger(DBSetupManager.class);
    
    private Cache<String, Object> cache= CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
    .maximumSize(100).build(new CacheLoader<String,Object >() {
		@Override
		public Object load(String key) throws Exception {
			return key;
	}});
 
    /**
     * get the sys schema status
     * @return
     * @throws Exception 
     */
    public Map getSysConfig() throws Exception{
        Map status = new HashMap();
        status.put("schema_create", daoHelper.checkSysSchema());
        String sysTableNames = this.checkSysTables();
        Map tableMap = new HashMap();
        tableMap.put("org", sysTableNames.contains("org"));
        tableMap.put("config", sysTableNames.contains("config"));
        tableMap.put("zipcode_us", sysTableNames.contains("zipcode_us"));
        tableMap.put("city", sysTableNames.contains("city"));
        status.put("tables", tableMap);
        
        status.put("city", checkCity());
        if(sysTableNames.contains("zipcode_us")){
            status.put("zipcode_import", this.checkZipcodeImported());
        }else{
            status.put("zipcode_import", false);
        }
        
        Map extensionMap = new HashMap();
        extensionMap.put("pgtrgm", this.checkExtension("pg_trgm"));
        extensionMap.put("cube", this.checkExtension("cube"));
        extensionMap.put("earthdistance", this.checkExtension("earthdistance"));
        status.put("extensions", extensionMap);
        
        return status;
    }
    
    /**
     * get the org schema status for the given orgName
     * @param orgName
     * @return
     * @throws SQLException
     * @throws IOException
     */
    public Map getOrgConfig(String orgName,boolean quick) throws SQLException, IOException{
        Map status = new HashMap();
        status.put("schema_create", this.checkSchema(orgName));
        String orgExtraTableNames = this.checkOrgExtra(orgName)+",";
        Map tableMap = new HashMap();
       // tableMap.put("label", orgExtraTableNames.contains("label,"));
       // tableMap.put("label_contact", orgExtraTableNames.contains("label_contact,"));
        tableMap.put("contact_ex", orgExtraTableNames.contains("contact_ex,"));
        tableMap.put("savedsearches", orgExtraTableNames.contains("savedsearches,"));
        tableMap.put("user", orgExtraTableNames.contains("user,"));
        tableMap.put("searchlog", orgExtraTableNames.contains("searchlog,"));
        tableMap.put("pref", orgExtraTableNames.contains("pref,"));
        tableMap.put("recordtype", orgExtraTableNames.contains("recordtype,"));
        status.put("tables", tableMap);
        
        Map triggers = new HashMap();
        String triggerNames = this.checkTriggers(orgName)+",";
        triggers.put("employer_trigger", triggerNames.contains("employer_trigger,"));
        triggers.put("education_trigger", triggerNames.contains("education_trigger,"));
        triggers.put("skill_trigger", triggerNames.contains("skill_trigger,"));
        
        status.put("triggers", triggers);
        
        status.put("ex_grouped_skills",  orgExtraTableNames.contains("ex_grouped_skills,"));
        status.put("ex_grouped_educations",  orgExtraTableNames.contains("ex_grouped_educations,"));
        status.put("ex_grouped_employers",  orgExtraTableNames.contains("ex_grouped_employers,"));
        status.put("ex_grouped_locations",  orgExtraTableNames.contains("ex_grouped_locations,"));

        status.put("pgtrgm", this.checkExtension("pg_trgm"));
        Map indexMap = new HashMap();
        String indexNames = this.checkOrgIndex(orgName)+",";
        
        Map<String,JSONArray> indexesInfo =  getIndexMapFromJsonFile();
        for(String key:indexesInfo.keySet()){
            JSONArray ja = indexesInfo.get(key);
            for(int i=0;i<ja.size();i++){
                JSONObject jo =  JSONObject.fromObject(ja.get(i));
                indexMap.put(jo.getString("name"), indexNames.contains(jo.getString("name")+","));
            }
        }
     
        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        for(Filter f:sc.getFilters()){
            if(f.getFilterType()==null){
                String indexName = String.format("%s_%s_index", f.getFilterField().getTable(), f.getFilterField().getColumn());
                indexMap.put(indexName, indexNames.contains(indexName+","));
            }
        }
        status.put("indexes", indexMap);
        
        if(orgExtraTableNames.contains("contact_ex,")){
            if(indexerManager.isOn()){
                status.put("resume", "running");
            }else{
                if(indexerManager.getStatus(orgName,quick).getRemaining()==0){
                    status.put("resume", "done");
                }else{
                    if(indexerManager.getStatus(orgName,quick).getPerform()>0){
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
        
        status.put("user_timeout",checkColumn("timeout","user",schemaname));
        
        if(orgExtraTableNames.contains("contact_ex,")){
            if(checkColumn("sfid", "contact_ex", schemaname)){
                if(sfidManager.isOn()){
                    status.put("sfid", "running");
                }else{
                    if(sfidManager.getStatus(orgName,quick).getRemaining()==0){
                        status.put("sfid", "done");
                    }else{
                        if(sfidManager.getStatus(orgName,quick).getPerform()>0){
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
                    if(contactTsvManager.getStatus(orgName,quick).getRemaining()==0){
                        status.put("contact_tsv", "done");
                    }else{
                        if(contactTsvManager.getStatus(orgName,quick).getPerform()>0){
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
    
    /**
     * check a table own a column or not
     * @param columnName
     * @param table
     * @param schemaName
     * @return
     * @throws SQLException
     */
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
    	if(checkExtension(extName)>0){
    		return true;
    	}
    	
    	daoHelper.executeUpdate(daoHelper.openDefaultRunner(), "CREATE extension  if not exists "+extName+"  with schema pg_catalog;");
		return result;
    }
    /**
     * create system schema,will excute all the sql files under /jss_sys
     * @return
     * @throws SQLException
     */
    public boolean createSysSchema() throws Exception{
        return excuteSqlUnderSys(null);
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
        		" where table_schema='jss_sys' and table_type='BASE TABLE' and table_name in ('zipcode_us','org','config','city')");
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
        		runner.executeUpdate(sql);
            }
            runner.commit();
            List<Map> orgs = orgConfigDao.getOrgByName(orgName);
            String schemaname="" ;
            if(orgs.size()==1){
                schemaname = orgs.get(0).get("schemaname").toString();
            }
            if(!checkColumn("timeout","user",schemaname)){
                daoHelper.executeUpdate(orgName,"alter table \"user\" add column timeout bigint not null default 0;");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        	result = false;
            try {
                runner.roolback();
            } catch (Exception e1) {
                log.error(e1.getMessage());
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
       Map<String,JSONArray> m = getIndexMapFromJsonFile();
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
               SearchConfiguration sc = scm.getSearchConfiguration(orgName);
               for(Filter f:sc.getFilters()){
                   if(f.getFilterType()==null){
                       FilterField ff = f.getFilterField();
                       JSONObject jo = new JSONObject();
                       jo.accumulate("name", String.format("%s_%s_index", f.getFilterField().getTable(),
                               f.getFilterField().getColumn()));
                       jo.accumulate("column", ff.getColumn());
                       jo.accumulate("operator", "");
                       jo.accumulate("unique", "");
                       jo.accumulate("type", "btree");
                       runner.executeUpdate(generateIndexSql(ff.getTable(),jo));
                   }
               }
           }
           result = true;
       }catch (Exception e) {
        	result = false;
        	log.error(e.getMessage());
            try {
                runner.roolback();
            } catch (Exception e1) {
                log.error(e1.getMessage());
            }
            throw e;
        }finally{
            runner.close();
        }
        return result;
    }
    
    /**
     * excute the sql files under /sql/jss_sys
     * @param prefix if null,would excute all
     * @return
     * @throws Exception
     */
    private boolean excuteSqlUnderSys(String prefix) throws Exception{
        boolean result = true;
        daoHelper.createSysSchemaIfNecessary();
        File sysFolder = new File(getRootSqlFolderPath() + "/jss_sys");
        File[] sqlFiles = sysFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for(File file : sqlFiles){
            if(prefix==null||file.getName().startsWith(prefix)){
                List subSqlList = loadSQLFile(file);
                allSqls.addAll(subSqlList);
            }
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
                log.error(e1.getMessage());
                result = false;
            }
           throw e;
        }finally{
            runner.close();
        }
        return result;
    }
    
    /**
     * generate the index sql from the indexes.json
     * @param tabelName
     * @param jo
     * @return
     */
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
        
    	sql.append( "select count(*) as count from pg_indexes " )
           .append("where indexname in (")
           .append(getIndexesNamesAndTables()[0])
           .append(getOrgCustomFilterIndex(orgName))
           .append(") and schemaname=current_schema ")
           .append(contactEx?" and tablename='contact_ex' ":" and tablename<>'contact_ex' ");
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
    	if(list.size()==1){
    			return Integer.parseInt(list.get(0).get("count").toString());
    	}
    	return 0;
    }
    
    /**
     * get the indexes for custom filter which defined in org search config
     * @param orgName
     * @return
     */
    private String getOrgCustomFilterIndex(String orgName){
        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        StringBuilder sb = new StringBuilder();
        for(Filter f:sc.getFilters()){
            if(f.getFilterType()==null){
                sb.append(",'"+f.getFilterField().getTable()+"_"+f.getFilterField().getColumn()+"_index'");
            }
        }
        return sb.toString();
    }
    
    public int getTotalIndexCount(String orgName,boolean containsContactEx){
        int count=0;
        Map<String,JSONArray> m = getIndexMapFromJsonFile();
        for(String key:m.keySet()){
            if(!containsContactEx&&key.equals("contact_ex")){
                continue;
            }
            JSONArray ja = m.get(key);
            for(int i=0;i<ja.size();i++){
                count++;
            }
        }
        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        for(Filter f:sc.getFilters()){
            if(f.getFilterType()==null){
               count++;
            }
        }
        return count;
    }
    
    public String getWrongIndex(String orgName){
        StringBuilder sql = new StringBuilder();
        sql.append( "select string_agg(tablename||'.'||indexname,', ') as indexes from pg_indexes ")
           .append("where indexname not in (")
           .append(getIndexesNamesAndTables()[0])
           .append(getOrgCustomFilterIndex(orgName))
           .append(") and tablename in(" )
           .append(getIndexesNamesAndTables()[1])
           .append(")")
           .append(" and indexname not ilike '%pkey%' and schemaname=current_schema ");
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
        if(list.size()==1){
                return list.get(0).get("indexes")==null?"":list.get(0).get("indexes").toString();
        }
        return "";
    }
    
    public String removeWrongIndex(String orgName) throws Exception{
        StringBuilder sql = new StringBuilder();
        sql.append( "select indexname from pg_indexes " )
           .append("where indexname not in (")
           .append(getIndexesNamesAndTables()[0])
           .append(getOrgCustomFilterIndex(orgName))
           .append(") and tablename in(" )
           .append(getIndexesNamesAndTables()[1])
           .append(")" )
           .append(" and indexname not ilike '%pkey%' and schemaname=current_schema ");
        List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName), sql.toString());
        
        Runner runner = daoHelper.openNewOrgRunner(orgName);
        try{
            for(Map m:list){
                runner.executeUpdate(" drop index "+m.get("indexname")+" ;");
            }
        }catch (Exception e) {
            log.error(e.getMessage());
            try {
                runner.roolback();
            } catch (Exception e1) {
                log.error(e1.getMessage());
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
            Collections.addAll(sqlList, sqls);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return sqlList;
    }
    
    /**
     * check the org extra tables are existed or not(main for 'contact_ex','savedsearches','user')
     * @param orgName
     * @return
     */
    public  String checkOrgExtra(String orgName){
    	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
    	String schemaname="" ;
    	if(orgs.size()==1){
    		schemaname = orgs.get(0).get("schemaname").toString();
    	}
    	List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select string_agg(table_name,',') as names from information_schema.tables" +
        		" where table_schema='"+schemaname+"' and table_type='BASE TABLE' and table_name in ('label_contact','label','searchlog','recordtype','contact_ex','savedsearches','user','pref','ex_grouped_skills','ex_grouped_educations','ex_grouped_employers','ex_grouped_locations')");
    	if(list.size()==1){
            String names = (String)list.get(0).get("names");
            if(names==null){
                return "";
            }
            return names;
        }
    	return "";
    }
    
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
           log.error(e.getMessage());
           result = false;
           try {
               runner.roolback();
           } catch (Exception e1) {
               log.error(e1.getMessage());
           }
           throw e;
       }finally{
           runner.close();
       }
      return result;
   }
   
   /**
    * drop all the indexes created by jss
    * @param orgName
    */
   public int dropIndexes(String orgName){
       Map<String,JSONArray> indexesInfo =  getIndexMapFromJsonFile();
       int indexesCount = 0;
       for(String key:indexesInfo.keySet()){
           JSONArray ja = indexesInfo.get(key);
           for(int i=0;i<ja.size();i++){
               JSONObject jo =  JSONObject.fromObject(ja.get(i));
               daoHelper.executeUpdate(daoHelper.openNewOrgRunner(orgName), "drop index if exists "+jo.getString("name"));
               indexesCount++;
           }
       }
    
       SearchConfiguration sc = scm.getSearchConfiguration(orgName);
       for(Filter f:sc.getFilters()){
           if(f.getFilterType()==null){
               String indexName = f.getFilterField().getTable()+"_"+f.getFilterField().getColumn()+"_index";
               daoHelper.executeUpdate(daoHelper.openNewOrgRunner(orgName), "drop index if exists "+indexName);
               indexesCount++;
           }
       }
       return  indexesCount;
   }
   
   public void  dropExTables(String orgName){
       daoHelper.executeUpdate(daoHelper.openNewOrgRunner(orgName),  "drop table if exists ex_grouped_locations;"
                                                                   + "drop table if exists ex_grouped_employers;"
                                                                   + "drop table if exists ex_grouped_educations;"
                                                                   + "drop table if exists ex_grouped_skills");
   }
   
   private boolean checkCity(){
       boolean done = false;
       String extraSysTables = checkSysTables();
       if(extraSysTables.contains("city")){
           done = true;
       }else{
           return done;
       }
       List<Map> list = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select count(*) from city");
       if(list.size()==1){
           if(!"0".equals(list.get(0).get("count").toString())){
               done =  true;
           }else{
               done = false;
           }
       }
       return done;
   }
   
   public void computeCity() throws Exception{
       String extraSysTables = checkSysTables();
       if(!extraSysTables.contains("city")){
           excuteSqlUnderSys("01_");
       }
       Runner runner = daoHelper.openNewSysRunner();
       runner.executeUpdate("insert into city(name,longitude,latitude)"
               +" select city,avg(longitude),avg(latitude) from zipcode_us group by city",
               new Object[0]);
       runner.close();
   }
   
   public void importCity() throws Exception{
       String extraSysTables = checkSysTables();
       if(!extraSysTables.contains("city")){
           excuteSqlUnderSys("01_");
       }
       try{
           URL url = new URL(cityPath);
           HttpURLConnection con =  (HttpURLConnection) url.openConnection();
           ZipInputStream in = new ZipInputStream(con.getInputStream());
           in.getNextEntry();
           BufferedReader br = new BufferedReader(new InputStreamReader(in));
           String line = br.readLine();
           Runner runner = daoHelper.openNewSysRunner();
           try{
               runner.startTransaction();
               while(line!=null){
                   runner.executeUpdate(line);
                   line = br.readLine();
               }
               runner.commit();
           }catch (Exception e) {
               try {
                   runner.roolback();
               } catch (Exception e1) {
                   e.printStackTrace();
               }
               throw e;
           }finally{
               runner.close();
           }
           in.close();
       }catch (IOException e) {
           throw e;
       }
   }
   
   private Map<String,JSONArray> getIndexMapFromJsonFile(){
       if(indexesMap!=null){
           return indexesMap;
       }
       File orgFolder = new File(getRootSqlFolderPath() + "/org");
       File[] sqlFiles = orgFolder.listFiles();
       String indexes = "";
       for(File file : sqlFiles){
            if(file.getName().equals("indexes.json")){//only load the indexes.json
                List<String> subSqlList = loadSQLFile(file);
                indexes = subSqlList.get(0);
            }
       }
       JSONObject indexesObj = JSONObject.fromObject(indexes);
       ConcurrentMap<String,JSONArray> m = new ConcurrentHashMap<String,JSONArray>();
       for(Object tableName:indexesObj.keySet()){
          m.put(tableName.toString(), JSONArray.fromObject(indexesObj.get(tableName)));
       }
       indexesMap = m;
       return indexesMap;
   }
   
   private  String[] getIndexesNamesAndTables(){
       StringBuilder names = new StringBuilder(),tables = new StringBuilder();
       Map<String,JSONArray> indexesInfo =  getIndexMapFromJsonFile();
       for(String key:indexesInfo.keySet()){
           JSONArray ja = indexesInfo.get(key);
           for(int i=0;i<ja.size();i++){
               JSONObject jo =  JSONObject.fromObject(ja.get(i));
               names.append(",'").append(jo.get("name")).append("'");
               tables.append(",'").append(key).append("'");
           }
       }
       if(names.length()>0){
           names.delete(0, 1);
           tables.delete(0, 1);
       }
       return new String[]{names.toString(),tables.toString()};
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
    * check the zipcode data imported completed or not
    * first it would get from cache,if not found,will get from the sql file on dropbox
    * @return
    * @throws Exception 
    */
   private boolean checkZipcodeImported() throws Exception{
       Integer zipcodeLoadCount = (Integer)cache.getIfPresent("zipcodeLoadCount");
       if(zipcodeLoadCount==null){
           zipcodeLoadCount = doUpdateZipCode(false);
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
           //To match csv dataType, if columns change should change this
           Class[] dataTypes = new Class[]{String.class,String.class,String.class,Double.class,Double.class, Integer.class, Integer.class};
           StringBuilder valueStr = new StringBuilder();
           for(int i = 0; i < dataTypes.length; i++){
               if(i != 0){
                   valueStr.append(",");
               }
               valueStr.append("?");
           }
           String sql = "INSERT INTO jss_sys.zipcode_us ("+line+") values ("+valueStr.toString()+");";
           Runner runner = daoHelper.openDefaultRunner();
           try{
               runner.startTransaction();
               line = br.readLine();
               PQuery pq = runner.newPQuery(sql);
               while(line!=null){
                   if(!line.trim().equals("")){
                       if(updateDb){
                           String lineValue = line.replaceAll("\'", "").replaceAll("\"", "");
                           String[] valueStrs = lineValue.split(",");
                           Object[] values = new Object[dataTypes.length];
                           for(int i = 0; i < values.length; i++){
                               if(dataTypes[i] == Double.class){
                                   values[i] = new Double(valueStrs[i]);
                               }else if(dataTypes[i] == Integer.class){
                                   values[i] = new Integer(valueStrs[i]);
                               }else{
                                   values[i] = valueStrs[i];
                               }
                           }
                           pq.executeUpdate(values);
                       }
                       rowCount++;
                   }
                   line = br.readLine();
               }
               if(updateDb){
                   runner.commit();
               }
           }catch (Exception e) {
               log.error(e.getMessage());
               try {
                   runner.roolback();
               } catch (Exception e1) {
                   log.error(e1.getMessage());
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
                   "where indexname in ("+getIndexesNamesAndTables()[0]+") and schemaname=current_schema ");
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
   private int checkExtension(String extName){
       List<Map> list = daoHelper.executeQuery(daoHelper.openDefaultRunner(), 
               "select sum(case when nspname='pg_catalog' then 1 else 0 end) as pg,"
             + "sum(case when nspname!='pg_catalog' then 1 else 0 end) as not_pg "
             + "from pg_catalog.pg_extension e left join pg_catalog.pg_namespace n on e.extnamespace=n.oid"
             + " and  extname='"+extName+"' ");
       if(list.size()==1){
           if("1".equals(list.get(0).get("pg").toString())){
               return 1;
           }else if("1".equals(list.get(0).get("not_pg").toString())){
               return 2;
           }else{
               return 0;
           }
       }
       return 0;
   }
   
   public  String checkTriggers(String orgName){
   	List<Map> orgs = orgConfigDao.getOrgByName(orgName);
   	String schemaname="" ;
   	if(orgs.size()==1){
   		schemaname = orgs.get(0).get("schemaname").toString();
   	}
   	List<Map> list = daoHelper.executeQuery(daoHelper.openNewOrgRunner(orgName),
   			"select string_agg(trigger_name,',') as names from information_schema.triggers where trigger_schema='"+schemaname+"'");
   	if(list.size()==1){
           String names = (String)list.get(0).get("names");
           if(names==null){
               return "";
           }
           return names;
       }
   	return "";
   }
}    