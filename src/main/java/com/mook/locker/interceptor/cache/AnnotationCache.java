package com.mook.locker.interceptor.cache;

import java.lang.annotation.Annotation;

import com.mook.locker.interceptor.cache.exception.UncachedAnnotationException;

/**
 * Created by wyx on 2016/6/1.
 */
public interface AnnotationCache<K, V extends Annotation> {
    void clear();

    V getAnnotation(K key) throws UncachedAnnotationException;
}
