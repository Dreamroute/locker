package com.mook.locker.mapper;

import java.util.Map;

import com.mook.locker.annotation.VersionLocker;
import com.mook.locker.domain.User;

/**
 * Created by wyx on 2016/5/31.
 */
public interface InvalidUserMapper {
    User getUser(int id);

    @VersionLocker
    int updateUser(User user);

    @VersionLocker
    int updateUser(String name, String password);

    @VersionLocker(false)
    int updateUser(Map map);

    int updateUser();
}
