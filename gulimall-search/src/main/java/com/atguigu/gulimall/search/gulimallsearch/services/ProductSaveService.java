package com.atguigu.gulimall.search.gulimallsearch.services;

import com.atguigu.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

public interface ProductSaveService {


    void productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
