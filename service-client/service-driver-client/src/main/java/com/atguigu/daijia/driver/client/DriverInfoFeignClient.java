package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(value = "service-driver", path = "/driver/info")
public interface DriverInfoFeignClient {

    /**
     * 小程序授权登录
     *
     * @param code
     * @return
     */
    @GetMapping("/login/{code}")
    Result<Long> login(@PathVariable("code") String code);

    @GetMapping("/getDriverLoginInfo/{driverId}")
    Result<DriverLoginVo> getDriverInfo(@PathVariable("driverId") Long driverId);

    @PostMapping("/updateDriverAuthInfo")
    Result<Boolean> updateDriverAuthInfo(@RequestBody UpdateDriverAuthInfoForm updateDriverAuthInfoForm);


    /**
     * 获取司机认证信息
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDriverAuthInfo/{driverId}")
    Result<DriverAuthInfoVo> getDriverAuthInfo(@PathVariable("driverId") Long driverId);

    /**
     * 创建司机人脸模型
     *
     * @param driverFaceModelForm
     * @return
     */
    @PostMapping("/creatDriverFaceModel")
    Result<Boolean> creatDriverFaceModel(@RequestBody DriverFaceModelForm driverFaceModelForm);


    /**
     * 获取司机个性化信息
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDriverSet/{driverId}")
    Result<DriverSetVo> getDriverSet(@PathVariable("driverId") Long driverId);

    /**
     * 批量获取司机个性化信息
     *
     * @param driverIds
     * @return
     */

    @PostMapping("/getDriverSets")
    Result<Map<Long, DriverSetVo>> getDriverSetMap(@RequestBody List<Long> driverIds);

    /**
     * 判断司机当日是否进行过人脸识别
     *
     * @param driverId
     * @return
     */
    @GetMapping("/isFaceRecognition/{driverId}")
    Result<Boolean> isFaceRecognition(@PathVariable("driverId") Long driverId);

    /**
     * "验证司机人脸"
     *
     * @param driverFaceModelForm
     * @return
     */
    @PostMapping("/verifyDriverFace")
    Result<Boolean> verifyDriverFace(@RequestBody DriverFaceModelForm driverFaceModelForm);


    /**
     * 更新接单状态
     *
     * @param driverId
     * @param status
     * @return
     */
    @GetMapping("/updateServiceStatus/{driverId}/{status}")
    Result<Boolean> updateServiceStatus(@PathVariable("driverId") Long driverId,
                                        @PathVariable("status") Integer status);

    /**
     * 获取获取司机信息(乘客显示)
     *
     * @param driverId
     * @return
     */
    @GetMapping("/getDriverInfoVo/{driverId}")
    Result<DriverInfoVo> getDriverInfoVo(@PathVariable("driverId") Long driverId);


}
