package com.github.dreamroute.locker.interceptor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author w.dehi
 */
@Data
@ConfigurationProperties(prefix = "locker")
public class LockerProperties {
    /**
     * 全局配置数据库乐观锁列名
     */
    private String versionColumn = "version";

    /**
     * 当因为并发修改失败时抛出异常
     */
    private boolean failThrowException = true;
}
