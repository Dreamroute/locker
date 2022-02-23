package com.github.dreamroute.locker.sample.mapper;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.dreamroute.locker.sample.domain.User;
import com.github.dreamroute.sqlprinter.starter.interceptor.SqlPrinter;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static com.github.dreamroute.locker.sample.mapper.AppenderUtil.create;
import static com.github.dreamroute.locker.sample.mapper.AppenderUtil.getMessage;
import static com.github.dreamroute.locker.sample.mapper.UserMapperTest.LOCKER;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class UserMapperWithoutLockerTest {

    @Resource
    private UserMapper userMapper;
    @Resource
    private DataSource dataSource;

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
     * 不启用插件
     */
    @Test
    void updateUserWithLockerTest() {
        User user = userMapper.selectById(100L);
        ListAppender<ILoggingEvent> appender = create(SqlPrinter.class);
        long result = userMapper.updateUserWithLocker(user);
        assertEquals(1, result);
        assertFalse(getMessage(appender, 0).contains(LOCKER));
    }

}
