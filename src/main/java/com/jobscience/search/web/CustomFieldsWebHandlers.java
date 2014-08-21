package com.jobscience.search.web;

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
    	WebResponse result = webResponseBuilder.success(customFields);
    	return result;
    }
    
    @WebGet("/getCustomFieldAutoCompleteData")
    public WebResponse getCustomFieldAutoCompleteData(@WebParam("fieldName") String fieldName, @WebParam("searchText") String searchText){
    	List<Map> fieldData = customFieldsDao.getCustomFieldCompleteData(orgHolder.getCurrentOrg(),fieldName, searchText);
    	WebResponse wr = webResponseBuilder.success(fieldData);
        return wr;
    }
}
