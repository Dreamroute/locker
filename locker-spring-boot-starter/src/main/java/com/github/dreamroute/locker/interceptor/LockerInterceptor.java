package com.github.dreamroute.locker.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.github.dreamroute.locker.anno.Locker;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cn.hutool.core.annotation.AnnotationUtil.hasAnnotation;
import static com.github.dreamroute.locker.util.PluginUtil.processTarget;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * @author w.dehi
 */
@Slf4j
@EnableConfigurationProperties(LockerProperties.class)
@Intercepts(@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}))
public class LockerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final LockerProperties lockerProperties;
    private List<String> ids = new ArrayList<>();
    private Configuration config;

    public LockerInterceptor(LockerProperties lockerProperties) {
        this.lockerProperties = lockerProperties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 将此方法移动到Spring容器初始化之后执行的原因是：如果放在下方的intercept方法中来执行，
        // 那么就会有并发问题（获取ms的sqlSource然后修改sqlSource），那么就需要对该方法加锁，影响性能
        SqlSessionFactory sqlSessionFactory = event.getApplicationContext().getBean(SqlSessionFactory.class);
        this.config = sqlSessionFactory.getConfiguration();
        updateSql();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        DefaultParameterHandler ph = (DefaultParameterHandler) processTarget(invocation.getTarget());
        MappedStatement ms = (MappedStatement) ReflectUtil.getFieldValue(ph, "mappedStatement");

        // 不需要乐观锁的方法，直接pass
        if (!ids.contains(ms.getId())) {
            return invocation.proceed();
        }

        Object param = ph.getParameterObject();
        MetaObject mo = this.config.newMetaObject(param);
        long value = (long) mo.getValue(lockerProperties.getVersionColumn());

        ParameterMapping versionMapping = new ParameterMapping.Builder(this.config, lockerProperties.getVersionColumn(), Object.class).build();
        TypeHandler typeHandler = versionMapping.getTypeHandler();
        JdbcType jdbcType = versionMapping.getJdbcType() == null ? this.config.getJdbcTypeForNull() : versionMapping.getJdbcType();
        List<ParameterMapping> pmList = (List<ParameterMapping>) this.config.newMetaObject(ms).getValue("sqlSource.sqlSource.parameterMappings");
        int versionLocation = pmList.size() + 1;
        try {
            PreparedStatement ps = (PreparedStatement) invocation.getArgs()[0];
            typeHandler.setParameter(ps, versionLocation, value, jdbcType);
        } catch (TypeException | SQLException e) {
            throw new TypeException("set parameter 'version' faild, Cause: " + e, e);
        }

        // increase version
        mo.setValue(lockerProperties.getVersionColumn(), value + 1);

        return invocation.proceed();
    }

    private void updateSql() {
        parseAnno();
        update();
    }

    private void update() {
        ofNullable(ids).orElseGet(ArrayList::new).stream().map(this.config::getMappedStatement).forEach(ms -> {
            MetaObject mo = this.config.newMetaObject(ms);
            String beforeSql = (String) mo.getValue("sqlSource.sqlSource.sql");
            String builder = beforeSql + " AND " + lockerProperties.getVersionColumn() + " = ?";
            mo.setValue("sqlSource.sqlSource.sql", builder);
        });
    }

    private void parseAnno() {
        Collection<Class<?>> mappers = this.config.getMapperRegistry().getMappers();
        this.ids = ofNullable(mappers).orElseGet(ArrayList::new).stream()
                .flatMap(mapper -> stream(mapper.getDeclaredMethods())
                        .filter(method -> hasAnnotation(method, Locker.class))
                        .map(m -> mapper.getName() + "." + m.getName()))
                .collect(toList());
    }
}
