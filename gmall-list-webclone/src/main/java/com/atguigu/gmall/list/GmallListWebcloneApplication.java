package com.atguigu.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.atguigu.gmall")
public class GmallListWebcloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallListWebcloneApplication.class, args);
    }

}
