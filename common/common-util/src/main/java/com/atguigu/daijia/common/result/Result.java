package com.atguigu.daijia.common.result;


import com.atguigu.daijia.common.execption.GuiguException;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Objects;

/**
 * 全局统一返回结果类
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class Result<T> {

    // 返回码
    private Integer code;



    // 返回消息
    private String message;

    // 返回数据
    private T data;

    public Result() {
    }

    // 返回数据
    protected static <T> Result<T> build(T data) {
        Result<T> result = new Result<T>();
        if (data != null)
            result.setData(data);
        return result;
    }

    public static <T> Result<T> build(T body, Integer code, String message) {
        Result<T> result = build(body);
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> build(T body, ResultCodeEnum resultCodeEnum) {
        Result<T> result = build(body);
        result.setCode(resultCodeEnum.getCode());
        result.setMessage(resultCodeEnum.getMessage());
        return result;
    }

    public static <T> Result<T> ok() {
        return Result.ok(null);
    }

    /**
     * 操作成功
     *
     * @param data baseCategory1List
     * @param <T>
     * @return
     */
    public static <T> Result<T> ok(T data) {
        return build(data, ResultCodeEnum.SUCCESS);
    }

    public static <T> Result<T> fail() {
        return Result.fail(null);
    }

    /**
     * 操作失败
     *
     * @param data
     * @param <T>
     * @return
     */
    public static <T> Result<T> fail(T data) {
        return build(data, ResultCodeEnum.FAIL);
    }

    public Result<T> message(String msg) {
        this.setMessage(msg);
        return this;
    }

    public Result<T> code(Integer code) {
        this.setCode(code);
        return this;
    }

    // 判断是否状态码OK
    public boolean checkIsSuccess() {
        return Objects.equals(ResultCodeEnum.SUCCESS.getCode(), this.code);
    }

    // 判断是否状态码OK
    public boolean checkIsError() {
        return !Objects.equals(ResultCodeEnum.SUCCESS.getCode(), this.code);
    }

    public void throwOnFailure() {
        if (checkIsError()) {
            throw new GuiguException(this.getCode(), this.getMessage());
        }
    }
}
