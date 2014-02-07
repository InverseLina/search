package com.jobscience.search.web;


import java.io.IOException;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.WebRequestType;
import com.britesnow.snow.web.exception.WebExceptionContext;
import com.britesnow.snow.web.exception.annotation.WebExceptionCatcher;
import com.britesnow.snow.web.renderer.JsonRenderer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.exception.OAuthConfigBuildException;
import com.jobscience.search.exception.OrganizationNotSelectException;
import com.jobscience.search.exception.PassCodeException;

@Singleton
public class WebExceptionProcessor {
    @Inject
    private JsonRenderer jsonRenderer;

    @WebExceptionCatcher
    public void processOauthException(OrganizationNotSelectException e, WebExceptionContext wec, RequestContext rc) {
        rc.getWebModel().put("errorCode", "NO_ORG");
        rc.getWebModel().put("errorMessage", "No organization selected, please, authenticate via SalesForce.com");
        rc.getWebModel().put("success", "false");
        if (rc.getWebRequestType() == WebRequestType.WEB_REST) {
            jsonRenderer.render(rc.getWebModel(), rc.getWriter());
        }
    }
    @WebExceptionCatcher
    public void processPassCodeException(PassCodeException e, WebExceptionContext wec, RequestContext rc) {
        rc.getWebModel().put("errorCode", "NO_PASSCODE");
        rc.getWebModel().put("errorMessage", "No passcode exists");
        rc.getWebModel().put("success", "false");
        rc.getRes().setHeader("Cache-Control","no-cache");
        rc.getRes().setHeader("Pragma","no-cache");
        rc.getRes().setDateHeader ("Expires", -1);
        if (rc.getWebRequestType() == WebRequestType.WEB_REST) {
            jsonRenderer.render(rc.getWebModel(), rc.getWriter());
        }
    }
    @WebExceptionCatcher
    public void processOAuthConfigBuildException(OAuthConfigBuildException e, WebExceptionContext wec, RequestContext rc) {
        rc.getWebModel().put("errorCode", "OAUTH_CONFIG_BUILD_ERROR");
        rc.getWebModel().put("errorMessage", "OAuth configs (key, secret) might be incorrect.");
        rc.getWebModel().put("success", "false");
        rc.getRes().setHeader("Cache-Control","no-cache");
        rc.getRes().setHeader("Pragma","no-cache");
        rc.getRes().setDateHeader ("Expires", -1);
        try {
            rc.getWriter().write("OAuth configs (key, secret, callback url) might be incorrect");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}

