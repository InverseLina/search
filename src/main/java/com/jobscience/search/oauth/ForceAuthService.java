package com.jobscience.search.oauth;

import static org.scribe.model.OAuthConstants.EMPTY_TOKEN;

import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.britesnow.snow.web.hook.AppPhase;
import com.britesnow.snow.web.hook.annotation.WebApplicationHook;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ForceAuthService {
    @Inject
    private OAuthHelper helper;

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

    public OAuthService getOAuthService(){
        return helper.getService();
    }
}
