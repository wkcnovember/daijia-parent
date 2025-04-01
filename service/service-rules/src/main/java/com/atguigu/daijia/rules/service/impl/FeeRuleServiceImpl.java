package com.atguigu.daijia.rules.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.model.convert.rule.RuleResponseConvert;
import com.atguigu.daijia.model.form.rules.FeeRuleRequest;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.rules.service.FeeRuleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class FeeRuleServiceImpl implements FeeRuleService {

    @Resource
    private KieContainer kieContainer;

    @Resource
    private RuleResponseConvert feeRuleResponseConvert;


    public static void main(String[] args) {
    }

    // todo 后续将规则封装到数据库中
    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        // 封装传入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(calculateOrderFeeForm.getDistance());
        // calculateOrderFeeForm.getStartTime()
        String startTime = DateTimeFormatter.ofPattern("HH:mm:ss").format(calculateOrderFeeForm.getStartTime());
        // String startTime = new DateTime(calculateOrderFeeForm.getStartTime()).toString("HH:mm:ss");
        feeRuleRequest.setStartTime(startTime);
        feeRuleRequest.setWaitMinute(calculateOrderFeeForm.getWaitMinute());
        log.info("传入参数：{}", JSON.toJSONString(feeRuleRequest));
        // 开启规则
        KieSession kieSession = kieContainer.newKieSession();

        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        kieSession.insert(feeRuleRequest);
        kieSession.fireAllRules();
        long identifier = kieSession.getIdentifier();
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(feeRuleResponse));

        // 封装返回对象
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseConvert.toFeeRuleResponseVo(feeRuleResponse);
        feeRuleResponseVo.setFeeRuleId(identifier);
        return feeRuleResponseVo;
    }
}
