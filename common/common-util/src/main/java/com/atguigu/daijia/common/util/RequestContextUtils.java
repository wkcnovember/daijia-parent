package com.atguigu.daijia.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 请求上下文工具类
 */
public class RequestContextUtils {

    /**
     * 获取当前请求的HttpServletRequest对象
     */
    public static HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 获取客户端操作系统信息
     */
    public static String getClientOS() {
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("windows")) {
            return "Windows";
        } else if (userAgent.contains("mac")) {
            return "Mac";
        } else if (userAgent.contains("x11")) {
            return "Unix";
        } else if (userAgent.contains("android")) {
            return "Android";
        } else if (userAgent.contains("iphone")) {
            return "iOS";
        } else if (userAgent.contains("linux")) {
            return "Linux";
        }

        return "Unknown";
    }

    /**
     * 获取客户端浏览器信息
     */
    public static String getClientBrowser() {
        HttpServletRequest request = getRequest();
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        if (userAgent.contains("msie") || userAgent.contains("trident")) {
            return "IE";
        } else if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("opera")) {
            return "Opera";
        }

        return "Unknown";
    }

    /**
     * 获取请求方法
     */
    public static String getRequestMethod() {
        return getRequest().getMethod();
    }

    /**
     * 获取请求URL
     */
    public static String getRequestUrl() {
        HttpServletRequest request = getRequest();
        return request.getRequestURL().toString();
    }

    /**
     * 获取请求URI
     */
    public static String getRequestUri() {
        return getRequest().getRequestURI();
    }

    /**
     * 获取请求来源（Referer）
     */
    public static String getRequestReferer() {
        return getRequest().getHeader("Referer");
    }

    /**
     * 获取用户代理字符串
     */
    public static String getUserAgent() {
        return getRequest().getHeader("User-Agent");
    }
}
