package com.atguigu.common.to.es;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 */
@Data
public class SkuEsModel {

    private Long skuId;

    private Long spuId;

    private String skuTitle;

    private BigDecimal skuPrice;

    private String skuImg;

    private Long saleCount;

    private Boolean hasStock;

    private Long hotScore;

    private Long brandId;

    private Long catalogId;

    private String brandName;

    private String brandImg;

    private String catalogName;

    private List<Attrs> attrs;

    public static class Attrs{
       private Long attrId;
       private String attrName;
       private String attrValue;

       public Attrs() {
       }

       public Attrs(Long attrId, String attrName, String attrValue) {
          this.attrId = attrId;
          this.attrName = attrName;
          this.attrValue = attrValue;
       }

       public Long getAttrId() {
          return attrId;
       }

       public void setAttrId(Long attrId) {
          this.attrId = attrId;
       }

       public String getAttrName() {
          return attrName;
       }

       public void setAttrName(String attrName) {
          this.attrName = attrName;
       }

       public String getAttrValue() {
          return attrValue;
       }

       public void setAttrValue(String attrValue) {
          this.attrValue = attrValue;
       }
    }

}
