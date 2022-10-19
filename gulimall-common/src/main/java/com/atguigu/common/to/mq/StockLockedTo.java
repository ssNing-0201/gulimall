package com.atguigu.common.to.mq;


import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class StockLockedTo {

    private Long id; // 库存工作单的id

    private StockDetailTo detailTo; // 工作单详情 Id


}
