package com.jobscience.search.web;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.ForceAuthService;
import com.jobscience.search.oauth.api.ForceDotComApi;
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
    @Inject
    private ForceAuthService forceAuthService;
    @Inject(optional = true)
    @Named("jss.passcode")
    private String passCode = "";

    @Inject
    private WebResponseBuilder webResponseBuilder;

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
        String ctoken = rc.getCookie("ctoken");
        if(ctoken != null) {
            boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
            if(!isSysSchemaExist){
                return null;
            }
            
            Map user = userCache.getIfPresent(ctoken);
            if (user == null) {
                boolean isUserExist = dbSetupManager.checkOrgExtra(orgHolder.getOrgName()).contains("jss_user");
                if(isUserExist){
                    user = userDao.getUserByToken(ctoken);
                }
                if (user != null) {
                    userCache.put(ctoken, user);
                }
            }
            AuthToken<Map> at = new AuthToken<Map>();
            at.setUser(user);
            return at;
        }else{
            return null;
        }
    }

    @WebPost("/validatePasscode")
    public WebResponse validatePasscode(@WebParam("passcode") String code ,RequestContext rc) {
        if (this.passCode != null && this.passCode.length() > 0 && passCode.equals(code)) {
            rc.setCookie("passCode", true);
            return webResponseBuilder.success(true);
        }
        return webResponseBuilder.fail("passcode error");
    }

    @WebModelHandler(startsWith = "/")
    public void home(@WebModel Map m, @WebUser Map user, RequestContext rc) {
        String orgName = rc.getParam("org");
        boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
        m.put("sys_schema", isSysSchemaExist);
        if (orgName != null) {
            rc.setCookie("org", orgName);
            m.put("user", user);
        }
        if(isSysSchemaExist){
            forceAuthService.auth(rc);
        }
        // check org is set or not
        try {
            Map configMap = configManager.getOrgInfo(orgHolder.getId());
            configMap.put("instanceUrl", rc.getCookie("instanceUrl"));
            m.put("orgConfigs", JSONObject.fromObject(configMap).toString());
        } catch (Exception e) {
            rc.removeCookie("ctoken");
        }
        //update token
        if (user != null && user.get("rtoken") != null) {
            long timeout = (Long) user.get("timeout");
            if (System.currentTimeMillis() - timeout > 0) {
                ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.updateToken((String) user.get("rtoken"));
                updateUserToken(token);
            }
        }
    }

    private void updateUserToken(ForceDotComApi.ForceDotComToken token) {
        try {
            String timeout = configManager.getConfig("sf_session_timeout", orgHolder.getId());
            long sfTimeout;
            try {
                sfTimeout = token.getIssuedAt().getTime() + Integer.valueOf(timeout);
            } catch (NumberFormatException e) {
                sfTimeout = token.getIssuedAt().getTime() + 1000*60 * 180;
            }
            userDao.checkAndUpdateUser(1, token.getId(), token.getToken(), sfTimeout, token.getRefreshToken());
        } catch (Exception e) {
            throw new AbortWithHttpRedirectException("/");
        }
    }

    public void updateCache(Map user){
        userCache.put((String)user.get("ctoken"), user);
    }
}