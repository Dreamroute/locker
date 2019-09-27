package com.github.dreamroute.locker.misc.test.mapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.dreamroute.locker.misc.domain.User;
import com.github.dreamroute.locker.misc.mapper.UserMapper;

public class UserMapperTest {
	
	private static SqlSession sqlSession = null;
	private User user;
	
	@BeforeClass
	public static void doInitTest() throws Exception {
		InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
		sqlSession = sqlSessionFactory.openSession(true);
	}
	
	@Before
	public void initTest() {
	    UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        user = userMapper.selectById(100);
        String name = new Random().nextInt(100) + "";
        user.setName(name);
	}
	
	@Test
	public void updateUserPojoTest() {
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		User user = userMapper.selectById(100);
		String name = new Random().nextInt(100) + "";
		user.setName(name);
		Integer result = userMapper.updateUser(user);
		Assert.assertEquals(1L, Long.parseLong(result + ""));
	}
	
	@Test
	public void updateUserAtParamTest() {
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		Integer result = userMapper.updateUser("test", "test", 100L, 100);
		Assert.assertEquals(1L, Long.parseLong(result + ""));
	}
	
	@Test
	public void updateUserMapTest() {
		Map<Object, Object> param = new HashMap<>();
		param.put("name", "test");
		param.put("password", "test");
		param.put("version", 100L);
		param.put("id", 100);
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		Integer result = userMapper.updateUser(param);
		Assert.assertEquals(1L, Long.parseLong(result + ""));
	}
	
	@Test(expected = BindingException.class)
	public void updateUserErrorTest() {
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		Integer result = userMapper.updateUserError("test", "test", 100L, 100);
		Assert.assertEquals(1L, Long.parseLong(result + ""));
	}
	
	@Test
	public void updateUserNoVersionLockerTest() {
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		Integer result = userMapper.updateUserNoVersionLocker(user);
		Assert.assertEquals(1L, Long.parseLong(result + ""));
	}
	
}
