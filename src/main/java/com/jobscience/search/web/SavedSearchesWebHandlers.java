package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.SavedSearchesDao;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.List;
import java.util.Map;

@Singleton
public class SavedSearchesWebHandlers {

    @Inject
    private SavedSearchesDao savedSearchesDao;
    @Inject
    private CurrentOrgHolder orgHolder;

    @Inject
    private WebResponseBuilder webResponseBuilder;
    
    @WebGet("/listSavedSearches")
    public WebResponse list(@WebParam("offset") Integer offset, @WebParam("limit") Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 999;
        }
        List<Map> map = savedSearchesDao.list(offset, limit,orgHolder.getCurrentOrg());
        return webResponseBuilder.success(map);
    }

    @WebPost("/saveSavedSearches")
    public WebResponse save(@WebParam("name") String name, @WebParam("content") String content) {
        try {
            savedSearchesDao.save(name, content,orgHolder.getCurrentOrg());
            return webResponseBuilder.success();
        } catch (Exception e) {
            e.printStackTrace();
            return webResponseBuilder.fail(e.getMessage());
        }
    }

    @WebPost("/deleteSavedSearches")
    public WebResponse delete(@WebParam("id") Long id) {
        savedSearchesDao.delete(id,orgHolder.getCurrentOrg());
        return webResponseBuilder.success();
    }
    @WebGet("/countSavedSearches")
    public WebResponse count(@WebParam("name") String name) {
        int result = savedSearchesDao.count(name,orgHolder.getCurrentOrg());
        return webResponseBuilder.success(result);
    }
    @WebGet("/getOneSavedSearches")
    public WebResponse get(@WebParam("id") Long id) {
        Map map = savedSearchesDao.get(id,orgHolder.getCurrentOrg());
        return webResponseBuilder.success(map);
    }
}
