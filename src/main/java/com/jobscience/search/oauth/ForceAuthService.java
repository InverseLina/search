package com.jobscience.search.oauth;

import static org.scribe.model.OAuthConstants.EMPTY_TOKEN;

import com.jobscience.search.oauth.api.ForceDotComApi;
import org.scribe.model.*;
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
}
