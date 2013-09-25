package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.UserDao;

import java.util.Map;

@Singleton
public class AppAuthRequest implements AuthRequest {
    @Inject
    private UserDao userDao;

    @Inject
    private DBSetupManager dbSetupManager;
    @Override
    public AuthToken authRequest(RequestContext rc) {
        OAuthToken token = OAuthToken.fromCookie(rc);
        if (token != null) {
            AuthToken<OAuthToken> at = new AuthToken<OAuthToken>();
            at.setUser(token);
            return at;
        }else{
            return null;
        }
    }

    @WebModelHandler(startsWith = "/")
    public void home(@WebModel Map m, @WebUser OAuthToken user, RequestContext rc) {
        String orgName = rc.getParam("org");
        m.put("sys_schema", dbSetupManager.checkSysTables());
        if (orgName != null) {
            rc.setCookie("org", orgName);
            m.put("user", user);
        }
        String path = rc.getReq().getRequestURI();
        if (path.equals("/")) {
            String ctoken = rc.getCookie("ctoken");
            if (ctoken == null) {
                ctoken = userDao.buildCToken(null);
                try {
                    userDao.insertUser(null,ctoken);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
                rc.setCookie("ctoken", ctoken);
            }
        }
    }

}