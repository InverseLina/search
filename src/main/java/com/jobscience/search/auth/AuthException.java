package com.jobscience.search.auth;



public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private AuthCode errorCode = null;

    public AuthException(AuthCode errorCode) {
        this.errorCode = errorCode;
    }

    public AuthCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(AuthCode errorCode) {
        this.errorCode = errorCode;
    }


}
