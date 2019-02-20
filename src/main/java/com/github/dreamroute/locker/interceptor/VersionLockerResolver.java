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
package com.github.dreamroute.locker.interceptor;

import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;

import com.github.dreamroute.locker.annotation.VersionLocker;
import com.github.dreamroute.locker.cache.Cache;
import com.github.dreamroute.locker.cache.LocalVersionLockerCache;
import com.github.dreamroute.locker.cache.VersionLockerCache;
import com.github.dreamroute.locker.exception.LockerException;
import com.github.dreamroute.locker.util.Constent;

class VersionLockerResolver {

    private static final Log log = LogFactory.getLog(VersionLockerResolver.class);

    /** versionLockerCache, mapperMap are ConcurrentHashMap, thread safe **/
    private static final VersionLockerCache versionLockerCache = new LocalVersionLockerCache();
    private static final Map<String, Class<?>> mapperMap = new ConcurrentHashMap<>();

    private static final VersionLocker trueLocker;
    private static final VersionLocker falseLocker;
    static {
        try {
            trueLocker = VersionLockerResolver.class.getDeclaredMethod("trueVersionValue").getAnnotation(VersionLocker.class);
            falseLocker = VersionLockerResolver.class.getDeclaredMethod("falseVersionValue").getAnnotation(VersionLocker.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new LockerException("Optimistic Locker Plugin init faild." + e, e);
        }
    }

    @VersionLocker(true)
    private void trueVersionValue() {
        // no thing to do.
    }

    @VersionLocker(false)
    private void falseVersionValue() {
        // no thing to do.
    }

    static VersionLocker resolve(MetaObject mo) {

        // if the method is not a 'update', return false
        MappedStatement ms = (MappedStatement) mo.getValue("mappedStatement");
        if (!Objects.equals(ms.getSqlCommandType(), SqlCommandType.UPDATE))
            return falseLocker;

        BoundSql boundSql = (BoundSql) mo.getValue("boundSql");
        Object paramObj = boundSql.getParameterObject();
        Class<?>[] paramCls = null;

        /****************** Process param must order by below ***********************/
        // 1、Process @Param param
        if (paramObj instanceof MapperMethod.ParamMap<?>) {
            MapperMethod.ParamMap<?> mmp = (MapperMethod.ParamMap<?>) paramObj;
            if (!mmp.isEmpty()) {
                paramCls = new Class<?>[mmp.size() >> 1];
                int mmpLen = mmp.size() >> 1;
                for (int i = 0; i < mmpLen; i++) {
                    Object index = mmp.get("param" + (i + 1));
                    paramCls[i] = index.getClass();
                }
            }

            // 2、Process Map param
        } else if (paramObj instanceof Map) {
            paramCls = new Class<?>[] {Map.class};

            // 3、Process POJO entity param
        } else {
            paramCls = new Class<?>[] {paramObj.getClass()};
        }

        String id = ms.getId();
        Cache.MethodSignature vm = new Cache.MethodSignature(id, paramCls);
        VersionLocker versionLocker = versionLockerCache.getVersionLocker(vm);
        if (null != versionLocker)
            return versionLocker;

        if (mapperMap.isEmpty()) {
            Collection<Class<?>> mappers = ms.getConfiguration().getMapperRegistry().getMappers();
            if (null != mappers && !mappers.isEmpty()) {
                for (Class<?> me : mappers) {
                    mapperMap.put(me.getName(), me);
                }
            }
        }

        int pos = id.lastIndexOf('.');
        String nameSpace = id.substring(0, pos);
        if (!mapperMap.containsKey(nameSpace) && log.isDebugEnabled()) {
            log.debug(Constent.LOG_PREFIX + "Config info error, maybe you have not config the Mapper interface");
            throw new LockerException("Config info error, maybe you have not config the Mapper interface");
        }
        Class<?> mapper = mapperMap.get(nameSpace);
        Method m = null;
        Method[] methods = getClassMethods(mapper);
        for (Method method : methods) {
            if (Objects.equals(method.getName(), id.substring(pos + 1))) {
                m = method;
                break;
            }
        }
        versionLocker = m.getAnnotation(VersionLocker.class);
        if (null == versionLocker) {
            versionLocker = trueLocker;
        }
        if (!versionLockerCache.containMethodSignature(vm)) {
            versionLockerCache.cacheMethod(vm, versionLocker);
        }
        return versionLocker;
    }
    
    private static Method[] getClassMethods(Class<?> cls) {
        Map<String, Method> uniqueMethods = new HashMap<>();
        Class<?> currentClass = cls;
        while (currentClass != null && currentClass != Object.class) {
            addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

            // we also need to look for interface methods -
            // because the class may be abstract
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                addUniqueMethods(uniqueMethods, anInterface.getMethods());
            }

            currentClass = currentClass.getSuperclass();
        }

        Collection<Method> methods = uniqueMethods.values();

        return methods.toArray(new Method[methods.size()]);
    }

    private static void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
        for (Method currentMethod : methods) {
            if (!currentMethod.isBridge()) {
                String signature = getSignature(currentMethod);
                // check to see if the method is already known
                // if it is known, then an extended class must have
                // overridden a method
                if (!uniqueMethods.containsKey(signature)) {
                    if (canAccessPrivateMethods()) {
                        try {
                            currentMethod.setAccessible(true);
                        } catch (Exception e) {
                            // Ignored. This is only a final precaution, nothing we can do.
                        }
                    }

                    uniqueMethods.put(signature, currentMethod);
                }
            }
        }
    }

    private static boolean canAccessPrivateMethods() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }
    
    private static String getSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            sb.append(returnType.getName()).append('#');
        }
        sb.append(method.getName());
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i == 0) {
                sb.append(':');
            } else {
                sb.append(',');
            }
            sb.append(parameters[i].getName());
        }
        return sb.toString();
    }

}
