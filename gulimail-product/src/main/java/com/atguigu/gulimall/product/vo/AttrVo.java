package com.atguigu.gulimall.product.vo;

import lombok.Data;

@Data
public class AttrVo {

        /**
         * 属性id
         */
        private Long attrId;
        /**
         * 属性名
         */
        private String attrName;
        /**
         * 是否需要检索
         */
        private Integer searchType;
        /**
         * 值类型
         */
        private String icon;
        /**
         * ??ѡֵ?б?[?ö??ŷָ
         */
        private String valueSelect;
        /**
         * ????????[0-???????ԣ?1-???????ԣ?2-???????????????ǻ???????]
         */
        private Integer attrType;
        /**
         * ????״̬[0 - ???ã?1 - ????]
         */
        private Long enable;
        /**
         * ???????
         */
        private Long catelogId;
        /**
         * 快速展示「是否展示在介绍上」，在sku中仍然可以调整
         */
        private Integer showDesc;

        private Integer valueType;

        private Long attrGroupId;

}
