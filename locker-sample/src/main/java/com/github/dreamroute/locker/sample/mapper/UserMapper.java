package com.github.dreamroute.locker.sample.mapper;

import com.github.dreamroute.locker.anno.Locker;
import com.github.dreamroute.locker.sample.domain.User;
import com.github.dreamroute.mybatis.pro.service.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * @author w.dehai
 */
public interface UserMapper extends BaseMapper<User, Long> {

    /**
     * 带有乐观锁的方法
     *
     * @param user 参数
     * @return 更新成功返回
     */
    @Locker
    @Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
    long updateUserWithLocker(User user);

    /**
     * 带有动态标签
     */
    @Locker
    long updateUserByDynamicTagWithLocker(User user);

    /**
     * 不带乐观锁的方法
     *
     * @param user 参数
     * @return 返回修改成功条数
     */
    @Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
    long updateUserNoLocker(User user);
}
