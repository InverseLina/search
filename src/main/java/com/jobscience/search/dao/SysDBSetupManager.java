package com.jobscience.search.dao;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SysDBSetupManager {
    
    private volatile Thread sysThread;
    @Inject
    private DBSetupManager dbSetupManager;
    
    private boolean firstSetup = false;
    private String step = null;
    
    // ---------- Thread ----------//
    public void startSetup(boolean force){
        if(sysThread != null && force){
                stop();
        }
        
        if(sysThread == null){
            sysThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setup();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            sysThread.setName("sysThread");
            sysThread.start();
        }
        
    }
    
    public void stop(){
        if(sysThread != null){
            sysThread.interrupt();
            sysThread = null;
        }
    }
    
    private boolean isRunning(){
        if(sysThread != null && sysThread.isAlive()){
            return true;
        }
        return false;
    }
    
    // ---------- /Thread ----------//

    private void setup() throws Exception{
        if (isRunning()) {
            Map status = getSetupStatus();

            Map stepCreateSysSchema = (Map) status.get("create_sys_schema");
            String statusStr = (String) stepCreateSysSchema.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                step = "create_sys_schema";
                dbSetupManager.createSysSchema();
            }

            Map stepImportZipcode = (Map) status.get("import_zipcode");
            statusStr = (String) stepImportZipcode.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                step = "import_zipcode";
                dbSetupManager.updateZipCode();
            }

            Map stepCreateExtension = (Map) status.get("create_extension");
            statusStr = (String) stepCreateExtension.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                step = "create_extension";
                dbSetupManager.createExtension("pg_trgm");
                dbSetupManager.createExtension("cube");
                dbSetupManager.createExtension("earthdistance");
            }

            Map stepImportCity = (Map) status.get("import_city");
            statusStr = (String) stepImportCity.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                step = "import_city";
                dbSetupManager.importCity();
            }

            Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
            statusStr = (String) stepCheckMissingColumns.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                step = "check_missing_columns";
                dbSetupManager.fixMissingColumns(null, true);
            }
            
            firstSetup = false;
        }
            
    }
    
    public Map getSetupStatus(){
        Map status = new HashMap();
        String statusKey = "status";
        status.put(statusKey, "notstarted");
        
        Map initMap = new HashMap();
        initMap.put(statusKey, "notstarted");
        status.put("create_sys_schema", initMap);
        
        initMap = new HashMap();
        initMap.put(statusKey, "notstarted");
        status.put("check_missing_columns", initMap);
        
        initMap = new HashMap();
        initMap.put(statusKey, "notstarted");
        status.put("import_zipcode", initMap);
        
        initMap = new HashMap();
        initMap.put(statusKey, "notstarted");
        status.put("create_extension", initMap);
        
        initMap = new HashMap();
        initMap.put(statusKey, "notstarted");
        status.put("import_city", initMap);
        
        int steps = 0;
        try {
            Map result = dbSetupManager.getSysConfig();
            
            if(isRunning()){
                status.put(statusKey, "running"); 
            }
            
            if(!(Boolean) result.get("schema_create")){
                return status;
            }
            
            Map<String, Boolean> tables = (Map) result.get("tables");
            Map stepCreateSysSchema = (Map) status.get("create_sys_schema");
            if(tables.get("org") || tables.get("config") || tables.get("city") || tables.get("zipcode_us")){
                if(tables.get("org") && tables.get("config") && tables.get("city") && tables.get("zipcode_us")){
                    stepCreateSysSchema.put(statusKey, "done");
                    steps++;
                }else{
                    if (!firstSetup) {
                        stepCreateSysSchema.put(statusKey, "incomplete");
                    } else {
                        stepCreateSysSchema.put(statusKey, "notstarted");
                    }
                    status.put(statusKey, "incomplete");
                }
            }
            
            Map stepImportZipcode = (Map) status.get("import_zipcode");
            boolean importZipcode = (Boolean) result.get("zipcode_import");
            if(importZipcode){
                stepImportZipcode.put(statusKey, "done");
                steps++;
            }else{
                if (!firstSetup) {
                    stepImportZipcode.put(statusKey, "incomplete");
                } else {
                    stepImportZipcode.put(statusKey, "notstarted");
                }
                status.put(statusKey, "incomplete");
            }
            

            Map stepCreateExtension = (Map) status.get("create_extension");
            Map<String, Integer> extensions = (Map) result.get("extensions");
            if(extensions.get("earthdistance") > 0 || extensions.get("pgtrgm") > 0 || extensions.get("cube") > 0){
                if(extensions.get("earthdistance") > 0 && extensions.get("pgtrgm") > 0 && extensions.get("cube") > 0){
                    stepCreateExtension.put(statusKey, "done");
                    steps++;
                }else{
                    if (!firstSetup) {
                        stepCreateExtension.put(statusKey, "incomplete");
                        stepCreateExtension.put("extensions", extensions);
                    } else {
                        stepCreateExtension.put(statusKey, "notstarted");
                    }
                    status.put(statusKey, "incomplete");
                }
            }

            Map stepImportCity = (Map) status.get("import_city");
            boolean importCity = (Boolean) result.get("city");
            if(importCity){
                stepImportCity.put(statusKey, "done");
                steps++;
            }else{
                if (!firstSetup) {
                    stepImportCity.put(statusKey, "incomplete");
                } else {
                    stepImportCity.put(statusKey, "notstarted");
                }
                status.put(statusKey, "incomplete");
            }
            
            Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
            String missingsColumns = (String) result.get("jssTables");
            if(missingsColumns ==null || missingsColumns.length() == 0){
                stepCheckMissingColumns.put(statusKey, "done");
                steps++;
            }else{
                if (!firstSetup) {
                    stepCheckMissingColumns.put(statusKey, "incomplete");
                    stepCheckMissingColumns.put("missingColumns", missingsColumns);
                } else {
                    stepCheckMissingColumns.put(statusKey, "notstarted");
                }
                status.put(statusKey, "incomplete");
            }
            
            if(steps == 5){
                status.put(statusKey, "done");
            }else{
                if(isRunning()){
                    status.put(statusKey, "running");
                    Map runningMap = (Map) status.get(step);
                    runningMap.put(statusKey, "running");
                }
            }
            
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }
    
    
}    