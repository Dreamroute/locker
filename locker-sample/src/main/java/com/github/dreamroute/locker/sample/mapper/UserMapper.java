package com.github.dreamroute.locker.sample.mapper;

import com.github.dreamroute.locker.anno.Locker;
import com.github.dreamroute.locker.sample.domain.User;
import com.github.dreamroute.mybatis.pro.service.mapper.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * @author w.dehai
 */
public interface UserMapper extends Mapper<User, Long> {

    /**
     * 带有乐观锁的方法
     *
     * @param user 参数
     * @return 返回修改成功条数
     */
    @Locker
    @Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
    long updateUu(User user);

    /**
     * 这里并发更新，需要抛出{@link com.github.dreamroute.locker.exception.DataHasBeenModifyException}异常
     * @param user
     * @return
     */
    @Locker
    @Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
    long updateUuThrowException(User user);

    /**
     * 不带乐观锁的方法
     *
     * @param user 参数
     * @return 返回修改成功条数
     */
    @Update("update smart_user set name = #{name}, version = #{version} where id = #{id}")
    long updateUu2(User user);

}
