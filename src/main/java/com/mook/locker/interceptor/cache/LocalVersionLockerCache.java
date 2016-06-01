package com.mook.locker.interceptor.cache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.session.Configuration;

/**
 * Created by wyx on 2016/6/1.
 */
public class LocalVersionLockerCache implements VersionLockerCache {

    private final Map<MethodSignature, VersionLocker> cachedMap = new HashMap<>();

    private final Lock cachedLock = new ReentrantLock();

    private final Map<String, Class<?>> mappedClass = new HashMap<>();

    private volatile boolean initFinished = false;

    @Override
    public boolean cacheMappers(Configuration configuration, String id, Class<?>[] params) {
        if (configuration == null) {
            throw new NullPointerException("configuration cannot be null.");
        }

        initMappedClass(configuration);

        final MethodSignature methodSignature = new MethodSignature(id, params);
        boolean updated = false;
        if (!cachedMap.containsKey(methodSignature)) {
            final Lock cacheInitLock = this.cachedLock;
            cacheInitLock.lock();
            try {
                if (!cachedMap.containsKey(methodSignature)) {
                    int lastPointPos = id.lastIndexOf('.');
                    String clsName = id.substring(0, lastPointPos);
                    String methodName = id.substring(lastPointPos + 1);
                    Class<?> mapper = mappedClass.get(clsName);
                    if (mapper == null) {
                        throw new RuntimeException("Class " + clsName + " isn't a mapper for Mybatis.");
                    }
                    Method m;
                    try {
                        m = mapper.getMethod(methodName, params);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(id + "(" + Arrays.toString(params) + ") is not a valid method.", e);
                    }
                    cachedMap.put(methodSignature, m.getAnnotation(VersionLocker.class));
                    updated = true;
                }
            } finally {
                cacheInitLock.unlock();
            }
        }
        return updated;
    }

    private void initMappedClass(Configuration configuration) {
        final Lock cacheInitLock = this.cachedLock;
        if (!initFinished) {
            cacheInitLock.lock();
            try {
                if (!initFinished) {
                    for (Class<?> mapper : configuration.getMapperRegistry().getMappers()) {
                        mappedClass.put(mapper.getName(), mapper);
                    }
                    initFinished = true;
                }
            } finally {
                cacheInitLock.unlock();
            }
        }
    }

    @Override
    public boolean hasCachedMappers() {
        return initFinished;
    }

    @Override
    public void clear() {
        cachedMap.clear();
    }

    @Override
    public VersionLocker getAnnotation(String id, Class<?>[] params) throws UncachedMapperException {
        return getAnnotation(new MethodSignature(id, params));
    }

    @Override
    public VersionLocker getAnnotation(MethodSignature methodSignature) throws UncachedMapperException {
        if (methodSignature == null) {
            throw new NullPointerException("methodSignature cannot be null.");
        }
        if (!cachedMap.containsKey(methodSignature)) {
            throw new UncachedMapperException("Cannot find cached data for " + methodSignature.getId() + "(" + Arrays.toString(methodSignature.getParams()) + ").");
        }
        return cachedMap.get(methodSignature);
    }
}
