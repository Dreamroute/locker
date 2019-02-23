package com.github.dreamroute.locker.misc.mapper;

import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.github.dreamroute.locker.misc.domain.User;

public interface UserMapper {
	
	// 参数为POJO对象方式(推荐使用方式1)
	Integer updateUser(User user);
	
	// 参数为单个参数方式(推荐使用方式2)
	Integer updateUser(@Param("name") String name, @Param("password") String password, @Param("version") Long version, @Param("id") Integer id);
	
	// 参数为Map方式(不推荐使用方式，不够直观)
	Integer updateUser(Map<Object, Object> user);
	
	// 单个参数未带@Param，报错(严重不推荐使用方式)
	Integer updateUserError(String name, String password, Long version, Integer id);
	
	// 不参与乐观锁控制
	Integer updateUserNoVersionLocker(User user);
	
	// 根据id查询
	User selectById(Integer id);

}
