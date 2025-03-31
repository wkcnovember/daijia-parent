package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.form.rules.RewardRuleRequest;
import com.atguigu.daijia.model.form.rules.RewardRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponse;
import com.atguigu.daijia.model.vo.rules.RewardRuleResponseVo;
import com.atguigu.daijia.rules.service.RewardRuleService;
import com.atguigu.daijia.rules.utils.DroolsHelper;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class RewardRuleServiceImpl implements RewardRuleService {


    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        //封装传入参数对象
        RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
        String startTime = DateTimeFormatter.ofPattern("HH:mm:ss").format(rewardRuleRequestForm.getStartTime());
        rewardRuleRequest.setOrderNum(rewardRuleRequestForm.getOrderNum());
        rewardRuleRequest.setStartTime(startTime);
        //创建规则引擎对象
        KieSession kieSession = DroolsHelper.loadForRule(DroolsHelper.REWARDRULE_DRL);

        //封装返回对象
        RewardRuleResponse rewardRuleResponse = new RewardRuleResponse();
        kieSession.setGlobal("rewardRuleResponse",rewardRuleResponse);

        //设置对象，触发规则
        kieSession.insert(rewardRuleRequest);
        kieSession.fireAllRules();
        long id = kieSession.getIdentifier();
        //终止会话
        kieSession.dispose();

        //封装RewardRuleResponseVo
        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardRuleId(id);
        rewardRuleResponseVo.setRewardAmount(rewardRuleResponse.getRewardAmount());
        return rewardRuleResponseVo;
    }
}
