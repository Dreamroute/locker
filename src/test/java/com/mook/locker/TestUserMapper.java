package com.mook.locker;

import java.io.IOException;
import java.io.Reader;

import com.mook.locker.domain.User;
import com.mook.locker.mapper.UserMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by wyx on 2016/5/31.
 */
public class TestUserMapper {
    private static SqlSessionFactory factory;

    private SqlSession session;

    private UserMapper userMapper;

    @BeforeClass
    public static void buildSessionFactory() throws IOException {
        Reader reader = Resources.getResourceAsReader("Configuration.xml");
        factory = new SqlSessionFactoryBuilder().build(reader, "test");
    }

    @Before
    public void getSession() {
        session = factory.openSession();
        userMapper = session.getMapper(UserMapper.class);
    }

    @After
    public void closeSession() {
        if (session != null) {
            session.close();
        }
    }

    @Test
    public void testUpdateUser() {
        User user1 = userMapper.getUser(1);
        User user2 = userMapper.getUser(1);
        Assert.assertEquals("bb", user1.getName());
        user1.setName("bb");
        Assert.assertTrue(userMapper.updateUser(user1) == 1);
        session.commit();
        user2.setName("cc");
        Assert.assertTrue(userMapper.updateUser(user2) == 0);
        session.commit();
    }
}
