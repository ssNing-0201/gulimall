package com.atguigu.gulimall.product.vo;

import lombok.Data;

import java.util.List;

@Data
// 二级分类 vo
public class CateLog2Vo {

    private String catelog1Id; // 一级父分类
    private List<CataLog3Vo> catalog3List; // 三级子分类
    private String id;
    private String name;

    // 三级分类 vo
    public static class CataLog3Vo {
        private String catelog2Id; // 父分类 二级分类Id
        private String id;
        private String name;

        public CataLog3Vo() {
        }

        public CataLog3Vo(String catelog2Id, String id, String name) {
            this.catelog2Id = catelog2Id;
            this.id = id;
            this.name = name;
        }

        public String getCatelog2Id() {
            return catelog2Id;
        }

        public void setCatelog2Id(String catelog2Id) {
            this.catelog2Id = catelog2Id;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public CateLog2Vo() {
    }

    public CateLog2Vo(String catelog1Id, List<CataLog3Vo> catalog3List, String id, String name) {
        this.catelog1Id = catelog1Id;
        this.catalog3List = catalog3List;
        this.id = id;
        this.name = name;
    }

    public String getCatelog1Id() {
        return catelog1Id;
    }

    public void setCatelog1Id(String catelog1Id) {
        this.catelog1Id = catelog1Id;
    }

    public List<CataLog3Vo> getCatalog3List() {
        return catalog3List;
    }

    public void setCatalog3List(List<CataLog3Vo> catalog3List) {
        this.catalog3List = catalog3List;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
