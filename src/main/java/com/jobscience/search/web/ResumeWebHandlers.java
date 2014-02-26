package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.organization.OrgContextManager;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;

@Singleton
public class ResumeWebHandlers {


    @Inject
    private DaoHelper daoHelper;
    @Inject
    private OrgContextManager orgHolder;

    @com.google.inject.Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebGet("/getResume")
    public WebResponse search(@WebParam("cid") Long cid) {
    	String sql = "select \"name\", \"ts2__text_resume__c\" from  contact where id = ?";
    	List<Map> map = daoHelper.executeQuery(orgHolder.getOrgName(), sql, cid);
        return webResponseBuilder.success(map);
    }
}
