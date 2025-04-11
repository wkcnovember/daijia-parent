package com.atguigu.daijia;

import com.atguigu.daijia.common.anno.EnableMyThreadPoolConfig;
import com.atguigu.daijia.common.anno.EnableRedisson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableRedisson
@EnableAsync
@EnableMyThreadPoolConfig
public class ServiceCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceCouponApplication.class, args);
    }
}
