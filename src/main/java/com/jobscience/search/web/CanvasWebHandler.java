package com.jobscience.search.web;

import java.util.Map;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.canvas.SignedRequest;

@Singleton
public class CanvasWebHandler {
    @Inject
    @Named("saleforce.apiSecret")
    private String  consumerSecret;
    
    @WebModelHandler(startsWith = "/sf2")
    public void canvasApp(@WebModel Map m, RequestContext rc,@Named("salesforce.canvasapp.key") String key) {
        // Pull the signed request out of the request body and verify/decode it.
        String signedRequest = rc.getParam("signed_request");

        if (signedRequest != null) {
            String signedRequestJson = SignedRequest.verifyAndDecodeAsJson(signedRequest, consumerSecret);
            m.put("signedRequestJson", signedRequestJson);
        }
    }
    
}
