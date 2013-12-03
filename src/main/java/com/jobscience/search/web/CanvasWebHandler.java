package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.UserDao;

@Singleton
public class CanvasWebHandler {
    @Inject
    private UserDao userDao;
    @Inject
    private ConfigManager configManager;
    
    @WebModelHandler(startsWith = "/sf2")
    public void canvasApp(@WebModel Map m, RequestContext rc) {
        // Pull the signed request out of the request body and verify/decode it.
        String signedRequest = rc.getParam("signed_request");

        if (signedRequest != null) {
            Map<String, String> map = configManager.getConfig("canvasapp_secret");
            String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, map.get("value"));
            m.put("signedRequestJson", signedRequestJson);
            try {
                userDao.checkAndUpdateUser(2, signedRequestJson, null);
            } catch (Exception e) {
                throw new AbortWithHttpRedirectException("/");
            }
        }
    }
    
}
