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
import com.jobscience.search.dao.DaoHelper;

@Singleton
public class TriggerTestWebHandlers {


    @Inject
    private DaoHelper daoHelper;

    
    @WebGet("/test/getOrgs")
    public WebResponse queryOrgs() {

    	List<Map> map = daoHelper.executeQuery(daoHelper.openNewSysRunner(), "select * from org");
    	return WebResponse.success(map);
    }

    @WebPost("/test/saveContact")
    public WebResponse saveContact(RequestContext rc, @WebParam("org") String org,
                                   @WebParam("skills") String skills,  @WebParam("educations") String educations,
                                   @WebParam("companies") String companies) {
        Map contact = rc.getParamMap("test.");
        String sfid = RandomStringUtils.random(18, "01234567890abcdedfhijklmnopqrst");
        contact.put("sfid", sfid);
        daoHelper.insert(daoHelper.openNewOrgRunner(org), "contact", contact);
        skills = skills.trim();
        if (skills.length() > 0) {
            String[] skill = skills.split(",");
            for (String s : skill) {
                daoHelper.insert(org, "insert into ts2__skill__c (ts2__contact__c, ts2__skill_name__c) values(?, ?)", sfid, s);
            }
        }
        educations = educations.trim();
        if (educations.length() > 0) {
            String[] education = educations.split(",");
            for (String s : education) {
                daoHelper.insert(org, "insert into ts2__education_history__c (ts2__contact__c, ts2__name__c) values(?, ?)", sfid, s);
            }
        }
        companies = companies.trim();
        if (companies.length() > 0) {
            String[] company = companies.split(",");
            for (String s : company) {
                daoHelper.insert(org, "insert into ts2__employment_history__c (ts2__contact__c, ts2__name__c) values(?, ?)", sfid, s);
            }
        }
        return WebResponse.success();
    }
}
