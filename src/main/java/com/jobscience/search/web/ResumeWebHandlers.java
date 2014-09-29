package com.jobscience.search.web;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.DaoRwHelper;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class ResumeWebHandlers {


    @Inject
    private DaoRwHelper daoRwHelper;
    @Inject
    private OrgContextManager orgHolder;

    @com.google.inject.Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebGet("/getResume")
    public WebResponse search(@WebParam("cid") Long cid, @WebParam("keyword") String keyword) {
    	boolean exact = keyword.matches("^\\s*\"[\\s\\S]+\"\\s*$");
        List<Map> result = null;
        Map map = null;
        if (!exact) {
            String query = keyword.trim().replaceAll("\"", "").replaceAll("(NOT|OR|AND)", "").replaceAll("\\s+", "|");
            query = query.replaceAll("^[|]+", "").replaceAll("[|]+$", "");
            String sql = "select \"name\", ts_headline(\"ts2__text_resume__c\", to_tsquery(?), \'StartSel=\"<span class=\"\"highlight\"\">\", StopSel=\"</span>\", HighlightAll=true\') as resume from  contact where id = ?";
            result = daoRwHelper.executeQuery(orgHolder.getOrgName(), sql, query, cid);
        } else {
            String sql = "select \"name\", \"ts2__text_resume__c\" as resume from  contact where id = ?";
            result = daoRwHelper.executeQuery(orgHolder.getOrgName(), sql, cid);
        }
        if(result.size() > 0){
            map = result.get(0);
            map.put("exact", exact);
        }
        return webResponseBuilder.success(map);
    }
}
