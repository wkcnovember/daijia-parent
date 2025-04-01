package com.atguigu.daijia.model.enums.order;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/3/31 15:27
 * @Version 1.0
 * Description:
 */
@Getter
public enum ProfitSharingStatus {

    OFF(1,"未分账"),ON(2,"已分账");
    private final Integer status;
    private final String comment;

    ProfitSharingStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }
}
