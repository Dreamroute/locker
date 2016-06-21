/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 342252328@qq.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.mook.locker.cache;

import java.util.Arrays;

import com.mook.locker.annotation.VersionLocker;

public interface Cache<T> {
	
	boolean containMethodSignature(VersionLockerCache.MethodSignature vm);
	
	void cacheMethod(VersionLockerCache.MethodSignature vm, VersionLocker locker);
	
	T getVersionLocker(VersionLockerCache.MethodSignature vm);
	
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
			// if the method signature is 'equal', must 2 conditions: 1.the method name be the same; 2.the parameters type be the same
			return id.equals(ms.id) && Arrays.equals(params, ms.params);
		}

	}
}
