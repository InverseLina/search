package com.jobscience.search.web;

import java.io.IOException;
import java.sql.SQLException;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.IndexerManager;
import com.jobscience.search.dao.SchemaType;
import com.jobscience.search.dao.SetupStatus;
import com.jobscience.search.exception.JSSSqlException;

@Singleton
public class DBSetupWebHanlder {

    @Inject
    private DBSetupManager dbSetupManager;

    @Inject
    private IndexerManager indexerManager;
    
    @WebPost("/createSysSchema")
    public WebResponse createSysSchema(){
       try{
    	   dbSetupManager.createSysSchema();
       }catch (SQLException e) {
    	   return WebResponse.success(new JSSSqlException(e));
       }
       return WebResponse.success();
    }
    
    @WebPost("/updateZipCode")
    public WebResponse updateZipCode() {
        try{
        	dbSetupManager.updateZipCode();
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
        } catch (IOException e) {
        	 return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
		}
        return WebResponse.success();
    }
    
    /**
     * Get the DB setup status
     * @param type SYSTEM for jss_sys schema,the the ORG for org schema
     * @param orgName 
     * @return
     * @see SetupStatus
     */
    @WebGet("/checkSetupStatus")
    public WebResponse checkSetupStatus(@WebParam("type")SchemaType type,@WebParam("orgName")String orgName){
        try{
        	 return WebResponse.success(dbSetupManager.checkSetupStatus(type,orgName));
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
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName) {
        try{
        	dbSetupManager.createIndexColumns(orgName);
        }catch (SQLException e) {
     	   return WebResponse.success(new JSSSqlException(e));
        }
        return WebResponse.success();
    }
    
    @WebGet("/getIndexColumnsStatus")
    public WebResponse getIndexColumnsStatus(@WebParam("orgName")String orgName){
       return WebResponse.success(dbSetupManager.getIndexCount(orgName));
    }
    
    @WebPost("/createIndexResume")
    public WebResponse createIndexResume(@WebParam("orgName")String orgName) {
    	 try{
    		 indexerManager.run(orgName);
    	 }catch (Exception e) {
       	    return WebResponse.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
    
    @WebGet("/getResumeIndexStatus")
    public WebResponse getStatus(@WebParam("orgName")String orgName) {
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
    
    @WebPost("/stopCreateIndexResume")
    public WebResponse createIndesxResume(@WebParam("orgName")String orgName) {
    	indexerManager.stop();
        return WebResponse.success(indexerManager.getStatus(orgName));
    }
}