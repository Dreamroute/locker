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
package com.mook.locker.interceptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.interceptor.cache.LocalVersionLockerCache;
import com.mook.locker.interceptor.cache.VersionLockerCache;
import com.mook.locker.interceptor.cache.exception.UncachedMapperException;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
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

/**
 * <p>MyBatis乐观锁插件<br>
 * 
 * @author 342252328@qq.com
 * @date 2016-05-27
 * @version 1.0
 * @since JDK1.7
 *
 */
@Intercepts({
	@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class}),
	@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
})
public class OptimisticLocker implements Interceptor {
	
	private static final Log log = LogFactory.getLog(OptimisticLocker.class);
	
	private Properties props = null;

    private VersionLockerCache versionLockerCache = new LocalVersionLockerCache();
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object intercept(Invocation invocation) throws Exception {

		String interceptMethod = invocation.getMethod().getName();
		String versionColumn = props.getProperty("versionColumn", "version");
		
		if("prepare".equals(interceptMethod)) {
			
			StatementHandler handler = (StatementHandler) invocation.getTarget();
			MetaObject hm = SystemMetaObject.forObject(handler);
			
			MappedStatement ms = (MappedStatement) hm.getValue("delegate.mappedStatement");
			SqlCommandType sqlCmdType = ms.getSqlCommandType();
			if(sqlCmdType != SqlCommandType.UPDATE) {
				return invocation.proceed();
			}
			
			BoundSql boundSql = (BoundSql) hm.getValue("delegate.boundSql");
			if(hasVersionLocker(ms, boundSql)) {
				return invocation.proceed();
			}
			
			Object originalVersion = hm.getValue("delegate.boundSql.parameterObject.version");
			Object versionIncr = castTypeAndOptValue(originalVersion, hm.getValue("delegate.boundSql.parameterObject"), ValueType.INCREASE);
			hm.setValue("delegate.boundSql.parameterObject.version", versionIncr);

			String originalSql = (String) hm.getValue("delegate.boundSql.sql");
			StringBuilder builder = new StringBuilder(originalSql);
			builder.append(" and ");
			builder.append(versionColumn);
			builder.append(" = ?");
			hm.setValue("delegate.boundSql.sql", builder.toString());
			
			if(log.isDebugEnabled()) {
				log.debug("==> originalSql: " + originalSql);
			}
			
			return invocation.proceed();
			
		} else if("setParameters".equals(interceptMethod)) {
			
			ParameterHandler handler = (ParameterHandler) invocation.getTarget();
			MetaObject hm = SystemMetaObject.forObject(handler);
			
			MappedStatement ms = (MappedStatement) hm.getValue("mappedStatement");
			SqlCommandType sqlCmdType = ms.getSqlCommandType();
			if(sqlCmdType != SqlCommandType.UPDATE) {
				return invocation.proceed();
			}
			
			Configuration configuration = (Configuration) hm.getValue("configuration");
			BoundSql boundSql = (BoundSql) hm.getValue("boundSql");
			
			if(hasVersionLocker(ms, boundSql)) {
				return invocation.proceed();
			}
			
			Object result = invocation.proceed();

			ParameterMapping versionMapping = new ParameterMapping.Builder(configuration, versionColumn, Object.class).build();

			Object parameterObject = boundSql.getParameterObject();
			
			MetaObject pm = configuration.newMetaObject(parameterObject);
			if(parameterObject instanceof MapperMethod.ParamMap<?>) {
				MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) parameterObject;
				if(!paramMap.containsKey(versionColumn)) {
					throw new TypeException("基本类型的接口参数必须全部加上MyBatis的@Param标记");
				}
			}
	        Object value = pm.getValue(versionColumn);
			TypeHandler typeHandler = versionMapping.getTypeHandler();
	        JdbcType jdbcType = versionMapping.getJdbcType();
	        
	        if (value == null && jdbcType == null) {
	        	 jdbcType = configuration.getJdbcTypeForNull();
	        }
	        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
	 		try {
	 			PreparedStatement ps = (PreparedStatement) invocation.getArgs()[0];
	 			Object val = castTypeAndOptValue(value, parameterObject, ValueType.DECREASE);
	 			typeHandler.setParameter(ps, parameterMappings.size() + 1, val, jdbcType);
	 		} catch (TypeException | SQLException e) {
	 			throw new TypeException("Could not set parameters for mapping: " + parameterMappings + ". Cause: " + e, e);
	 		}
			return result;
		}
		return invocation.proceed();
	}

	private Object castTypeAndOptValue(Object value, Object parameterObject, ValueType vt) {
		Class<?> valType = value.getClass();
		if(valType == Long.class || valType == long.class) {
			return (Long) value + vt.value;
		} else if(valType == Integer.class || valType == int.class) {
			return (Integer) value + vt.value;
		} else if(valType == Float.class || valType == float.class) {
			return (Float) value + vt.value;
		} else if(valType == Double.class || valType == double.class) {
			return (Double) value + vt.value;
		} else {
			if(parameterObject instanceof MapperMethod.ParamMap<?>) {
				throw new TypeException("基本类型的接口参数必须全部加上MyBatis的@Param标记");
			} else {
				throw new  TypeException("Property 'version' in " + parameterObject.getClass().getSimpleName() +
						" must be [ long, int, float, double ] or [ Long, Integer, Float, Double ]");
			}
		}
	}

	private boolean hasVersionLocker(MappedStatement ms, BoundSql boundSql) {
        Object paramObj = boundSql.getParameterObject();
        Class<?>[] paramCls = new Class<?>[]{
                paramObj.getClass()
        };

		if(paramObj instanceof MapperMethod.ParamMap<?>) {
			MapperMethod.ParamMap<?> mmp = (MapperMethod.ParamMap<?>) paramObj;
			if(null != mmp && !mmp.isEmpty()) {
				paramCls = new Class<?>[mmp.size() / 2];
				int mmpLen = mmp.size() / 2;
				for(int i=0; i<mmpLen; i++) {
					Object index = mmp.get("param" + (i + 1));
					paramCls[i] = index.getClass();
				}
			}
		}
		
		String id = ms.getId();
        versionLockerCache.cacheMappers(ms.getConfiguration(), id, paramCls);
		VersionLocker versionLocker = null;
		try {
			versionLocker = versionLockerCache.getAnnotation(id, paramCls);
		} catch (UncachedMapperException e) {
			throw new RuntimeException("配置错误", e);
		}
		if (null != versionLocker && versionLocker.value() == false) {
			return true;
		} else {
			return false;
		}

	}

	@Override
	public Object plugin(Object target) {
		if (target instanceof StatementHandler || target instanceof ParameterHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
	}

	@Override
	public void setProperties(Properties properties) {
		if(null != properties && !properties.isEmpty()) props = properties;
	}
	
	private enum ValueType {
		INCREASE(1), DECREASE(-1);
		
		private Integer value;
		
		private ValueType(Integer value) {
			this.value = value;
		}
	}

}