package com.atguigu.daijia.common.thread;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix = "thread")
@Data
public class ThreadPoolConfigProperties {

    private Integer coreSize ;

    private Integer maxSize ;

    private Integer keepAliveTime ;
    private Integer maxQueue;


}
