package com.mook.locker.cache;

import com.mook.locker.annotation.VersionLocker;

public interface VersionLockerCache {

	void cacheMethod(VersionLockerCache.MethodSignature vm, VersionLocker locker);

	VersionLocker getVersionLocker(VersionLockerCache.MethodSignature vm);

	static class MethodSignature {
		
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

	}
}
