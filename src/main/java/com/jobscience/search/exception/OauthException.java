package com.jobscience.search.exception;


public class OauthException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private String oauthUrl;

    public OauthException(String message, String oauthUrl) {
        super(message);
        this.oauthUrl = oauthUrl;
    }

    public OauthException(String oauthUrl) {
        super();
        this.oauthUrl = oauthUrl;
    }

    public String getOauthUrl() {
        return oauthUrl;
    }

}
