package com.jobscience.search;

import com.britesnow.snow.web.auth.AuthRequest;
import com.jobscience.search.web.AppAuthRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;

public class AppConfig extends AbstractModule {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);
    
    @Override
    protected void configure() {
        bind(AuthRequest.class).to(AppAuthRequest.class);
    }
}
