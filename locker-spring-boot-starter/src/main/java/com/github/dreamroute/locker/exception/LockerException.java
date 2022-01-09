package com.github.dreamroute.locker.exception;

/**
 * 异常
 *
 * @author w.dehi
 */
public class LockerException extends RuntimeException {

    public LockerException() {}

    public LockerException(String message) {
        super(message);
    }

    public LockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockerException(Throwable cause) {
        super(cause);
    }
}
