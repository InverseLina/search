package com.jobscience.search.exception;


public class OauthTokenExpireException extends OauthException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public OauthTokenExpireException(String message, String oauthUrl) {
        super(message, oauthUrl);
    }

    public OauthTokenExpireException(String oauthUrl) {
        super(oauthUrl);
    }
}
