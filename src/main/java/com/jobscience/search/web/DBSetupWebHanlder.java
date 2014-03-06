package com.jobscience.search.web;

import static com.britesnow.snow.util.MapUtil.mapIt;

import java.io.IOException;
import java.sql.SQLException;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.auth.RequireAdmin;
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
    private WebResponseBuilder webResponseBuilder;
    
    @Inject
    private ContactTsvManager contactTsvManager;
    
    @WebPost("/createSysSchema")
    @RequireAdmin
    public WebResponse createSysSchema(){
       try{
    	   dbSetupManager.createSysSchema();
       }catch (Exception e) {
           e.printStackTrace();
    	   return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
       }
       return webResponseBuilder.success();
    }
    
    @WebPost("/updateZipCode")
    @RequireAdmin
    public WebResponse updateZipCode() {
        try{
        	dbSetupManager.updateZipCode();
        }catch (SQLException e) {
     	   return webResponseBuilder.success(new JSSSqlException(e));
		} catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/createExtraGrouped")
    @RequireAdmin
    public WebResponse createExtraGrouped(@WebParam("orgName")String orgName,@WebParam("tableName")String tableName){
       try{
            return webResponseBuilder.success(dbSetupManager.createExtraGroup(orgName,tableName));
       }catch (SQLException e) {
           return webResponseBuilder.success(new JSSSqlException(e) );
       } catch (Exception e) {
           return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
       }
    }
    
    @WebGet("/checkSysSchema")
    @RequireAdmin
    public WebResponse checkSysSchema (){
        try{
             return webResponseBuilder.success(dbSetupManager.getSysConfig());
        }catch (SQLException e) {
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(e) );
        } catch (Exception e) {
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()) );
        }
    }
    
    @WebGet("/checkOrgSchema")
    @RequireAdmin
    public WebResponse checkOrgSchema (@WebParam("org")String orgName,@WebParam("quick")Boolean quick){
        if(quick==null){
            quick = false;
        }
        try{
             return webResponseBuilder.success(dbSetupManager.getOrgConfig(orgName,quick));
        }catch (SQLException e) {
            return webResponseBuilder.success(new JSSSqlException(e) );
        } catch (IOException e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()) );
        }
    }
    
    @WebPost("/createExtraTables")
    @RequireAdmin
    public WebResponse createExtraTables(@WebParam("orgName")String orgName) {
        try{
        	dbSetupManager.createExtraTables(orgName);
        }catch (SQLException e) {
     	   return webResponseBuilder.success(new JSSSqlException(e));
        }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/createPgTrgm")
    @RequireAdmin
    public WebResponse createExtraTables() {
        try{
        	dbSetupManager.createExtension("pg_trgm");
        	dbSetupManager.createExtension("cube");
        	dbSetupManager.createExtension("earthdistance");
        }catch (SQLException e) {
     	   return webResponseBuilder.success(new JSSSqlException(e));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/createIndexColumns")
    @RequireAdmin
    public WebResponse createIndexColumns(@WebParam("orgName")String orgName,@WebParam("contactEx")Boolean contactEx) {
        try{
            if(contactEx==null){
                contactEx = false;
            }
        	dbSetupManager.createIndexColumns(orgName,contactEx);
        }catch (SQLException e) {
     	   return webResponseBuilder.success(new JSSSqlException(e));
        }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebGet("/getIndexColumnsStatus")
    @RequireAdmin
    public WebResponse getIndexColumnsStatus(@WebParam("orgName")String orgName,@WebParam("contactEx")Boolean contactEx){
        if(contactEx==null){
            contactEx = false;
        }
        return webResponseBuilder.success(mapIt("created",dbSetupManager.getIndexStatus(orgName,contactEx),"all",dbSetupManager.getTotalIndexCount(orgName,false)));
    }
    
    @WebPost("/createIndexResume")
    @RequireAdmin
    public WebResponse createIndexResume(@WebParam("orgName")String orgName) {
    	 try{
    		 indexerManager.run(orgName);
    	 }catch (Exception e) {
       	    return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return webResponseBuilder.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/copySfid")
    @RequireAdmin
    public WebResponse copySfid(@WebParam("orgName")String orgName) {
         try{
             sfidManager.run(orgName);
         }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return webResponseBuilder.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebGet("/getSfidStatus")
    @RequireAdmin
    public WebResponse getSfidStatus(@WebParam("orgName")String orgName) {
        return webResponseBuilder.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCopySfid")
    @RequireAdmin
    public WebResponse stopCopySfid(@WebParam("orgName")String orgName) {
        sfidManager.stop();
        return webResponseBuilder.success(sfidManager.getStatus(orgName,false));
    }
    
    @WebGet("/getResumeIndexStatus")
    @RequireAdmin
    public WebResponse getStatus(@WebParam("orgName")String orgName) {
        return webResponseBuilder.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCreateIndexResume")
    @RequireAdmin
    public WebResponse createIndesxResume(@WebParam("orgName")String orgName) {
    	indexerManager.stop();
        return webResponseBuilder.success(indexerManager.getStatus(orgName,false));
    }
    
    @WebPost("/createContactTsv")
    @RequireAdmin
    public WebResponse createContactTsv(@WebParam("orgName")String orgName) {
         try{
             contactTsvManager.run(orgName);
         }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
         }
        return webResponseBuilder.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebGet("/getContactTsvStatus")
    @RequireAdmin
    public WebResponse getContactTsvStatus(@WebParam("orgName")String orgName) {
        return webResponseBuilder.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebPost("/stopCreateContactTsv")
    @RequireAdmin
    public WebResponse stopCreateContactTsv(@WebParam("orgName")String orgName) {
        contactTsvManager.stop();
        return webResponseBuilder.success(contactTsvManager.getStatus(orgName,false));
    }
    
    @WebGet("/getWrongIndexes")
    @RequireAdmin
    public WebResponse getWrongIndexes(@WebParam("orgName")String orgName){
        return webResponseBuilder.success(dbSetupManager.getWrongIndex(orgName));
    }
    
    @WebPost("/removeWrongIndexes")
    @RequireAdmin
    public WebResponse removeWrongIndex(@WebParam("orgName")String orgName){
        try {
            return webResponseBuilder.success(dbSetupManager.removeWrongIndex(orgName));
        } catch (SQLException e) {
            return webResponseBuilder.success(new JSSSqlException(e));
        }catch (Exception e) {
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
    }
    
    @WebPost("/removeAllIndexes")
    @RequireAdmin
    public WebResponse removeAllIndexes(@WebParam("orgName")String orgName){
        return webResponseBuilder.success(dbSetupManager.dropIndexes(orgName));
    }
    
    @WebPost("/dropExTables")
    @RequireAdmin
    public WebResponse dropExTables(@WebParam("orgName")String orgName){
        dbSetupManager.dropExTables(orgName);
        return webResponseBuilder.success();
    }
    
    @WebPost("/computeCity")
    @RequireAdmin
    public WebResponse computeCity(@WebParam("orgName")String orgName){
        try{
            dbSetupManager.computeCity();
        }catch(Exception e){
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/importCity")
    @RequireAdmin
    public WebResponse importCity(@WebParam("orgName")String orgName){
        try{
            dbSetupManager.importCity();
        }catch(Exception e){
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
   
    @WebPost("/fixJssTableNames")
    @RequireAdmin
    public WebResponse fixJssTableNames(@WebParam("orgName")String orgName){
        try{
            dbSetupManager.fixJssTableNames(orgName);
        }catch(Exception e){
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
    @WebPost("/fixJssColumns")
    @RequireAdmin
    public WebResponse fixJssColumns(@WebParam("orgName")String orgName,@WebParam("sys")Boolean sys){
        if(sys==null){
            sys = false;
        }
        try{
            dbSetupManager.fixMissingColumns(orgName,sys);
        }catch(Exception e){
            e.printStackTrace();
            return webResponseBuilder.success(new JSSSqlException(-1,e.getLocalizedMessage()));
        }
        return webResponseBuilder.success();
    }
    
}