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
package com.github.dreamroute.locker.cache;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

import com.github.dreamroute.locker.annotation.Locker;
import com.github.dreamroute.locker.util.Constent;

public class LockerCacheImpl implements LockerCache {

    private static final Log log = LogFactory.getLog(LockerCacheImpl.class);
    private ConcurrentHashMap<String, ConcurrentHashMap<LockerCache.MethodSignature, Locker>> caches = new ConcurrentHashMap<>();

    @Override
    public boolean containMethodSignature(LockerCache.MethodSignature signature) {
        String nameSpace = getNameSpace(signature);
        ConcurrentHashMap<LockerCache.MethodSignature, Locker> cache = caches.get(nameSpace);
        if (null == cache || cache.isEmpty()) {
            return false;
        }
        boolean containsMethodSignature = cache.containsKey(signature);
        if (containsMethodSignature && log.isDebugEnabled()) {
            log.debug(Constent.LOG_PREFIX + "The method " + nameSpace + signature.getId() + "is hit in cache.");
        }
        return containsMethodSignature;
    }

    @Override
    public void cacheMethod(LockerCache.MethodSignature signature, Locker locker) {
        String nameSpace = getNameSpace(signature);
        ConcurrentHashMap<LockerCache.MethodSignature, Locker> cache = caches.get(nameSpace);
        if (null == cache || cache.isEmpty()) {
            cache = new ConcurrentHashMap<>();
            cache.put(signature, locker);
            caches.put(nameSpace, cache);
            if (log.isDebugEnabled()) {
                log.debug(Constent.LOG_PREFIX + nameSpace + ": " + signature.getId() + " is cached.");
            }
        } else {
            cache.put(signature, locker);
        }
    }

    @Override
    public Locker getVersionLocker(LockerCache.MethodSignature signature) {
        String nameSpace = getNameSpace(signature);
        ConcurrentHashMap<LockerCache.MethodSignature, Locker> cache = caches.get(nameSpace);
        if (null == cache || cache.isEmpty()) {
            return null;
        }
        return cache.get(signature);
    }

    private String getNameSpace(LockerCache.MethodSignature vm) {
        String id = vm.getId();
        int pos = id.lastIndexOf('.');
        return id.substring(0, pos);
    }
    
}
