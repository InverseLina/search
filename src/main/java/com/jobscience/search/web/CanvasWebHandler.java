package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class CanvasWebHandler {
    @Inject
    private UserDao userDao;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OrgContextManager currentOrgHolder;
    @Inject 
    private AppAuthRequest appAuthRequest;
    @WebModelHandler(startsWith = "/sf-canvas")
    public void canvasApp(@WebModel Map m, RequestContext rc) {
        // Pull the signed request out of the request body and verify/decode it.
        String signedRequest = rc.getParam("signed_request");

        if (signedRequest != null) {
            Integer orgId = null;
            try{
                orgId = currentOrgHolder.getId();
            }catch(Exception e){
                rc.getWebModel().put("errorCode", "ERROR_ORG");
                rc.getWebModel().put("errorMessage", "Organization is not correct, Please enter correct organization");
                rc.getWebModel().put("success", "false");
            }
            
            if (orgId != null) {
                String canvasappSecretStr = configManager.getConfig("canvasapp_secret", orgId);
                try {
                    String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, canvasappSecretStr);
                    m.put("signedRequestJson", signedRequestJson);
                    appAuthRequest.updateCache(userDao.checkAndUpdateUser(2, signedRequestJson, null, 0, null));
                    
                } catch (Exception e) {
                    rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
                    rc.getWebModel().put("errorMessage", "The app secret might be incorrect, Make sure you have correct secret");
                    rc.getWebModel().put("success", "false");
                }
            }
            
            
        }
    }
    
}
