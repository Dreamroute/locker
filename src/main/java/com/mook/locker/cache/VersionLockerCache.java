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

import java.util.Arrays;

import com.mook.locker.annotation.VersionLocker;

public interface VersionLockerCache {

	boolean containMethodSinature(VersionLockerCache.MethodSignature vm);
	void cacheMethod(VersionLockerCache.MethodSignature vm, VersionLocker locker);
	VersionLocker getVersionLocker(VersionLockerCache.MethodSignature vm);

	class MethodSignature {

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
			// 对同一个方法的判断：1、方法名相同；2、参数列表相同
			return id.equals(ms.id) && Arrays.equals(params, ms.params);
		}

	}
}
