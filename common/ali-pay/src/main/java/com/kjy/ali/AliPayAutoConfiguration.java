package com.kjy.ali;

import com.kjy.ali.api.AlipayClientTemplate;
import com.kjy.ali.config.AlipayConfigProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @Author 柯佳元
 * @Create 2024/8/16 18:49
 * @Version 1.0
 * Description:
 */
@Configuration
@EnableConfigurationProperties({AlipayConfigProperties.class})
@Import({AlipayClientTemplate.class})
@Slf4j
public class AliPayAutoConfiguration {

    @PostConstruct
    public void init(){
        log.info("装配了支付宝支付配置");
    }
}
