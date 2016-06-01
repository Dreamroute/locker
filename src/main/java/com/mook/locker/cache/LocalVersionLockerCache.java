package com.mook.locker.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.mook.locker.annotation.VersionLocker;

public class LocalVersionLockerCache implements VersionLockerCache {
	
	private ConcurrentHashMap<String, ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker>> caches = new ConcurrentHashMap<>();
	
	@Override
	public synchronized void cacheMethod(VersionLockerCache.MethodSignature vm, VersionLocker locker) {
		String id = vm.getId();
		int pos = id.lastIndexOf(".");
		String nameSpace = id.substring(0, pos);
		ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker> cache = caches.get(nameSpace);
		if(null == cache || cache.isEmpty()) {
			cache = new ConcurrentHashMap<>();
			cache.put(vm, locker);
			caches.put(nameSpace, cache);
		}
	}

	@Override
	public VersionLocker getVersionLocker(VersionLockerCache.MethodSignature vm) {
		String id = vm.getId();
		int pos = id.lastIndexOf(".");
		String nameSpace = id.substring(0, pos);
		ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker> cache = caches.get(nameSpace);
		if(null == cache || cache.isEmpty()) {
			return null;
		}
		return cache.get(vm);
	}

}
