package com.boa.client.exception;

public class BzException  extends Exception{
    public BzException() {
    }

    public BzException(String message) {
        super(message);
    }

    public BzException(String message, Throwable cause) {
        super(message, cause);
    }

    public BzException(Throwable cause) {
        super(cause);
    }

    public BzException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
