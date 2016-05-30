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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.Resources;
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
import org.apache.ibatis.reflection.Reflector;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;

/**
 * <p>MyBatis乐观锁插件<br>
 * 
 * <p>该插件比较适合与mybatis generator或者其他自动化插件整合；<br>
 * 不太适合所有单表CRUD都自己手写的用户<br>
 * 
 * 原理描述：只拦截update方法，改写原生sql，自动递增version。举例：<br>
 * ==>原生SQL：[update user set name = ?, password = ?, version = ? where id = ?]<br>
 * ==>改写之后的SQL：[update user set name = ?, password = ?, version = ? where id = ? and version = ?]<br>
 * <p>
 * 第一个version的值为原始从数据库查询的返回值自增1的结果，第二个version为数据库的值<br>
 * <p>
 * 原则上来说，只要是配置了本插件，所有的update方法都会被拦截并且改写，<br>
 * 如果希望某些update方法不被拦截，那么只需要在该方法对应的接口上面增加注解@NoVersion<br>
 * <p>本插件也只拦截StatementType为prepared的方法，后续在优化其他的
 * <p>数据库的支持，测试只在mysql上做过，不过update属于标准sql，原则上支持所有数据库
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
	
	Properties props = null;
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object intercept(Invocation invocation) throws Throwable {
		
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
			
			Map<String, Class<?>> mapperMap = new HashMap<String, Class<?>>();
			Collection<Class<?>> mappers = ms.getConfiguration().getMapperRegistry().getMappers();
			if(null != mappers && !mappers.isEmpty()) {
				for (Class<?> me : mappers) {
					mapperMap.put(me.getName(), me);
				}
			}
			
			String id = ms.getId();
			int pos = id.lastIndexOf(".");
			String nameSpace = id.substring(0, pos);
			if(mapperMap.containsKey(nameSpace)) {
				Class<?> mapper = mapperMap.get(nameSpace);
				System.err.println(mapper);
			}
			
			String originalSql = (String) hm.getValue("delegate.boundSql.sql");
			StringBuilder builder = new StringBuilder(originalSql);
			builder.append(" and ");
			builder.append(versionColumn);
			builder.append(" = ?");
			hm.setValue("delegate.boundSql.sql", builder.toString());
			
			if(log.isDebugEnabled()) {
				log.debug("==> originalSql: " + originalSql);
			}
			
			Configuration configuration = (Configuration) hm.getValue("delegate.configuration");
			ParameterMapping versionMapping = new ParameterMapping.Builder(configuration, versionColumn, Object.class).build();
			List<ParameterMapping> paramMappings = (List<ParameterMapping>) hm.getValue("delegate.boundSql.parameterMappings");
			paramMappings.add(versionMapping);
			
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
			Object parameterObject = boundSql.getParameterObject();
			List<ParameterMapping> mappings = boundSql.getParameterMappings();
			ParameterMapping parameterMapping = mappings.get(mappings.size() - 1);
			
			MetaObject pm = configuration.newMetaObject(parameterObject);
	        Object value = pm.getValue(versionColumn);
			TypeHandler typeHandler = parameterMapping.getTypeHandler();
	        JdbcType jdbcType = parameterMapping.getJdbcType();
	        
	        if (value == null && jdbcType == null) {
	        	 jdbcType = configuration.getJdbcTypeForNull();
	        }
	 		try {
	 			PreparedStatement ps = (PreparedStatement) invocation.getArgs()[0];
	 			typeHandler.setParameter(ps, mappings.size(), value, jdbcType);
	 			
	 			Object val = castTypeAndIncr(value, parameterObject);
	 			pm.setValue(versionColumn, val);
	 			mappings.remove(mappings.size() - 1);
	 			
	 		} catch (TypeException e) {
	 			throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
	 		} catch (SQLException e) {
	 			throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
	 		}
			return invocation.proceed();
		}
		return invocation.proceed();
		
	}

	private Object castTypeAndIncr(Object value, Object parameterObject) {
		Class<?> valType = value.getClass();
		if(valType == Long.class || valType == long.class) {
			return (Long) value + 1;
		} else if(valType == Integer.class || valType == int.class) {
			return (Integer) value + 1;
		} else if(valType == Float.class || valType == float.class) {
			return (Float) value + 1;
		} else if(valType == Double.class || valType == double.class) {
			return (Double) value + 1;
		} else {
			throw new  TypeException("Property 'version' in " + parameterObject.getClass().getSimpleName() + 
					" must be [ long, int, float, double ] or [ Long, Integer, Float, Double ]");
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

}