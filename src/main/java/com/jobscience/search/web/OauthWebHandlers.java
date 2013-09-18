package com.jobscience.search.web;


import java.util.Map;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.UserDao;
import com.jobscience.search.oauth.ForceAuthService;
import com.jobscience.search.oauth.SalesForceService;
import com.jobscience.search.oauth.api.ForceDotComApi;


@Singleton
public class OauthWebHandlers {
    @Inject
    private ForceAuthService forceAuthService;

    @Inject
    private SalesForceService salesForceService;
    @Inject
    private UserDao userDao;

    @WebModelHandler(startsWith="/sf1")
    public void authorize(RequestContext rc) {
        String url = forceAuthService.getAuthorizationUrl();
        throw new AbortWithHttpRedirectException(url);
    }

    @WebModelHandler(startsWith = "/forceCallback")
    public void callback(RequestContext rc, @WebParam("code") String code) throws Exception {
        ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.getAccessToken(code);
        Map<String,String> info = salesForceService.getloginInfo(token.getToken());
        String orgName = info.get("orgName").replaceAll("\"", "");
        rc.setCookie("userName", info.get("userName").replaceAll("\"", ""));
        rc.setCookie("org", orgName);
        String ctoken = userDao.checkAndUpdateUser(1, token.getId());

        OAuthToken oAuthToken = new OAuthToken(token.getToken(), token.getIssuedAt().getTime());
        oAuthToken.updateCookie(rc);
    }
}
