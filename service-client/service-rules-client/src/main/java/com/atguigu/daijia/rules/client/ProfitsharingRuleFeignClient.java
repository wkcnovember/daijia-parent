package com.atguigu.daijia.rules.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.atguigu.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-rules",path = "/rules/profitsharing")
public interface ProfitsharingRuleFeignClient {

    /**
     * 计算系统分账费用
     *
     * @param profitsharingRuleRequestForm
     * @return
     */
    @PostMapping("/calculateOrderProfitsharingFee")
    Result<ProfitsharingRuleResponseVo> calculateOrderProfitsharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm);


}
