package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.mapper.DriverAccountDetailMapper;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.service.DriverAccountService;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverAccountDetail;
import com.atguigu.daijia.model.form.driver.TransferForm;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DriverAccountServiceImpl extends ServiceImpl<DriverAccountMapper, DriverAccount> implements DriverAccountService {


    @Resource
    private DriverAccountDetailMapper driverAccountDetailMapper;

    @Override
    @Transactional
    public Boolean transfer(TransferForm transferForm) {
        // 1 去重
        LambdaQueryWrapper<DriverAccountDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverAccountDetail::getTradeNo, transferForm.getTradeNo());
        Long count = driverAccountDetailMapper.selectCount(wrapper);
        if (count > 0) {
            return Boolean.TRUE;
        }

        // 2 添加奖励到司机账户表
        int i = baseMapper.add(transferForm.getDriverId(), transferForm.getAmount());
        if(i < 1) {
            log.error("添加奖励到司机账户={}失败",transferForm.getDriverId());
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        // 3 添加交易记录
        DriverAccountDetail driverAccountDetail = new DriverAccountDetail();
        BeanUtils.copyProperties(transferForm, driverAccountDetail);
        i = driverAccountDetailMapper.insert(driverAccountDetail);

        if(i < 1) {
            log.error("添加交易记录到司机账户={}失败",transferForm.getDriverId());
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        return Boolean.TRUE;
    }
}
