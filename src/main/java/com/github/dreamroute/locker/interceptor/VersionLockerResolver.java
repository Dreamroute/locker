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
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;

import com.github.dreamroute.locker.annotation.Locker;
import com.github.dreamroute.locker.cache.Cache;
import com.github.dreamroute.locker.cache.LockerCache;
import com.github.dreamroute.locker.cache.LockerCacheImpl;
import com.github.dreamroute.locker.exception.LockerException;
import com.github.dreamroute.locker.util.Constent;
import com.github.dreamroute.reflect.MethodFactory;

class VersionLockerResolver {

    private static final Log log = LogFactory.getLog(VersionLockerResolver.class);

    private static final LockerCache lockerCache = new LockerCacheImpl();
    private static final Map<String, Class<?>> mapperMap = new ConcurrentHashMap<>();

    private static final Locker falseLocker;
    static {
        try {
            falseLocker = VersionLockerResolver.class.getDeclaredMethod("falseVersionValue").getAnnotation(Locker.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new LockerException("Optimistic Locker Plugin init faild." + e, e);
        }
    }

    @Locker(false)
    private void falseVersionValue() {
        // no thing to do.
    }

    static boolean resolve(MetaObject mo, String originalSql) {
        MappedStatement ms = (MappedStatement) mo.getValue("mappedStatement");
        if (Objects.equals(ms.getSqlCommandType(), SqlCommandType.UPDATE)) {
            return Pattern.matches("\\s*?version\\s*?=\\s*?\\?\\s*?", originalSql);
        }
        return false;
    }

    static Locker resolve(MetaObject mo) {

        // if the method is not a 'update', return false
        MappedStatement ms = (MappedStatement) mo.getValue("mappedStatement");
        if (!Objects.equals(ms.getSqlCommandType(), SqlCommandType.UPDATE))
            return null;

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
        Cache.MethodSignature signature = new Cache.MethodSignature(id, paramCls);
        Locker locker = lockerCache.getVersionLocker(signature);
        if (null != locker)
            return locker;

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
        Collection<Method> methods = MethodFactory.findForClass(mapper).values();
        for (Method method : methods) {
            if (Objects.equals(method.getName(), id.substring(pos + 1))) {
                m = method;
                break;
            }
        }
        locker = m.getAnnotation(Locker.class);
        if (null == locker) {
            locker = falseLocker;
        }
        if (!lockerCache.containMethodSignature(signature)) {
            lockerCache.cacheMethod(signature, locker);
        }
        return locker;
    }

}
