package com.github.dreamroute.locker.sample.mapper;

import com.github.dreamroute.locker.anno.Locker;
import com.github.dreamroute.locker.sample.domain.User;
import com.github.dreamroute.mybatis.pro.service.mapper.Mapper;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends Mapper<User, Long> {

	@Locker
	@Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
	long updateUu(User user);

	@Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
	long updateUu2(User user);

}
