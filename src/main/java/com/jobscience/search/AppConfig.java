package com.jobscience.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.jobscience.search.auth.AclInterceptor;
import com.jobscience.search.web.AppAuthRequest;

public class AppConfig extends AbstractModule {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    @Override
    protected void configure() {
        bind(AuthRequest.class).to(AppAuthRequest.class);
        
        AclInterceptor aclInterceptor = new AclInterceptor();
        requestInjection(aclInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(WebGet.class),aclInterceptor);        
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(WebPost.class),aclInterceptor);
    }
}
