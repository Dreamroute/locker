package com.github.dreamroute.locker.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.github.dreamroute.locker.anno.Locker;
import com.github.dreamroute.locker.exception.DataHasBeenModifyException;
import com.github.dreamroute.locker.util.PluginUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
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
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.Transaction;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.StringUtils;

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

import static cn.hutool.core.annotation.AnnotationUtil.hasAnnotation;
import static com.github.dreamroute.locker.util.PluginUtil.getAllMethods;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * 原理：
 * 1. 拦截被@Locker标记的方法，执行更新操作；
 * 2. 上一步骤中的update方法如果返回值是0，那么查询一次被更新的数据；
 * 3. 如果version > 当前值，那么就抛出异常（如果需要抛出异常的话）；
 *
 * @author w.dehi
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class LockerInterceptor implements Interceptor, ApplicationListener<ContextRefreshedEvent> {

    private final String versionColumn;
    private final boolean failThrowException;

    private List<String> ids = new ArrayList<>();
    private final Map<String, String> selectMap = new ConcurrentHashMap<>();
    private Configuration config;

    private static final Integer UPDATE_FAILD = 0;

    public LockerInterceptor(String versionColumn, boolean failThrowException) {
        this.versionColumn = versionColumn;
        this.failThrowException = failThrowException;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        SqlSessionFactory sqlSessionFactory = event.getApplicationContext().getBean(SqlSessionFactory.class);
        this.config = sqlSessionFactory.getConfiguration();
        Collection<Class<?>> mappers = this.config.getMapperRegistry().getMappers();
        this.ids = ofNullable(mappers).orElseGet(ArrayList::new).stream()
                .flatMap(mapper -> stream(getAllMethods(mapper))
                        .filter(method -> hasAnnotation(method, Locker.class))
                        .map(m -> mapper.getName() + "." + m.getName()))
                .collect(toList());
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object param = args[1];
        String id = ms.getId();

        // 不需要乐观锁的方法，直接pass
        if (!ids.contains(id)) {
            return invocation.proceed();
        }

        // 1. 执行更新操作
        MetaObject pmmo = config.newMetaObject(param);
        Long versionValue = (Long) pmmo.getValue(versionColumn);
        pmmo.setValue(versionColumn, versionValue + 1);

        Executor executor = (Executor) PluginUtil.processTarget(invocation.getTarget());
        BoundSql boundSql = ms.getBoundSql(param);
        ParameterMapping vpm = new ParameterMapping.Builder(config, versionColumn + "_v", Object.class).build();
        List<ParameterMapping> pms = newArrayList(boundSql.getParameterMappings());
        pms.add(vpm);
        String old = boundSql.getSql();

        // 获取新的sql
        Update update = (Update) CCJSqlParserUtil.parse(old);
        Expression where = update.getWhere();
        Parenthesis p = new Parenthesis(where);
        EqualsTo lock = new EqualsTo(new Column(versionColumn), new JdbcParameter());
        AndExpression newWhere = new AndExpression().withLeftExpression(p).withRightExpression(lock);
        update.setWhere(newWhere);
        String newSql = update.toString();

        BoundSql newBoundSql = new BoundSql(config, newSql, pms, param);
        newBoundSql.setAdditionalParameter(versionColumn + "_v", versionValue);

        MappedStatement m = new Builder(config, "com.[plugin]optimistic_locker_update_with_locker._inner_update", new StaticSqlSource(config, newSql), SqlCommandType.UPDATE).build();
        StatementHandler sh = config.newStatementHandler(executor, m, param, RowBounds.DEFAULT, null, newBoundSql);
        Statement updateStmt = prepareStatement(executor.getTransaction(), sh);
        ((PreparedStatement) updateStmt).execute();
        int result = updateStmt.getUpdateCount();
        updateStmt.close();
        pmmo.setValue(versionColumn, versionValue);

        // 2. 如果返回值是0，说明没更新成功，那么判断是否是因为并发修改造成的，如果是并发修改，那么抛异常
        if (Objects.equals(result, UPDATE_FAILD) && failThrowException) {

            String selectSql = selectMap.get(id);
            if (StringUtils.isEmpty(selectSql)) {
                selectSql = createSelect(ms, param);
                selectMap.put(id, selectSql);
            }
            String[] split = selectSql.split(":");
            String idName = split[1];
            String sql = split[0];

            ParameterMapping pm = new ParameterMapping.Builder(config, idName, Object.class).build();
            List<ParameterMapping> parameterMappings = newArrayList(pm);

            Object value = ReflectUtil.getFieldValue(param, idName);
            BoundSql select = new BoundSql(config, sql, parameterMappings, value);
            MappedStatement selectMs = new Builder(config, "com.[plugin]optimistic_locker_update_faild._inner_select", new StaticSqlSource(config, sql), SqlCommandType.SELECT).build();
            StatementHandler selectSh = config.newStatementHandler(executor, selectMs, value, RowBounds.DEFAULT, null, select);
            Statement selectStmt = prepareStatement(executor.getTransaction(), selectSh);
            ((PreparedStatement) selectStmt).execute();
            ResultSet rs = selectStmt.getResultSet();
            Long v = null;
            while (rs.next()) {
                v = rs.getLong(versionColumn);
            }
            updateStmt.close();

            long currentVersion = (long) ReflectUtil.getFieldValue(param, versionColumn);
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

    private String createSelect(MappedStatement ms, Object arg) throws JSQLParserException {
        String sql = ms.getSqlSource().getBoundSql(arg).getSql();
        Update update = (Update) CCJSqlParserUtil.parse(sql);
        String tableName = update.getTable().getName();
        EqualsTo et = (EqualsTo) update.getWhere();
        String id = et.getLeftExpression().toString();

        StringJoiner joiner = new StringJoiner(" ");
        String selectSql = joiner.add("SELECT").add(versionColumn).add("FROM").add(tableName).add("WHERE").add(et.toString()).toString();

        return selectSql + ":" + id;
    }
}
