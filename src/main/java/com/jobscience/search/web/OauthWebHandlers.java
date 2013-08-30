package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.oauth.ForceAuthService;
import com.jobscience.search.oauth.api.ForceDotComApi;

import java.io.IOException;

@Singleton
public class OauthWebHandlers {
    @Inject
    private ForceAuthService forceAuthService;

    @WebGet("/authorize")
    public void authorize(RequestContext rc) throws IOException {
        String url = forceAuthService.getAuthorizationUrl();
        rc.getRes().sendRedirect(url);
    }

    @WebModelHandler(startsWith = "/forceCallback")
    public void callback(RequestContext rc, @WebParam("code") String code) throws Exception {
        ForceDotComApi.ForceDotComToken token = (ForceDotComApi.ForceDotComToken) forceAuthService.getAccessToken(code);
        OAuthToken oAuthToken = new OAuthToken(token.getToken(), token.getIssuedAt().getTime());
        oAuthToken.updateCookie(rc);
    }
}
