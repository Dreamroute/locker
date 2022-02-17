package com.github.dreamroute.locker.sample;

import com.github.dreamroute.sqlprinter.starter.anno.EnableSQLPrinter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author w.dehai
 */
@EnableSQLPrinter
@SpringBootApplication
@MapperScan("com.github.dreamroute.locker.sample.mapper")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
