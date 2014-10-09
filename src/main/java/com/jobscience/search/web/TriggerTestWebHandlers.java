package com.jobscience.search.web;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.RandomStringUtils;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.auth.RequireAdmin;
import com.jobscience.search.dao.DaoRwHelper;

@Singleton
public class TriggerTestWebHandlers {


    @Inject
    private DaoRwHelper daoRwHelper;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    
    @WebGet("/test/getOrgs")
    @RequireAdmin
    public WebResponse queryOrgs() {

    	List<Map> map = daoRwHelper.executeQuery(daoRwHelper.newSysRunner(), "select * from org");
        return webResponseBuilder.success(map);
    }

    @WebPost("/test/saveContact")
    @RequireAdmin
    public WebResponse saveContact(RequestContext rc, @WebParam("org") String org,
                                   @WebParam("skills") String skills,  @WebParam("educations") String educations,
                                   @WebParam("companies") String companies) {
        Map contact = rc.getParamMap("test.");
        String sfid = RandomStringUtils.random(18, "01234567890abcdedfhijklmnopqrst");
        contact.put("sfid", sfid);
        daoRwHelper.insert(daoRwHelper.newOrgRunner(org), "contact", contact);
        skills = skills.trim();
        if (skills.length() > 0) {
            String[] skill = skills.split(",");
            for (String s : skill) {
                daoRwHelper.create(org, "insert into ts2__skill__c (ts2__contact__c, ts2__skill_name__c) values(?, ?)  returning id", sfid, s);
            }
        }
        educations = educations.trim();
        if (educations.length() > 0) {
            String[] education = educations.split(",");
            for (String s : education) {
                daoRwHelper.create(org, "insert into ts2__education_history__c (ts2__contact__c, ts2__name__c) values(?, ?)  returning id", sfid, s);
            }
        }
        companies = companies.trim();
        if (companies.length() > 0) {
            String[] company = companies.split(",");
            for (String s : company) {
                daoRwHelper.create(org, "insert into ts2__employment_history__c (ts2__contact__c, ts2__name__c) values(?, ?)  returning id", sfid, s);
            }
        }
        return webResponseBuilder.success();
    }
}
