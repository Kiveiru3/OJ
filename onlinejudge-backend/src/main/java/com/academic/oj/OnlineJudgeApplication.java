package com.academic.oj;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.academic.oj.mapper")
public class OnlineJudgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnlineJudgeApplication.class, args);
    }
}

