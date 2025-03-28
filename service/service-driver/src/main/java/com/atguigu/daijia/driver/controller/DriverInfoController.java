package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.groups.Default;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "司机API接口管理")
@RestController
@RequestMapping(value = "/driver/info")
@Validated
public class DriverInfoController {


    @Resource
    private DriverInfoService driverInfoService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable("code") @NotNull String code) {
        return Result.ok(driverInfoService.login(code));
    }

    @Operation(summary = "获取司机登录信息")
    @GetMapping("/getDriverLoginInfo/{driverId}")
    public Result<DriverLoginVo> getDriverInfo(@PathVariable @NotNull Long driverId) {
        DriverLoginVo driverLoginVo = driverInfoService.getDriverInfo(driverId);
        return Result.ok(driverLoginVo);
    }

    // 更新司机认证信息
    @Operation(summary = "更新司机认证信息")
    @PostMapping("/updateDriverAuthInfo")
    public Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        Boolean isSuccess = driverInfoService.updateDriverAuthInfo(updateDriverAuthInfoForm);
        return Result.ok(isSuccess);
    }

    @Operation(summary = "获取司机认证信息")
    @GetMapping("/getDriverAuthInfo/{driverId}")
    public Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable Long driverId) {
        DriverAuthInfoVo driverAuthInfoVo = driverInfoService.getDriverAuthInfo(driverId);
        return Result.ok(driverAuthInfoVo);
    }

    // 创建司机人脸模型
    @Operation(summary = "创建司机人脸模型")
    @PostMapping("/creatDriverFaceModel")
    public Result<Boolean> creatDriverFaceModel(@RequestBody @Validated DriverFaceModelForm driverFaceModelForm) {
        Boolean isSuccess = driverInfoService.creatDriverFaceModel(driverFaceModelForm);
        return Result.ok(isSuccess);
    }

    @Operation(summary = "获取司机设置信息")
    @GetMapping("/getDriverSet/{driverId}")
    public Result<DriverSetVo> getDriverSet(@PathVariable @NotNull Long driverId) {
        return Result.ok(driverInfoService.getDriverSet(driverId));
    }

    @Operation(summary = "批量获取司机设置信息")
    @PostMapping("/getDriverSets")
    public Result<Map<Long, DriverSetVo>> getDriverSetMap(@RequestBody List<Long> driverIds) {
        return Result.ok(driverInfoService.getDriverSetMap(driverIds));
    }

    @Operation(summary = "判断司机当日是否进行过人脸识别")
    @GetMapping("/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable("driverId") @NotNull Long driverId) {
        return Result.ok(driverInfoService.isFaceRecognition(driverId));
    }

    @Operation(summary = "验证司机人脸")
    @PostMapping("/verifyDriverFace")
    public Result<Boolean> verifyDriverFace(@RequestBody @Validated({ServiceGroup.class, Default.class})
                                            DriverFaceModelForm driverFaceModelForm) {
        return Result.ok(driverInfoService.verifyDriverFace(driverFaceModelForm));
    }

    @Operation(summary = "更新接单状态")
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    public Result<Boolean> updateServiceStatus(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                               @PathVariable("status") @NotNull @Range(min = 0, max = 1) Integer status) {
        return Result.ok(driverInfoService.updateServiceStatus(driverId, status));
    }

    @Operation(summary = "获取获取司机信息(乘客显示)")
    @GetMapping("/getDriverInfoVo/{driverId}")
    public Result<DriverInfoVo> getDriverInfoVo(@PathVariable("driverId") @NotNull @Positive Long driverId) {
        DriverInfoVo driverInfo = driverInfoService.getDriverInfoVo(driverId);
        return Result.ok(driverInfo);
    }


}

