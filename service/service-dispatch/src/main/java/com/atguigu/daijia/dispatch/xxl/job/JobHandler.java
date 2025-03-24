package com.atguigu.daijia.dispatch.xxl.job;

import com.atguigu.daijia.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.model.constants.xxl.XxlJobConstants;
import com.atguigu.daijia.model.entity.dispatch.XxlJobLog;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author 柯佳元
 * @Create 2025/3/24 9:31
 * @Version 1.0
 * Description:
 */

@Component
@Schema(name = "下单搜索附件司机")
public class JobHandler {


    private final XxlJobLogMapper xxlJobLogMapper;
    private final NewOrderService newOrderService;

    @Autowired
    public JobHandler(XxlJobLogMapper xxlJobLogMapper, NewOrderService newOrderService) {
        this.xxlJobLogMapper = xxlJobLogMapper;
        this.newOrderService = newOrderService;
    }

    @XxlJob("newOrderTaskHandler")
    public void newOrderTaskHandler() {
        //记录任务调度日志
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();

        try {
            //执行任务：搜索附近代驾司机
            newOrderService.executeTask(XxlJobHelper.getJobId());

            //成功状态
            xxlJobLog.setStatus(XxlJobConstants.XxlJobLogStatus.SUCCESS.getStatus());
        } catch (Exception e) {
            //失败状态
            xxlJobLog.setStatus(XxlJobConstants.XxlJobLogStatus.ERROR.getStatus());
            xxlJobLog.setError(e.getMessage());
            e.printStackTrace();
        } finally {
            // 记录任务执行时间~
            long times = System.currentTimeMillis()- startTime;
            xxlJobLog.setTimes(times);
            xxlJobLogMapper.insert(xxlJobLog);
        }

    }
}
