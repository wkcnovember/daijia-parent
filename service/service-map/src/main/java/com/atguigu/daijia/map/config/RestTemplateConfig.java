package com.atguigu.daijia.map.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient5()));
        return restTemplate;
    }

    private CloseableHttpClient httpClient5()  {
        // 配置SSL上下文（如果需要）
        // TrustStrategy trustStrategy = (x509Certificates, s) -> true;
        // SSLContext sslContext = SSLContexts.custom()
        //         .loadTrustMaterial(null, trustStrategy)
        //         .build();

        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200); // 最大连接数
        connectionManager.setDefaultMaxPerRoute(20); // 每个路由的最大连接数

        // 创建HttpClient 5实例
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }
}
