// package com.atguigu.daijia.map.config;
//
// import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
// import org.apache.hc.client5.http.impl.classic.HttpClients;
// import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
// import org.springframework.boot.web.client.RestTemplateBuilder;
// import org.springframework.boot.web.client.RestTemplateCustomizer;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
// import org.springframework.web.client.RestTemplate;
//
// import java.time.Duration;
//
// @Configuration
// public class RestTemplateConfig {
//
//     // @Bean
//     // public RestTemplateCustomizer restTemplateCustomizer() {
//     //     return restTemplate -> {
//     //         // 创建连接池管理器
//     //         PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//     //         connectionManager.setMaxTotal(200);          // 最大连接数
//     //         connectionManager.setDefaultMaxPerRoute(20);   // 每个路由的最大连接数
//     //
//     //         // 创建 HttpClient
//     //         CloseableHttpClient httpClient = HttpClients.custom()
//     //                 .setConnectionManager(connectionManager)
//     //                 .build();
//     //
//     //         // 设置请求工厂
//     //         HttpComponentsClientHttpRequestFactory factory =
//     //                 new HttpComponentsClientHttpRequestFactory(httpClient);
//     //
//     //         factory.setConnectTimeout(5000);    // 连接超时（毫秒）
//     //         factory.setReadTimeout(5000);
//     //             // 读取超时（毫秒）
//     //
//     //         restTemplate.setRequestFactory(factory);
//     //     };
//     // }
//     // @Bean
//     // public RestTemplate restTemplate() {
//     //     return new RestTemplate();
//     // }
// }
