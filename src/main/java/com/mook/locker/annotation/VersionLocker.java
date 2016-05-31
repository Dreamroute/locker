package com.mook.locker.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface VersionLocker {
	
	// 插件默认拦截所有update方法，不拦截标记@VersinoLocker(false)的方法
	boolean value() default true;
}
