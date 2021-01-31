package com.github.dreamroute.locker.interceptor;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author w.dehi
 */
@Data
@ConfigurationProperties(prefix = "locker")
public class LockerProperties {
    private String versionColumn = "version";
}
