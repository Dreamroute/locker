package com.mook.locker.cache;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperRegistry;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.exception.UncachedMapperException;

/**
 * Created by wyx on 2016/6/1.
 */
public class LocalVersionLockerCache implements VersionLockerCache {

    private final ConcurrentHashMap<MapperRegistry, Map<MethodSignature, VersionLocker>> cachedMap = new ConcurrentHashMap<>();

    public static class MethodSignature {
        private String id;

        private Class<?>[] params;

        public MethodSignature(String id, Class<?>[] params) {
            this.id = id;
            this.params = params;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Class<?>[] getParams() {
            return params;
        }

        public void setParams(Class<?>[] params) {
            this.params = params;
        }

        @Override
        public int hashCode() {
            int idHash = id.hashCode();
            int paramsHash = Arrays.hashCode(params);
            return ((idHash >> 16 ^ idHash) << 16) | (paramsHash >> 16 ^ paramsHash);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodSignature)) {
                return super.equals(obj);
            }
            MethodSignature ms = (MethodSignature) obj;
            return id.equals(ms.id) && Arrays.equals(params, ms.params);
        }
    }

    @Override
    public void cacheMappers(MapperRegistry mapperRegistry) {
        if (mapperRegistry == null) {
            throw new NullPointerException("mapperRegistry cannot be null.");
        }
        if (!hasCachedMappers(mapperRegistry)) {
            Map<MethodSignature, VersionLocker> map = new HashMap<>();
            Collection<Class<?>> mappers = mapperRegistry.getMappers();
            if (null != mappers && !mappers.isEmpty()) {
                for (Class<?> mapper : mappers) {
                    addCacheEntry(map, mapper);
                }
            }
            cachedMap.putIfAbsent(mapperRegistry, map);
        }
    }

    private void addCacheEntry(Map<MethodSignature, VersionLocker> map, Class<?> mapper) {
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
    public boolean hasCachedMappers(MapperRegistry mapperRegistry) {
        return cachedMap.containsKey(mapperRegistry);
    }

    @Override
    public void clear(MapperRegistry mapperRegistry) {
        cachedMap.remove(mapperRegistry);
    }

    @Override
    public void clear() {
        cachedMap.clear();
    }

    @Override
    public VersionLocker getCachedVersionLock(MapperRegistry mapperRegistry, String id, Class<?>[] params) throws UncachedMapperException {
        Map<MethodSignature, VersionLocker> map = cachedMap.get(mapperRegistry);
        if (map == null) {
            throw new UncachedMapperException("Cannot find cached data for " + mapperRegistry + " " + id + "(" + Arrays.toString(params) + ").");
        }
        return map.get(new MethodSignature(id, params));
    }

    @Override
    public VersionLocker getCachedVersionLock(String id, Class<?>[] params) throws UncachedMapperException {
        for (MapperRegistry mapperRegistry : cachedMap.keySet()) {
            try {
                return getCachedVersionLock(mapperRegistry, id, params);
            } catch (UncachedMapperException ignored) {

            }
        }
        throw new UncachedMapperException("Cannot find cached data for " + id + "(" + Arrays.toString(params) + ").");
    }
}