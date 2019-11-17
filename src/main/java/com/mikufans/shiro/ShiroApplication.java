package com.mikufans.shiro;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.mikufans.shiro.mapper")
public class ShiroApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(ShiroApplication.class, args);
    }

}
