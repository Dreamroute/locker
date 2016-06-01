package com.mook.locker.cache;

import org.apache.ibatis.binding.MapperRegistry;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.exception.UncachedMapperException;

/**
 * Created by wyx on 2016/6/1.
 */
public interface VersionLockerCache {
	    void cacheMappers(MapperRegistry mapperRegistry);
	
	    boolean hasCachedMappers(MapperRegistry mapperRegistry);
	
	    void clear(MapperRegistry mapperRegistry);
	
	    void clear();
	
	    VersionLocker getCachedVersionLock(MapperRegistry mapperRegistry, String id, Class<?>[] params) throws UncachedMapperException;
	
	    VersionLocker getCachedVersionLock(String id, Class<?>[] params) throws UncachedMapperException;
	}
