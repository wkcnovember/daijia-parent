package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.repository.OrderServiceLocationRepository;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.entity.map.OrderServiceLocation;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LocationServiceImpl implements LocationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private OrderServiceLocationRepository orderServiceLocationRepository;
    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;


    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                updateDriverLocationForm.getLatitude().doubleValue());
        Long add = stringRedisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point,
                updateDriverLocationForm.getDriverId().toString());
        if (add == null) return Boolean.FALSE;
        return add > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        stringRedisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return Boolean.TRUE;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {

        // 查询附件5公里滴司机~
        GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()  // 查到详细坐标
                        .includeDistance()  //  查到距离
                        .limit(10) // 限制10条
                        .sortAscending(); // 升序
        GeoResults<RedisGeoCommands.GeoLocation<String>> driverRes = ops.radius(RedisConstant.DRIVER_GEO_LOCATION,
                new Circle(new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                        searchNearByDriverForm.getLatitude().doubleValue()),
                        new Distance(DriverConstant.NEARBY_DRIVER_RADIUS, Metrics.KILOMETERS)),
                args);
        if (null == driverRes) return Collections.emptyList();

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = driverRes.getContent();
        List<Long> driverIds = content.stream()
                .map(item -> JSON.parseObject(item.getContent().getName(), Long.class)).toList();

        if (CollectionUtils.isEmpty(driverIds)) return Collections.emptyList();

        // 批量查询司机的个性化设置 比如 接单距离 跑单距离 还有 接单状态
        Result<Map<Long, DriverSetVo>> driverSetMap =
                driverInfoFeignClient.getDriverSetMap(driverIds);

        driverSetMap.throwOnFailure();
        Map<Long, DriverSetVo> data = driverSetMap.getData();
        if (CollectionUtils.isEmpty(data)) return Collections.emptyList();
        // 根据司机个性化设计信息过滤超过接单范围的司机
        return content.stream()
                .filter(item -> {
                    String name = item.getContent().getName();
                    Long driverId = JSON.parseObject(name, Long.class);
                    DriverSetVo driverSetVo = data.get(driverId);
                    // Result<DriverSetVo> driverSetVoResult = driverInfoFeignClient.getDriverSet(driverId);
                    // DriverSetVo driverSetVo = driverSetVoResult.getData();
                    if (driverSetVo == null) {
                        return false;
                    }


                    // 乘客距离司机的距离 > 司机接客距离
                    BigDecimal distance = BigDecimal.valueOf(item.getDistance().getValue());
                    BigDecimal acceptDistance = driverSetVo.getAcceptDistance();
                    if (distance.compareTo(acceptDistance) > 0) {
                        return false;
                    }

                    // 司机接单的范围
                    // 0 表示不受限制~
                    BigDecimal orderDistance = driverSetVo.getOrderDistance();
                    if (BigDecimal.ZERO.compareTo(orderDistance) == 0) {
                        return true;
                    }

                    // 司机接单的范围和乘客的距离比较 >=0 在范围内  否则不在
                    return orderDistance.compareTo(searchNearByDriverForm.getMileageDistance()) >= 0;
                }).map(item -> {
                    String name = item.getContent().getName();
                    Long driverId = JSON.parseObject(name, Long.class);
                    return new NearByDriverVo(driverId,
                            BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP));
                }).toList();
    }


    private static final String UPDATE_ORDER_LOCATION_SCRIPT =
            "redis.call('HMSET', KEYS[1], 'longitude', ARGV[1], 'latitude', ARGV[2])\n" +
                    "return redis.call('EXPIRE', KEYS[1], ARGV[3])";

    /**
     * lua 保证 添加和过期一致性
     *
     * @param form
     * @return
     */
    @Override
    public Boolean updateOrderLocationToCache(UpdateOrderLocationForm form) {
        String orderKey = RedisConstant.UPDATE_ORDER_LOCATION + form.getOrderId();
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(UPDATE_ORDER_LOCATION_SCRIPT, Long.class),
                Collections.singletonList(orderKey),
                form.getLongitude().toString(),
                form.getLatitude().toString(),
                String.valueOf(RedisConstant.UPDATE_ORDER_LOCATION_EXPIRES_TIME)
        );
        return Boolean.TRUE;
    }

    // @Override
    // public Boolean updateOrderLocationToCache(UpdateOrderLocationForm updateOrderLocationForm) {
    //
    //     String orderKey = RedisConstant.UPDATE_ORDER_LOCATION + updateOrderLocationForm.getOrderId();
    //     // 1. 转为 Map
    //     Map<String, String> map = new HashMap<>();
    //     map.put("longitude", updateOrderLocationForm.getLongitude().toString());
    //     map.put("latitude", updateOrderLocationForm.getLatitude().toString());
    //     stringRedisTemplate.opsForHash().putAll(orderKey, map);
    //     stringRedisTemplate.expire(orderKey, DriverConstant.ACCEPT_DISTANCE, TimeUnit.MINUTES);
    //     return Boolean.TRUE;
    // }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long orderId) {
        String orderKey = RedisConstant.UPDATE_ORDER_LOCATION + orderId;
        // 1. 从 Redis Hash 获取 Map 数据
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(orderKey);
        if (CollectionUtils.isEmpty(entries)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        // 2. 将 Map 转为 JSON 字符串
        String jsonString = JSON.toJSONString(entries);
        // 3. 使用 FastJSON 将 JSON 字符串转为 OrderLocationVo 对象
        OrderLocationVo orderLocationVo = JSON.parseObject(jsonString, OrderLocationVo.class);
        return orderLocationVo;
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        List<OrderServiceLocation> orderServiceLocations = orderLocationServiceFormList.stream().map(item -> {
            OrderServiceLocation orderServiceLocation = new OrderServiceLocation();
            orderServiceLocation.setOrderId(item.getOrderId());
            orderServiceLocation.setLongitude(item.getLongitude());
            orderServiceLocation.setLatitude(item.getLatitude());
            orderServiceLocation.setId(ObjectId.get().toString());
            orderServiceLocation.setCreateTime(LocalDateTime.now());
            return orderServiceLocation;
        }).toList();
        orderServiceLocationRepository.saveAll(orderServiceLocations);
        return Boolean.TRUE;
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long orderId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("orderId").is(orderId));
        query.with(Sort.by(Sort.Order.desc("createTime")));
        query.limit(1);
        OrderServiceLocation orderServiceLocation = mongoTemplate.findOne(query, OrderServiceLocation.class);
        if (orderServiceLocation == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 封装返回对象
        OrderServiceLastLocationVo orderServiceLastLocationVo = new OrderServiceLastLocationVo();
        orderServiceLastLocationVo.setLongitude(orderServiceLocation.getLongitude());
        orderServiceLastLocationVo.setLatitude(orderServiceLocation.getLatitude());
        return orderServiceLastLocationVo;
    }

    @Override
    public BigDecimal calculateOrderRealDistance(Long orderId) {
        List<OrderServiceLocation> orderServiceLocations =
                orderServiceLocationRepository.getByOrderIdOrderByCreateTimeAsc(orderId);
        if (CollectionUtils.isEmpty(orderServiceLocations)) {
            return new BigDecimal("0.0");
        }
        double realDistance = 0;
        // 总两点距离
        for (int i = 0, size = orderServiceLocations.size() - 1; i < size; i++) {
            OrderServiceLocation location1 = orderServiceLocations.get(i);
            OrderServiceLocation location2 = orderServiceLocations.get(i + 1);

            double distance = LocationUtil.getDistance(location1.getLatitude().doubleValue(),
                    location1.getLongitude().doubleValue(), location2.getLatitude().doubleValue(),
                    location2.getLongitude().doubleValue());
            realDistance += distance;
        }
        // 测试过程中，没有真正代驾，实际代驾GPS位置没有变化，模拟：实际代驾里程 = 预期里程 + 5
        if (realDistance == 0) {
            return orderInfoFeignClient.getOrderInfo(orderId).getData().getExpectDistance().add(new BigDecimal("5"));
        }
        return new BigDecimal(realDistance);
    }

    public static void main(String[] args) {
        BigDecimal bigDecimal1 = BigDecimal.valueOf(0.00);
        System.out.println(BigDecimal.ZERO.compareTo(bigDecimal1) == 0);
    }
}
