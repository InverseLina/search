package com.jobscience.search;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.renderer.JsonRenderer;
import com.britesnow.snow.web.rest.annotation.WebGet;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;
import com.jobscience.search.auth.AclInterceptor;
import com.jobscience.search.dao.DaoHelper;
import com.jobscience.search.perf.PerfInterceptor;
import com.jobscience.search.perf.annotation.ToMonitor;
import com.jobscience.search.searchconfig.SearchConfigurationManager;
import com.jobscience.search.web.AppAuthRequest;
import com.jobscience.search.web.AppJsonRenderer;

public class AppConfig extends AbstractModule {
    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(AppConfig.class);

	public static String JSS_VERSION = "042";
    
    @Override
    protected void configure() {
        bind(AuthRequest.class).to(AppAuthRequest.class);
        
        // bind the jsonRender
        bind(JsonRenderer.class).to(AppJsonRenderer.class);
        
        AclInterceptor aclInterceptor = new AclInterceptor();
        requestInjection(aclInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(WebGet.class),aclInterceptor);        
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(WebPost.class),aclInterceptor);
        
        PerfInterceptor perfInterceptor = new PerfInterceptor();
        requestInjection(perfInterceptor);
        bindInterceptor(perfClassMatchers(), perfMethodMatchers() , perfInterceptor);
        
    }
    
    private Matcher perfClassMatchers(){
        Matcher m = Matchers.inSubpackage("com.jobscience.search.dao");
        m = m.or(Matchers.annotatedWith(ToMonitor.class));
        m = m.or(new ClassSetMatcher(DaoHelper.class));
        m = m.or(Matchers.identicalTo(SearchConfigurationManager.class));
        return m;
    }

    /**
     * See: https://groups.google.com/forum/#!topic/google-guice/GqGJr2P99tU
     *
     * This allows to avoid intercepting the Synthetic method.
     * @return
     */
    private Matcher perfMethodMatchers(){
        Matcher m = new AbstractMatcher<Method>() {
            @Override
            public boolean matches(Method m) {
                return !m.isSynthetic();
            }
        };
        return m;
    }
    
    class ClassSetMatcher extends AbstractMatcher<Class>{
        Set<Class> classSet = new HashSet<Class>();

        ClassSetMatcher(Class... classes){
            for (Class cls : classes) {
                classSet.add(cls);
            }
        }

        @Override
        public boolean matches(Class c) {
            return classSet.contains(c);
        }
    }
    
}
