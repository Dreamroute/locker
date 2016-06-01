package com.mook.locker.interceptor.cache.exception;

/**
 * Created by wyx on 2016/6/1.
 */
public class UncachedMapperException extends UncachedAnnotationException {
    public UncachedMapperException() {
        super();
    }

    public UncachedMapperException(String message) {
        super(message);
    }

    public UncachedMapperException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncachedMapperException(Throwable cause) {
        super(cause);
    }

    protected UncachedMapperException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
