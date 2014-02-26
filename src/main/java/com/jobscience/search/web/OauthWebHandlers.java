package com.jobscience.search.web;

import java.io.IOException;
import java.util.Map;

import com.britesnow.snow.util.JsonUtil;
import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.ForceAuthService;
import com.jobscience.search.oauth.api.ForceDotComApi;
import com.jobscience.search.organization.OrgContextManager;
import com.jobscience.search.service.SalesForceCommonService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Singleton
public class OauthWebHandlers {
    @Inject
    private ForceAuthService forceAuthService;
    @Inject
    private SalesForceCommonService salesForceService;
    @Inject
    private UserDao userDao;
    @Named("jss.prod")
    @Inject
    private boolean productMode;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OrgContextManager orgHolder;

    private final Logger log = LoggerFactory.getLogger(OauthWebHandlers.class);

    /**
     * web get auth flow
     * @param rc
     */
    @WebModelHandler(startsWith="/sf1")
    public void authorize(@WebUser Map user, RequestContext rc) {
        if(user==null || user.get("rtoken") == null){
            String url = forceAuthService.getAuthorizationUrl();
            throw new AbortWithHttpRedirectException(url);
        }else {
            //update token
            long timeout = (Long)user.get("timeout");
            if(System.currentTimeMillis() - timeout > 0){
                ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.updateToken((String) user.get("rtoken"));
                updateUserToken(token);
            }
            throw new AbortWithHttpRedirectException(rc.getContextPath());
        }

    }

    /**
     * web get auth flow
     * @param rc
     */
    @WebModelHandler(startsWith = "/sf1test")
    public void sf1test(RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        String instanceUrl = rc.getCookie("instanceUrl");
        if (ctoken != null && instanceUrl != null) {
            try {
                Map info = salesForceService.getFullLoginInfo(ctoken, instanceUrl);
                rc.getWebModel().put("loginInfo", JsonUtil.toJson(info));
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * callback when salesforce authorized
     * @param rc
     * @param code
     * @throws Exception
     */
    @WebModelHandler(startsWith = "/forceCallback")
    public void callback(RequestContext rc, @WebParam("code") String code) throws Exception {
        ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.getAccessToken(code);
        Map<String,String> info = salesForceService.getloginInfo(token);
        String orgName = info.get("orgName").replaceAll("\"", "");
        rc.setCookie("userName", info.get("userName").replaceAll("\"", ""));
        rc.setCookie("instanceUrl", token.getInstanceUrl());
        if(productMode){
            rc.setCookie("org", orgName);
        }


        /*        OAuthToken oAuthToken = new OAuthToken(token.getToken(), token.getIssuedAt().getTime());
        oAuthToken.updateCookie(rc);*/
        updateUserToken(token);

    }

    private void updateUserToken(ForceDotComApi.ForceDotComToken token) {
        try {
            String timeout = configManager.getConfig("sf_session_timeout", orgHolder.getId());
            long sfTimeout;
            try {
                sfTimeout = token.getIssuedAt().getTime() + Integer.valueOf(timeout)*1000*60;
            } catch (NumberFormatException e) {
                sfTimeout = token.getIssuedAt().getTime() + 1000*60 * 180;
            }
            userDao.checkAndUpdateUser(1, token.getId(), token.getToken(), sfTimeout, token.getRefreshToken());
        } catch (Exception e) {
            throw new AbortWithHttpRedirectException("/");
        }
    }
}
