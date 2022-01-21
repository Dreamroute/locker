package com.github.dreamroute.locker.interceptor;

import static cn.hutool.core.annotation.AnnotationUtil.hasAnnotation;
import static com.github.dreamroute.locker.util.PluginUtil.processTarget;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import java.lang.reflect.InvocationTargetException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.MappedStatement.Builder;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeException;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringUtils;
import com.github.dreamroute.locker.anno.Locker;
import com.github.dreamroute.locker.exception.DataHasBeenModifyException;
import com.github.dreamroute.locker.util.Constent;
import com.google.common.base.CaseFormat;
import cn.hutool.core.util.ReflectUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.update.Update;

/**
 * 原理：
 * 1. 拦截被@Locker标记的setParameters方法进行参数设置，拦截update方法进行更新；
 * 2. 上一步骤中的update方法如果返回值是0，那么查询一次被更新的数据；
 * 3. 如果version > 当前值，那么就抛出异常；
 *
 * @author w.dehi
 * 
 * 版本字段的类型不再需要是 Long，支持 Integer 和 Short
 * @author moriyasujapan
 */
@EnableConfigurationProperties({LockerProperties.class, MybatisProperties.class})
@Intercepts({
        @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class LockerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final LockerProperties lockerProperties;
    private final MybatisProperties mybatisProperties;

    private List<String> ids = new ArrayList<>();
    private final Map<String, String> selectMap = new ConcurrentHashMap<>();
    private Configuration config;

    private static final Integer UPDATE_FAILD = 0;

    public LockerInterceptor(LockerProperties lockerProperties, MybatisProperties mybatisProperties) {
        this.lockerProperties = lockerProperties;
        this.mybatisProperties = mybatisProperties;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 将此方法移动到Spring容器初始化之后执行的原因是：如果放在下方的intercept方法中来执行，
        // 那么就会有并发问题（获取ms的sqlSource然后修改sqlSource），那么就需要对该方法加锁，影响性能
        SqlSessionFactory sqlSessionFactory = event.getApplicationContext().getBean(SqlSessionFactory.class);
        this.config = sqlSessionFactory.getConfiguration();
        updateSql();
    }


    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getName();
        if (Objects.equals(methodName, "setParameters")) {
            return setParameters(invocation);
        }

        Integer result = (Integer) invocation.proceed();

        // 如果返回值是0，说明没更新成功，那么判断是否是因为并发修改造成的，如果是并发修改，那么抛异常
        if (Objects.equals(result, UPDATE_FAILD) && lockerProperties.isFailThrowException()) {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            String id = ms.getId();
            // 不需要乐观锁的方法，直接pass
            if (!ids.contains(id)) {
                return invocation.proceed();
            }

            String selectSql = selectMap.get(id);
            if (StringUtils.isEmpty(selectSql)) {
                selectSql = createSelect(ms);
                selectMap.put(id, selectSql);
            }
            String[] split = selectSql.split(":");
            String idName = split[1];
            String sql = split[0];
            //将下划线映射为CamelCase的话，则将其转换
            if (mybatisProperties.isMapUnderscodeToCamelCase()) {
              idName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, idName);
            }
            Executor executor = (Executor) (processTarget(invocation.getTarget()));
            Transaction transaction = executor.getTransaction();
            ParameterMapping pm = new ParameterMapping.Builder(config, idName, Object.class).build();
            List<ParameterMapping> parameterMappings = newArrayList(pm);

            Object arg = invocation.getArgs()[1];
            Object value = ReflectUtil.getFieldValue(arg, idName);
            BoundSql select = new BoundSql(config, sql, parameterMappings, value);
            // 凡是使用config.newXxx的和自己使用newXxx的，区别在于前者 会被插件拦截，而后者不会
            // 说明：这里不能使用上方的ms而是新创建ms使用特殊id，是因为如果使用上方的ms，那么就ms的id就是update的id，在此插件的缓存中，设置参数的时候会报错，而这里根本不需要执行下方的setParameters方法
            // 如果新建ms的话，id不在缓存中，就不需要执行setParameters方法
            MappedStatement m = new Builder(config, "com.[plugin]optimistic_locker_update_faild._inner_select", new StaticSqlSource(config, sql), SqlCommandType.SELECT).build();
            StatementHandler sh = config.newStatementHandler(executor, m, value, RowBounds.DEFAULT, null, select);
            Statement selectStmt = prepareStatement(transaction, sh);
            ((PreparedStatement) selectStmt).execute();
            ResultSet rs = selectStmt.getResultSet();
            Long v = null;
            while (rs.next()) {
                v = rs.getLong(lockerProperties.getVersionColumn());
            }
            selectStmt.close();

            Long currentVersion = null;
            Object cv = ReflectUtil.getFieldValue(invocation.getArgs()[1], lockerProperties.getVersionColumn());
            if (Objects.equals(cv.getClass(), Long.class) || Objects.equals(cv.getClass(), long.class)) {
              currentVersion = (long)cv;
            } else if(Objects.equals(cv.getClass(), Integer.class) || Objects.equals(cv.getClass(), int.class)) {
              currentVersion = Long.valueOf(String.valueOf(cv));
            } else if(Objects.equals(cv.getClass(), Short.class) || Objects.equals(cv.getClass(), short.class)) {
              currentVersion = Long.valueOf(String.valueOf(cv));
            } else {
              throw new TypeException(Constent.LOG_PREFIX + "property type error, the type of version is "+cv.getClass().getName()+"the type of version property must be Long or long or Integer or int or Short or short.");
            }
            if (v != null && v > currentVersion) {
                throw new DataHasBeenModifyException("data has been modify");
            }
        }
        return result;
    }

    private Statement prepareStatement(Transaction transaction, StatementHandler handler) throws SQLException {
        Statement stmt = handler.prepare(transaction.getConnection(), transaction.getTimeout());
        handler.parameterize(stmt);
        return stmt;
    }

    private String createSelect(MappedStatement ms) throws JSQLParserException {
        String sql = ms.getSqlSource().getBoundSql(null).getSql();
        Update update = (Update) CCJSqlParserUtil.parse(sql);
        String tableName = update.getTable().getName();
        AndExpression ae = (AndExpression) update.getWhere();
        String id = ae.getLeftExpression().toString();

        StringJoiner joiner = new StringJoiner(" ");
        // id字段
        String idName = id.replace("=", "").replace("?", "").trim();
        String selectSql = joiner.add("SELECT").add(lockerProperties.getVersionColumn()).add("FROM").add(tableName).add("WHERE").add(id).toString();

        return selectSql + ":" + idName;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object setParameters(Invocation invocation) throws InvocationTargetException, IllegalAccessException {
        DefaultParameterHandler ph = (DefaultParameterHandler) processTarget(invocation.getTarget());
        MappedStatement ms = (MappedStatement) ReflectUtil.getFieldValue(ph, "mappedStatement");

        // 不需要乐观锁的方法，直接pass
        if (!ids.contains(ms.getId())) {
            return invocation.proceed();
        }

        Object param = ph.getParameterObject();
        MetaObject mo = this.config.newMetaObject(param);
        Object v = mo.getValue(lockerProperties.getVersionColumn());
        Long value = Long.MIN_VALUE;
        Object incrementedValue = null;
        if (Objects.equals(v.getClass(), Long.class) || Objects.equals(v.getClass(), long.class)) {
          value = (long) v;
          incrementedValue = value + 1;
        } else if(Objects.equals(v.getClass(), Integer.class) || Objects.equals(v.getClass(), int.class)) {
          value = Long.valueOf(String.valueOf((int) v));
          incrementedValue = Integer.valueOf(String.valueOf(value + 1));
        } else if(Objects.equals(v.getClass(), Short.class) || Objects.equals(v.getClass(), short.class)) {
          value = Long.valueOf(String.valueOf((short) v));
          incrementedValue = Short.valueOf(String.valueOf(value + 1));
        } else {
          throw new TypeException(Constent.LOG_PREFIX + "property type error, the type of version is "+v.getClass().getName()+"the type of version property must be Long or long or Integer or int or Short or short.");
        }

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

        // 记录原始值，这里执行完毕需要设置回去，不然乐观锁并发修改时候使用parameterHandler里的参数会使用这里设置的值，version比对会出错
        Object original = mo.getValue(lockerProperties.getVersionColumn());

        // 自增
        mo.setValue(lockerProperties.getVersionColumn(), incrementedValue);

        Object result = invocation.proceed();

        // 还原
        mo.setValue(lockerProperties.getVersionColumn(), original);
        return result;
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
