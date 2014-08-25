package com.jobscience.search.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.jobscience.search.dao.CustomFieldsDao;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class CustomFieldsWebHandlers {

	@Inject
    private OrgContextManager orgHolder;

	@Inject
	private WebResponseBuilder webResponseBuilder;
	
	@Inject
	private CustomFieldsDao customFieldsDao;
    
    @WebGet("/getCustomFields")
    public WebResponse getCustomFields(){
    	List<Map> customFields = customFieldsDao.getCustomFields(orgHolder.getCurrentOrg());
    	if(customFields.size() > 10){
    		customFields = customFields.subList(0, 10);
    	}
    	return webResponseBuilder.success(customFields);
    }
    
    @WebGet("/getCustomFieldAutoCompleteData")
    public WebResponse getCustomFieldAutoCompleteData(@WebParam("fieldName") String fieldName, @WebParam("searchText") String searchText){
    	List<Map> fieldData = customFieldsDao.getCustomFieldCompleteData(orgHolder.getCurrentOrg(),fieldName, searchText);
    	Map result = new HashMap();
    	result.put("data", fieldData);
    	result.put("searchText", searchText);
    	WebResponse wr = webResponseBuilder.success(result);
        return wr;
    }
}
