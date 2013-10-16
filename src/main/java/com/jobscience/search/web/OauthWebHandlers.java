package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.util.JsonUtil;
import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.ForceAuthService;
import com.jobscience.search.oauth.SalesForceService;
import com.jobscience.search.oauth.api.ForceDotComApi;

import javax.servlet.http.Cookie;

@Singleton
public class OauthWebHandlers {
    @Inject
    private ForceAuthService forceAuthService;
    @Inject
    private SalesForceService salesForceService;
    @Inject
    private UserDao userDao;
    @Named("jss.prod")
    @Inject
    private boolean productMode;

    /**
     * web get auth flow
     * @param rc
     */
    @WebModelHandler(startsWith="/sf1")
    public void authorize(RequestContext rc) {
        rc.removeCookie("doSf1Test");
        String url = forceAuthService.getAuthorizationUrl();
        throw new AbortWithHttpRedirectException(url);
    }

    /**
     * web get auth flow
     * @param rc
     */
    @WebModelHandler(startsWith = "/sf1test")
    public void sf1test(RequestContext rc) {
        rc.getWebModel().put("oauthToken", rc.getCookie("oauthToken"));
        rc.getWebModel().put("loginInfo", rc.getCookie("loginInfo"));
        rc.removeCookie("oauthToken");
        rc.removeCookie("loginInfo");
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
        if(productMode){
            rc.setCookie("org", orgName);
        }


/*        OAuthToken oAuthToken = new OAuthToken(token.getToken(), token.getIssuedAt().getTime());
        oAuthToken.updateCookie(rc);*/
        try {
            rc.setCookie("oauthToken", token.getRawResponse());
            rc.setCookie("loginInfo", JsonUtil.toJson(info));
            userDao.checkAndUpdateUser(1, token.getId());
        } catch (Exception e) {
            throw new AbortWithHttpRedirectException("/");
        }

    }
}
