package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.service.SalesForceSyncService;

@Singleton
public class SyncWebHandlers {

    @Inject
    private SalesForceSyncService salesForceSyncService;

    @WebGet("/sync")
    public void saveConfig(@WebModel Map m,RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        String instanceUrl = rc.getCookie("instanceUrl");
        if (ctoken != null && instanceUrl != null) {
            try {
                salesForceSyncService.syncData(ctoken, instanceUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
