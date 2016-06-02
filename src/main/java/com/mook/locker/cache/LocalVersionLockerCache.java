/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.mook.locker.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.mook.locker.annotation.VersionLocker;

public class LocalVersionLockerCache implements VersionLockerCache {
	
	private ConcurrentHashMap<String, ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker>> caches = new ConcurrentHashMap<>();
	
	@Override
	public boolean containMethodSinature(MethodSignature vm) {
		String nameSpace = getNameSpace(vm);
		ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker> cache = caches.get(nameSpace);
		if(null == cache || cache.isEmpty()) {
			return false;
		}
		return cache.containsKey(vm);
	}
	
	@Override
	public synchronized void cacheMethod(VersionLockerCache.MethodSignature vm, VersionLocker locker) {
		String nameSpace = getNameSpace(vm);
		ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker> cache = caches.get(nameSpace);
		if(null == cache || cache.isEmpty()) {
			cache = new ConcurrentHashMap<>();
			cache.put(vm, locker);
			caches.put(nameSpace, cache);
		} else {
			cache.put(vm, locker);
		}
	}

	@Override
	public VersionLocker getVersionLocker(VersionLockerCache.MethodSignature vm) {
		String nameSpace = getNameSpace(vm);
		ConcurrentHashMap<VersionLockerCache.MethodSignature, VersionLocker> cache = caches.get(nameSpace);
		if(null == cache || cache.isEmpty()) {
			return null;
		}
		return cache.get(vm);
	}

	private String getNameSpace(VersionLockerCache.MethodSignature vm) {
		String id = vm.getId();
		int pos = id.lastIndexOf(".");
		String nameSpace = id.substring(0, pos);
		return nameSpace;
	}

}
