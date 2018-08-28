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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;

import com.github.dreamroute.locker.annotation.VersionLocker;
import com.github.dreamroute.locker.util.Constent;
import com.github.dreamroute.locker.util.PluginUtil;

/**
 * <p>
 * MyBatis乐观锁插件<br>
 * <p>
 * MyBatis Optimistic Locker Plugin<br>
 * 
 * @author 342252328@qq.com
 * @date 2016-05-27
 * @version 1.0
 * @since JDK1.7
 *
 */
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }),
        @Signature(type = ParameterHandler.class, method = "setParameters", args = { PreparedStatement.class }) })
public class OptimisticLocker implements Interceptor {

    private static final Log log = LogFactory.getLog(OptimisticLocker.class);
    private String versionColumn;
    
    @Override
    public void setProperties(Properties properties) {
        versionColumn = properties.getProperty("versionColumn", "version");
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object intercept(Invocation invocation) throws Exception {

        String interceptMethod = invocation.getMethod().getName();
        if ("prepare".equals(interceptMethod)) {

            StatementHandler routingHandler = (StatementHandler) PluginUtil.processTarget(invocation.getTarget());
            MetaObject routingMeta = SystemMetaObject.forObject(routingHandler);
            MetaObject hm = routingMeta.metaObjectForProperty("delegate");

            VersionLocker vl = VersionLockerResolver.resolve(hm);
            if (null != vl && !vl.value()) {
                return invocation.proceed();
            }

            String originalSql = (String) hm.getValue("boundSql.sql");
            StringBuilder builder = new StringBuilder(originalSql);
            builder.append(" AND ");
            builder.append(versionColumn);
            builder.append(" = ?");
            hm.setValue("boundSql.sql", builder.toString());

        } else if ("setParameters".equals(interceptMethod)) {

            ParameterHandler handler = (ParameterHandler) PluginUtil.processTarget(invocation.getTarget());
            MetaObject hm = SystemMetaObject.forObject(handler);

            VersionLocker vl = VersionLockerResolver.resolve(hm);
            if (null != vl && !vl.value()) {
                return invocation.proceed();
            }

            BoundSql boundSql = (BoundSql) hm.getValue("boundSql");
            Object parameterObject = boundSql.getParameterObject();
            if (parameterObject instanceof MapperMethod.ParamMap<?>) {
                MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) parameterObject;
                if (!paramMap.containsKey(versionColumn)) {
                    throw new TypeException("All the primitive type parameters must add MyBatis's @Param Annotaion");
                }
            }

            Configuration configuration = ((MappedStatement) hm.getValue("mappedStatement")).getConfiguration();
            MetaObject pm = configuration.newMetaObject(parameterObject);
            Object value = pm.getValue(versionColumn);
            ParameterMapping versionMapping = new ParameterMapping.Builder(configuration, versionColumn, Object.class).build();
            TypeHandler typeHandler = versionMapping.getTypeHandler();
            JdbcType jdbcType = versionMapping.getJdbcType();

            if (value == null && jdbcType == null) {
                jdbcType = configuration.getJdbcTypeForNull();
            }

            int versionLocation = boundSql.getParameterMappings().size() + 1;
            try {
                PreparedStatement ps = (PreparedStatement) invocation.getArgs()[0];
                typeHandler.setParameter(ps, versionLocation, value, jdbcType);
            } catch (TypeException | SQLException e) {
                throw new TypeException("set parameter 'version' faild, Cause: " + e, e);
            }

            if (!Objects.equals(value.getClass(), Long.class) && Objects.equals(value.getClass(), long.class) && log.isDebugEnabled()) {
                log.error(Constent.LOG_PREFIX + "property type error, the type of version property must be Long or long.");
            }

            // increase version
            pm.setValue(versionColumn, (long) value + 1);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler || target instanceof ParameterHandler)
            return Plugin.wrap(target, this);
        return target;
    }

}