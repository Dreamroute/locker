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
package com.github.dreamroute.locker.util;

import java.lang.reflect.Proxy;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import com.github.dreamroute.locker.exception.LockerException;

public final class PluginUtil {

    private static final Log log = LogFactory.getLog(PluginUtil.class);

    private PluginUtil() {} // private constructor

    /**
     * <p>
     * Recursive get the original target object.
     * <p>
     * If integrate more than a plugin, maybe there are conflict in these plugins,
     * because plugin will proxy the object.<br>
     * So, here get the orignal target object
     * 
     * @param target proxy-object
     * @return original target object
     */
    public static Object processTarget(Object target) {
        if (Proxy.isProxyClass(target.getClass())) {
            MetaObject mo = SystemMetaObject.forObject(target);
            return processTarget(mo.getValue("h.target"));
        }

        // must keep the result object is StatementHandler or ParameterHandler in
        // Optimistic Loker plugin
        if (!(target instanceof StatementHandler) && !(target instanceof ParameterHandler)) {
            if (log.isDebugEnabled()) {
                log.error(Constent.LOG_PREFIX + "plugin init faild.");
            }
            throw new LockerException(Constent.LOG_PREFIX + "plugin init faild.");
        }
        return target;
    }

}
