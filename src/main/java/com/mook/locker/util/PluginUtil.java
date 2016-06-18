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
package com.mook.locker.util;

import java.lang.reflect.Proxy;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

public final class PluginUtil {
	
	private PluginUtil() {} // private constructor
	
	/**
	 * <p>Recursive get the original target object.
	 * <p>If integrate more than a plugin, maybe there are conflict in these plugins, because plugin will proxy the object.
	 * 
	 * @param target proxy-object
	 * @return original target object
	 */
	public static Object processTarget(Object target) {
		if(Proxy.isProxyClass(target.getClass())) {
			MetaObject mo = SystemMetaObject.forObject(target);
			return processTarget(mo.getValue("h.target"));
		}
		return target;
	}
	
}
