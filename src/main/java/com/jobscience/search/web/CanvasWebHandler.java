package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.OAuthHelper;

@Singleton
public class CanvasWebHandler {
    @Inject
    private UserDao userDao;
    @Inject
    private OAuthHelper oAuthHelper;
    
    @WebModelHandler(startsWith = "/sf2")
    public void canvasApp(@WebModel Map m, RequestContext rc) {
        // Pull the signed request out of the request body and verify/decode it.
        String signedRequest = rc.getParam("signed_request");

        if (signedRequest != null) {
            String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, oAuthHelper.getApiSecret());
            m.put("signedRequestJson", signedRequestJson);
            try {
                userDao.checkAndUpdateUser(2, signedRequestJson);
            } catch (Exception e) {
                throw new AbortWithHttpRedirectException("/");
            }
        }
    }
    
}
