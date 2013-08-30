package com.jobscience.search.oauth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.oauth.api.ForceDotComApi;
import org.scribe.builder.ServiceBuilder;
import org.scribe.oauth.OAuthService;

@Singleton
public class OAuthHelper {
    @Inject
    @Named("saleforce.apiKey")
    private String apiKey;

    @Inject
    @Named("saleforce.apiSecret")
    private String apiSecret;

    @Inject
    @Named("saleforce.callBackUrl")
    private String callbackUrl;

    public OAuthService  getService(){
        ServiceBuilder builder = new ServiceBuilder().provider(ForceDotComApi.class).apiKey(apiKey).apiSecret(apiSecret);
        builder.callback(callbackUrl);
        return builder.build();
    }
}
