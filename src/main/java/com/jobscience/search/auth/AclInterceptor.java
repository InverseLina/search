package com.jobscience.search.auth;

import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.google.inject.Inject;

public class AclInterceptor implements MethodInterceptor {

    @Inject
    private CurrentRequestContextHolder rcHolder;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RequestContext rc = rcHolder.getCurrentRequestContext();
        Map webModel = rcHolder.getCurrentRequestContext().getWebModel();
        boolean canAccess = true;
        
        if(!rc.getPathInfo().equals("/passcode") && !rc.getPathInfo().equals("/admin-login")){
            if(webModel.get("errorCode") != null){
                String errorCode = (String) webModel.get("errorCode");
                AuthCode code = AuthCode.valueOf(errorCode);
                if(code == AuthCode.NO_ADMIN_ACCESS || code == AuthCode.NO_PASSCODE){
                    throw new AuthException(code);
                }
            }
        }
        
        
        if(!canAccess){
            return null;
        }

        return invocation.proceed();
    }

}
