package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Singleton;

@Singleton
public class SyncSfWebHandlers {

    private boolean isDownloading = false;

    @WebGet("/syncsf/startDownload")
    public WebResponse download(RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {

        }
        return WebResponse.fail();
    }
    @WebGet("/syncsf/stopDownload")
    public WebResponse stopDownload(RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {

        }
        return WebResponse.fail();
    }
    @WebGet("/syncsf/downloadStatus")
    public WebResponse getDownStatus(RequestContext rc){
        return WebResponse.success(isDownloading);
    }
    

}
