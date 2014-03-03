package com.jobscience.search.oauth;

import static org.scribe.model.OAuthConstants.EMPTY_TOKEN;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.hook.AppPhase;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.api.ForceDotComApi;
import com.jobscience.search.organization.OrgContextManager;
import com.jobscience.search.web.AppAuthRequest;

@Singleton
public class ForceAuthService {
    @Inject
    private OAuthHelper helper;
    @Inject
    private UserDao             userDao;
    @Inject
    OrgContextManager            orgHolder;
    @Inject
    private ConfigManager       configManager;
    @Inject
    private OrgContextManager currentOrgHolder;
    @Inject 
    private AppAuthRequest appAuthRequest;
    @Inject(optional = true)
    @Named("jss.passcode")
    private String passCode = "";
    private static final Logger log = LoggerFactory.getLogger(ForceAuthService.class);
    
    @WebApplicationHook(phase = AppPhase.INIT)
    public void init() {
    }

    public String getAuthorizationUrl() {
        return getOAuthService().getAuthorizationUrl(EMPTY_TOKEN);
    }

    public Token getAccessToken(String code) {
        Verifier verifier = new Verifier(code);
        Token accessToken = getOAuthService().getAccessToken(EMPTY_TOKEN, verifier);
        return accessToken;
    }

    public static final String UPDATE_TOKEN_URL = "https://login.salesforce.com/services/oauth2/token";

    public Token updateToken(String rtoken){
        OAuthRequest request = new OAuthRequest(Verb.POST, UPDATE_TOKEN_URL);
//        grant_type=refresh_token&client_id=3MVG9lKcPoNINVBIPJjdw1J9LLM82HnFVVX19KY1uA5mu0
//        QqEWhqKpoW3svG3XHrXDiCQjK1mdgAvhCscA9GE&client_secret=1955279925675241571
//                &refresh_token=your token here
        request.addBodyParameter("grant_type", "refresh_token");
        request.addBodyParameter("client_id", helper.getApiKey());
        request.addBodyParameter("client_secret", helper.getApiSecret());
        request.addBodyParameter("refresh_token", rtoken);
        Response resp = request.send();
        ForceDotComApi.ForceDotComTokenExtractor extractor = new ForceDotComApi.ForceDotComTokenExtractor(rtoken);
        return extractor.extract(resp.getBody());
    }

    public OAuthService getOAuthService(){
        return helper.getService();
    }
    
    public void auth(RequestContext rc){
        if(rc.getReq().getRequestURI().equals("/sf-canvas")){
            String signedRequest = rc.getParam("signed_request");

            if (signedRequest != null) {
                Integer orgId = null;
                try{
                    orgId = currentOrgHolder.getId();
                }catch(Exception e){
                    e.printStackTrace();
                    rc.getWebModel().put("errorCode", "ERROR_ORG");
                    rc.getWebModel().put("errorMessage", "Organization is not correct, Please enter correct organization");
                    rc.getWebModel().put("success", "false");
                }
                if (orgId != null) {
                    String canvasappSecretStr = configManager.getConfig("canvasapp_secret", orgId);
                    try {
                        String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, canvasappSecretStr);
                        rc.getWebModel().put("signedRequestJson", signedRequestJson);
                        appAuthRequest.updateCache(userDao.checkAndUpdateUser(2, signedRequestJson, null, 0, null));
                        String sfid = (String)currentOrgHolder.getCurrentOrg().getOrgMap().get("sfid");
                        if(sfid!=null&&!sfid.equals(userDao.getSFIDbySF2(signedRequestJson))){
                            rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
                            rc.getWebModel().put("errorMessage", "Cannot Access Org SFID from SFCanvas does not match JSS Org SFID");
                            rc.getWebModel().put("success", "false");
                        }
                    } catch (Exception e) {
                        rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
                        rc.getWebModel().put("errorMessage", "The app secret might be incorrect, Make sure you have correct secret");
                        rc.getWebModel().put("success", "false");
                    }
                }
            }else{
                rc.getWebModel().put("errorCode", "CANVAS_AUTH_ERROR");
                rc.getWebModel().put("errorMessage", "The signedRequest can't be empty.");
                rc.getWebModel().put("success", "false");
            }
        }else{
            if (passCode != null && passCode.length() > 0 ) {
                String pcode = rc.getCookie("passCode");
                if (pcode == null || !pcode.equals("true")) {
                    rc.getWebModel().put("errorCode", "NO_PASSCODE");
                    rc.getWebModel().put("errorMessage", "No passcode exists");
                    rc.getWebModel().put("success", "false");
                    rc.getWebModel().put("errorCode", "NO_ORG");
                    rc.getWebModel().put("errorMessage", "No organization selected, please, authenticate via SalesForce.com");
                    rc.getWebModel().put("success", "false");
                    return;
                }
            }
    
            String path = rc.getReq().getRequestURI();
            if ((Strings.isNullOrEmpty(rc.getReq().getContextPath()) || path.equals(rc.getReq().getContextPath() + "/")) ) {
                String ctoken = rc.getCookie("ctoken");
                if (ctoken == null) {
                    try {
                        ctoken = userDao.buildCToken(null);
                        userDao.insertUser(null, ctoken, 0l, null);
                        rc.setCookie("ctoken", ctoken);
                    }catch (AbortWithHttpRedirectException ar){
                        throw ar;
                    } catch (Exception e) {
                        rc.removeCookie("ctoken");
                        log.warn("add user fail");
                    }
                }
            }
        }
    }
}
