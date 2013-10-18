package com.jobscience.search.web;

import com.britesnow.snow.web.RequestContext;

public class OAuthToken {
    private String token;
    private long expire;

    public static final String TOKEN_ID = "ctoken";
    public static final String TOKEN_EXPIRE = "oauthToken_expire";

    public OAuthToken() {
    }

    public OAuthToken(String token, long expire) {
        this.token = token;
        this.expire = expire;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpire() {
        return expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }

    public static OAuthToken fromCookie(RequestContext rc) {
        String tokenIdStr = rc.getCookie(TOKEN_ID);
        String expireStr = rc.getCookie(TOKEN_EXPIRE);

        try {
            if (tokenIdStr != null && expireStr != null) {
                long expire = Long.valueOf(expireStr);
                if(System.currentTimeMillis() < expire) {
                    return new OAuthToken(tokenIdStr, expire);
                }else{
                    return null;
                }
            }else{
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void updateCookie(RequestContext rc) {
        rc.setCookie(TOKEN_ID, this.getToken(), true);
        rc.setCookie(TOKEN_EXPIRE, String.valueOf(this.getExpire()), true);
    }
}
