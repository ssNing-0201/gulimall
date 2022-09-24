package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SpuItemGroupVo {
    private String groupName;
    private List<Attr> attrs;
}
