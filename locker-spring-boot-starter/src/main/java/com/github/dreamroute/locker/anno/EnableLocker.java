package com.github.dreamroute.locker.anno;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 描述：开启乐观锁
 *
 * @author w.dehi.2022-02-17
 */
@Target(TYPE)
@Retention(RUNTIME)
@Import(LockerConfig.class)
public @interface EnableLocker {

    /**
     * 乐观锁列名，默认version
     */
    String versionColumn() default "version";

    /**
     * 并发修改失败时是否抛出异常，默认：是，如果false：那么返回更新条数
     */
    boolean failThrowException() default true;

}
