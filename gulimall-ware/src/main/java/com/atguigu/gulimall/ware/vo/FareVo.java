package com.atguigu.gulimall.ware.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class FareVo {

    private MemberAddressVo address;

    private BigDecimal Fare;
}
