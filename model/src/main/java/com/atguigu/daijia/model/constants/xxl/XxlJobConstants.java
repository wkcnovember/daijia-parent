package com.atguigu.daijia.model.constants.xxl;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/3/24 9:37
 * @Version 1.0
 * Description:
 */
public class XxlJobConstants {


    @Getter
    public  enum XxlJobLogStatus {
        SUCCESS(1,"调度成功"),ERROR(0,"调度失败");
        private final Integer status;
        private final String msg;

        XxlJobLogStatus(Integer status, String msg) {
            this.status = status;
            this.msg = msg;
        }
    }
}
