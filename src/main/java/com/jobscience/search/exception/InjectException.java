package com.jobscience.search.exception;

import com.britesnow.snow.web.AbortException;

public class InjectException extends AbortException {
    private int code;

    public InjectException(String message) {
        super(message);
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
