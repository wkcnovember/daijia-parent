package com.atguigu.daijia.common.constant;


import lombok.Getter;

public class DriverConstant {

    // 附近司机搜索半径
    public static final double NEARBY_DRIVER_RADIUS = 5;

    // 取消订单延迟时间，单位：秒
    public static final int CANCEL_ORDER_DELAY_TIME = 15 * 60;

    // 默认接单距离，单位：公里
    public static final int ACCEPT_DISTANCE = 5;

    // 司机的位置与代驾起始点位置的确认距离，单位：米
    public static final int DRIVER_START_LOCATION_DISTION = 1000;

    // 司机的位置与代驾终点位置的确认距离，单位：米
    public static final int DRIVER_END_LOCATION_DISTION = 2000;

    // 分账延迟时间，单位：秒
    public static final int PROFITSHARING_DELAY_TIME = 2 * 60;

    // 司机默认头像
    public static final String DRIVER_DEFAULT_AVATAR = "https://c-ssl.duitang" +
            ".com/uploads/item/201910/01/20191001125343_shtxh.jpg";

    // 订单走多远 0：无限制
    public static final int ORDER_DEFAULT_DISTANCE = 0;

    // 自动接单吗
    public static final int NOT_AUTO_ORDER = 0;

    @Getter
    public enum ServiceStatus {
        NOT_ACCEPTED_ORDERS(0,"未接单"),
        ACCEPTING_ORDERS(1,"开始接单");
        private final int status;
        private final String description;

        ServiceStatus(int status, String description) {
            this.status = status;
            this.description = description;
        }
    }

    @Getter
    public enum AuthStatus {

        NOT_AUTHENTICATED(0, "未认证"),
        IN_REVIEW(1, "审核中"),
        APPROVED(2, "认证通过"),
        REJECTED(-1, "认证未通过");

        private final int code;
        private final String description;

        AuthStatus(int code, String description) {
            this.code = code;
            this.description = description;
        }


    }


}
