package com.jobscience.search.hook;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.WebRequestType;
import com.britesnow.snow.web.hook.On;
import com.britesnow.snow.web.hook.ReqPhase;
import com.britesnow.snow.web.hook.annotation.WebRequestHook;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.exception.JsonAuthException;
import com.jobscience.search.exception.PassCodeException;

import java.util.Map;

@Singleton
public class SSAuthHook {
    @Inject(optional = true)
    @Named("jss.passcode")
    private String passCode = "";

    @WebRequestHook(phase = ReqPhase.AUTH, on = On.AFTER)
    public void checkAuth(@WebUser Map user, RequestContext rc) {
        String resource =  rc.getReq().getRequestURI();
        if (resource.endsWith(".json") || resource.endsWith(".do") && !resource.endsWith("/login.do")) {
            if (user == null) {
                throw new JsonAuthException();
            }
        }
    }
    @WebRequestHook(phase = ReqPhase.START, on = On.AFTER)
    public void checkPasscode(RequestContext rc) {
        if (passCode != null && passCode.length() > 0 ) {
            if (rc.getWebRequestType() == WebRequestType.WEB_REST) {
                String uri = rc.getReq().getRequestURI();
                if(!uri.equals("/validatePasscode") && !uri.equals("/") && !uri.startsWith("/admin")){
                    String pcode = rc.getCookie("passCode");
                    if (pcode == null || !pcode.equals("true")) {
                        throw new PassCodeException();
                    }
                }
            }else if(rc.getReq().getRequestURI().startsWith("/sf-canvas")){
                rc.setCookie("passCode", "true");
            }
        }
    }
}
