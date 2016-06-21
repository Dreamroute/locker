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
package com.mook.locker.interceptor;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.cache.Cache;
import com.mook.locker.cache.Cache.MethodSignature;
import com.mook.locker.cache.LocalVersionLockerCache;
import com.mook.locker.cache.VersionLockerCache;
import com.mook.locker.util.PluginUtil;

/**
 * <p>MyBatis乐观锁插件<br>
 * <p>MyBatis Optimistic Locker Plugin<br>
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
	
	private static VersionLocker trueLocker;
	static {
		try {
			trueLocker = OptimisticLocker.class.getDeclaredMethod("versionValue").getAnnotation(VersionLocker.class);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("The plugin init faild.", e);
		}
	}
	
	private Properties props = null;
	private VersionLockerCache versionLockerCache = new LocalVersionLockerCache();
	Map<String, Class<?>> mapperMap = null;
	
	@VersionLocker(true)
	private void versionValue() {}
	
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object intercept(Invocation invocation) throws Exception {
		
		String versionColumn;
		if(null == props || props.isEmpty()) {
			versionColumn = "version";
		} else {
			versionColumn = props.getProperty("versionColumn", "version");
		}
		
		String interceptMethod = invocation.getMethod().getName();
		if("prepare".equals(interceptMethod)) {
			
			StatementHandler handler = (StatementHandler) PluginUtil.processTarget(invocation.getTarget());
			MetaObject hm = SystemMetaObject.forObject(handler);
			
			MappedStatement ms = (MappedStatement) hm.getValue("delegate.mappedStatement");
			SqlCommandType sqlCmdType = ms.getSqlCommandType();
			if(sqlCmdType != SqlCommandType.UPDATE) {
				return invocation.proceed();
			}
			
			BoundSql boundSql = (BoundSql) hm.getValue("delegate.boundSql");
			
			VersionLocker vl = getVersionLocker(ms, boundSql);
			if(null != vl && !vl.value()) {
				return invocation.proceed();
			}
			
			Object originalVersion = hm.getValue("delegate.boundSql.parameterObject." + versionColumn);
			Object versionIncr = castTypeAndOptValue(originalVersion, hm.getValue("delegate.boundSql.parameterObject"), ValueType.INCREASE);
			hm.setValue("delegate.boundSql.parameterObject." + versionColumn, versionIncr);
			
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
			
			ParameterHandler handler = (ParameterHandler) PluginUtil.processTarget(invocation.getTarget());
			MetaObject hm = SystemMetaObject.forObject(handler);
			
			MappedStatement ms = (MappedStatement) hm.getValue("mappedStatement");
			SqlCommandType sqlCmdType = ms.getSqlCommandType();
			if(sqlCmdType != SqlCommandType.UPDATE) {
				return invocation.proceed();
			}
			
			Configuration configuration = ms.getConfiguration();
			BoundSql boundSql = (BoundSql) hm.getValue("boundSql");
			
			VersionLocker vl = getVersionLocker(ms, boundSql);
			if(null != vl && !vl.value()) {
				return invocation.proceed();
			}
			
			Object result = invocation.proceed();
			
			ParameterMapping versionMapping = new ParameterMapping.Builder(configuration, versionColumn, Object.class).build();
			
			Object parameterObject = boundSql.getParameterObject();
			
			MetaObject pm = configuration.newMetaObject(parameterObject);
			if(parameterObject instanceof MapperMethod.ParamMap<?>) {
				MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) parameterObject;
				if(!paramMap.containsKey(versionColumn)) {
					throw new TypeException("All the base type parameters must add MyBatis's @Param Annotaion");
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
	 		} catch (TypeException e) {
	 			throw new TypeException("Could not set parameters for mapping: " + parameterMappings + ". Cause: " + e, e);
	 		} catch (SQLException e) {
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
				throw new TypeException("All the base type parameters must add MyBatis's @Param Annotaion");
			} else {
				throw new  TypeException("Property 'version' in " + parameterObject.getClass().getSimpleName() + 
						" must be [ long, int, float, double ] or [ Long, Integer, Float, Double ]");
			}
		}
	}
	
	private VersionLocker getVersionLocker(MappedStatement ms, BoundSql boundSql) {
		
		Class<?>[] paramCls = null;
		Object paramObj = boundSql.getParameterObject();
		
		/******************下面处理参数只能按照下面3个的顺序***********************/
		/******************Process param must order by below ***********************/
		// 1、处理@Param标记的参数
		// 1、Process @Param param
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
			
		// 2、处理Map类型参数
		// 2、Process Map param
		} else if (paramObj instanceof Map) {
			paramCls = new Class<?>[] {Map.class};
			
		// 3、处理POJO实体对象类型的参数
		// 3、Process POJO entity param
		} else {
			paramCls = new Class<?>[] {paramObj.getClass()};
		}
		
		String id = ms.getId();
		Cache.MethodSignature vm = new MethodSignature(id, paramCls);
		VersionLocker versionLocker = versionLockerCache.getVersionLocker(vm);
		if(null != versionLocker) {
			return versionLocker;
		}
		
		// 这里去掉synchronized或者重入锁，因为这里的操作满足幂等性
		// Here remove synchronized keyword or ReentrantLock, because it's a idempotent operation
		if(null == mapperMap || mapperMap.isEmpty()) {
			mapperMap = new HashMap<>();
			Collection<Class<?>> mappers = ms.getConfiguration().getMapperRegistry().getMappers();
			if(null != mappers && !mappers.isEmpty()) {
				for (Class<?> me : mappers) {
					mapperMap.put(me.getName(), me);
				}
			}
		}
		
		int pos = id.lastIndexOf(".");
		String nameSpace = id.substring(0, pos);
		if(mapperMap.containsKey(nameSpace)) {
			Class<?> mapper = mapperMap.get(nameSpace);
			Method m;
			try {
				m = mapper.getDeclaredMethod(id.substring(pos + 1), paramCls);
				
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException("The Map type param error." + e, e);
			}
			versionLocker = m.getAnnotation(VersionLocker.class);
			if(null == versionLocker) {
				versionLocker = trueLocker;
			}
			if(!versionLockerCache.containMethodSignature(vm)) {
				versionLockerCache.cacheMethod(vm, versionLocker);
			}
			return versionLocker;
		} else {
			throw new RuntimeException("Config info error, maybe you have not config the Mapper interface");
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