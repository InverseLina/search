package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.db.DBHelper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class ResumeWebHandlers {


    @Inject
    private DBHelper dbHelper;

    @Inject
    private CurrentOrgHolder orgHolder;
    
    
    
    @WebGet("/getResume")
    public WebResponse search(@WebParam("cid") Long cid) {
    	
    	String sql = "select \"Name\", \"ts2__Text_Resume__c\" from "+orgHolder.getSchema()+".contact where id = ?";
        
    	List<Map> map = dbHelper.executeQuery(sql, cid);
        
    	return WebResponse.success(map);
    }
}
