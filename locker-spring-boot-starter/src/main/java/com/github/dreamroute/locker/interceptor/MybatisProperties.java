package com.github.dreamroute.locker.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * @author w.dehi
 */
@Data
@ConfigurationProperties(prefix = "mybatis.configuration")
public class MybatisProperties {
    /**
     * 是否将下划线映射到驼峰式
     */
    @Value("${mybatis.configuration.map-underscore-to-camel-case}")
    private boolean mapUnderscodeToCamelCase = false;
}
