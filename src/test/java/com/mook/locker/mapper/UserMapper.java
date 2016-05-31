package com.mook.locker.mapper;

import com.mook.locker.domain.User;

/**
 * Created by wyx on 2016/5/31.
 */
public interface UserMapper {
    User getUser(int id);

    int updateUser(User user);
}
