package com.atguigu.gulimall.search.gulimallsearch.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.gulimall.search.gulimallsearch.constant.EsConstant;
import com.atguigu.gulimall.search.gulimallsearch.services.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Resource
    ElasticsearchClient client;

    @Override
    public void productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //数据保存到es
        BulkRequest.Builder br = new BulkRequest.Builder();
        for (SkuEsModel sku:skuEsModels){
            br.operations(op->op.index(idx->idx.index(EsConstant.PRODUCT_INDEX).document(sku).id(sku.getSkuId()+"")));
        }
        BulkResponse result = client.bulk(br.build());

        // TODO 如果保存出错，处理错误信息（要知道什么商品保存错误）
        //如果保存出错，日志保存错误信息
        if (result.errors()) {
            log.error("Bulk had errors");
            for (BulkResponseItem item: result.items()) {
                if (item.error() != null) {
                    log.error(item.error().reason());
                }
            }
        }
    }
}
