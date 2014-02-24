package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.service.SalesForceSyncService;

@Singleton
public class SyncSfWebHandlers {

    private boolean isDownloading = false;
    
    @Inject
    private SalesForceSyncService salesForceSyncService;

    @Inject
    private WebResponseBuilder webResponseBuilder;

    @WebGet("/syncsf/startDownload")
    public WebResponse download(RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        String instanceUrl = rc.getCookie("instanceUrl");
        if (ctoken != null && instanceUrl != null) {
            try {
                salesForceSyncService.syncFromSF(ctoken, instanceUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    
        return webResponseBuilder.fail();
    }
    @WebGet("/syncsf/stopDownload")
    public WebResponse stopDownload(RequestContext rc){
        String ctoken = rc.getCookie("ctoken");
        if (ctoken != null) {

        }
        return webResponseBuilder.fail();
    }
    @WebGet("/syncsf/downloadStatus")
    public WebResponse getDownStatus(RequestContext rc){
        return webResponseBuilder.success(isDownloading);
    }
    

}
