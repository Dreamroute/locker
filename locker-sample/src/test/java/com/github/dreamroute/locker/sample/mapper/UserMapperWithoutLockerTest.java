package com.github.dreamroute.locker.sample.mapper;

import com.github.dreamroute.locker.sample.domain.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import javax.sql.DataSource;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;

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
     * 手动移除插件
     */
    @Test
    void updateUserWithLockerTest() {
        User user = userMapper.selectById(100L);
        long result = userMapper.updateUserWithLocker(user);
        Assertions.assertEquals(1, result);
    }

}
