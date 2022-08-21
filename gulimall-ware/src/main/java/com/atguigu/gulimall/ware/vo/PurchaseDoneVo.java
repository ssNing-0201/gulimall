package com.atguigu.gulimall.ware.vo;

import javax.validation.constraints.NotNull;
import java.util.List;

public class PurchaseDoneVo {

    @NotNull
    private Long id;

    private List<PurchaseItemVo> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PurchaseItemVo> getItems() {
        return items;
    }

    public void setItems(List<PurchaseItemVo> items) {
        this.items = items;
    }
}
