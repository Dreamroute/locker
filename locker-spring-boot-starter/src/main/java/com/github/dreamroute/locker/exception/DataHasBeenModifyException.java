package com.github.dreamroute.locker.exception;

/**
 * 描述：乐观锁数据被修改异常
 *
 * @author w.dehi.2022-01-09
 */
public class DataHasBeenModifyException extends RuntimeException{

    public DataHasBeenModifyException() {}

    public DataHasBeenModifyException(String message) {
        super(message);
    }

    public DataHasBeenModifyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataHasBeenModifyException(Throwable cause) {
        super(cause);
    }
}
