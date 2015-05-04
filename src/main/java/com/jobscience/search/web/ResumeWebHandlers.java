package com.jobscience.search.web;

import java.util.ArrayList;
import java.util.HashMap;
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
        // the keyword maybe has the exact word event if without all parameters contain quotes,like "Account Manager" And Director
        boolean hasExact = false;
        boolean hasNotExact = false;
        Map map = null;
        List<String> hasExactValue = new ArrayList<String>();
        List<String> notExactValue = new ArrayList<String>();
        // note that to fix exact issue for now
        //if (!exact) {
            String[] queryArray = keyword.trim().replaceAll("(NOT|OR|AND)", "|").split("\\|");
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<queryArray.length;i++){
                String query = queryArray[i].trim();
                if(query.matches("^\\s*\"[\\s\\S]+\"\\s*$")){
                    hasExact = true;
                    hasExactValue.add(query.trim());
                }else if(query.contains(" ")){
                    hasNotExact = true;
                    notExactValue.add(query.trim());
                }else{
                    sb.append(" "+query);
                }
            }
            String queryString = sb.toString().replaceAll("\\s+", "|").replaceAll("^[|]+", "").replaceAll("[|]+$", "");
            String sql = "select \"name\", ts_headline(\"ts2__text_resume__c\", plainto_tsquery(?), \'StartSel=\"<span class=\"\"highlight\"\">\", StopSel=\"</span>\", HighlightAll=true\') as resume from  contact where id = ?";
            result = daoRwHelper.executeQuery(orgHolder.getOrgName(), sql, queryString, cid);
        //} else {
        //    String sql = "select \"name\", \"ts2__text_resume__c\" as resume from  contact where id = ?";
        //    result = daoRwHelper.executeQuery(orgHolder.getOrgName(), sql, cid);
        //}
        if(result.size() > 0){
            map = result.get(0);
            map.put("exact", exact);
            if(hasExact){
                map.put("hasExact",hasExact);
                map.put("hasExactValue",hasExactValue);
            }if(hasNotExact){
                map.put("hasNotExact",hasNotExact);
                map.put("notExactValue",notExactValue);
            }
        }
        return webResponseBuilder.success(map);
    }
}
