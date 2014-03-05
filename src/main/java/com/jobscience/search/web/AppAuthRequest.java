package com.jobscience.search.web;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.WebRequestType;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.hash.Hashing;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.auth.AuthCode;
import com.jobscience.search.canvas.SignedRequest;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.organization.OrgContextManager;

@Singleton
public class AppAuthRequest implements AuthRequest {
    @Inject
    private UserDao             userDao;
    @Inject
    OrgContextManager            orgHolder;
    @Inject
    private ConfigManager       configManager;
    @Inject
    private DBSetupManager      dbSetupManager;
    
    @Inject(optional = true)
    @Named("jss.passcode")
    private String passCode;
    
    @Inject(optional = true)
    @Named("jss.sysadmin.pwd")
    private String configPassword;

    @Inject
    private WebResponseBuilder webResponseBuilder;
    
    private static final Logger log = LoggerFactory.getLogger(AppAuthRequest.class);
    
    static private final String COOKIE_ORG_USER_TOKEN = "ctoken";
    static private final String COOKIE_ORG = "org";
    static private final String COOKIE_ADMIN_TOKEN = "atoken";
    static private final String COOKIE_PASSCODE = "pcode";

    private final Cache<String, Map> userCache;

    public AppAuthRequest() {
        userCache = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
                .maximumSize(100).build(new CacheLoader<String, Map>() {
                    @Override
                    public Map load(String ctoken) throws Exception {
                        return userDao.getUserByToken(ctoken);
                    }
                });
    }

    @Override
    public AuthToken authRequest(RequestContext rc) {
        WebRequestType wrt = rc.getWebRequestType();
        AuthToken authToken = null;
        switch(wrt){
            // All the dynamic resources, we need to auth
            case WEB_RESOURCE:
            case WEB_REST:
            case WEB_TEMPLATE:
                String orgName = rc.getParam("org");
                if (orgName != null) {
                  rc.setCookie(COOKIE_ORG, orgName);
                }
                return authWebRequest(rc);
            // static files and generated files (.less, webbundle) we do not need to auth.
            case GENERATED_ASSET:
            case STATIC_FILE:break;
        }
        return authToken;
    }

    @WebPost("/passcode")
    public WebResponse passcode(@WebParam("passcode") String code ,RequestContext rc) {
        if (passCode != null && passCode.length() > 0 && passCode.equals(code)) {
            String codeSha1 = Hashing.sha1().hashString(code,Charset.forName("UTF-8")).toString();
            rc.setCookie(COOKIE_PASSCODE, codeSha1);
            return webResponseBuilder.success(true);
        }
        return webResponseBuilder.fail("passcode error");
    }
    
    @WebPost("/admin-login")
    public WebResponse adminLogin(RequestContext rc,
                            @WebParam("password") String password) throws SQLException {
        if (configPassword.equals(password)) {
            String passwordSha1 = Hashing.sha1().hashString(password,Charset.forName("UTF-8")).toString();
            rc.setCookie(COOKIE_ADMIN_TOKEN, passwordSha1);
            return webResponseBuilder.success();
        } else {
            return webResponseBuilder.fail();
        }
    }

    @WebModelHandler(startsWith = "/")
    public void home(@WebModel Map m, @WebUser Map user, RequestContext rc) {
		if (!rc.getPathInfo().equals("/admin")){
			String orgName = orgHolder.getOrgName();
			boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
			m.put("sys_schema", isSysSchemaExist);
			if (orgName != null) {
				m.put("user", user);
			}
			// check org is set or not
			try {
				Map configMap = configManager.getOrgInfo(orgHolder.getId());
				configMap.put("instanceUrl", rc.getCookie("instanceUrl"));
				m.put("orgConfigs", JSONObject.fromObject(configMap).toString());
			} catch (Exception e) {
				rc.removeCookie(COOKIE_ORG_USER_TOKEN);
			}

			//update token
//			if (user != null && user.get("rtoken") != null) {
//				long timeout = (Long) user.get("timeout");
//				if (System.currentTimeMillis() - timeout > 0) {
//					ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.updateToken((String) user.get("rtoken"));
//					updateUserToken(token);
//				}
//			}
		}
    }
    
    private AuthToken authWebRequest(RequestContext rc){
        AuthToken authToken = null;
        String path = rc.getPathInfo();
        if(path.startsWith("/admin")){
            //FIXME:Hard code, although we are in canvas mode, just this request is sf-canvas
            String codeSha1 = Hashing.sha1().hashString(passCode,Charset.forName("UTF-8")).toString();
            rc.setCookie(COOKIE_PASSCODE, codeSha1);
            String atoken = rc.getCookie(COOKIE_ADMIN_TOKEN);
            if(Strings.isNullOrEmpty(atoken) || !atoken.equals(Hashing.sha1().hashString(configPassword,Charset.forName("UTF-8")).toString())){
                rc.getWebModel().put("errorCode", AuthCode.NO_ADMIN_ACCESS.toString());
                rc.getWebModel().put("errorMessage", "Have no rights to access /admin");
                rc.getWebModel().put("success", "false");
            }
            return null;
        }else if(path.equals("/sf-canvas")){
            //FIXME:Hard code, although we are in canvas mode, just this request is sf-canvas
            String codeSha1 = Hashing.sha1().hashString(passCode,Charset.forName("UTF-8")).toString();
            rc.setCookie(COOKIE_PASSCODE, codeSha1);
            
            String signedRequest = rc.getParam("signed_request");

            if (signedRequest != null) {
                Integer orgId = null;
                try{
                    orgId = orgHolder.getId();
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
                        updateCache(userDao.checkAndUpdateUser(2, signedRequestJson, null, 0, null));
                        String sfid = (String)orgHolder.getCurrentOrg().getOrgMap().get("sfid");
                        if(!Strings.isNullOrEmpty(sfid)&&!userDao.getSFIDbySF2(signedRequestJson).startsWith(sfid)){
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
                String pcode = rc.getCookie(COOKIE_PASSCODE);
                if (pcode == null || !pcode.equals(Hashing.sha1().hashString(passCode,Charset.forName("UTF-8")).toString())) {
                    rc.getWebModel().put("errorCode", AuthCode.NO_PASSCODE.toString());
                    rc.getWebModel().put("errorMessage", "No passcode exists or incorrect");
                    rc.getWebModel().put("success", "false");
                    return null;
                }
            }
    
            String contextPath = rc.getContextPath();
            boolean isRootUrl = path.equals(contextPath + "/") || path.equals(rc.getContextPath()) ? true : false;
            String ctoken = rc.getCookie(COOKIE_ORG_USER_TOKEN);
            try {
                Map user = null;
                
                if (ctoken != null) {
                    user = userCache.getIfPresent(ctoken);
                }
                
                if (user == null) {
                    boolean isUserExist = false;
                    if (orgHolder.getOrgName() != null) {
                        dbSetupManager.checkOrgExtra(orgHolder.getOrgName()).contains("jss_user");
                    }
                    if (isUserExist) {
                        user = userDao.getUserByToken(ctoken);
                    }
                }
                
                if(isRootUrl){
                    ctoken = userDao.buildCToken(null);
                    userDao.insertUser(null, ctoken, 0l, null);
                    rc.setCookie(COOKIE_ORG_USER_TOKEN, ctoken);
                    user = userDao.getUserByToken(ctoken);
                    updateCache(user);
                }
                
                if(user != null){
                    authToken = new AuthToken();
                    authToken.setUser(user);
                }
                
            } catch (Exception e) {
                rc.removeCookie(COOKIE_ORG_USER_TOKEN);
                log.warn("add user fail");
            }
        }
    
        return authToken;
    }

//    private void updateUserToken(ForceDotComApi.ForceDotComToken token) {
//        try {
//            String timeout = configManager.getConfig("sf_session_timeout", orgHolder.getId());
//            long sfTimeout;
//            try {
//                sfTimeout = token.getIssuedAt().getTime() + Integer.valueOf(timeout);
//            } catch (NumberFormatException e) {
//                sfTimeout = token.getIssuedAt().getTime() + 1000*60 * 180;
//            }
//            userDao.checkAndUpdateUser(1, token.getId(), token.getToken(), sfTimeout, token.getRefreshToken());
//        } catch (Exception e) {
//            throw new AbortWithHttpRedirectException("/");
//        }
//    }

    public void updateCache(Map user){
        userCache.put((String)user.get(COOKIE_ORG_USER_TOKEN), user);
    }
}