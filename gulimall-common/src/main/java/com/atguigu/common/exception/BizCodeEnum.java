package com.atguigu.common.exception;

/**
 * 错误码列表
 * 01：通用
 *     001：参数格式校验
 *     002：验证码使用过频繁
 * 11：商品
 * 12：订单
 * 13：购物车
 * 14：物流
 * 15：用户
 * 21：库存
 */
public enum BizCodeEnum {
    UNKNOW_EXCEPTION(10000, "系统未知异常"),
    VAILD_EXCEPTION(10001, "参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002, "验证码使用过频繁"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001,"用户名存在"),
    PHONE_EXIST_EXCEPTION(15002,"手机号存在"),
    LOGINACCT_PWD_EXCEPTION(15003,"账号或密码错误"),
    NO_STOCK_EXCEPTION(21000,"库存不足");

    private int code;
    private String msg;

    BizCodeEnum(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
