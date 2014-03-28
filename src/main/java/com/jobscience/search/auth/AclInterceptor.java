package com.jobscience.search.auth;

import java.lang.reflect.Method;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.britesnow.snow.web.CurrentRequestContextHolder;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.inject.Inject;

public class AclInterceptor implements MethodInterceptor {

    @Inject
    private CurrentRequestContextHolder rcHolder;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        RequestContext rc = rcHolder.getCurrentRequestContext();
        
        Method method = invocation.getMethod();
        RequireAdmin adminAnno = method.getAnnotation(RequireAdmin.class);
        Map user = rc.getUser(Map.class);
        // secure the Admin WebRest methods
        if (adminAnno != null){
            boolean isAdmin = false;
            if(user == null){
                throw new AuthException(AuthCode.NO_ADMIN_ACCESS);
            }
            if(user.containsKey("isAdmin")){
                isAdmin = (Boolean) user.get("isAdmin");
            }
            
            if(!isAdmin){
                throw new AuthException(AuthCode.NO_ADMIN_ACCESS);
            }
        }
        // secture the other method (obviously, except "admin-login" and "passcode")
        else if ((user == null || user.get("ctoken") == null) && !rc.getPathInfo().startsWith("/perf")){
            WebPost postAnnotation = method.getAnnotation(WebPost.class);
            
            boolean isLoginMethod = false;
            if(postAnnotation != null){
                for(String value : postAnnotation.value()){
                    if(value.equals("/passcode") || value.equals("/admin-login")){
                        isLoginMethod = true;
                        break;
                    }
                }
            }
            
            
            if(!isLoginMethod){
                //should not only throw no password exception
                //cause if we remove the ctoken manually, the cavas does not need passcode to relogin
                if(user == null || user.get("ctoken") == null){
                    throw new AuthException(AuthCode.NO_ORG_CTOKEN);
                }else{
                    throw new AuthException(AuthCode.NO_PASSCODE);
                }
            }
        }

        return invocation.proceed();
    }

}
