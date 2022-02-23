package com.github.dreamroute.locker.sample.mapper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import cn.hutool.core.util.ReflectUtil;
import com.github.dreamroute.locker.anno.EnableLocker;
import com.github.dreamroute.locker.exception.DataHasBeenModifyException;
import com.github.dreamroute.locker.interceptor.LockerInterceptor;
import com.github.dreamroute.locker.sample.domain.User;
import com.github.dreamroute.sqlprinter.starter.interceptor.SqlPrinter;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static com.github.dreamroute.locker.sample.mapper.AppenderUtil.create;
import static com.github.dreamroute.locker.sample.mapper.AppenderUtil.getMessage;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableLocker
@SpringBootTest
class UserMapperTest {

    @Resource
    private UserMapper userMapper;
    @Resource
    private DataSource dataSource;
    @Resource
    private LockerInterceptor interceptor;

    public static final String LOCKER = "AND version = ";

    @BeforeEach
    void init() {
        new DbSetup(new DataSourceDestination(dataSource), truncate("smart_user")).launch();
        Insert initUser = insertInto("smart_user")
                .columns("id", "name", "password", "version")
                .values(100L, "w.dehai", "123456", 100L)
                .build();
        new DbSetup(new DataSourceDestination(dataSource), initUser).launch();
    }

    /**
     * 无乐观锁更新
     */
    @Test
    void updateUserNoLockerTest() {
        User user = userMapper.selectById(100L);
        ListAppender<ILoggingEvent> appender = create(SqlPrinter.class);
        long result = userMapper.updateUserNoLocker(user);
        Assertions.assertEquals(1, result);
        Assertions.assertFalse(getMessage(appender, 0).contains(LOCKER));
    }

    /**
     * 单线程乐观锁更新，更新成功
     */
    @Test
    void updateUserWithLockerTest() {
        User user = userMapper.selectById(100L);
        ListAppender<ILoggingEvent> appender = create(SqlPrinter.class);
        long result = userMapper.updateUserWithLocker(user);
        Assertions.assertEquals(1, result);
        Assertions.assertTrue(getMessage(appender, 0).contains(LOCKER));
    }

    /**
     * 并发更新
     */
    @Test
    void concurrentUpdateTest() {
        User user = User.builder()
                .id(100)
                .name("w.dehai")
                .password("123456")
                .version(100L).build();

        ListAppender<ILoggingEvent> appender = create(SqlPrinter.class);

        // 成功
        assertEquals(1, userMapper.updateUserWithLocker(user));
        assertTrue(getMessage(appender, 0).contains(LOCKER));

        // 抛出异常
        assertThrows(MyBatisSystemException.class, () -> userMapper.updateUserWithLocker(user));

        // 抛出异常，获取真实异常
        try {
            userMapper.updateUserWithLocker(user);
        } catch (MyBatisSystemException e) {
            Throwable cause = e.getCause().getCause();
            assertEquals(DataHasBeenModifyException.class, cause.getClass());
        }

        // 手动将failThrowException设置成false，更新失败返回0
        // 原始值
        boolean failThrowException = (boolean) ReflectUtil.getFieldValue(interceptor, "failThrowException");
        ReflectUtil.setFieldValue(interceptor, "failThrowException", false);
        assertEquals(0, userMapper.updateUserWithLocker(user));
        assertTrue(getMessage(appender, 5).contains(LOCKER));
        // 将原始值设置回去，避免影响其他测试方法
        ReflectUtil.setFieldValue(interceptor, "failThrowException", failThrowException);
    }

    /**
     * sql中带有动态标签<set>
     */
    @Test
    void updateUserByDynamicTagWithLockerTest() {
        User user = User.builder()
                .id(100)
                .name("w.dehai")
                .password("123456")
                .version(100L).build();

        ListAppender<ILoggingEvent> appender = create(SqlPrinter.class);

        // 成功
        assertEquals(1, userMapper.updateUserByDynamicTagWithLocker(user));
        assertTrue(getMessage(appender, 0).contains(LOCKER));

        // 抛出异常
        assertThrows(MyBatisSystemException.class, () -> userMapper.updateUserByDynamicTagWithLocker(user));

        // 抛出异常，获取真实异常
        try {
            userMapper.updateUserByDynamicTagWithLocker(user);
        } catch (MyBatisSystemException e) {
            Throwable cause = e.getCause().getCause();
            assertEquals(DataHasBeenModifyException.class, cause.getClass());
        }

        // 手动将failThrowException设置成false，更新失败返回0
        // 原始值
        boolean failThrowException = (boolean) ReflectUtil.getFieldValue(interceptor, "failThrowException");
        ReflectUtil.setFieldValue(interceptor, "failThrowException", false);
        assertEquals(0, userMapper.updateUserByDynamicTagWithLocker(user));
        assertTrue(getMessage(appender, 5).contains(LOCKER));
        // 将原始值设置回去，避免影响其他测试方法
        ReflectUtil.setFieldValue(interceptor, "failThrowException", failThrowException);
    }

}
