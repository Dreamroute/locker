package com.github.dreamroute.locker.sample.mapper;

import com.github.dreamroute.locker.sample.domain.User;
import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Insert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.truncate;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
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

    @Test
    void updateUuTest() {
        User user = userMapper.selectById(100L);
        long result = userMapper.updateUu(user);
        Assertions.assertEquals(1, result);
    }

    @Test
    void updateUuThrowExceptionTest() {
        User user = userMapper.selectById(100L);
        long result = userMapper.updateUu(user);
        Assertions.assertEquals(1, result);
        user.setVersion(100L);
        Assertions.assertThrows(MyBatisSystemException.class, () -> userMapper.updateUuThrowException(user));

    }

    @Test
    void updateUu2Test() {
        User user = userMapper.selectById(100L);
        long result = userMapper.updateUu2(user);
        Assertions.assertEquals(1, result);
    }
}
