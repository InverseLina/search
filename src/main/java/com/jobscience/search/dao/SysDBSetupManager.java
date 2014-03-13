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
                dbSetupManager.createSysSchema();
            }

            Map stepImportZipcode = (Map) status.get("import_zipcode");
            statusStr = (String) stepImportZipcode.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                dbSetupManager.updateZipCode();
            }

            Map stepCreateExtension = (Map) status.get("create_extension");
            statusStr = (String) stepCreateExtension.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                dbSetupManager.createExtension("pg_trgm");
                dbSetupManager.createExtension("cube");
                dbSetupManager.createExtension("earthdistance");
            }

            Map stepImportCity = (Map) status.get("import_city");
            statusStr = (String) stepImportCity.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                dbSetupManager.importCity();
            }

            Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
            statusStr = (String) stepCheckMissingColumns.get("status");
            if (statusStr.equals("notstarted") || statusStr.equals("incomplete") || statusStr.equals("error")) {
                dbSetupManager.fixMissingColumns(null, true);
            }
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
            if(tables.get("org") || tables.get("config") || tables.get("city")){
                if(tables.get("org") && tables.get("config") && tables.get("city")){
                    stepCreateSysSchema.put(statusKey, "done");
                }else{
                    if(isRunning()){
                        stepCreateSysSchema.put(statusKey, "running");
                        status.put(statusKey, "running"); 
                    }else{
                        stepCreateSysSchema.put(statusKey, "incomplete");
                        status.put(statusKey, "incomplete");
                    }
                    return status;
                }
            }else{
                if(isRunning()){
                    stepCreateSysSchema.put(statusKey, "running");
                    status.put(statusKey, "running");
                }
                return status;
            }
            
            Map stepImportZipcode = (Map) status.get("import_zipcode");
            boolean importZipcode = (Boolean) result.get("zipcode_import");
            if(importZipcode){
                stepImportZipcode.put(statusKey, "done");
            }else{
                if(isRunning()){
                    stepImportZipcode.put(statusKey, "running");
                    status.put(statusKey, "running");
                }else{
                    stepImportZipcode.put(statusKey, "incomplete");
                    status.put(statusKey, "incomplete");
                }
                return status;
            }
            

            Map stepCreateExtension = (Map) status.get("create_extension");
            Map<String, Integer> extensions = (Map) result.get("extensions");
            if(extensions.get("earthdistance") > 0 || extensions.get("pgtrgm") > 0 || extensions.get("cube") > 0){
                if(extensions.get("earthdistance") > 0 && extensions.get("pgtrgm") > 0 && extensions.get("cube") > 0){
                    stepCreateExtension.put(statusKey, "done");
                }else{
                    if(isRunning()){
                        stepCreateExtension.put(statusKey, "running");
                        status.put(statusKey, "running");
                    }else{
                        stepCreateExtension.put(statusKey, "incomplete");
                        status.put(statusKey, "incomplete");
                        stepCreateExtension.put("extensions", extensions);
                    }
                    return status;
                }
            }else{
                if(isRunning()){
                    stepCreateExtension.put(statusKey, "running");
                    status.put(statusKey, "running");
                }
                return status;
            }

            Map stepImportCity = (Map) status.get("import_city");
            boolean importCity = (Boolean) result.get("city");
            if(importCity){
                stepImportCity.put(statusKey, "done");
            }else{
                if(isRunning()){
                    stepImportCity.put(statusKey, "running");
                    status.put(statusKey, "running");
                }else{
                    stepImportCity.put(statusKey, "incomplete");
                    status.put(statusKey, "incomplete");
                }
            }
            
            Map stepCheckMissingColumns = (Map) status.get("check_missing_columns");
            String missingsColumns = (String) result.get("jssTables");
            if(missingsColumns ==null || missingsColumns.length() == 0){
                stepCheckMissingColumns.put(statusKey, "done");
                status.put(statusKey, "done");
            }else{
                if(isRunning()){
                    stepCheckMissingColumns.put(statusKey, "running");
                    status.put(statusKey, "running");
                }else{
                    stepCheckMissingColumns.put(statusKey, "incomplete");
                    status.put(statusKey, "incomplete");
                    status.put("missingColumns", missingsColumns);
                }
                return status;
            }
            
            return status;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return status;
    }
    
    
}    