package com.mook.locker.interceptor.cache.exception;

/**
 * Created by wyx on 2016/6/1.
 */
public class UncachedAnnotationException extends Exception {
    public UncachedAnnotationException() {
        super();
    }

    public UncachedAnnotationException(String message) {
        super(message);
    }

    public UncachedAnnotationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncachedAnnotationException(Throwable cause) {
        super(cause);
    }

    protected UncachedAnnotationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
