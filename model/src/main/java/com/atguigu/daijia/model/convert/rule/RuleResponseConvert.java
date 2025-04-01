package com.atguigu.daijia.model.convert.rule;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponse;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponse;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/3/21 10:38
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface RuleResponseConvert {
    FeeRuleResponseVo toFeeRuleResponseVo(FeeRuleResponse ruleResponse);

    ProfitsharingRuleResponseVo toProfitsharingRuleResponseVo(ProfitsharingRuleResponse profitsharingRuleResponse);
}
