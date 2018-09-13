package com.boa.client.exception;


public class HighLoadException extends Exception{

    public HighLoadException() {
    }

    public HighLoadException(String message) {
        super(message);
    }

    public HighLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public HighLoadException(Throwable cause) {
        super(cause);
    }

    public HighLoadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
