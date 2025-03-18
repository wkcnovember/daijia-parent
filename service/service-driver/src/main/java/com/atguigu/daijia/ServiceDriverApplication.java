package com.atguigu.daijia;

import jakarta.annotation.PostConstruct;
import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableFileStorage
public class ServiceDriverApplication {


    public static void main(String[] args) {
        SpringApplication.run(ServiceDriverApplication.class, args);
    }

}
