package com.mook.locker.interceptor.cache;

import java.util.Arrays;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.session.Configuration;

/**
 * Created by wyx on 2016/6/1.
 */
public interface VersionLockerCache extends AnnotationCache<VersionLockerCache.MethodSignature, VersionLocker> {
    boolean cacheMappers(Configuration configuration, String id, Class<?>[] params);

    boolean hasCachedMappers();

    VersionLocker getAnnotation(String id, Class<?>[] params) throws UncachedMapperException;

    VersionLocker getAnnotation(MethodSignature methodSignature) throws UncachedMapperException;

    class MethodSignature {
        private String id;

        private Class<?>[] params;

        MethodSignature(String id, Class<?>[] params) {
            this.id = id;
            this.params = params;
        }

        String getId() {
            return id;
        }

        void setId(String id) {
            this.id = id;
        }

        Class<?>[] getParams() {
            return params;
        }

        void setParams(Class<?>[] params) {
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
}
