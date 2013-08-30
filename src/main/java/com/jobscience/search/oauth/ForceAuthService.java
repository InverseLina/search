package com.jobscience.search.oauth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import static org.scribe.model.OAuthConstants.EMPTY_TOKEN;

@Singleton
public class ForceAuthService {
    private OAuthService oAuthService;

    @Inject
    public ForceAuthService(OAuthHelper helper) {
        oAuthService = helper.getService();
    }

    public String getAuthorizationUrl() {
        return oAuthService.getAuthorizationUrl(EMPTY_TOKEN);
    }

    public Token getAccessToken(String code) {
        Verifier verifier = new Verifier(code);
        Token accessToken = oAuthService.getAccessToken(EMPTY_TOKEN, verifier);
        return accessToken;
    }
}
