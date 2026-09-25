## 关于本项目

本项目基于开源教程学习搭建，用于系统实践 Spring Cloud 微服务架构。

我的主要贡献：

- 独立设计与实现「支付超时自动关单」模块（RabbitMQ TTL + 死信队列，含幂等与竞态处理）
- 独立完成项目环境搭建、部署与持续维护

---

# 智能代驾服务平台（Daijia-Parent）

面向乘客与司机的代驾服务平台，基于 **Spring Cloud 微服务架构**，包含订单、位置、司乘管理、支付、优惠券、规则引擎、派单等 7 大核心模块，覆盖「乘客下单 → 司机接单 → 代驾服务 → 账单支付」完整业务链路。

## 技术栈

| 类别 | 技术 |
|---|---|
| 基础框架 | Spring Boot 3.0.9 · Spring Cloud 2022.0.3 · Spring Cloud Alibaba |
| 服务治理 | Nacos（注册/配置中心）· Sentinel（限流熔断）· Seata（分布式事务） |
| 数据存储 | MySQL 8（主数据）· Redis（缓存/Geo/分布式锁/延迟队列）· MongoDB（轨迹） |
| 消息中间件 | RabbitMQ（支付通知、延迟关单、异步解耦）· Redisson |
| 其他 | MyBatis-Plus · MinIO（对象存储）· Drools（规则引擎）· XXL-Job（定时任务）· 微信/支付宝支付 |

## 功能模块

- **service-order**：订单核心服务（下单、抢单、状态流转、账单）
- **service-dispatch**：智能派单（附近司机搜索与推送）
- **service-map**：位置服务（司机实时位置、轨迹存储与回放）
- **service-payment**：支付服务（微信/支付宝支付、回调处理、分账）
- **service-rules**：计费规则服务（基于 Drools 规则引擎）
- **service-coupon**：优惠券服务
- **service-customer / service-driver**：乘客/司机端服务
- **service-system**：系统服务（认证、权限）

---

## 核心亮点：支付超时自动关单（自主实现）

### 业务场景

代驾结束、司机发出账单后，订单进入 **待付款（UNPAID）** 状态。若乘客在 **15 分钟**内未完成支付，系统自动关闭订单，避免订单长时间挂起。

### 技术方案

采用 **RabbitMQ TTL + 死信队列（DLX）** 实现延迟关单：

```
发账单（订单置 UNPAID）
    ↓ 投递延迟消息（订单 ID）
延迟队列（x-message-ttl = 15 分钟）
    ↓ TTL 到期，自动转入死信交换机
死信队列（业务队列，消费者监听）
    ↓ 消费者查询订单当前状态
    ├─ 仍为 UNPAID → 置为 ORDER_TIMEOUT（超时取消），记录状态流水
    └─ 已 PAID → 忽略（幂等，不做处理）
```

### 设计考量

1. **为什么用 TTL + 死信队列，而不是定时扫描**
   定时任务扫描全表对数据库压力大，且时效取决于调度周期；TTL + DLX 由消息中间件在消息到期瞬间触发，延迟精准、与业务服务解耦，水平扩展能力强。

2. **消息幂等**
   消费者处理前先校验订单状态，只有仍处于 `UNPAID` 的订单才会被关单。重复消费、支付与关单并发到达等场景下，不会出现重复关单或状态错乱。

3. **竞态处理（关单 vs 支付成功）**
   支付回调更新订单状态时使用**条件更新**（仅 `UNPAID` 可更新为 `PAID`）：若订单已被超时关单，支付成功回调会被拒绝，不会出现"已关单订单被复活为已支付"的脏状态。

4. **消息可靠性**
   消费者手动 ACK；处理异常时消息不重回队列，避免消费失败死循环，配合日志定位问题。

### 关键代码

| 文件 | 职责 |
|---|---|
| `OrderMqConfig.java` | 声明延迟队列（TTL + DLX）、死信交换机、死信队列及绑定关系 |
| `OrderCancelReceiver.java` | 死信队列消费者，手动 ACK |
| `OrderInfoServiceImpl#sendOrderBillInfo` | 发账单：订单置 UNPAID + 投递延迟消息 |
| `OrderInfoServiceImpl#orderTimeoutCancel` | 关单核心逻辑（状态校验 + 置 ORDER_TIMEOUT + 状态流水） |
| `OrderInfoServiceImpl#updateOrderPayStatus` | 支付状态条件更新（防竞态） |
| `MqConst.java` | 延迟/死信相关常量定义 |

---

## 项目结构

```
daijia-parent
├── common/            # 公共模块（工具类、RabbitMQ/Redis 封装、日志）
├── model/             # 实体与枚举（OrderStatus 等）
├── service-client/    # Feign 接口声明
├── service/           # 微服务实现
│   ├── service-order/     # 订单服务（含支付超时关单）
│   ├── service-dispatch/  # 派单服务
│   ├── service-map/       # 位置服务
│   ├── service-payment/   # 支付服务
│   ├── service-rules/     # 规则服务
│   ├── service-coupon/    # 优惠券服务
│   ├── service-customer/  # 乘客服务
│   ├── service-driver/    # 司机服务
│   └── service-system/    # 系统服务
├── server-gateway/    # 网关
└── web/               # 前端调用入口（web-customer / web-driver / web-mgr）
```

## 运行说明

1. 环境依赖：JDK 17、Maven 3.6+、MySQL 8、Redis、RabbitMQ、Nacos、MongoDB
2. 在 Nacos 配置中心创建各服务的 `-dev.yaml` 配置（数据源、Redis、RabbitMQ 连接信息）
3. 按依赖顺序启动：gateway → service-order → 其余服务

> 注：本项目基于开源教程项目学习与实践，`支付超时自动关单` 为自主设计与实现。
