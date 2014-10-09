package com.jobscience.search.dao;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jasql.PQuery;
import org.jasql.RSQLException;
import org.jasql.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.binding.WebAppFolder;
import com.google.common.base.Strings;
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
    private DaoRwHelper daoRwHelper;
    @Named("zipcode.path")
    @Inject
    private String zipcodePath;
    @Named("org.path")
    @Inject
    private String orgPath;
    @Named("city.path")
    @Inject
    private String cityPath;
    @Named("city_world.path")
    @Inject
    private String cityWorldPath;
    @Inject
    private OrgConfigDao orgConfigDao;
    @Inject
    private IndexerManager indexerManager;
    @Inject
    private SfidManager sfidManager;
    @Inject
    private ContactTsvManager contactTsvManager;
    @Inject
    private ResumeLowerManager resumeLowerManager;
    @Inject
    private SearchConfigurationManager scm;
    
    private volatile ConcurrentMap<String,JSONArray> indexesMap;
    @Inject
    private CurrentRequestContextHolder currentRequestContextHolder;
    @Inject
    @Named("jss.db.user")
    private String user;
    
    @Inject 
    private @WebAppFolder File  webAppFolder;
    
    private volatile ConcurrentMap<String,Map> jsonMap = new ConcurrentHashMap<String, Map>();
    private Logger log = LoggerFactory.getLogger(DBSetupManager.class);
    private String sysSchema = "jss_sys";
    private String[][] newTableNameChanges = {{"contact_ex","jss_contact"},
                                              {"ex_grouped_educations","jss_grouped_educations"},
                                              {"ex_grouped_employers","jss_grouped_employers"},
                                              {"ex_city_score","city_score"},
                                              {"ex_grouped_skills","jss_grouped_skills"},
                                              {"pref","jss_pref"},
                                              {"searchlog","jss_searchlog"},
                                              {"user","jss_user"},
                                              {"savedsearches","jss_savedsearches"}};
    private String[] manyTomanyTable = {"jss_contact_jss_groupby_skills","jss_contact_jss_groupby_educations","jss_contact_jss_groupby_employers"};
    private String[] allowCustomIndexColumnName = {"name","email","title","firstname","lastname","phone","mailingcity"};
    private String[] allowCustomIndexColumnType = {"integer","boolean","timestamp","character varying","date","timestamp without time zone","double precision"};
    
    private Cache<String, Object> cache= CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
    .maximumSize(100).build(new CacheLoader<String,Object >() {
		@Override
		public Object load(String key) throws Exception {
			return key;
	}});
    
    private Map<String, Boolean> orgSetupStatus = new ConcurrentHashMap<String, Boolean>();
    private Map<String, Map<String,String>> orgSetupStatusMsg = new ConcurrentHashMap<String, Map<String,String>>();
    private volatile ConcurrentMap<String,Map<String,Integer>> orgGroupTableCountMap = new ConcurrentHashMap<String, Map<String,Integer>>();
    private volatile ConcurrentMap<String,Map<String,Boolean>> orgGroupTableInsertStatusMap = new ConcurrentHashMap<String, Map<String,Boolean>>();
    private volatile ConcurrentMap<String,Map<String,Integer>> orgGroupTableInsertScheduleMap = new ConcurrentHashMap<String, Map<String,Integer>>();
    
    private String DONE="done",RUNNING="running",NOTSTARTED="notstarted",ERROR="error",PART="part",INCOMPLETE="incomplete";
    private String webPath;
    private volatile Thread sysThread;
    private volatile boolean sysReseting = false;
    private volatile HttpGet zipCodeConnection = null;
    private volatile HttpGet cityConnection = null;
    private volatile HttpGet cityWorldConnection = null;
    private boolean firstSetup = true;
    private String step = null;
    private ConcurrentHashMap<String,Thread> orgThreads = new ConcurrentHashMap<String, Thread>();
    private ConcurrentHashMap<String, CurrentOrgSetupStatus>  currentOrgSetupStatus = 
    		new ConcurrentHashMap<String, DBSetupManager.CurrentOrgSetupStatus>();
    
    // ---------- organization setup interfaces ----------//
   
    public void orgSetup(final String orgName){
    	if(orgSetupStatusMsg.get(orgName) != null){
    		orgSetupStatusMsg.get(orgName).clear();
    	}
        if(webPath == null){
            webPath = currentRequestContextHolder.getCurrentRequestContext().getServletContext().getRealPath("/");
        }
        Thread orgThread = orgThreads.get(orgName);
        if(orgThread == null){
            orgThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        doOrgSetup(orgName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error(e.getMessage());
                    }
                }
            });
            orgThreads.put(orgName, orgThread);
            orgThread.setName(orgName);
            orgThread.start();
        }
    }
    
    
    private void doOrgSetup(String orgName) throws Exception {
        if (orgSetupStatus.get(orgName) != null && orgSetupStatus.get(orgName)) {
            return;
        }
        orgSetupStatus.put(orgName, true);
        fixJssTableNames(orgName);

        createExtraTables(orgName);
        createExtraGroup(orgName, "jss_grouped_skills");
        createExtraGroup(orgName, "jss_grouped_educations");
        createExtraGroup(orgName, "jss_grouped_employers");
        createExtraGroupCity(orgName, "city_score");
        fixMissingColumns(orgName, false);

        indexerManager.run(orgName,webPath);
        sfidManager.run(orgName,webPath);
        contactTsvManager.run(orgName,webPath);
        resumeLowerManager.run(orgName,webPath);
        dropInvalidIndexes(orgName);
        createIndexColumns(orgName, true);
        createIndexColumns(orgName, false);
        removeWrongIndex(orgName);
        updateContactCustomIndex(orgName);
        Map<String,Boolean> GroupTableInsertStatus = orgGroupTableInsertStatusMap.get(orgName);
        if(GroupTableInsertStatus != null && GroupTableInsertStatus.get("jss_contact_jss_groupby_skills") != null && !GroupTableInsertStatus.get("jss_contact_jss_groupby_skills")){
            renderManyToMany(orgName,"jss_contact_jss_groupby_skills");
        }
        if(GroupTableInsertStatus != null && GroupTableInsertStatus.get("jss_contact_jss_groupby_educations") != null && !GroupTableInsertStatus.get("jss_contact_jss_groupby_educations")){
            renderManyToMany(orgName,"jss_contact_jss_groupby_educations");
        }
        if(GroupTableInsertStatus != null && GroupTableInsertStatus.get("jss_contact_jss_groupby_employers") != null && !GroupTableInsertStatus.get("jss_contact_jss_groupby_employers")){
            renderManyToMany(orgName,"jss_contact_jss_groupby_employers");
        }
        createPKOfmanyTManyTable(orgName);
        stopOrgSetup(orgName);
    }
   
    private Boolean renderManyToMany(String orgName,String table){
    	Map<String,Boolean> groupTableStatus = orgGroupTableInsertStatusMap.get(orgName);
    	if(groupTableStatus == null){
    		groupTableStatus = getGroupTableStatusMap(orgName);
        }else if(groupTableStatus.get(table)){
        	return true;
    	}    
        Map<String,Integer> insertSchedule = orgGroupTableInsertScheduleMap.get(orgName);
        if(insertSchedule == null){
        	insertSchedule = getGroupTableInsertStatusMap(orgName);
        }
        int offset = 1;
        if(insertSchedule.get(table) != null){
        	offset = insertSchedule.get(table);;
        }
        if(offset == 1){
        	daoRwHelper.executeUpdate(orgName, "delete from "+table);
        	dropPkOfmanyTomanyTable(orgName,table,table+"_pkey");
        }
        //get insert sql
    	File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        String sqlString = "";
        for (File file : sqlFiles) {
            if (file.getName().contains(table)) {
                List<String> subSqlList = loadSQLFile(file);
                sqlString = subSqlList.get(1);
                break;
            }
        }
        int contactTotal = getContactMaxId("contact", orgName);
        if(insertSchedule.get(table) != null){
        	offset = insertSchedule.get(table);
        }
        try{
			while (offset < contactTotal) {
				if(contactTotal - offset < 100){
					daoRwHelper.executeUpdate(orgName, sqlString, offset, contactTotal);
					insertSchedule.put(table, contactTotal);
					groupTableStatus.put(table, true);
				}else{
					daoRwHelper.executeUpdate(orgName, sqlString, offset, offset+99);
					insertSchedule.put(table, offset);
				}
		        offset += 100;
			}
        }catch (RSQLException e){
        	groupTableStatus.put(table, true);
        }finally{
        	
        }
        return true;
    }
    
    public void resetOrgSetup(String orgName) {
        dropAllPkOfmanyTomanyTable(orgName);
        StringBuilder sb = new StringBuilder();
        for (String[] tables : newTableNameChanges) {
            sb.append(",").append(tables[1]);
        }
        for (String table : manyTomanyTable) {
            sb.append(",").append(table);
        }
        daoRwHelper.executeUpdate(orgName,
                "drop table if exists " + sb.delete(0, 1) +" CASCADE");
        dropIndexes(orgName);
        //reset status
        if(orgGroupTableCountMap.get(orgName) != null){
        	orgGroupTableCountMap.remove(orgName);
        	Map<String, Integer> groupTableCountMap = getGroupTableCountMap(orgName);
        	orgGroupTableCountMap.put(orgName,groupTableCountMap);
        }
        if(orgGroupTableInsertStatusMap.get(orgName) != null){
        	orgGroupTableInsertStatusMap.remove(orgName);
        	Map<String, Boolean> groupTableInsertStatusMap = getGroupTableStatusMap(orgName);
        	orgGroupTableInsertStatusMap.put(orgName,groupTableInsertStatusMap);
        }
        if(orgGroupTableInsertScheduleMap.get(orgName) != null){
        	orgGroupTableInsertScheduleMap.remove(orgName);
        	Map<String, Integer> groupTableInsertScheduleMap = getGroupTableInsertStatusMap(orgName);
        	orgGroupTableInsertScheduleMap.put(orgName,groupTableInsertScheduleMap);
        }
    }

    public void stopOrgSetup(String orgName) {
        Thread orgThread = orgThreads.get(orgName);
        if(orgThread != null){
            orgThread = null;
            orgThreads.remove(orgName);
        }
        orgSetupStatus.put(orgName, false);
        indexerManager.stop();
        sfidManager.stop();
        contactTsvManager.stop();
    }

    public Map orgStatus(String orgName) throws Exception {
        Map status = new HashMap();
        String totalStatus = DONE;
        List<Map> setups = new ArrayList<Map>();
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }

        boolean schemaExist = this.checkSchema(orgName);
        setups.add(mapIt("name", "schema", "status", schemaExist ? DONE : NOTSTARTED, "msg",
                schemaExist ? "Org Schema Exists" : "Org Schema Not Exists"));
        if (!schemaExist) {
            totalStatus = ERROR;
        }
        String missingColumns = checkColumns(schemaname, "org-sync-tables.def", false);
        setups.add(mapIt("name", "ts2_table", "status", missingColumns.length() > 0 ? ERROR : DONE,
                "msg", missingColumns.length() <= 0 ? "Default sync tables valid"
                        : "Default sync tables missing columns: " + missingColumns));
        if (missingColumns.length() > 0) {
            totalStatus = ERROR;
        }
        missingColumns = checkColumns(schemaname, "org-jss-tables.def", true);
        setups.add(mapIt("name", "jss_table", "status", missingColumns.length() > 0 ? ERROR : DONE,
                "msg", missingColumns.length() <= 0 ? "jss tables valid valid"
                        : "jss tables Missing columns: " + missingColumns));
        if (missingColumns.length() > 0 && totalStatus.equals(DONE)) {
            totalStatus = NOTSTARTED;
        }
        setups.add(mapIt("name", "fix_old_jss_table", "status",
                hasOldJssTableNames(orgName, schemaname) ? NOTSTARTED : DONE));

        List<Map> jssTables = this.checkOrgExtra(orgName);
        StringBuffer missingTables = new StringBuffer();
        for (Map m : jssTables) {
            missingTables.append(", ").append(m.get("tablename"));
        }
        boolean triggerValid = checkTriggerContent(orgName);
        setups.add(mapIt("name", "create_extra_table", 
        		"status",
                missingTables.length() == 0 && triggerValid ? DONE : NOTSTARTED,
                "msg",
                missingTables.length() > 0 ? "Missing Tables:" + missingTables.delete(0, 1)
                        : (triggerValid?"Jss tables created":"Some triggers not valid")));
        if(totalStatus.equals(DONE)&&(missingTables.length()>0||!triggerValid)){
            totalStatus = PART;
        }
        setups.add(mapIt("pgtrgm", this.checkExtension("pg_trgm") > 0 ? DONE : NOTSTARTED));
        IndexerStatus cityscoreStatus = checkCityScoreStatus(orgName);
        String csStatus = "notstarted";
        if(cityscoreStatus.getPerform() == -1){
        	setups.add(mapIt("name", "cityscore", "status", csStatus, "progress", 0));
        }else{
        	setups.add(mapIt("name", "cityscore", "status", cityscoreStatus.getRemaining() > 0 ? PART : DONE, "progress", cityscoreStatus));
        }
        if (cityscoreStatus.getRemaining() > 0 && totalStatus.equals(DONE)) {
            totalStatus = PART;
        }
        if (missingTables.indexOf("jss_contact") == -1) {
            if (checkColumn("contact_tsv", "jss_contact", schemaname)) {
                IndexerStatus tsvIs = contactTsvManager.getStatus(orgName, false);
                String tsvStatus = contactTsvManager.isOn() ? RUNNING
                        : (tsvIs.getRemaining() > 0 ? PART : DONE);
                setups.add(mapIt("name", "tsv", "status", tsvStatus, "progress", tsvIs));
                if (tsvIs.getRemaining() > 0 && totalStatus.equals(DONE)) {
                    totalStatus = PART;
                }
            } else {
                setups.add(mapIt("name", "tsv", "status", NOTSTARTED, "progress", 0));
            }

            if(checkColumn("resume_tsv", "jss_contact", schemaname)){
	            IndexerStatus is = indexerManager.getStatus(orgName, false);
	            String indexStatus = indexerManager.isOn() ? RUNNING : (is.getRemaining() > 0 ? PART
	                    : DONE);
	            setups.add(mapIt("name", "resume", "status", indexStatus, "progress", is));
	            if (is.getRemaining() > 0 && totalStatus.equals(DONE)) {
	                totalStatus = PART;
	            }
            }else{
            	setups.add(mapIt("name", "resume", "status",PART, "progress", 0));
            }

            IndexerStatus sfIs = sfidManager.getStatus(orgName, false);
            String sfidStatus = sfidManager.isOn() ? RUNNING : (sfIs.getRemaining() > 0 ? PART
                    : DONE);
            setups.add(mapIt("name", "sfid", "status", sfidStatus, "progress", sfIs));

            if (sfIs.getRemaining() > 0 && totalStatus.equals(DONE)) {
                totalStatus = PART;
            }

            if(checkColumn("resume_lower", "jss_contact", schemaname)){
            	IndexerStatus resumeLower = resumeLowerManager.getStatus(orgName, false);
	            String resumeLowerStatus = resumeLower.getRemaining() > 0 ? DONE :PART; 
	            setups.add(mapIt("name", "resume_lower", "status",resumeLowerStatus, "progress", resumeLower));
	            
	            if (resumeLower.getRemaining() > 0 && totalStatus.equals(DONE)) {
	                totalStatus = PART;
	            }
            }else{
            	setups.add(mapIt("name", "resume_lower", "status",PART, "progress", 0));
            	totalStatus = PART;
            }
            
        } else {
            setups.add(mapIt("name", "resume", "status", NOTSTARTED, "progress", 0));
            setups.add(mapIt("name", "sfid", "status", NOTSTARTED, "progress", 0));
            setups.add(mapIt("name", "tsv", "status", NOTSTARTED, "progress", 0));
            setups.add(mapIt("name", "resume_lower", "status",PART, "progress", 0));
            if (totalStatus.equals(DONE)) {
                totalStatus = PART;
            }
        }
        //set the org index status
        int indexCount = getIndexStatus(orgName);
        int totalIndexCount = getTotalIndexCount(orgName);
        setups.add(mapIt("name", "indexes", "status", totalIndexCount > indexCount ? PART : DONE,
                "progress", new IndexerStatus(totalIndexCount - indexCount, indexCount)));
        if(currentOrgSetupStatus.get(orgName) != null && currentOrgSetupStatus.get(orgName).getCurrentIndex() != null){
        	setups.add(mapIt("name", "current_index", "value",currentOrgSetupStatus.get(orgName).getCurrentIndex(),
        			"status",indexCount==totalIndexCount?DONE:PART));
        }
        //set the orgCustomField index status
        int customFieldIndexCount = getCustomFieldIndexStatus(orgName);
        int customFieldTotalIndexCount = getCustomFieldTotalIndexCount(orgName);
        setups.add(mapIt("name", "customFieldIndexes", "status", customFieldTotalIndexCount > customFieldIndexCount ? PART : DONE,
                "progress", new IndexerStatus(customFieldTotalIndexCount - customFieldIndexCount, customFieldIndexCount)));
        if(currentOrgSetupStatus.get(orgName) != null && currentOrgSetupStatus.get(orgName).getCurrentCustomIndex() != null){
        	setups.add(mapIt("name", "current_custom_index", "value",currentOrgSetupStatus.get(orgName).getCurrentCustomIndex(),
        			"status",customFieldIndexCount==customFieldTotalIndexCount?DONE:PART));
        }
        if(orgSetupStatusMsg.get(orgName) != null && orgSetupStatusMsg.get(orgName).get("createIndex") != null){
        	setups.add(mapIt("name", "indexes", "status", ERROR,
        			"msg",  orgSetupStatusMsg.get(orgName).get("createIndex")));
        }
        Map<String,Integer> groupTableCount = orgGroupTableCountMap.get(orgName);
        if(groupTableCount == null){
        	groupTableCount = getGroupTableCountMap(orgName);
        }
        Map<String,Boolean> groupedTableInsertStatus = orgGroupTableInsertStatusMap.get(orgName);
        if(groupedTableInsertStatus == null){
         	groupedTableInsertStatus = getGroupTableStatusMap(orgName);
        }
        if(groupTableCount.get("jss_contact_jss_groupby_skills") == 0){
        	groupTableCount = getGroupTableCountMap(orgName);
        }
		int totalSkilltablesCount = groupTableCount.get("jss_contact_jss_groupby_skills");
		int SkilltablesCount = 0;
		if(groupedTableInsertStatus.get("jss_contact_jss_groupby_skills") != null && groupedTableInsertStatus.get("jss_contact_jss_groupby_skills")){
			 SkilltablesCount = totalSkilltablesCount;
		}
		if(SkilltablesCount == 0){
			 SkilltablesCount = getMtmTableseStatus(orgName,"jss_contact_jss_groupby_skills");
		}
		setups.add(mapIt("name", "skill", "status",
				totalSkilltablesCount > SkilltablesCount ? PART : DONE, "progress",
				totalSkilltablesCount > SkilltablesCount ? new IndexerStatus(totalSkilltablesCount - (SkilltablesCount/1000)*1000, (SkilltablesCount/1000)*1000) : new IndexerStatus(totalSkilltablesCount - SkilltablesCount, SkilltablesCount)));

		if(groupTableCount.get("jss_contact_jss_groupby_educations") == 0){
        	groupTableCount = getGroupTableCountMap(orgName);
        }
		int totalEducationtablesCount = groupTableCount.get("jss_contact_jss_groupby_educations");
		int EducationtablesCount = 0;
		if(groupedTableInsertStatus.get("jss_contact_jss_groupby_educations") != null && groupedTableInsertStatus.get("jss_contact_jss_groupby_educations")){
			EducationtablesCount = totalEducationtablesCount;
		}
        if(EducationtablesCount == 0){
			EducationtablesCount = getMtmTableseStatus(orgName,"jss_contact_jss_groupby_educations");
		}
		setups.add(mapIt("name", "education", "status",
				totalEducationtablesCount > EducationtablesCount ? PART : DONE, "progress",
				totalEducationtablesCount > EducationtablesCount ? new IndexerStatus(totalEducationtablesCount - (EducationtablesCount/1000)*1000, (EducationtablesCount/1000)*1000): new IndexerStatus(totalEducationtablesCount - EducationtablesCount, EducationtablesCount)));

		if(groupTableCount.get("jss_contact_jss_groupby_employers") == 0){
        	groupTableCount = getGroupTableCountMap(orgName);
        }
		int totalEmployertablesCount = groupTableCount.get("jss_contact_jss_groupby_employers");
		int EmployertablesCount = 0;
		if(groupedTableInsertStatus.get("jss_contact_jss_groupby_employers") != null && groupedTableInsertStatus.get("jss_contact_jss_groupby_employers")){
			EmployertablesCount = totalEmployertablesCount;
		}
		if(EmployertablesCount == 0){
			EmployertablesCount = getMtmTableseStatus(orgName,"jss_contact_jss_groupby_employers");
		}
		setups.add(mapIt("name", "employer", "status",
				totalEmployertablesCount > EmployertablesCount ? PART : DONE, "progress",
				totalEmployertablesCount > EmployertablesCount ? new IndexerStatus(totalEmployertablesCount - (EmployertablesCount/1000)*1000, (EmployertablesCount/1000)*1000):new IndexerStatus(totalEmployertablesCount - EmployertablesCount, EmployertablesCount)));
			
        if (((totalSkilltablesCount > SkilltablesCount)||(totalEducationtablesCount > EducationtablesCount)||(totalEmployertablesCount > EmployertablesCount)||(totalIndexCount > indexCount)||(customFieldTotalIndexCount > customFieldIndexCount)) && totalStatus.equals(DONE)) {
            totalStatus = PART;
        }

        String customFieldIndexRemove = getCustomIndexRemoveStr(orgName);
        if(customFieldIndexRemove.length() > 0){
        	totalStatus = PART;
        	status.put("RemoveIndex", customFieldIndexRemove);
        }
        
        if (orgSetupStatus.get(orgName) != null && orgSetupStatus.get(orgName)) {
            totalStatus = RUNNING;
        }
        status.put("status", totalStatus);
        status.put("setups", setups);
        return status;

    }
    // ---------- /organization setup interfaces ----------//

    // ---------- system setup interfaces ----------//
    // ---------- Thread ----------//
    public void systemSetup() {
        if (sysThread != null) {
            stopSystemSetup();
        }
        
        if (sysThread == null) {
            sysThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setupSystemSchema();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            sysThread.setName("sysThread");
            sysThread.start();
        }

    }

    public void stopSystemSetup() {
        if (sysThread != null) {
            sysThread.interrupt();
            sysThread = null;
        }
    }
    
    public void resetSysSetup() {
        try {
            sysReseting = true;
            if(cityConnection != null){
                cityConnection.abort();
                cityConnection = null;
            }
            if(cityWorldConnection != null){
                cityWorldConnection.abort();
                cityWorldConnection = null;
            }
            if(zipCodeConnection != null){
                zipCodeConnection.abort();
                zipCodeConnection = null;
            }
            stopSystemSetup();
            clearSysSetup();
            step = null;
            firstSetup = false;
            sysReseting = false;
        } catch (Exception e) {
            //FIXME: need once more
            try {
                clearSysSetup();
                step = null;
                firstSetup = false;
                sysReseting = false;
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private boolean isSystemSetupRunning() {
        if (sysThread != null && sysThread.isAlive()) {
            return true;
        }
        return false;
    }

    // ---------- /Thread ----------//

    public Map getSystemSetupStatus() {
        Map status = new HashMap();
        String statusKey = "status";
        status.put(statusKey, NOTSTARTED);

        Map initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("create_sys_schema", initMap);

        initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("check_missing_columns", initMap);

        initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("import_zipcode", initMap);

        initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("create_extension", initMap);

        initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("import_city", initMap);
        
        initMap = new HashMap();
        initMap.put(statusKey, NOTSTARTED);
        status.put("import_city_world", initMap);

        int steps = 0;
        try {
            Map result = getSysConfig();

            if (isSystemSetupRunning()) {
                status.put(statusKey, RUNNING);
            }
            
            if (sysReseting) {
                status.put("reseting", true);
            }

            if (!(Boolean) result.get("schema_create")) {
                return status;
            }

            Map<String, Boolean> tables = (Map) result.get("tables");
            Map stepCreateSysSchema = (Map) status.get("create_sys_schema");
            if (tables.get("org") || tables.get("config") || tables.get("city") || tables.get("city_world")
                    || tables.get("zipcode_us")) {
                if (tables.get("org") && tables.get("config") && tables.get("city") && tables.get("city_world")
                        && tables.get("zipcode_us")) {
                    stepCreateSysSchema.put(statusKey, DONE);
                    steps++;
                } else {
                    if (!firstSetup) {
                        stepCreateSysSchema.put(statusKey, INCOMPLETE);
                    } else {
                        stepCreateSysSchema.put(statusKey, NOTSTARTED);
                    }
                    status.put(statusKey, INCOMPLETE);
                }
            }

            Map stepImportZipcode = (Map) status.get("import_zipcode");
            boolean importZipcode = (Boolean) result.get("zipcode_import");
            if (importZipcode) {
                stepImportZipcode.put(statusKey, DONE);
                steps++;
            } else {
                if (!firstSetup) {
                    stepImportZipcode.put(statusKey, INCOMPLETE);
                } else {
                    stepImportZipcode.put(statusKey, NOTSTARTED);
                }
                status.put(statusKey, INCOMPLETE);
            }

            Map stepCreateExtension = (Map) status.get("create_extension");
            Map<String, Integer> extensions = (Map) result.get("extensions");
            if (extensions.get("earthdistance") > 0 || extensions.get("pgtrgm") > 0
                    || extensions.get("cube") > 0) {
                if (extensions.get("earthdistance") > 0 && extensions.get("pgtrgm") > 0
                        && extensions.get("cube") > 0) {
                    stepCreateExtension.put(statusKey, DONE);
                    steps++;
                } else {
                    if (!firstSetup) {
                        stepCreateExtension.put(statusKey, INCOMPLETE);
                        stepCreateExtension.put("extensions", extensions);
                    } else {
                        stepCreateExtension.put(statusKey, NOTSTARTED);
                    }
                    status.put(statusKey, INCOMPLETE);
                }
            }

            Map stepImportCity = (Map) status.get("import_city");
            boolean importCity = (Boolean) result.get("city");
            if (importCity) {
                stepImportCity.put(statusKey, DONE);
                steps++;
            } else {
                if (!firstSetup) {
                    stepImportCity.put(statusKey, INCOMPLETE);
                } else {
                    stepImportCity.put(statusKey, NOTSTARTED);
                }
                status.put(statusKey, INCOMPLETE);
            }
            
            Map stepImportCityWorld = (Map) status.get("import_city_world");
            boolean importCityWorld = (Boolean) result.get("city_world");
            if (importCityWorld) {
                stepImportCityWorld.put(statusKey, DONE);
                steps++;
            } else {
                if (!firstSetup) {
                    stepImportCityWorld.put(statusKey, INCOMPLETE);
                } else {
                    stepImportCityWorld.put(statusKey, NOTSTARTED);
                }
                status.put(statusKey, INCOMPLETE);
            }

            Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
            String missingsColumns = (String) result.get("jssTables");
            if (missingsColumns == null || missingsColumns.length() == 0) {
                stepCheckMissingColumns.put(statusKey, DONE);
                steps++;
            } else {
                if (!firstSetup) {
                    stepCheckMissingColumns.put(statusKey, INCOMPLETE);
                    stepCheckMissingColumns.put("missingColumns", missingsColumns);
                } else {
                    stepCheckMissingColumns.put(statusKey, NOTSTARTED);
                }
                status.put(statusKey, INCOMPLETE);
            }

            if (steps == status.size() - 1) {
                status.put(statusKey, DONE);
            } else {
                if (isSystemSetupRunning()) {
                    status.put(statusKey, RUNNING);
                    Map runningMap = (Map) status.get(step);
                    if(runningMap == null){
                     	runningMap = new HashMap();
                     	status.put(step, runningMap);
                     }
                    runningMap.put(statusKey, RUNNING);
                }
            }

            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }

    // ---------- /system setup interfaces ----------//

    // ---------- public methods ----------//
    
    /**
     * drop extension for db
     * 
     * @param extName
     * @return
     * @throws SQLException
     */
    public boolean dropExtension(String extName) throws SQLException {
        boolean result = false;

        if (checkExtension(extName) != 0) {
            result = true;
            daoRwHelper.executeUpdate(daoRwHelper.newRunner(), "drop extension  if  exists " + extName
                                    + "  cascade;");
        }

        return result;
    }

    public void clearSysSetup() throws SQLException {
        dropExtension("earthdistance");
        dropExtension("cube");
        dropExtension("pg_trgm");
        dropSysSchemaIfNecessary();
    }

    
    /**
     * create extension for public schema
     * 
     * @param extName
     * @return
     * @throws SQLException
     */
    public boolean createExtension(String extName) throws SQLException {
        boolean result = false;
        if (checkExtension(extName) > 0) {
            return true;
        }
        daoRwHelper.executeUpdate(daoRwHelper.newRunner(), "CREATE extension  if not exists "
                + extName + "  with schema pg_catalog;");
        return result;
    }

    /**
     * create system schema,will excute all the sql files under /jss_sys
     * 
     * @return
     * @throws SQLException
     */
    public boolean createSysSchema() throws Exception {
        return excuteSqlUnderSys("01_") && excuteSqlUnderSys("02_") && excuteSqlUnderSys("03_");
    }

    public boolean updateZipCode() throws Exception {
        return doUpdateZipCode(true) != 0;
    }

    /**
     * check the system tables existed or not
     * 
     * @return
     */
    public String checkSysTables() {
        List<Map> list = daoRwHelper
                .executeQuery(daoRwHelper.newRunner(),
                        "select string_agg(table_name,',') as names from information_schema.tables"
                                + " where table_schema='jss_sys' and table_type='BASE TABLE' and table_name in ('zipcode_us','org','config','city','city_world')");
        if (list.size() == 1) {
            String names = (String) list.get(0).get("names");
            if (names == null) {
                return "";
            }
            return names;
        }
        return "";
    }

    /**
     * create extra table for given ORG,will excute 01_create_extra.sql under
     * /org folder
     * 
     * @param orgName
     * @return
     * @throws SQLException
     */
    public boolean createExtraTables(String orgName) throws Exception {
        if (orgSetupStatus.get(orgName) != null && !orgSetupStatus.get(orgName).booleanValue()) {
            return false;
        }
        boolean result = true;
        File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for (File file : sqlFiles) {
            if (file.getName().startsWith("01_")) {// only load the
                                                   // 01_create_extra.sql
                List<String> subSqlList = loadSQLFile(file);
                allSqls.addAll(subSqlList);
            }
        }
        Runner runner = daoRwHelper.newOrgRunner(orgName);
        try {
            runner.startTransaction();
            for (String sql : allSqls) {
                // when stop the org setup,should jump out
                if (orgSetupStatus.get(orgName) != null && !orgSetupStatus.get(orgName).booleanValue()) {
                    runner.commit();
                    return false;
                }
                runner.executeUpdate(sql);
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
        } finally {
            runner.close();
        }
        return result;
    }
    
    /**
     * add and delete customField index on contact table by org searchConfig
     * @param orgName
     */
    private void updateContactCustomIndex(String orgName){
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
    	
    	/******** set the current index ********/
        CurrentOrgSetupStatus coss = currentOrgSetupStatus.get(orgName);
        if(coss == null){
        	coss = new CurrentOrgSetupStatus();
        	currentOrgSetupStatus.put(orgName, coss);
        }
        
    	//create all column index
    	List<String> contactColumns = getOrgCustomFieldCloumns(orgName);
		for(String columnName : contactColumns){
			String indexName = "jss_contact_customindex_"+columnName;
			if(allowCreateIndex(orgName, "contact", columnName) && !checkIndexexist(orgName, indexName)){
		        coss.setCurrentCustomIndex(indexName);
	    /********  /set the current index ********/
		        String sql = renderCustomIndex(orgName, "contact", columnName, indexName);
		        runner.executeUpdate(sql);
			}
		}
		//drop no use column index
		List<Map> customIndex = getTableIndexs(orgName,"contact","jss_contact_customindex_");
		for(int i = 0, length = customIndex.size(); i < length; i++){
			if(!contactColumns.contains(customIndex.get(i).get("index_name").toString().substring(24))){
				runner.executeUpdate("DROP INDEX if exists " + customIndex.get(i).get("index_name").toString());
			}
		}
		runner.close();
    }
    
    /**
     * get all the customField index name that need to delete 
     * @param orgName
     * @return
     */
    private String getCustomIndexRemoveStr(String orgName){
    	StringBuilder removeCustomIndex = new StringBuilder();
    	List<String> contactColumns = getOrgCustomFieldCloumns(orgName);
    	List<Map> customIndex = getTableIndexs(orgName,"contact","jss_contact_customindex_");
    	for(int i = 0, length = customIndex.size(); i < length; i++){
			if(!contactColumns.contains(customIndex.get(i).get("index_name").toString().substring(24))){
				removeCustomIndex.append(customIndex.get(i).get("index_name").toString()).append("; ");
			}
		}
    	return removeCustomIndex.toString();
    }
    
    private String renderCustomIndex(String orgName, String tableName, String columnName, String indexName){
    	StringBuilder sql = new StringBuilder();
    	sql.append("CREATE INDEX  ").append(indexName).append(" ON contact  USING ");
    	String column_type = getTableColumnDataType(orgName, tableName, columnName);
    	if("character varying".equals(column_type)){
    		sql.append("gin (\"").append(columnName).append("\" gin_trgm_ops)");
    	}else {
    		sql.append("btree (\"").append(columnName).append("\")");
    	}
    	return sql.toString();
    }
    
    private boolean allowCreateIndex(String orgName, String tableName, String columnName){
    	if(Arrays.asList(allowCustomIndexColumnName).contains(columnName)){
			return true;
		}
    	String column_type = getTableColumnDataType(orgName, tableName, columnName);
    	if(!Strings.isNullOrEmpty(column_type) && Arrays.asList(allowCustomIndexColumnType).contains(column_type)){
    			return true;
    	}
    	return false;
    }
    
    private String getTableColumnDataType(String orgName, String tableName, String columnName){
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
    	String sql = "SELECT data_type FROM information_schema.\"columns\" WHERE table_name='"+tableName+"' and table_schema = current_schema "
				+"and column_name = '"+columnName+"'";
		List<Map> list = runner.executeQuery(sql);
		runner.close();
		String column_type = null;
		if(list.size() == 1){
			column_type = list.get(0).get("data_type").toString().trim();
		}
		return column_type;
    }
    
    /**
     * create index for contact and jss_contact
     * @param orgName
     * @param contactEx
     * @return
     * @throws Exception
     */
    public boolean createIndexColumns(String orgName, boolean contactEx) throws Exception {
        if (!orgSetupStatus.get(orgName).booleanValue()) {
            return false;
        }
        boolean result = true;
        Map<String, JSONArray> m = getIndexMapFromJsonFile();
        Runner runner = daoRwHelper.newOrgRunner(orgName);
        try {
            if (contactEx && m.get("jss_contact") != null) {
                JSONArray ja = m.get("jss_contact");
                for (int i = 0; i < ja.size(); i++) {
                    if (!orgSetupStatus.get(orgName).booleanValue()) {
                        return false;
                    }
                    JSONObject jo = JSONObject.fromObject(ja.get(i));
                    runner.executeUpdate(generateIndexSql("jss_contact", jo,orgName));
                }
            } else {
                for (String key : m.keySet()) {
                    if (!key.equals("jss_contact")) {
                        JSONArray ja = m.get(key);
                        for (int i = 0; i < ja.size(); i++) {
                        	if (!orgSetupStatus.get(orgName).booleanValue()) {
                                return false;
                            }
                            JSONObject jo = JSONObject.fromObject(ja.get(i));
                            if(!jo.get("type").equals("pk")){
                                runner.executeUpdate(generateIndexSql(key, jo,orgName));
                            }
                        }
                    }
                }
                SearchConfiguration sc = scm.getSearchConfiguration(orgName);
                for (Filter f : sc.getFilters()) {
                	if (!orgSetupStatus.get(orgName).booleanValue()) {
                        return false;
                    }
                    if (f.getType() == null) {
                        FilterField ff = f.getFilterField();
                        JSONObject jo = new JSONObject();
                        jo.accumulate("name", String.format("%s_%s_index", f.getFilterField()
                                .getTable(), f.getFilterField().getColumn()));
                        jo.accumulate("column", ff.getColumn());
                        jo.accumulate("operator", "");
                        jo.accumulate("unique", "");
                        jo.accumulate("type", "btree");
                        if(jo.get("name") != null)
                        runner.executeUpdate(generateIndexSql(ff.getTable(), jo,orgName));
                    }
                }
            }
            result = true;
        } catch (Exception e) {
            result = false;
            log.error(e.getMessage());
            try {
                runner.roolback();
            } catch (Exception e1) {
                log.error(e1.getMessage());
            }
            Map<String,String> exceptionMsg = orgSetupStatusMsg.get(orgName);
            if(exceptionMsg != null){
            	if(exceptionMsg.get("createIndex") != null){
            		exceptionMsg.remove("createIndex");
            	}
            	exceptionMsg.put("createIndex", e.getMessage());
            }else{
                Map<String,String> exceptionMsgs = new HashMap<String,String>();
                exceptionMsgs.put("createIndex", e.getMessage());
                orgSetupStatusMsg.put(orgName, exceptionMsgs);
            }
            //stop setup thread
            stopOrgSetup(orgName);
            throw e;
        } finally {
            runner.close();
        }
        return result;
    }

    /**
     * create pk for jss_contact_jss_groupby_skills and jss_contact_jss_groupby_educations
     *               and jss_contact_jss_groupby_employers
     * 
     * @param orgName
     * @return
     * @throws SQLException
     */
    public boolean createPKOfmanyTManyTable(String orgName) throws Exception {
    	dropAllPkOfmanyTomanyTable(orgName);
    	boolean result = true;
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
    	Map<String, JSONArray> m = getIndexMapFromJsonFile();
    	try{
    		for (String key : m.keySet()) {
                    JSONArray ja = m.get(key);
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = JSONObject.fromObject(ja.get(i));
                        if(jo.get("type").equals("pk")){
                        	
                        	/******** set the current index ********/
                            CurrentOrgSetupStatus coss = currentOrgSetupStatus.get(orgName);
                            if(coss == null){
                            	coss = new CurrentOrgSetupStatus();
                            	currentOrgSetupStatus.put(orgName, coss);
                            }
                            
                            if(checkPkexist(orgName,jo.get("tablename").toString(),jo.get("pkname").toString())){
                            	dropPkOfmanyTomanyTable(orgName,jo.get("tablename").toString(),jo.get("pkname").toString());
    	                	}else{
                                coss.setCurrentIndex(jo.get("pkname").toString());
                            /********  /set the current index ********/
        	                	String sql = "ALTER TABLE "+jo.get("tablename")+" ADD CONSTRAINT "
   	                			     +jo.get("pkname")+" PRIMARY KEY("
   	                			     +jo.get("column")+")";
        	                	runner.executeUpdate(sql);
    	                	}
    	                }
                    }
            }
    	} finally {
    		runner.close();
    	}
    	return result;
    }
    
    public String removeWrongIndex(String orgName) throws Exception {
        if (!orgSetupStatus.get(orgName).booleanValue()) {
            return "";
        }
        StringBuilder sql = new StringBuilder();
        sql.append("select indexname from pg_indexes ").append("where indexname not in (")
                .append(getIndexesNamesAndTables()[0]).append(getOrgCustomFilterIndex(orgName))
                .append(") and tablename in(").append(getIndexesNamesAndTables()[1]).append(")")
                .append(" and indexname not ilike '%pkey%' and indexname not ilike 'jss_contact_customindex_%' and schemaname=current_schema ");
        List<Map> list = daoRwHelper
                .executeQuery(orgName, sql.toString());

        Runner runner = daoRwHelper.newOrgRunner(orgName);
        try {
            for (Map m : list) {
                if (!orgSetupStatus.get(orgName).booleanValue()) {
                    return "";
                }
                runner.executeUpdate(" drop index " + m.get("indexname") + " ;");
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                runner.roolback();
            } catch (Exception e1) {
                log.error(e1.getMessage());
            }
            throw e;
        } finally {
            runner.close();
        }
        return "";
    }
    
    /**
     * check the org extra tables are existed or not(main for
     * 'jss_contact','savedsearches','user')
     * 
     * @param orgName
     * @return
     */
    public List<Map> checkOrgExtra(String orgName) {
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }
        StringBuffer sb = new StringBuffer();
        for (String[] tables : newTableNameChanges) {
            sb.append(",'").append(tables[1]).append("'");
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
                "select unnest(ARRAY[" + sb.delete(0, 1) + "]) as tablename " + " EXCEPT "
                        + " select table_name as tablename from information_schema.tables"
                        + " where table_schema='" + schemaname
                        + "' and table_type='BASE TABLE' and table_name in (" + sb + ");");
        return list;
    }

    public int getMtmTableseStatus(String orgName,String tableName) {
    	int count = 0;
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
        try{
        	count = getDataCount(tableName,runner);
        }finally{
        	runner.close();
        }
        return count;
    }

    public List<String> getSqlCommandForOrg(String fileName) {
        return loadSQLFile(new File(getRootSqlFolderPath() + "/org/" + fileName));
    }

    public void dropExTables(String orgName) {
        daoRwHelper.executeUpdate(orgName, 
                "drop table if exists city_score;"
                        + "drop table if exists jss_grouped_employers;"
                        + "drop table if exists jss_grouped_educations;"
                        + "drop table if exists jss_grouped_skills");
    }
    
    public void rebuildResume(String orgName) {
    	daoRwHelper.executeUpdate(orgName, 
    			"ALTER TABLE jss_contact DROP COLUMN IF EXISTS \"resume_tsv\" CASCADE");
    }
   
    public void importCityWorld() throws Exception {
        String extraSysTables = checkSysTables();
        if (!extraSysTables.contains("city_world")) {
            excuteSqlUnderSys("01_");
        }
        try {

            CloseableHttpClient httpclient = HttpClients.createDefault();
            cityWorldConnection = new HttpGet(cityWorldPath);
            HttpResponse httpResponse = httpclient.execute(cityWorldConnection);
            ZipInputStream in = new ZipInputStream(httpResponse.getEntity().getContent());
            in.getNextEntry();
            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line = br.readLine();
            Runner runner = daoRwHelper.newSysRunner();
            try {
                runner.startTransaction();
                while (line != null) {
                    runner.executeUpdate(line);
                    line = br.readLine();
                }
                runner.commit();
            } catch (Exception e) {
                try {
                    runner.roolback();
                } catch (Exception e1) {
                    e.printStackTrace();
                }
                throw e;
            } finally {
                runner.close();
            }
            in.close();
            
            //FIXME CREATE index here now
            runner = daoRwHelper.newSysRunner();
            runner.executeUpdate("CREATE INDEX city_world_earth_distance_idx on city_world USING gist(ll_to_earth(latitude ,longitude));",new Object[0]);
            runner.close();
            
        } catch (IOException e) {
            throw e;
        }
    }
    
    public void computeCity() throws Exception {
        String extraSysTables = checkSysTables();
        if (!extraSysTables.contains("city")) {
            excuteSqlUnderSys("01_");
        }
        Runner runner = daoRwHelper.newSysRunner();
        runner.executeUpdate("insert into city(name,longitude,latitude)"
                + " select city,avg(longitude),avg(latitude) from zipcode_us group by city",
                new Object[0]);
        runner.close();
    }
    
    public void importCity() throws Exception {
        String extraSysTables = checkSysTables();
        if (!extraSysTables.contains("city")) {
            excuteSqlUnderSys("01_");
        }
        try {

            CloseableHttpClient httpclient = HttpClients.createDefault();
            cityConnection = new HttpGet(cityPath);
            HttpResponse httpResponse = httpclient.execute(cityConnection);
            ZipInputStream in = new ZipInputStream(httpResponse.getEntity().getContent());
            in.getNextEntry();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            Runner runner = daoRwHelper.newSysRunner();
            try {
                runner.startTransaction();
                while (line != null) {
                    runner.executeUpdate(line);
                    line = br.readLine();
                }
                runner.commit();
            } catch (Exception e) {
                try {
                    runner.roolback();
                } catch (Exception e1) {
                    e.printStackTrace();
                }
                throw e;
            } finally {
                runner.close();
            }
            in.close();
        } catch (IOException e) {
            throw e;
        }
    }

    public String checkTriggers(String orgName) {
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }
        List<Map> list = daoRwHelper
                .executeQuery(orgName, 
                        "select string_agg(trigger_name,',') as names from information_schema.triggers where trigger_schema='"
                                + schemaname + "'");
        if (list.size() == 1) {
            String names = (String) list.get(0).get("names");
            if (names == null) {
                return "";
            }
            return names;
        }
        return "";
    }
    
    /**
     * drop all the indexes created by jss
     * 
     * @param orgName
     */
    public int dropIndexes(String orgName) {
        Map<String, JSONArray> indexesInfo = getIndexMapFromJsonFile();
        int indexesCount = 0;
        for (String key : indexesInfo.keySet()) {
            JSONArray ja = indexesInfo.get(key);
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = JSONObject.fromObject(ja.get(i));
                if(!jo.get("type").equals("pk")){
                	if ("true".equals(jo.get("system"))) {
						continue;
					}
                    daoRwHelper.executeUpdate(orgName, 
                            "drop index if exists " + jo.getString("name"));
                    indexesCount++;
                }
            }
        }

        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        for (Filter f : sc.getFilters()) {
            if (f.getType() == null) {
                String indexName = f.getFilterField().getTable() + "_"
                        + f.getFilterField().getColumn() + "_index";
                daoRwHelper.executeUpdate(orgName, 
                        "drop index if exists " + indexName);
                indexesCount++;
            }
        }
        
        currentOrgSetupStatus.remove(orgName);
        return indexesCount;
    }

    /**
     * drop the pk of manyTomanyTable by jss
     * 
     * @param orgName
     */
    public void dropAllPkOfmanyTomanyTable(String orgName) {
    	Map<String, JSONArray> m = getIndexMapFromJsonFile();
		for (String key : m.keySet()) {
                JSONArray ja = m.get(key);
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = JSONObject.fromObject(ja.get(i));
                    if(jo.get("type").equals("pk")){
                    	 dropPkOfmanyTomanyTable(orgName,jo.get("tablename").toString(),jo.get("pkname").toString());
	                }
                }
        }
    }
    
    /**
     * drop the pk of manyTomanyTable
     * 
     * @param orgName
     */
    public void dropPkOfmanyTomanyTable(String orgName,String table,String pk) {
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
    	Map<String, JSONArray> m = getIndexMapFromJsonFile();
    	try{
    		for (String key : m.keySet()) {
                    JSONArray ja = m.get(key);
                    for (int i = 0; i < ja.size(); i++) {
                        JSONObject jo = JSONObject.fromObject(ja.get(i));
                        if(jo.get("type").equals("pk")){
                        	if(checkTableExist(orgName, jo.get("tablename").toString())){
        	                	String sql = "ALTER TABLE "+table+" DROP CONSTRAINT IF EXISTS "
   	                			     +pk+";";
   	                	        runner.executeUpdate(sql);
                        	}
    	                }
                    }
            }
    	} finally {
    		runner.close();
    	}
    }

    // ---------- /public methods ----------//
    

    /**
     * get the sys schema status
     * 
     * @return
     * @throws Exception
     */
    private Map getSysConfig() throws Exception {
        Map status = new HashMap();
        status.put("schema_create", checkSysSchema());
        String sysTableNames = this.checkSysTables();
        Map tableMap = new HashMap();
        tableMap.put("org", sysTableNames.contains("org"));
        tableMap.put("config", sysTableNames.contains("config"));
        tableMap.put("zipcode_us", sysTableNames.contains("zipcode_us"));
        tableMap.put("city", sysTableNames.contains("city"));
        tableMap.put("city_world", sysTableNames.contains("city_world"));
        status.put("tables", tableMap);

        status.put("city", checkCity());
        status.put("city_world", checkCityWorld());
        if (sysTableNames.contains("zipcode_us")) {
            status.put("zipcode_import", this.checkZipcodeImported());
        } else {
            status.put("zipcode_import", false);
        }

        Map extensionMap = new HashMap();
        extensionMap.put("pgtrgm", this.checkExtension("pg_trgm"));
        extensionMap.put("cube", this.checkExtension("cube"));
        extensionMap.put("earthdistance", this.checkExtension("earthdistance"));
        status.put("extensions", extensionMap);
        status.put("jssTable", checkColumns("jss_sys", "jss-sys-tables.def", true));
        return status;
    }

    /**
     * check a table own a column or not
     * 
     * @param columnName
     * @param table
     * @param schemaName
     * @return
     * @throws SQLException
     */
    private boolean checkColumn(String columnName, String table, String schemaName)
            throws SQLException {
        boolean result = false;
        List list = daoRwHelper.executeQuery(daoRwHelper.newRunner(),
                " select 1 from information_schema.columns "
                        + " where table_name =? and table_schema=?  and column_name=? ", table,
                schemaName, columnName);
        if (list.size() > 0) {
            result = true;
        }
        return result;
    }
    /**
     * excute the sql files under /sql/jss_sys
     * 
     * @param prefix
     *            if null,would excute all
     * @return
     * @throws Exception
     */
    private boolean excuteSqlUnderSys(String prefix) throws Exception {
        boolean result = true;
        createSysSchemaIfNecessary();
        File sysFolder = new File(getRootSqlFolderPath() + "/jss_sys");
        File[] sqlFiles = sysFolder.listFiles();
        List<String> allSqls = new ArrayList();
        for (File file : sqlFiles) {
            if (prefix == null || file.getName().startsWith(prefix)) {
                List subSqlList = loadSQLFile(file);
                allSqls.addAll(subSqlList);
            }
        }
        Runner runner = daoRwHelper.newSysRunner();
        try {
            runner.startTransaction();
            for (String sql : allSqls) {
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
        } finally {
        	daoRwHelper.updateDB(null);
            runner.close();
        }
        return result;
    }

    /**
     * generate the index sql from the indexes.json
     * 
     * @param tabelName
     * @param jo
     * @param orgName
     * @return
     */
    private String generateIndexSql(String tabelName, JSONObject jo,String orgName) {
        StringBuilder sb = new StringBuilder();
        
        /******** set the current index ********/
        CurrentOrgSetupStatus coss = currentOrgSetupStatus.get(orgName);
        if(coss == null){
        	coss = new CurrentOrgSetupStatus();
        	currentOrgSetupStatus.put(orgName, coss);
        }
        coss.setCurrentIndex(jo.get("name").toString());
        /********  /set the current index ********/
        
        sb.append(" DO $$  ").append("  BEGIN").append("    IF NOT EXISTS (")
                .append("        SELECT 1").append("        FROM   pg_class c")
                .append("        JOIN   pg_namespace n ON n.oid = c.relnamespace")
                .append("        WHERE  c.relname = '").append(jo.get("name"))
                .append("'        AND    n.nspname =   current_schema").append("        ) THEN")
                .append("       CREATE ")
                .append(jo.get("unique"))
                .append(" INDEX ").append(jo.get("name")).append("  ON ")
                .append(tabelName).append(" USING ").append(jo.get("type")).append(" ( ")
                .append(jo.get("column")).append(" ").append(jo.get("operator"))
                .append(");    END IF;").append("    END$$;");

        return sb.toString();
    }

    /**
     * 
     * @param tabelName
     * @param jo
     * @param withCollate when the withCollate if true ,do the create sql function,but for the false do the compare sql String
     * @return
     */
    private String generateIndexDef(String tabelName, JSONObject jo, boolean withCollate) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ")
        .append(jo.get("unique"))
        .append(" INDEX ").append(jo.get("name")).append("  ON ")
        .append(tabelName).append(" USING ").append(jo.get("type")).append(" (");
        if (withCollate) {
        	sb.append(jo.get("column"));
        }else {
        	sb.append(jo.get("column").toString().split(" ")[0]);
        }
        
        sb.append(" ").append(jo.get("operator"))
        .append(")");

        return sb.toString();
    }
    
    /**
     * Get the count for index count for contact and jss_contact
     * 
     * @param orgName
     * @return
     * @see {@link #createIndexColumns(String)}
     */
    private Integer getIndexStatus(String orgName) {
        StringBuilder sql = new StringBuilder();

        sql.append("select indexname,tablename,indexdef from pg_indexes ").append("where indexname in (")
                .append(getIndexesNamesAndTables()[0]).append(getOrgCustomFilterIndex(orgName))
                .append(") and schemaname=current_schema ");
        
        List<Map> list =daoRwHelper.executeQuery(orgName, sql.toString());
        Map<String, JSONArray> indexsDef = getIndexMapFromJsonFile();
        
        //check the index content
        int count = 0;
        for(Map m : list){
            JSONArray ja = indexsDef.get(m.get("tablename"));
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = JSONObject.fromObject(ja.get(i));
                if(!jo.get("type").equals("pk") && jo.get("name").equals(m.get("indexname"))){
                	if(generateIndexDef(m.get("tablename").toString(), jo, false)
                			.replaceAll("\\s", "").equalsIgnoreCase(
                					m.get("indexdef").toString().replaceAll("\\s", ""))){
                		count++;
                	}else{
                		System.out.println(generateIndexDef(m.get("tablename").toString(), jo, false)
                    			.replaceAll("\\s", ""));
                		System.out.println(m.get("indexdef").toString().replaceAll("\\s", ""));
                	}
                }
            }
        }
       //check the primarykey in manyTomanyTable
		for (String key : indexsDef.keySet()) {
                JSONArray ja = indexsDef.get(key);
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject jo = JSONObject.fromObject(ja.get(i));
                    if(jo.get("type").equals("pk")){
                    	if(checkPkexist(orgName,jo.get("tablename").toString(),jo.get("pkname").toString())){
	                		count++;
	                	}
	                }
                }
        }
        return count;
    }

    private Integer getCustomFieldIndexStatus(String orgName) {
    	int count = 0;
		//check the custom index
		List<String> columns = getOrgCustomFieldCloumns(orgName);
		HashSet<String> columnsSet = new HashSet<String>(columns);
		for(String columnName : columnsSet){
			String indexName = "jss_contact_customindex_"+columnName;
			if(allowCreateIndex(orgName, "contact", columnName) && checkIndexexist(orgName,indexName)){
				count++;
			}
		}
        return count;
    }
    
    /**
     * get the indexes for custom filter which defined in org search config
     * 
     * @param orgName
     * @return
     */
    private String getOrgCustomFilterIndex(String orgName) {
        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        StringBuilder sb = new StringBuilder();
        for (Filter f : sc.getFilters()) {
            if (f.getType() == null) {
                sb.append(",'" + f.getFilterField().getTable() + "_"
                        + f.getFilterField().getColumn() + "_index'");
            }
        }
        return sb.toString();
    }

    private int getTotalIndexCount(String orgName) throws Exception {
        int count = 0;
        Map<String, JSONArray> m = getIndexMapFromJsonFile();
        for (String key : m.keySet()) {
            JSONArray ja = m.get(key);
            for (int i = 0; i < ja.size(); i++) {
                count++;
            }
        }
        SearchConfiguration sc = scm.getSearchConfiguration(orgName);
        for (Filter f : sc.getFilters()) {
            if ("column".equals(f.getType())) {
                count++;
            }
        }
        return count;
    }

    private int getCustomFieldTotalIndexCount(String orgName) throws Exception {
    	int count = 0;
    	List<String> columns = getOrgCustomFieldCloumns(orgName);
    	HashSet<String> columnsSet = new HashSet<String>(columns);
        for(String column : columnsSet){
            if(allowCreateIndex(orgName, "contact", column)){
            	count++;
            }
        }
        return count;
    }
    
    private List<String> getOrgCustomFieldCloumns(String orgName){
 	   List<Map> orgs = orgConfigDao.getOrgByName(orgName);
       String schemaname = null;
       if (orgs.size() == 1) {
           schemaname = orgs.get(0).get("schemaname").toString();
       }
    	List<String> cloumns = new ArrayList<String>();
    	String orgConfigContent;
		try {
			orgConfigContent = scm.getMergedNodeContent(orgName);
			DocumentBuilder db  = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	        Document document = db.parse(new ByteArrayInputStream(orgConfigContent.getBytes()));
	        NodeList filterNodes = document.getElementsByTagName("filter");
	        if(filterNodes.getLength() > 0){
	        	for(int i = 0, j = filterNodes.getLength(); i < j; i++){
	        		Node filterField = filterNodes.item(i);
                    if(filterField.getNodeType() == 1 && checkAttribute(filterField, "display") && "side".equals(filterField.getAttributes().getNamedItem("display").getNodeValue())){
                    	NodeList customFields = filterField.getChildNodes();
                    	for(int m = 0, n = customFields.getLength(); m < n; m++){
                            Node customFieldNode = filterField.getChildNodes().item(m);
                            if(customFieldNode != null && customFieldNode.getNodeType() == 1){
                        		if(checkAttribute(customFieldNode, "table") && checkAttribute(customFieldNode, "column")){
                                	String column = customFieldNode.getAttributes().getNamedItem("column").getNodeValue();
                                	if(checkColumn(column,"contact",schemaname)){
         	                           cloumns.add(column);
         	                	   }
                            	}
                            }
                    	}   
                    }
	        	}
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cloumns;
    }
    
    private boolean checkAttribute(Node n, String name){
        if(n.getAttributes().getNamedItem(name) != null){
            return true;
        }
        return false;
    }
    
    private List<String> loadSQLFile(File file) {
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

    private int getDataCount(String table, Runner runner){
        int tableCount = runner.executeCount("select count(*) as count from information_schema.tables " 
                            + " where table_schema= current_schema "
                            +" and table_type='BASE TABLE' " + "and table_name =?",table);
        if(tableCount > 0){
            return runner.executeCount("select count(*) from "+table);
        }else {
            return 0;
        }
    }
    
    private int getContactMaxId(String table, String orgName){
    	
        int tableCount = daoRwHelper.executeCount(orgName, "select count(*) as count from information_schema.tables " 
                            + " where table_schema= current_schema "
                            +" and table_type='BASE TABLE' " + "and table_name =?",table);
        if(tableCount > 0){
            return daoRwHelper.executeCount(orgName, "select max(id) from "+table);
        }else {
            return 0;
        }
    }
    
	private boolean createExtraGroup(String orgName, String tableName)
			throws Exception {
		Runner runner = daoRwHelper.newOrgRunner(orgName);
		boolean result = true;
		try {
			if (!orgSetupStatus.get(orgName).booleanValue()) {
				return false;
			}
			// when there already has data
			if (getDataCount(tableName, runner) > 0) {
				return true;
			}
			File orgFolder = new File(getRootSqlFolderPath() + "/org");
			File[] sqlFiles = orgFolder.listFiles();
			List<String> allSqls = new ArrayList();
			String filePrexName = "";
			if (tableName.contains("skills")) {
				filePrexName = "06_";
			} else if (tableName.contains("educations")) {
				filePrexName = "07_";
			} else if (tableName.contains("employers")) {
				filePrexName = "08_";
			}
			for (File file : sqlFiles) {
				if (file.getName().startsWith(filePrexName)) {
					List<String> subSqlList = loadSQLFile(file);
					allSqls.addAll(subSqlList);
				}
			}
			runner.startTransaction();
			for (String sql : allSqls) {
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
		} finally {
			runner.close();
		}
		return result;
	}
	
	private boolean createExtraGroupCity(String orgName, String tableName){
        boolean result = true;
        Runner runner = daoRwHelper.newOrgRunner(orgName);
        try{
        	if (!orgSetupStatus.get(orgName).booleanValue()) {
				return false;
			}
        	// when the data already has been updated
        	IndexerStatus status = checkCityScoreStatus(orgName);
			if (status.getPerform() != -1 && status.getRemaining() == 0 ) {
				return true;
			}
	        File orgFolder = new File(getRootSqlFolderPath() + "/org");
	        File[] sqlFiles = orgFolder.listFiles();
	        List<String> subSqlList = null;
	        for (File file : sqlFiles) {
	            if (file.getName().startsWith("09_")) {
	                subSqlList = loadSQLFile(file);
	            }
	        }
	        if(!checkTableExist(orgName,"city_score")){
		        runner.executeUpdate(subSqlList.get(0));
	        }
	        if(getDataCount("city_score",daoRwHelper.newOrgRunner(orgName)) == 0){
		        runner.executeUpdate(subSqlList.get(1));
	        }
	        if(!checkIndexexist(orgName,"contact_earth_distance_idx")){
	            runner.executeUpdate(subSqlList.get(2));
	        }
	        if(!checkIndexexist(orgName,"city_score_earth_distance_idx")){
	            runner.executeUpdate(subSqlList.get(3));
	        }
	        //FIXME: need to split
	        int total = runner.executeCount(subSqlList.get(4));
	        int offset = 1;
	        if(status.getPerform() > 0){
	        	offset += status.getPerform();
	        }
	        for(; offset < total; ){
	        	if (!orgSetupStatus.get(orgName).booleanValue()) {
					return false;
				}
	            runner.executeUpdate(subSqlList.get(5),offset,offset+9999);
	            offset += 10000;
	        }
	        runner.executeUpdate(subSqlList.get(6));
        }catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            runner.close();
        }       
        return result;
    }

    private boolean checkCity() {
        boolean done = false;
        String extraSysTables = checkSysTables();
        if (extraSysTables.contains("city")) {
            done = true;
        } else {
            return done;
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
                "select count(*) from city");
        if (list.size() == 1) {
            if (!"0".equals(list.get(0).get("count").toString())) {
                done = true;
            } else {
                done = false;
            }
        }
        return done;
    }
    
    private boolean checkCityWorld() {
        boolean done = false;
        String extraSysTables = checkSysTables();
        if (extraSysTables.contains("city_world")) {
            done = true;
        } else {
            return done;
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
                "select count(*) from city_world");
        if (list.size() == 1) {
            if (!"0".equals(list.get(0).get("count").toString())) {
                done = true;
            } else {
                done = false;
            }
        }
        return done;
    }

    private Map<String, JSONArray> getIndexMapFromJsonFile() {
        if (indexesMap != null) {
            return indexesMap;
        }
        File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        String indexes = "";
        for (File file : sqlFiles) {
            if (file.getName().equals("indexes.json")) {// only load the
                                                        // indexes.json
                List<String> subSqlList = loadSQLFile(file);
                indexes = subSqlList.get(0);
            }
        }
        JSONObject indexesObj = JSONObject.fromObject(indexes);
        ConcurrentMap<String, JSONArray> m = new ConcurrentHashMap<String, JSONArray>();
        for (Object tableName : indexesObj.keySet()) {
            m.put(tableName.toString(), JSONArray.fromObject(indexesObj.get(tableName)));
        }
        indexesMap = m;
        return indexesMap;
    }

    private String[] getIndexesNamesAndTables() {
        StringBuilder names = new StringBuilder(), tables = new StringBuilder();
        Map<String, JSONArray> indexesInfo = getIndexMapFromJsonFile();
        for (String key : indexesInfo.keySet()) {
            JSONArray ja = indexesInfo.get(key);
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = JSONObject.fromObject(ja.get(i));
                names.append(",'").append(jo.get("name")).append("'");
                tables.append(",'").append(key).append("'");
            }
        }
        if (names.length() > 0) {
            names.delete(0, 1);
            tables.delete(0, 1);
        }
        return new String[] { names.toString(), tables.toString() };
    }

    /**
     * check the schema is existed or not for org
     * 
     * @param orgName
     * @return
     */
    private boolean checkSchema(String orgName) {
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newRunner(),
                "select count(*) as count from information_schema.schemata"
                        + " where schema_name='" + schemaname + "'");
        if (list.size() == 1) {
            if ("1".equals(list.get(0).get("count").toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * check the zipcode data imported completed or not first it would get from
     * cache,if not found,will get from the sql file on dropbox
     * 
     * @return
     * @throws Exception
     */
    private boolean checkZipcodeImported() throws Exception {
        Integer zipcodeLoadCount = (Integer) cache.getIfPresent("zipcodeLoadCount");
        if (zipcodeLoadCount == null) {
            zipcodeLoadCount = doUpdateZipCode(false);
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
                "select count(*) as count from zipcode_us");
        if (list.size() == 1) {
            if (list.get(0).get("count").toString().equals(zipcodeLoadCount + "")) {
                return true;
            }
        }
        return false;
    }

    /**
     * update zipcode data from bropbox
     * 
     * @param updateDb
     *            if<code>true</code> will insert into table,else just get the
     *            recordes count
     * @return
     * @throws SQLException
     * @throws IOException
     */
    private int doUpdateZipCode(boolean updateDb) throws Exception {
        try {
            int rowCount = 0;
            
            CloseableHttpClient httpclient = HttpClients.createDefault();
            zipCodeConnection = new HttpGet(zipcodePath);
            HttpResponse httpResponse = httpclient.execute(zipCodeConnection);
            ZipInputStream in = new ZipInputStream(httpResponse.getEntity().getContent());
            in.getNextEntry();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            // To match csv dataType, if columns change should change this
            Class[] dataTypes = new Class[] { String.class, String.class, String.class,
                    Double.class, Double.class, Integer.class, Integer.class };
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < dataTypes.length; i++) {
                if (i != 0) {
                    valueStr.append(",");
                }
                valueStr.append("?");
            }
            String sql = "INSERT INTO jss_sys.zipcode_us (" + line + ") values ("
                    + valueStr.toString() + ");";
            Runner runner = daoRwHelper.newRunner();
            try {
                runner.startTransaction();
                line = br.readLine();
                PQuery pq = runner.newPQuery(sql);
                while (line != null) {
                    if (!line.trim().equals("")) {
                        if (updateDb) {
                            String lineValue = line.replaceAll("\'", "").replaceAll("\"", "");
                            String[] valueStrs = lineValue.split(",");
                            Object[] values = new Object[dataTypes.length];
                            for (int i = 0; i < values.length; i++) {
                                if (dataTypes[i] == Double.class) {
                                    values[i] = new Double(valueStrs[i]);
                                } else if (dataTypes[i] == Integer.class) {
                                    values[i] = new Integer(valueStrs[i]);
                                } else {
                                    values[i] = valueStrs[i];
                                }
                            }
                            pq.executeUpdate(values);
                        }
                        rowCount++;
                    }
                    line = br.readLine();
                }
                if (updateDb) {
                    runner.commit();
                }
            } catch (Exception e) {
                log.error(e.getMessage());
                try {
                    runner.roolback();
                } catch (Exception e1) {
                    log.error(e1.getMessage());
                }
                throw e;
            } finally {
                runner.close();
            }
            in.close();
            cache.put("zipcodeLoadCount", rowCount);
            return rowCount;
        } catch (IOException e) {
            throw e;
        }
    }

    /**
     * check the extension by extension name
     * 
     * @param extName
     * @return
     */
    private int checkExtension(String extName) {
        List<Map> list = daoRwHelper
                .executeQuery(
                		daoRwHelper.newRunner(),
                        "select sum(case when nspname='pg_catalog' then 1 else 0 end) as pg,"
                                + "sum(case when nspname!='pg_catalog' then 1 else 0 end) as not_pg "
                                + "from pg_catalog.pg_extension e left join pg_catalog.pg_namespace n on e.extnamespace=n.oid"
                                + " and  extname='" + extName + "' ");
        if (list.size() == 1) {
            if ("1".equals(list.get(0).get("pg").toString())) {
                return 1;
            } else if ("1".equals(list.get(0).get("not_pg").toString())) {
                return 2;
            } else {
                return 0;
            }
        }
        return 0;
    }

    /**
     * check the city_score table status
     * 
     * @return
     */
    private IndexerStatus checkCityScoreStatus(String orgName){
    	if(!checkTableExist(orgName,"city_score")){
    		return new IndexerStatus(0,-1);
    	}
    	int total = 0;
    	int  perform = 0;
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
    	try{
        	total = runner.executeCount("select count(*) as count from city_score;");
        	perform = runner.executeCount("select count(*) as count from city_score where city_score.score is not null;");
    	}finally{
    		runner.close();
    	}
    	return new IndexerStatus(total-perform,perform);
    }
    
    private void fixJssTableNames(String orgName) {
        Runner runner = daoRwHelper.newOrgRunner(orgName);
        runner.startTransaction();
        for (String[] tables : newTableNameChanges) {
            runner.executeUpdate(" Alter table if exists \"" + tables[0] + "\" rename to \""
                    + tables[1] + "\";");
        }
        runner.commit();
        runner.close();
    }

    private boolean hasOldJssTableNames(String orgName, String schemaname) {
        StringBuilder sb = new StringBuilder();
        for (String[] tables : newTableNameChanges) {
            sb.append(",'").append(tables[0]).append("'");
        }
        List<Map> list = daoRwHelper.executeQuery(orgName, 
                "select count(*) as count from information_schema.tables " + "where table_schema='"
                        + schemaname + "' and table_type='BASE TABLE' " + "and table_name in ("
                        + sb.delete(0, 1).toString() + ")");
        if (list.size() == 1) {
            Integer count = Integer.valueOf(list.get(0).get("count").toString());
            return count > 0;
        }
        return false;
    }

    private void dropInvalidIndexes(String orgName){
    	Runner runner = daoRwHelper.newOrgRunner(orgName);
	    	try{
	    	StringBuilder sql = new StringBuilder();
	        sql.append("select indexname,tablename,indexdef from pg_indexes ").append("where indexname in (")
	                .append(getIndexesNamesAndTables()[0]).append(getOrgCustomFilterIndex(orgName))
	                .append(") and schemaname=current_schema ");
	        
	        List<Map> list =runner.executeQuery(sql.toString());
	        Map<String, JSONArray> indexsDef = getIndexMapFromJsonFile();
	        
	        //check the index content
	        for(Map m:list){
	            JSONArray ja = indexsDef.get(m.get("tablename"));
	            for (int i = 0; i < ja.size(); i++) {
	                JSONObject jo = JSONObject.fromObject(ja.get(i));
	                if(!jo.get("type").equals("pk")&&jo.get("name").equals(m.get("indexname"))){
	                	if(!generateIndexDef(m.get("tablename").toString(), jo, false)
	                			.replaceAll("\\s", "").equalsIgnoreCase(
	                					m.get("indexdef").toString().replaceAll("\\s", ""))){
	                		runner.executeUpdate("drop index if exists " + jo.getString("name"));
	                	}
	                }
	            }
	        }
    	}finally{
    		runner.close();
    	}
    }

    private String checkColumns(String schemaName, String fileName, boolean jssTable) {
        Map<String, JSONArray> arrays = loadJsonFile(fileName);
        Map columnsMap = getColumnsGroupbyTable(schemaName);
        StringBuilder result = new StringBuilder();
        for (String key : arrays.keySet()) {
            checkMissingColumns(result, arrays.get(key), (String) columnsMap.get(key), key,
                    jssTable);
        }
        return result.length() > 0 ? result.substring(1) : result.toString();
    }

    private Map<String, JSONArray> loadJsonFile(String name) {
        return loadJsonFile(name,"tabledef");
    }
    
    private Map<String, JSONArray> loadJsonFile(String name,String parentFolder) {
        if (jsonMap.containsKey(parentFolder+"_"+name)) {
            return jsonMap.get(parentFolder+"_"+name);
        }
        
        StringBuilder path = new StringBuilder(webAppFolder.getAbsolutePath());
        path.append("/WEB-INF/"+parentFolder);
        File orgFolder = new File(path.toString());
        File[] sqlFiles = orgFolder.listFiles();
        String indexes = "";
        for (File file : sqlFiles) {
            if (file.getName().equals(name)) {
                List<String> subSqlList = loadSQLFile(file);
                indexes = subSqlList.get(0);
            }
        }
        JSONObject indexesObj = JSONObject.fromObject(indexes);
        ConcurrentMap<String, JSONArray> m = new ConcurrentHashMap<String, JSONArray>();
        for (Object tableName : indexesObj.keySet()) {
            m.put(tableName.toString(), JSONArray.fromObject(indexesObj.get(tableName)));
        }
        jsonMap.put(parentFolder+"_"+name, m);
        return m;
    }

    private Map getColumnsGroupbyTable(String org) {
        List<Map> list = daoRwHelper
                .executeQuery(
                		daoRwHelper.newRunner(),
                        " select string_agg(\"column_name\",',') as column_string,table_name from information_schema.columns"
                                + "  where table_schema=? group by table_name", org);
        Map<String, String> columnsStringMap = new HashMap<String, String>();
        for (Map m : list) {
            columnsStringMap.put((String) m.get("table_name"), m.get("column_string") + ",");
        }
        return columnsStringMap;
    }

    private Map<String, Boolean> getGroupTableStatusMap(String orgName){
    	if(orgGroupTableInsertStatusMap.get(orgName)!=null)
    	   orgGroupTableInsertStatusMap.remove(orgName);
    	String[] tables = {"jss_contact_jss_groupby_skills","jss_contact_jss_groupby_educations","jss_contact_jss_groupby_employers"};
    	Map<String, Boolean> groupTableInsertStatusMap = new HashMap<String, Boolean>();
    	for(String table : tables){
    		if(checkPkexist(orgName, table, table+"_pkey")){
    			groupTableInsertStatusMap.put(table, true);
        	}else{
        		groupTableInsertStatusMap.put(table, false);
        	}
    	}
    	orgGroupTableInsertStatusMap.put(orgName, groupTableInsertStatusMap);
    	return groupTableInsertStatusMap;
    }
    
    private Map<String, Integer> getGroupTableCountMap(String orgName){
    	if(orgGroupTableCountMap.get(orgName)!=null)
    		orgGroupTableCountMap.remove(orgName);
    	String[] tables = {"jss_contact_jss_groupby_skills","jss_contact_jss_groupby_educations","jss_contact_jss_groupby_employers"};
    	Map<String, Integer> groupTableCountMap = new HashMap<String, Integer>();
    	for(String table : tables){
    		Integer count = getGroupTableCount(orgName, table);
    		groupTableCountMap.put(table, count);
    	}
    	orgGroupTableCountMap.put(orgName, groupTableCountMap);
    	return groupTableCountMap;
    }

    private Map<String, Integer> getGroupTableInsertStatusMap(String orgName){
    	if(orgGroupTableInsertScheduleMap.get(orgName)!=null){
    		return orgGroupTableInsertScheduleMap.get(orgName);
    	}
    	String[] tables = {"jss_contact_jss_groupby_skills","jss_contact_jss_groupby_educations","jss_contact_jss_groupby_employers"};
    	Map<String, Integer> groupTableInsertScheduleMap = new HashMap<String, Integer>();
    	for(String table : tables){
    		groupTableInsertScheduleMap.put(table, 0);
    	}
    	orgGroupTableInsertScheduleMap.put(orgName, groupTableInsertScheduleMap);
    	return groupTableInsertScheduleMap;
    }
    
    private int getGroupTableCount(String orgName,String table){
    	File orgFolder = new File(getRootSqlFolderPath() + "/org");
        File[] sqlFiles = orgFolder.listFiles();
        String sqlString = "";
        for (File file : sqlFiles) {
            if (file.getName().contains(table)) {
                List<String> subSqlList = loadSQLFile(file);
                sqlString = subSqlList.get(2);
                break;
            }
        }
        long count = 0;
        try{
           	List<Map> results = daoRwHelper.executeQuery(orgName, sqlString);
           	count = (Long)results.get(0).get("count");
        } catch (Exception e){
        } 
        return (int)count;
    }
    
    private StringBuilder checkMissingColumns(StringBuilder sb, JSONArray array,
            String columnsString, String tableName, boolean jssTable) {
        columnsString += ",";
        String temp;
        if (jssTable) {
            for (int i = 0, j = array.size(); i < j; i++) {
                temp = ((JSONObject) array.get(i)).getString("name");
                if (!columnsString.contains(temp + ",")) {
                    sb.append(", ").append(tableName).append(".").append(temp);
                }
            }
        } else {
            for (int i = 0, j = array.size(); i < j; i++) {
                temp = array.getString(i);
                if (!columnsString.contains(temp + ",")) {
                    sb.append(", ").append(tableName).append(".").append(temp);
                }
            }
        }
        return sb;
    }
    
    private boolean checkTableExist(String orgName,String table){
    	String sql = "SELECT *  FROM information_schema.tables WHERE table_schema = current_schema"
    	             +" AND "
    	             +" table_name = '"
    	             +table+"';";
    	List<Map> results = daoRwHelper.executeQuery(orgName, sql);
    	if(results.size() == 1){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    private void fixMissingColumns(String orgName, Boolean sys) {
        Map<String, JSONArray> arrays = loadJsonFile(sys ? "jss-sys-tables.def"
                : "org-jss-tables.def");
        Runner runner = sys ? daoRwHelper.newSysRunner() : daoRwHelper.newOrgRunner(orgName);
        runner.startTransaction();
        for (String key : arrays.keySet()) {
            JSONArray ja = arrays.get(key);
            for (int i = 0, j = ja.size(); i < j; i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                runner.executeUpdate("  DO $$ BEGIN BEGIN ALTER TABLE " + key + " ADD COLUMN "
                        + jo.getString("name") + " " + jo.getString("type")
                        + "; EXCEPTION WHEN duplicate_column THEN RAISE NOTICE 'column exists.';"
                        + " END; END;$$;");
            }
        }
        runner.commit();
        runner.close();
    }

    private void setupSystemSchema() throws Exception {
        Map status = getSystemSetupStatus();

        Map stepCreateSysSchema = (Map) status.get("create_sys_schema");
        String statusStr = (String) stepCreateSysSchema.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "create_sys_schema";
            createSysSchema();
        }

        Map stepImportZipcode = (Map) status.get("import_zipcode");
        statusStr = (String) stepImportZipcode.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "import_zipcode";
            updateZipCode();
        }

        Map stepCreateExtension = (Map) status.get("create_extension");
        statusStr = (String) stepCreateExtension.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "create_extension";
            createExtension("pg_trgm");
            createExtension("cube");
            createExtension("earthdistance");
        }

        Map stepImportCity = (Map) status.get("import_city");
        statusStr = (String) stepImportCity.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "import_city";
            importCity();
        }
        
        Map stepImportCityWorld = (Map) status.get("import_city_world");
        statusStr = (String) stepImportCityWorld.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "import_city_world";
            importCityWorld();
        }
        
        Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
        statusStr = (String) stepCheckMissingColumns.get("status");
        if (statusStr.equals(NOTSTARTED) || statusStr.equals(INCOMPLETE) || statusStr.equals(ERROR)) {
            step = "check_missing_columns";
            fixMissingColumns(null, true);
        }
        firstSetup = false;
    }
    
    private boolean checkSysSchema(){
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newRunner(),"select count(*) as count from information_schema.schemata" +
                " where schema_name='"+sysSchema+"'");
		if (list.size() == 1) {
			if ("1".equals(list.get(0).get("count").toString())) {
				return true;
			}
		}
        return false;
    }

	private void createSysSchemaIfNecessary() {
		if (!checkSysSchema()) {
			daoRwHelper.executeUpdate(daoRwHelper.newRunner(),
					"CREATE SCHEMA " + sysSchema + " AUTHORIZATION " + user,
					new Object[0]);
		}
		daoRwHelper.updateDB(null);
	}

	private void dropSysSchemaIfNecessary() {
		if (checkSysSchema()) {
			daoRwHelper.executeUpdate(daoRwHelper.newRunner(),
					"Drop SCHEMA " + sysSchema + " CASCADE ", new Object[0]);
		}
		daoRwHelper.updateDB(null);
	}
    
    private boolean checkTriggerContent(String orgName){
        boolean valid = false;
        Map<String,JSONArray> m = loadJsonFile("triggers.json","sql/org");
        StringBuilder sb = new StringBuilder();
        for(String key : m.keySet()){
        	sb.append(",'").append(key).append("'");
        }
        if(sb.length() == 0){
            return true;
        }
        List<Map> triggers = daoRwHelper.executeQuery(orgName, "select t.trigger_name,p.prosrc"
                + " from information_schema.triggers t join pg_proc p"
                + " on p.proname||'()' = regexp_replace(t.action_statement,'execute\\s+procedure\\s+(\\\".*\\\"\\.)?','','i')"
                + " and t.trigger_schema=current_schema"
                + " and t.trigger_name in("
                + sb.delete(0, 1)
                + ") join pg_namespace pn on p.pronamespace=pn.oid and pn.nspname = current_schema ");
        
        String databaseContent,fileContent;
        
        if(2*m.keySet().size() != triggers.size()){
        	return false;
        }
        
        for(Map trigger : triggers){
            fileContent = (String) JSONObject.fromObject(m.get(trigger.get("trigger_name")).get(0)).get("content");
            databaseContent = (String) trigger.get("prosrc");
			if (!fileContent.replaceAll("\\s+", "").equalsIgnoreCase(databaseContent.replaceAll("\\s+", ""))) {
				valid = false;
				return valid;
			}
        }
        valid = true;
        return valid;
    }
    
    private boolean checkPkexist(String orgName, String table, String pkName){
    	String querySql = "select count(*) as count from information_schema.table_constraints a "
                +"where a.constraint_type = 'PRIMARY KEY' and a.table_name = '"
		        +table+"' and a.constraint_name = '"
                +pkName+"' "
                +"AND    a.constraint_schema = current_schema";
		List<Map> lists = daoRwHelper.executeQuery(orgName, querySql);
		if (Integer.parseInt(lists.get(0).get("count")+"") == 1) {
			return true;
		}else{
			return false;
		}
    }
    
    private boolean checkIndexexist(String orgName,String indexName){
    	String querySql = "SELECT count(*) as count FROM   pg_class c "
                         +"JOIN   pg_namespace n ON n.oid = c.relnamespace "
                         +"WHERE  c.relname = '"+indexName+"' "
                         +"AND    n.nspname = current_schema";
		List<Map> lists = daoRwHelper.executeQuery(orgName, querySql);
		if (Integer.parseInt(lists.get(0).get("count")+"") == 1) {
			return true;
		}else{
			return false;
		}
    }
    
    private String getRootSqlFolderPath() {
        StringBuilder path = new StringBuilder(webAppFolder.getAbsolutePath());
        path.append("/WEB-INF/sql");
        return path.toString();
    }

    private List<Map> getTableIndexs(String orgName, String tableName, String indexPrefix){
    	String querySql = "SELECT t.relname as table_name, i.relname as index_name, a.attname as column_name "
                         +"FROM  pg_class t, pg_class i, pg_namespace n, pg_index ix, pg_attribute a "
                         +"WHERE  n.oid = t.relnamespace and t.oid = ix.indrelid and i.oid = ix.indexrelid and a.attrelid = t.oid and a.attnum = ANY(ix.indkey) and t.relkind = 'r' and t.relname = '"+tableName+"' and i.relname like '"+indexPrefix+"%'"     
                         +"AND n.nspname = current_schema";
		List<Map> lists = daoRwHelper.executeQuery(orgName, querySql);
		return lists;
    }
    
    class CurrentOrgSetupStatus{
    	private String orgName;
    	private String currentIndex;
    	private String currentCustomIndex;
    	
    	public void setOrgName(String orgName) {
			this.orgName = orgName;
		}
    	
    	public String getOrgName() {
			return orgName;
		}
    	
    	public void setCurrentIndex(String currentIndex) {
			this.currentIndex = currentIndex;
		}
    	
    	public String getCurrentIndex() {
			return currentIndex;
		}

		public String getCurrentCustomIndex() {
			return currentCustomIndex;
		}

		public void setCurrentCustomIndex(String currentCustomIndex) {
			this.currentCustomIndex = currentCustomIndex;
		}
    }
    
    // ---------- public methods(not use) ----------//
    
    public String getWrongIndex(String orgName) {
        StringBuilder sql = new StringBuilder();
        sql.append("select string_agg(tablename||'.'||indexname,', ') as indexes from pg_indexes ")
                .append("where indexname not in (").append(getIndexesNamesAndTables()[0])
                .append(getOrgCustomFilterIndex(orgName)).append(") and tablename in(")
                .append(getIndexesNamesAndTables()[1]).append(")")
                .append(" and indexname not ilike '%pkey%' and schemaname=current_schema ");
        List<Map> list = daoRwHelper
                .executeQuery(orgName, sql.toString());
        if (list.size() == 1) {
            return list.get(0).get("indexes") == null ? "" : list.get(0).get("indexes").toString();
        }
        return "";
    }

    public boolean recreateTriggers(String orgName) throws Exception{
        return createExtraTables(orgName);
    }

    public boolean hasOrgTable(String orgName, String tableName) {
        List<Map> orgs = orgConfigDao.getOrgByName(orgName);
        String schemaname = "";
        if (orgs.size() == 1) {
            schemaname = orgs.get(0).get("schemaname").toString();
        }
        List<Map> list = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(),
                "select count(*) as count from information_schema.tables" + " where table_schema='"
                        + schemaname + "' and table_type='BASE TABLE' and table_name=?", tableName);
        if (list.size() == 1) {
            return (Long) list.get(0).get("count") > 0;
        }
        return false;
    }
    
    public void computeCityWorld() throws Exception {
        String extraSysTables = checkSysTables();
        if (!extraSysTables.contains("city_world")) {
            excuteSqlUnderSys("01_");
        }
    }
 
    // ---------- /public methods(not use) ----------//
}