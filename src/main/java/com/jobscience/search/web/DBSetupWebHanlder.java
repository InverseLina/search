package com.jobscience.search.web;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.IOException;
import java.sql.SQLException;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.ContactTsvManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.IndexerManager;
import com.jobscience.search.dao.SfidManager;
import com.jobscience.search.exception.JSSSqlException;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private IndexerManager indexerManager;
    
    @Inject
    private SfidManager sfidManager;
    
    @Inject
    private ContactTsvManager contactTsvManager;
    @WebPost("/createSysSchema")
    public WebResponse createSysSchema(){
       try{
    	   dbSetupManager.createSysSchema();
       }catch (Exception e) {
           e.printStackTrace();
    	   return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
       }
       return WebResponse.success();
    }
    
    @WebPost("/updateZipCode")
    public WebResponse updateZipCode() {
        try{
        	dbSetupManager.updateZipCode();
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
		} catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    
    @WebPost("/createExtraGrouped")
    public WebResponse createExtraGrouped(@WebParam("orgName")String orgName,@WebParam("tableName")String tableName){
       try{
            return WebResponse.success(dbSetupManager.createExtraGroup(orgName,tableName));
       }catch (SQLException e) {
           return WebResponse.success(new JSSSqlException(e) );
       } catch (Exception e) {
           return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
       }
    }
    
    @WebGet("/checkSysSchema")
    public WebResponse checkSysSchema (){
        try{
             return WebResponse.success(dbSetupManager.getSysConfig());
        }catch (SQLException e) {
            return WebResponse.success(new JSSSqlException(e) );
        } catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()) );
        }
    }
    
    @WebGet("/checkOrgSchema")
    public WebResponse checkOrgSchema (@WebParam("org")String orgName,@WebParam("quick")Boolean quick){
        if(quick==null){
            quick = false;
        }
        try{
             return WebResponse.success(dbSetupManager.getOrgConfig(orgName,quick));
        }catch (SQLException e) {
            return WebResponse.success(new JSSSqlException(e) );
        } catch (IOException e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()) );
        }
    }
    
    @WebPost("/createExtraTables")
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
        try{
        	dbSetupManager.createExtraTables(orgName);
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
        }catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    
    @WebPost("/createPgTrgm")
    public WebResponse createExtraTables() {
        try{
        	dbSetupManager.createExtension("pg_trgm");
        	dbSetupManager.createExtension("cube");
        	dbSetupManager.createExtension("earthdistance");
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
        }
        return WebResponse.success();
    }
    
    @WebPost("/createIndexColumns")
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName,@WebParam("contactEx")Boolean contactEx) {
        try{
            if(contactEx==null){
                contactEx = false;
            }
        	dbSetupManager.createIndexColumns(orgName,contactEx);
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
        }catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    
    @WebGet("/getIndexColumnsStatus")
    public WebResponse getIndexColumnsStatus(@WebParam("orgName")String orgName,@WebParam("contactEx")Boolean contactEx){
        if(contactEx==null){
            contactEx = false;
        }
        return WebResponse.success(mapIt("created",dbSetupManager.getIndexStatus(orgName,contactEx),"all",dbSetupManager.getTotalIndexCount(orgName,false)));
    }
    
    @WebPost("/createIndexResume")
    public WebResponse createIndexResume(@WebParam("orgName")String orgName) {
    	 try{
    		 indexerManager.run(orgName);
    	 }catch (Exception e) {
       	    return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return WebResponse.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/copySfid")
    public WebResponse copySfid(@WebParam("orgName")String orgName) {
         try{
             sfidManager.run(orgName);
         }catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return WebResponse.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebGet("/getSfidStatus")
    public WebResponse getSfidStatus(@WebParam("orgName")String orgName) {
        return WebResponse.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCopySfid")
    public WebResponse stopCopySfid(@WebParam("orgName")String orgName) {
        sfidManager.stop();
        return WebResponse.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebGet("/getResumeIndexStatus")
    public WebResponse getStatus(@WebParam("orgName")String orgName) {
        return WebResponse.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCreateIndexResume")
    public WebResponse createIndesxResume(@WebParam("orgName")String orgName) {
    	indexerManager.stop();
        return WebResponse.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/createContactTsv")
    public WebResponse createContactTsv(@WebParam("orgName")String orgName) {
         try{
             contactTsvManager.run(orgName);
         }catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return WebResponse.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebGet("/getContactTsvStatus")
    public WebResponse getContactTsvStatus(@WebParam("orgName")String orgName) {
        return WebResponse.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCreateContactTsv")
    public WebResponse stopCreateContactTsv(@WebParam("orgName")String orgName) {
        contactTsvManager.stop();
        return WebResponse.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebGet("/getWrongIndexes")
    public WebResponse getWrongIndexes(@WebParam("orgName")String orgName){
        return WebResponse.success(dbSetupManager.getWrongIndex(orgName));
    }
    
    @WebPost("/removeWrongIndexes")
    public WebResponse removeWrongIndex(@WebParam("orgName")String orgName){
        try {
            return WebResponse.success(dbSetupManager.removeWrongIndex(orgName));
        } catch (SQLException e) {
            return WebResponse.success(new JSSSqlException(e));
        }catch (Exception e) {
            return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
    }
    
    @WebPost("/removeAllIndexes")
    public WebResponse removeAllIndexes(@WebParam("orgName")String orgName){
        return WebResponse.success(dbSetupManager.dropIndexes(orgName));
    }
    
    @WebPost("/dropExTables")
    public WebResponse dropExTables(@WebParam("orgName")String orgName){
        return WebResponse.success(dbSetupManager.dropExTables(orgName));
    }
   
}