package com.jobscience.search.web;

import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.jobscience.search.dao.SavedSearchesDao;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class SavedSearchesWebHandlers {

    @Inject
    private SavedSearchesDao savedSearchesDao;



    @WebGet("/getSavedSearches")
    public WebResponse search(@WebParam("offset") Integer offset, @WebParam("limit") Integer limit) {
        if (offset == null) {
            offset = 0;
        }
        if (limit == null) {
            limit = 6;
        }
        List<Map> map = savedSearchesDao.getSavedSearches(offset, limit);
        return WebResponse.success(map);
    }

    @WebPost("/saveSavedSearches")
    public WebResponse save(@WebParam("name") String name, @WebParam("content") String content) {
        try {
            savedSearchesDao.saveSavedSearches(name, content);
            return WebResponse.success();
        } catch (Exception e) {
            return WebResponse.fail(e.getMessage());
        }
    }

    @WebPost("/deleteSavedSearches")
    public WebResponse delete(@WebParam("id") Long id) {
        savedSearchesDao.deleteSavedSearches(id);
        return WebResponse.success();
    }
}
