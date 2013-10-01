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
    	   return WebResponse.success(new DBSetupResult(e.getErrorCode(),e.getLocalizedMessage()));
       }
       return WebResponse.success();
    }
    
    @WebPost("/updateZipCode")
    public WebResponse updateZipCode() {
        try{
        	dbSetupManager.updateZipCode();
        }catch (SQLException e) {
     	   return WebResponse.success(new DBSetupResult(e.getErrorCode(),e.getLocalizedMessage()));
        } catch (IOException e) {
        	 return WebResponse.success(new DBSetupResult(-1,e.getLocalizedMessage()));
		}
        return WebResponse.success();
    }
    
    @WebGet("/checkSetupStatus")
    public WebResponse checkSetupStatus(@WebParam("type")SchemaType type,@WebParam("orgName")String orgName){
        try{
        	 return WebResponse.success(dbSetupManager.checkSetupStatus(type,orgName));
        }catch (SQLException e) {
        	return WebResponse.success(new DBSetupResult(3,e.getNextException().getLocalizedMessage()) );
		} catch (IOException e) {
			return WebResponse.success(new DBSetupResult(-1,e.getLocalizedMessage()) );
		}
    }
  
    @WebPost("/createExtraTables")
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
        try{
        	dbSetupManager.createExtraTables(orgName);
        }catch (SQLException e) {
     	   return WebResponse.success(new DBSetupResult(e.getErrorCode(),e.getNextException().getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    
    @WebPost("/createPgTrgm")
    public WebResponse createExtraTables() {
        try{
        	dbSetupManager.createExtension("pg_trgm");
        }catch (SQLException e) {
     	   return WebResponse.success(new DBSetupResult(e.getErrorCode(),e.getNextException().getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    @WebPost("/createIndexColumns")
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName) {
        try{
        	dbSetupManager.createIndexColumns(orgName);
        }catch (SQLException e) {
     	   return WebResponse.success(new DBSetupResult(e.getErrorCode(),e.getNextException().getLocalizedMessage()));
        }
        return WebResponse.success();
    }
    
    @WebPost("/createIndexResume")
    public WebResponse createIndexResume(@WebParam("orgName")String orgName) {
    	 try{
    		 indexerManager.run(orgName);
    	 }catch (Exception e) {
       	    return WebResponse.success(new DBSetupResult(-1,e.getLocalizedMessage()));
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