package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.DaoHelper;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;

@Singleton
public class ResumeWebHandlers {


    @Inject
    private DaoHelper dbHelper;
    @Inject
    private CurrentOrgHolder orgHolder;
    
    @WebGet("/getResume")
    public WebResponse search(@WebParam("cid") Long cid) {
    	String sql = "select \"name\", \"ts2__text_resume__c\" from  contact where id = ?";
    	List<Map> map = dbHelper.executeQuery(orgHolder.getOrgName(), sql, cid);
    	return WebResponse.success(map);
    }
}
