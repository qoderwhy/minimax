package com.qkit.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * qkit 启动入口。
 */
@EnableAsync
@SpringBootApplication(scanBasePackages = "com.qkit")
@MapperScan("com.qkit.**.mapper")
public class ScaffoldApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScaffoldApplication.class, args);
    }
}
