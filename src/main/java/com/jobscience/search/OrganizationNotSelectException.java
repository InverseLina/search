package com.jobscience.search;

@SuppressWarnings("serial")
public class OrganizationNotSelectException extends RuntimeException {

    public OrganizationNotSelectException() {
        super();
    }

    public OrganizationNotSelectException(String message) {
        super(message);
    }

    public OrganizationNotSelectException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrganizationNotSelectException(Throwable cause) {
        super(cause);
    }
}
