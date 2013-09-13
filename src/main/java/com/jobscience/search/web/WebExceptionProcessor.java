package com.jobscience.search.web;


import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.exception.WebExceptionContext;
import com.britesnow.snow.web.exception.annotation.WebExceptionCatcher;
import com.britesnow.snow.web.renderer.JsonRenderer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.OrganizationNotSelectException;

@Singleton
public class WebExceptionProcessor {
    @Inject
    private JsonRenderer jsonRenderer;
    @WebExceptionCatcher
    public void processOauthException(OrganizationNotSelectException e, WebExceptionContext wec, RequestContext rc)  {
        rc.getWebModel().put("errorCode", "NO_ORG");
        rc.getWebModel().put("errorMessage", "No organization selected, please, authenticate via SalesForce.com");
        rc.getWebModel().put("success", "false");
        jsonRenderer.render(rc.getWebModel(), rc.getWriter());
    }

}
