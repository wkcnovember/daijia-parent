package com.atguigu.daijia.driver.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.common.util.LocationUtil;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.order.OrderStatus;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderFeeForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.query.order.OrderCount;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.CustomerInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderListVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import com.atguigu.daijia.rules.client.ProfitsharingRuleFeignClient;
import com.atguigu.daijia.rules.client.RewardRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {


    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;
    @Resource
    private OrderInfoConvert orderInfoConvert;
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Resource
    private MapFeignClient mapFeignClient;


    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private FeeRuleFeignClient feeRuleFeignClient;

    @Resource
    private RewardRuleFeignClient rewardRuleFeignClient;

    @Resource
    private ProfitsharingRuleFeignClient profitsharingRuleFeignClient;

    @Resource
    private ThreadPoolExecutor sharedThreadPool;


    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.isDriverOrder(AuthContextHolder.getUserId(), orderId);
        result.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(result.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        orderStatus.throwOnFailureOrDataIsNull();
        return orderStatus.getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        Result<List<NewOrderDataVo>> newOrderQueueData = newOrderFeignClient.findNewOrderQueueData(driverId);
        newOrderQueueData.throwOnFailure();
        return newOrderQueueData.getData();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.robNewOrder(driverId, orderId);
        return result.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchDriverCurrentOrder(driverId);
        currentOrderInfoVoResult.throwOnFailureOrDataIsNull();
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        OrderInfo orderInfo = validateDriverOrder(orderId, driverId);
        OrderInfoVo orderInfoVo = orderInfoConvert.toOrderInfoVo(orderInfo);
        orderInfoVo.setOrderId(orderId);

        // 2. 使用CompletableFuture并行获取乘客信息和可能需要的账单/分账信息
        CompletableFuture<Result<CustomerInfoVo>> customerFuture = CompletableFuture.supplyAsync(() ->
                customerInfoFeignClient.getCustomerInfoVo(orderInfo.getCustomerId()), sharedThreadPool);

        CompletableFuture<Void> billAndProfitFuture = CompletableFuture.completedFuture(null);

        if (orderInfo.getStatus() >= OrderStatus.END_SERVICE.getStatus() && orderInfo.getStatus() <= OrderStatus.FINISH.getStatus()) {
            // 并行获取账单和分账信息
            billAndProfitFuture = CompletableFuture.allOf(
                    CompletableFuture.supplyAsync(() ->
                                    orderInfoFeignClient.getOrderBillInfo(orderId), sharedThreadPool)
                            .thenAccept(result ->
                                    orderInfoVo.setOrderBillVo(result.throwOnFailureOrDataIsNull().getData())),

                    CompletableFuture.supplyAsync(() ->
                                    orderInfoFeignClient.getOrderProfitSharing(orderId), sharedThreadPool)
                            .thenAccept(result ->
                                    orderInfoVo.setOrderProfitsharingVo(result.throwOnFailureOrDataIsNull().getData()))
            );
        }
        // 3. 等待所有异步任务完成
        try {
            // 设置乘客信息
            orderInfoVo.setCustomerInfoVo(
                    customerFuture.get(2, TimeUnit.SECONDS).throwOnFailureOrDataIsNull().getData()
            );

            // 等待账单和分账信息完成(如果有)
            billAndProfitFuture.get(2, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }

        // // 乘客信息
        // Result<CustomerInfoVo> customerInfoVoResult =
        //         customerInfoFeignClient.getCustomerInfoVo(orderInfo.getCustomerId());
        // customerInfoVoResult.throwOnFailureOrDataIsNull();
        // orderInfoVo.setCustomerInfoVo(customerInfoVoResult.getData());
        //
        // // 结束代驾and之后才有账单和分账信息~
        // if (orderInfo.getStatus() >= OrderStatus.END_SERVICE.getStatus()) {
        //     // 账单信息
        //     OrderBillVo orderBillVo = orderInfoFeignClient
        //             .getOrderBillInfo(orderId)
        //             .throwOnFailureOrDataIsNull()
        //             .getData();
        //     // 分账信息
        //     OrderProfitsharingVo orderProfitsharing = orderInfoFeignClient
        //             .getOrderProfitSharing(orderId)
        //             .throwOnFailureOrDataIsNull()
        //             .getData();
        //     orderInfoVo.setOrderBillVo(orderBillVo);
        //     orderInfoVo.setOrderProfitsharingVo(orderProfitsharing);
        // }

        return orderInfoVo;
    }

    @NotNull
    private OrderInfo validateDriverOrder(Long orderId, Long driverId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        orderInfoResult.throwOnFailureOrDataIsNull();
        OrderInfo orderInfo = orderInfoResult.getData();
        if (!Objects.equals(orderInfo.getDriverId(), driverId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        return orderInfo;
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        drivingLineVoResult.throwOnFailureOrDataIsNull();
        return drivingLineVoResult.getData();
    }

    @Override
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {


        try {
            CompletableFuture<OrderInfo> orderInfoFuture = getOrderInfoCompletableFuture(orderId, driverId)
                    .orTimeout(2, TimeUnit.SECONDS);

            CompletableFuture<OrderLocationVo> orderLocationVoFuture = CompletableFuture.supplyAsync(() -> {
                Result<OrderLocationVo> cacheOrderLocationRes = locationFeignClient.getCacheOrderLocation(orderId);
                return cacheOrderLocationRes.throwOnFailureOrDataIsNull().getData();
            }, sharedThreadPool);


            OrderInfo orderInfo = orderInfoFuture.get(2, TimeUnit.SECONDS);
            OrderLocationVo location = orderLocationVoFuture.get(2, TimeUnit.SECONDS);
            // ❌防止刷单，计算司机的经纬度与代驾的起始经纬度是否在1公里范围内
            validateDistance(orderInfo, location);
            // 开始代驾
            Result<Boolean> result = orderInfoFeignClient.driverArriveStartLocation(orderId, driverId);
            return result.throwOnFailureOrDataIsNull().getData();
        } catch (TimeoutException e) {
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }


    }

    private void validateDistance(OrderInfo orderInfo, OrderLocationVo orderLocationVo) {
        // 司机的位置与代驾起始点位置的距离
        double distance = LocationUtil.getDistance(orderInfo.getStartPointLatitude().doubleValue(),
                orderInfo.getStartPointLongitude().doubleValue(),
                orderLocationVo.getLatitude().doubleValue(),
                orderLocationVo.getLongitude().doubleValue());
        if (distance > DriverConstant.DRIVER_START_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_START_LOCATION_DISTION_ERROR);
        }
    }

    private static void validateDriverOrder(Long driverId, OrderInfo orderInfo) {
        if (!Objects.equals(driverId, orderInfo.getDriverId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
    }

    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        Long orderId = updateOrderCartForm.getOrderId();
        Long driverId = updateOrderCartForm.getDriverId();
        Result<Boolean> isValidate = orderInfoFeignClient.isDriverOrder(driverId, orderId);
        isValidate.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(isValidate.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Boolean> result = orderInfoFeignClient.updateOrderCart(updateOrderCartForm);
        result.throwOnFailureOrDataIsNull();
        return result.getData();
    }

    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        Long orderId = startDriveForm.getOrderId();
        Long driverId = startDriveForm.getDriverId();
        Result<Boolean> isValidate = orderInfoFeignClient.isDriverOrder(driverId, orderId);
        isValidate.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(isValidate.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Boolean> startDrive = orderInfoFeignClient.startDrive(startDriveForm);
        startDrive.throwOnFailureOrDataIsNull();
        return startDrive.getData();
    }

    @Override
    public Boolean endDriveThread(OrderFeeForm orderFeeForm) {
        Long orderId = orderFeeForm.getOrderId();
        Long driverId = orderFeeForm.getDriverId();

        CompletableFuture<OrderInfo> orderInfoFuture = getOrderInfoCompletableFuture(orderId, driverId);
        CompletableFuture<Void> validateFuture = orderInfoFuture.thenCompose(order -> ValidateEndServiceAsync(order,
                orderId));

        CompletableFuture<BigDecimal> distanceFuture =
                orderInfoFuture.thenCompose(order -> calculateRealDistanceAsync(orderId));
        CompletableFuture<Long> countFuture = getOrderCountAsync(driverId);

        CompletableFuture<FeeRuleResponseVo> feeFuture = orderInfoFuture.thenCombine(distanceFuture, (order,
                                                                                                      distance) ->
                calculatetotalAmountAsync(order, orderFeeForm, distance)).thenCompose(f -> f);

        CompletableFuture<RewardRuleResponseVo> rewardFuture = orderInfoFuture.thenCombine(countFuture,
                        this::calculateReward)
                .thenCompose(f -> f);
        CompletableFuture<ProfitsharingRuleResponseVo> profitFuture = feeFuture.thenCombine(countFuture,
                        this::calculateProfitSharingFuture)
                .thenCompose(f -> f);

        // 合并所有结果
        CompletableFuture<Boolean> resultFuture = CompletableFuture.allOf(validateFuture, feeFuture, rewardFuture,
                        profitFuture)
                .thenApply(v -> {
                    UpdateOrderBillForm updateOrderBillForm = getUpdateOrderBillForm(orderFeeForm,
                            orderInfoFuture,
                            distanceFuture,
                            feeFuture,
                            rewardFuture,
                            profitFuture);
                    Result<Boolean> result = orderInfoFeignClient.endDrive(updateOrderBillForm);
                    return result.throwOnFailureOrDataIsNull().getData();
                });
        try {
            return resultFuture.get(5, TimeUnit.SECONDS); // 设置总超时
        } catch (TimeoutException e) {
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            log.warn("订单id={}结束服务异常={}", orderId, e);
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }


    }

    @NotNull
    private static UpdateOrderBillForm getUpdateOrderBillForm(OrderFeeForm orderFeeForm,
                                                              CompletableFuture<OrderInfo> orderInfoFuture,
                                                              CompletableFuture<BigDecimal> distanceFuture,
                                                              CompletableFuture<FeeRuleResponseVo> feeFuture,
                                                              CompletableFuture<RewardRuleResponseVo> rewardFuture,
                                                              CompletableFuture<ProfitsharingRuleResponseVo> profitFuture) {
        // 构建并提交订单
        // 6 封装实体类，结束代驾更新订单，添加账单和分账信息
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        // 路桥费、停车费、其他费用
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        OrderInfo orderInfo = orderInfoFuture.join();
        // 乘客好处费
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());
        // 实际里程
        BigDecimal realDistance = distanceFuture.join();
        updateOrderBillForm.setRealDistance(realDistance);
        // 订单奖励信息
        RewardRuleResponseVo rewardRuleResponseVo = rewardFuture.join();
        updateOrderBillForm.setRewardRuleId(rewardRuleResponseVo.getRewardRuleId());
        updateOrderBillForm.setRewardAmount(rewardRuleResponseVo.getRewardAmount());
        // 代驾费用信息
        FeeRuleResponseVo feeRuleResponseVo = feeFuture.join();
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setDistanceFee(feeRuleResponseVo.getDistanceFee());
        // 分账相关信息
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitFuture.join();
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
        return updateOrderBillForm;
    }


    /**
     * 计算分账信息
     *
     * @param feeRuleResponseVo
     * @param orderCount
     * @return
     */
    private CompletableFuture<ProfitsharingRuleResponseVo> calculateProfitSharingFuture(FeeRuleResponseVo feeRuleResponseVo, Long orderCount) {
        return CompletableFuture.supplyAsync(() -> {

            ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
            profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
            profitsharingRuleRequestForm.setOrderNum(orderCount);
            Result<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoResult =
                    profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm);

            ProfitsharingRuleResponseVo profitsharingRuleResponseVo =
                    profitsharingRuleResponseVoResult
                            .throwOnFailureOrDataIsNull()
                            .getData();
            return profitsharingRuleResponseVo;
        }, sharedThreadPool);


    }

    private CompletableFuture<RewardRuleResponseVo> calculateReward(OrderInfo orderInfo, Long orderCount) {
        return CompletableFuture.supplyAsync(() -> {
            RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
            rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            rewardRuleRequestForm.setOrderNum(orderCount);
            Result<RewardRuleResponseVo> rewardRuleResponseVoResult =
                    rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm);
            RewardRuleResponseVo rewardRuleResponseVo =
                    rewardRuleResponseVoResult.throwOnFailureOrDataIsNull().getData();
            log.info("当日每单奖励={}", JSON.toJSONString(rewardRuleResponseVo));
            return rewardRuleResponseVo;
        });


    }

    /**
     * 计算司机订单当日开始接单数量
     *
     * @param driverId
     * @return
     */
    private CompletableFuture<Long> getOrderCountAsync(Long driverId) {
        return CompletableFuture.supplyAsync(() -> {
            OrderCount orderCount = new OrderCount();
            orderCount.setDriverId(driverId);
            LocalDateTime startTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
            orderCount.setStartServiceTime(startTime);
            orderCount.setEndServiceTime(endTime);
            Result<Long> orderNumByTimeRes = orderInfoFeignClient.getOrderNumByTime(orderCount);
            Long orderCounts = orderNumByTimeRes.throwOnFailureOrDataIsNull().getData();
            log.info("当日接单数量={}", orderCounts);
            return orderCounts;
        }, sharedThreadPool);


    }

    /**
     * 计算 实际费用 = 代驾费用 + 其他费用（停车费）
     *
     * @param orderInfo
     * @param realDistance
     * @return
     */
    private CompletableFuture<FeeRuleResponseVo> calculatetotalAmountAsync(OrderInfo orderInfo,
                                                                           OrderFeeForm orderFeeForm,
                                                                           BigDecimal realDistance) {
        return CompletableFuture.supplyAsync(() -> {
            // 3.计算真实价格
            FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
            feeRuleRequestForm.setDistance(realDistance);
            feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
            // todo 等待时间优化~
            feeRuleRequestForm.setWaitMinute(0);
            Result<FeeRuleResponseVo> feeRuleResponseVoResult =
                    feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
            FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.throwOnFailureOrDataIsNull().getData();
            log.info("订单={}真实价格={}", orderInfo.getId(), JSON.toJSONString(feeRuleResponseVo));
            // 实际费用 = 代驾费用 + 其他费用（停车费）
            BigDecimal totalAmount =
                    feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getTollFee())
                            .add(orderFeeForm.getParkingFee())
                            .add(orderFeeForm.getOtherFee())
                            .add(orderInfo.getFavourFee());
            feeRuleResponseVo.setTotalAmount(totalAmount);

            log.info("订单={}实际费用={}", orderInfo.getId(), totalAmount);

            return feeRuleResponseVo;
        }, sharedThreadPool);

    }

    private CompletableFuture<BigDecimal> calculateRealDistanceAsync(Long orderId) {
        return CompletableFuture.supplyAsync(() -> {
            // 2.计算真实距离
            Result<BigDecimal> orderRealDistance = locationFeignClient.calculateOrderRealDistance(orderId);
            BigDecimal realDistance = orderRealDistance.throwOnFailureOrDataIsNull().getData();
            log.info("结束代驾，订单实际里程：{}", realDistance);
            return realDistance;
        }, sharedThreadPool);
    }

    /**
     * 校验并返回订单信息
     *
     * @param orderId
     * @param driverId
     * @return
     */
    @NotNull
    private CompletableFuture<OrderInfo> getOrderInfoCompletableFuture(Long orderId, Long driverId) {
        return CompletableFuture.supplyAsync(() -> {
            Result<OrderInfo> orderInfoRes = orderInfoFeignClient.getOrderInfo(orderId);
            OrderInfo orderInfo = orderInfoRes
                    .throwOnFailureOrDataIsNull()
                    .getData();
            validateDriverOrder(driverId, orderInfo);
            return orderInfo;
        }, sharedThreadPool);
    }

    /**
     * // 防止刷单，计算司机的最新经纬度与代驾的终点经纬度是否在2公里范围内
     *
     * @param orderInfo
     * @param orderId
     * @return
     */
    private CompletableFuture<Void> ValidateEndServiceAsync(OrderInfo orderInfo, Long orderId) {
        return CompletableFuture.runAsync(() -> {
            Result<OrderServiceLastLocationVo> orderServiceLastLocationRes =
                    locationFeignClient.getOrderServiceLastLocation(orderId);
            // 2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
            OrderServiceLastLocationVo orderServiceLastLocationVo =
                    orderServiceLastLocationRes.throwOnFailureOrDataIsNull().getData();
            validateEndService(orderInfo, orderServiceLastLocationVo);
        }, sharedThreadPool);
    }

    private static void validateEndService(OrderInfo orderInfo, OrderServiceLastLocationVo orderServiceLastLocationVo) {
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                orderInfo.getEndPointLongitude().doubleValue(),
                orderServiceLastLocationVo.getLatitude().doubleValue(),
                orderServiceLastLocationVo.getLongitude().doubleValue());
        if (distance > DriverConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }
    }

    @Override
    public Boolean endDrive(OrderFeeForm orderFeeForm) {
        // 1.校验订单合法性
        // 1.1
        Long orderId = orderFeeForm.getOrderId();
        Long driverId = orderFeeForm.getDriverId();
        Result<Boolean> isDriverOrderRes = orderInfoFeignClient.isDriverOrder(driverId,
                orderId);
        isDriverOrderRes.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(isDriverOrderRes.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }


        Result<OrderInfo> orderInfoRes = orderInfoFeignClient.getOrderInfo(orderId);
        orderInfoRes.throwOnFailureOrDataIsNull();
        OrderInfo orderInfo = orderInfoRes.getData();


        // 1.2 防刷单
        Result<OrderServiceLastLocationVo> orderServiceLastLocationRes =
                locationFeignClient.getOrderServiceLastLocation(orderId);
        orderServiceLastLocationRes.throwOnFailureOrDataIsNull();
        // 2.防止刷单，计算司机的经纬度与代驾的终点经纬度是否在2公里范围内
        OrderServiceLastLocationVo orderServiceLastLocationVo = orderServiceLastLocationRes.getData();
        // 司机的位置与代驾终点位置的距离
        double distance = LocationUtil.getDistance(orderInfo.getEndPointLatitude().doubleValue(),
                orderInfo.getEndPointLongitude().doubleValue(),
                orderServiceLastLocationVo.getLatitude().doubleValue(),
                orderServiceLastLocationVo.getLongitude().doubleValue());
        if (distance > DriverConstant.DRIVER_END_LOCATION_DISTION) {
            throw new GuiguException(ResultCodeEnum.DRIVER_END_LOCATION_DISTION_ERROR);
        }

        // 2.计算真实距离
        Result<BigDecimal> orderRealDistance = locationFeignClient.calculateOrderRealDistance(orderId);
        orderRealDistance.throwOnFailureOrDataIsNull();
        BigDecimal realDistance = orderRealDistance.getData();
        log.info("结束代驾，订单实际里程：{}", realDistance);

        // 3.计算真实价格
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(realDistance);
        feeRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        // todo 等待时间优化~
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        feeRuleResponseVoResult.throwOnFailureOrDataIsNull();
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();
        log.info("订单={}真实价格={}", orderId, JSON.toJSONString(feeRuleResponseVo));
        // 实际费用 = 代驾费用 + 其他费用（停车费）
        BigDecimal totalAmount =
                feeRuleResponseVo.getTotalAmount().add(orderFeeForm.getTollFee())
                        .add(orderFeeForm.getParkingFee())
                        .add(orderFeeForm.getOtherFee())
                        .add(orderInfo.getFavourFee());
        feeRuleResponseVo.setTotalAmount(totalAmount);

        log.info("订单={}实际费用={}", orderId, totalAmount);

        // 4 计算系统奖励
        // 4.1 获取当日开始接单数量
        OrderCount orderCount = new OrderCount();
        orderCount.setDriverId(driverId);
        LocalDateTime startTime = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endTime = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);
        orderCount.setStartServiceTime(startTime);
        orderCount.setEndServiceTime(endTime);
        Result<Long> orderNumByTimeRes = orderInfoFeignClient.getOrderNumByTime(orderCount);
        orderNumByTimeRes.throwOnFailureOrDataIsNull();
        Long orderCounts = orderNumByTimeRes.getData();
        log.info("当日接单数量={}", orderCounts);

        // 4.2 计算奖励信息
        RewardRuleRequestForm rewardRuleRequestForm = new RewardRuleRequestForm();
        rewardRuleRequestForm.setStartTime(orderInfo.getStartServiceTime());
        rewardRuleRequestForm.setOrderNum(orderCounts);
        Result<RewardRuleResponseVo> rewardRuleResponseVoResult =
                rewardRuleFeignClient.calculateOrderRewardFee(rewardRuleRequestForm);
        rewardRuleResponseVoResult.throwOnFailureOrDataIsNull();
        RewardRuleResponseVo rewardRuleResponseVo = rewardRuleResponseVoResult.getData();
        log.info("当日每单奖励={}", JSON.toJSONString(rewardRuleResponseVo));

        // 5 计算分账信息
        ProfitsharingRuleRequestForm profitsharingRuleRequestForm = new ProfitsharingRuleRequestForm();
        profitsharingRuleRequestForm.setOrderAmount(feeRuleResponseVo.getTotalAmount());
        profitsharingRuleRequestForm.setOrderNum(orderNumByTimeRes.getData());
        Result<ProfitsharingRuleResponseVo> profitsharingRuleResponseVoResult =
                profitsharingRuleFeignClient.calculateOrderProfitsharingFee(profitsharingRuleRequestForm);

        profitsharingRuleResponseVoResult.throwOnFailureOrDataIsNull();
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = profitsharingRuleResponseVoResult.getData();
        log.info("订单={}的分账信息={}", orderId, JSON.toJSONString(profitsharingRuleResponseVo));

        // 6 封装实体类，结束代驾更新订单，添加账单和分账信息
        UpdateOrderBillForm updateOrderBillForm = new UpdateOrderBillForm();
        updateOrderBillForm.setOrderId(orderFeeForm.getOrderId());
        updateOrderBillForm.setDriverId(orderFeeForm.getDriverId());
        // 路桥费、停车费、其他费用
        updateOrderBillForm.setTollFee(orderFeeForm.getTollFee());
        updateOrderBillForm.setParkingFee(orderFeeForm.getParkingFee());
        updateOrderBillForm.setOtherFee(orderFeeForm.getOtherFee());
        // 乘客好处费
        updateOrderBillForm.setFavourFee(orderInfo.getFavourFee());

        // 实际里程
        updateOrderBillForm.setRealDistance(realDistance);
        // 订单奖励信息
        updateOrderBillForm.setRewardRuleId(rewardRuleResponseVo.getRewardRuleId());
        updateOrderBillForm.setRewardAmount(rewardRuleResponseVo.getRewardAmount());
        // 代驾费用信息
        BeanUtils.copyProperties(feeRuleResponseVo, updateOrderBillForm);
        // 分账相关信息
        BeanUtils.copyProperties(profitsharingRuleResponseVo, updateOrderBillForm);
        updateOrderBillForm.setProfitsharingRuleId(profitsharingRuleResponseVo.getProfitsharingRuleId());
        Result<Boolean> result = orderInfoFeignClient.endDrive(updateOrderBillForm);
        result.throwOnFailureOrDataIsNull();

        return result.getData();
    }


    @Override
    public PageVo<OrderListVo> findDriverOrderPage(Long driverId, Long page, Long limit) {
        Result<PageVo<OrderListVo>> driverOrderPage = orderInfoFeignClient.findDriverOrderPage(driverId, page, limit);
        driverOrderPage.throwOnFailureOrDataIsNull();
        return driverOrderPage.getData();
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        return orderInfoFeignClient
                .sendOrderBillInfo(orderId, driverId)
                .throwOnFailureOrDataIsNull()
                .getData();
    }


}
