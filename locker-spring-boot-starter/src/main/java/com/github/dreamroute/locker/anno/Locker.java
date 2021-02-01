package com.github.dreamroute.locker.anno;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 乐观锁标记，被此注解标记的update方法将被插件拦截改写sql
 *
 * @author w.dehi
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Locker {}
