package com.github.dreamroute.locker.sample.mapper;

import cn.hutool.core.util.ReflectUtil;
import com.github.dreamroute.locker.interceptor.LockerInterceptor;
import com.github.dreamroute.locker.interceptor.LockerProperties;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private LockerInterceptor interceptor;

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
        long result = userMapper.updateUserNoLocker(user);
        Assertions.assertEquals(1, result);
    }

    /**
     * 单线程乐观锁更新，更新成功
     */
    @Test
    void updateUserWithLockerTest() {
        User user = userMapper.selectById(100L);
        long result = userMapper.updateUserWithLocker(user);
        Assertions.assertEquals(1, result);
    }

    /**
     * 并发更新，第一条成功，第二条失败
     */
    @Test
    void concurrentUpdateTest() {
        User user = User.builder()
                .id(100)
                .name("w.dehai")
                .password("123456")
                .version(100L).build();

        // 第一次成功
        assertEquals(1, userMapper.updateUserWithLocker(user));

        // 第二次抛出异常
        assertThrows(MyBatisSystemException.class, () -> userMapper.updateUserWithLocker(user));

        // 第三次，手动将failThrowException设置成false，更新失败返回0
        LockerProperties properties = new LockerProperties();
        // 原始值
        boolean failThrowException = properties.isFailThrowException();
        properties.setFailThrowException(false);
        ReflectUtil.setFieldValue(interceptor, "lockerProperties", properties);
        assertEquals(0, userMapper.updateUserWithLocker(user));
        // 将原始值设置回去，避免影响其他测试方法
        properties.setFailThrowException(failThrowException);
    }

}
