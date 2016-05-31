package com.mook.locker.interceptor.cache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.binding.MapperRegistry;

/**
 * Created by wyx on 2016/6/1.
 */
public class LocalVersionLockerCache implements VersionLockerCache {

    private final Map<MethodSignature, VersionLocker> cachedMap = new HashMap<>();

    private final Lock cachedLock = new ReentrantLock();

    @Override
    public void cacheMappers(MapperRegistry mapperRegistry) {
        if (mapperRegistry == null) {
            throw new NullPointerException("mapperRegistry cannot be null.");
        }
        if (!hasCachedMappers()) {
            Collection<Class<?>> mappers = mapperRegistry.getMappers();
            if (null != mappers && !mappers.isEmpty()) {
                Map<MethodSignature, VersionLocker> map = new HashMap<>();
                for (Class<?> mapper : mappers) {
                    addCacheEntry(map, mapper);
                }
                final Lock cachedLock = this.cachedLock;
                cachedLock.lock();
                try {
                    if (!hasCachedMappers()) {
                        cachedMap.putAll(map);
                    }
                } finally {
                    cachedLock.unlock();
                }
            }
        }
    }

    protected void addCacheEntry(Map<MethodSignature, VersionLocker> map, Class<?> mapper) {
        if (mapper == null) {
            throw new NullPointerException("mapper cannot be null.");
        }
        String mapperName = mapper.getName();
        for (Method method : mapper.getDeclaredMethods()) {
            String id = mapperName + "." + method.getName();
            Class<?>[] params = method.getParameterTypes();
            MethodSignature methodSignature = new MethodSignature(id, params);
            VersionLocker versionLocker = method.getAnnotation(VersionLocker.class);
            map.put(methodSignature, versionLocker);
        }
    }

    @Override
    public boolean hasCachedMappers() {
        return !cachedMap.isEmpty();
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
