package com.atguigu.daijia.rules.service.impl;

import com.atguigu.daijia.model.convert.rule.RuleResponseConvert;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequest;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponse;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.atguigu.daijia.rules.service.ProfitsharingRuleService;
import com.atguigu.daijia.rules.utils.DroolsHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

import static com.atguigu.daijia.rules.utils.DroolsHelper.PROFITSHARINGRULE_DRL;

@Slf4j
@Service
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {

    @Resource
    private RuleResponseConvert ruleResponseConvert;


    @Override
    public ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        //传入参数对象封装
        ProfitsharingRuleRequest profitsharingRuleRequest = new ProfitsharingRuleRequest();
        profitsharingRuleRequest.setOrderAmount(profitsharingRuleRequestForm.getOrderAmount());
        profitsharingRuleRequest.setOrderNum(profitsharingRuleRequestForm.getOrderNum());

        //创建kieSession
        KieSession kieSession = DroolsHelper.loadForRule(PROFITSHARINGRULE_DRL);

        //封装返回对象
        ProfitsharingRuleResponse profitsharingRuleResponse = new ProfitsharingRuleResponse();
        kieSession.setGlobal("profitsharingRuleResponse",profitsharingRuleResponse);

        //触发规则，返回vo对象
        kieSession.insert(profitsharingRuleRequest);
        kieSession.fireAllRules();
        long id = kieSession.getIdentifier();
        kieSession.dispose();

        ProfitsharingRuleResponseVo profitsharingRuleResponseVo =
                ruleResponseConvert.toProfitsharingRuleResponseVo(profitsharingRuleResponse);
        // todo 规则暂时这样后续持久化
        profitsharingRuleResponseVo.setProfitsharingRuleId(1L);
        return profitsharingRuleResponseVo;
    }
}
