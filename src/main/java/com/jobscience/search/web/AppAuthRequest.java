package com.jobscience.search.web;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.britesnow.snow.web.AbortWithHttpRedirectException;
import com.britesnow.snow.web.RequestContext;
import com.britesnow.snow.web.auth.AuthRequest;
import com.britesnow.snow.web.auth.AuthToken;
import com.britesnow.snow.web.handler.annotation.WebModelHandler;
import com.britesnow.snow.web.param.annotation.WebModel;
import com.britesnow.snow.web.param.annotation.WebParam;
import com.britesnow.snow.web.param.annotation.WebUser;
import com.britesnow.snow.web.rest.annotation.WebPost;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.jobscience.search.CurrentOrgHolder;
import com.jobscience.search.dao.ConfigManager;
import com.jobscience.search.dao.DBSetupManager;
import com.jobscience.search.dao.UserDao;

@Singleton
public class AppAuthRequest implements AuthRequest {
    @Inject
    private UserDao             userDao;
    @Inject
    CurrentOrgHolder            orgHolder;
    @Inject
    private ConfigManager       configManager;
    @Inject
    private DBSetupManager      dbSetupManager;
    @Inject(optional = true)
    @Named("jss.passcode")
    private String passCode = "";

    private static final Logger log = LoggerFactory.getLogger(AppAuthRequest.class);

    private final Cache<String, Map> userCache;

    public AppAuthRequest() {
        userCache = CacheBuilder.newBuilder().expireAfterAccess(8, TimeUnit.MINUTES)
                .maximumSize(100).build(new CacheLoader<String, Map>() {
                    @Override
                    public Map load(String ctoken) throws Exception {
                        return userDao.getUserByToken(ctoken);
                    }
                });
    }

    @Override
    public AuthToken authRequest(RequestContext rc) {
        String ctoken = rc.getCookie("ctoken");
        if(ctoken != null) {
            boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
            if(!isSysSchemaExist){
                return null;
            }
            
            Map user = userCache.getIfPresent(ctoken);
            if (user == null) {
                boolean isUserExist = dbSetupManager.checkOrgExtra(orgHolder.getOrgName()).contains("user");
                if(isUserExist){
                    user = userDao.getUserByToken(ctoken);
                }
                if (user != null) {
                    userCache.put(ctoken, user);
                }
            }
            AuthToken<Map> at = new AuthToken<Map>();
            at.setUser(user);
            return at;
        }else{
            return null;
        }
    }

    @WebPost("/validatePasscode")
    public WebResponse validatePasscode(@WebParam("passcode") String code ,RequestContext rc) {
        if (this.passCode != null && this.passCode.length() > 0 && passCode.equals(code)) {
            rc.setCookie("passCode", true);
            return WebResponse.success(true);
        }
        return WebResponse.fail("passcode error");
    }

    @WebModelHandler(startsWith = "/")
    public void home(@WebModel Map m, @WebUser Map user, RequestContext rc) {
        String orgName = rc.getParam("org");
        boolean isSysSchemaExist = dbSetupManager.checkSysTables().contains("config");
        m.put("sys_schema", isSysSchemaExist);
        if (orgName != null) {
            rc.setCookie("org", orgName);
            m.put("user", user);
        }
        
        if (passCode != null && passCode.length() > 0 ) {
            String pcode = rc.getCookie("passCode");
            if (pcode == null || !pcode.equals("true")) {
                rc.getWebModel().put("errorCode", "NO_PASSCODE");
                rc.getWebModel().put("errorMessage", "No passcode exists");
                rc.getWebModel().put("success", "false");
                rc.getWebModel().put("errorCode", "NO_ORG");
                rc.getWebModel().put("errorMessage", "No organization selected, please, authenticate via SalesForce.com");
                rc.getWebModel().put("success", "false");
                return;
            }
        }

        String path = rc.getReq().getRequestURI();
        if ((Strings.isNullOrEmpty(rc.getReq().getContextPath()) || path.equals(rc.getReq().getContextPath() + "/")) && isSysSchemaExist) {
            String ctoken = rc.getCookie("ctoken");
            if (ctoken == null) {
                try {
                    ctoken = userDao.buildCToken(null);
                    userDao.insertUser(null, ctoken);
                    rc.setCookie("ctoken", ctoken);
                }catch (AbortWithHttpRedirectException ar){
                    throw ar;
                } catch (Exception e) {
                    rc.removeCookie("ctoken");
                    log.warn("add user fail");
                }
            }
        }
        // check org is set or not
        try {
            Map configMap = configManager.getOrgInfo(orgHolder.getId());
            configMap.put("instanceUrl", rc.getCookie("instanceUrl"));
            m.put("orgConfigs", JSONObject.fromObject(configMap).toString());
        } catch (Exception e) {
            rc.removeCookie("ctoken");
        }
    }

}