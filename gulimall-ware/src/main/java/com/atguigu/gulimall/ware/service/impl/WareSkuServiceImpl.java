package com.atguigu.gulimall.ware.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockDetailTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.atguigu.gulimall.ware.entity.WareOrderTaskEntity;
import com.atguigu.gulimall.ware.exception.NoStockException;
import com.atguigu.gulimall.ware.feign.OrderFeignService;
import com.atguigu.gulimall.ware.feign.ProductFeignService;
import com.atguigu.gulimall.ware.service.WareOrderTaskDetailService;
import com.atguigu.gulimall.ware.service.WareOrderTaskService;
import com.atguigu.gulimall.ware.vo.OrderItemVo;
import com.atguigu.gulimall.ware.vo.OrderVo;
import com.atguigu.gulimall.ware.vo.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
import com.mysql.cj.util.StringUtils;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Resource
    private WareSkuDao wareSkuDao;
    @Resource
    private ProductFeignService productFeignService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private WareOrderTaskService wareOrderTaskService;
    @Resource
    private WareOrderTaskDetailService wareOrderTaskDetailService;
    @Resource
    private OrderFeignService orderFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isNullOrEmpty(skuId)) {
            wrapper.eq("sku_id", skuId);
        }
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isNullOrEmpty(wareId)) {
            wrapper.eq("ware_id", wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //判断如果没有这个库存记录则新增
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        if (wareSkuEntities.size() == 0 || wareSkuEntities == null) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // 调用远程接口查找商品名
            // TODO 还可以如何让异常出现不回滚
            try {
                R info = productFeignService.info(skuId);
                Integer code = (Integer) info.get("code");
                if (code == 0) {
                    Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            //有库存记录，更新库存
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }

    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId -> {
            // 查询当前sku的总库存量
            SkuHasStockVo vo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 为某个订单锁定库存
     * 场景
     * 1、下单成功，订单过期没有支付系统自动取消，或用户手动取消，需要解锁库存。
     * <p>
     * 2、下单成功，库存锁定成功，但是接下来业务失败，导致订单回滚。之前锁定的库存订单需要自动解锁。
     */
    @Transactional
    @Override
    public Boolean OrderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情，追溯。
         */
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(wareOrderTaskEntity);
        // 找到每个商品在哪个仓库有库存
        List<OrderItemVo> looks = vo.getLooks();
        List<SkuHasStock> collect = looks.stream().map(item -> {
            SkuHasStock skuHasStock = new SkuHasStock();
            long skuId = item.getSkuId();
            skuHasStock.setSkuId(skuId);
            skuHasStock.setNum(item.getCount());
            List<Long> wareIds = wareSkuDao.ListWareIdHasStock(skuId);
            skuHasStock.setWareId(wareIds);
            return skuHasStock;
        }).collect(Collectors.toList());
        // 锁定库存
        for (SkuHasStock hasStock : collect) {
            Boolean skuStock = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null && wareIds.size() == 0) {
                throw new NoStockException(skuId);
            }
            /*
             * 库存解锁场景
             * 锁定成功，将当前锁定记录发送给mq
             * 锁定失败，前面保存工单信息回滚，发送出去的消息，即便要解锁记录，由于去数据库查不到id也无法解锁
             * */
            for (Long wareId : wareIds) {
                // 成功是1，失败是0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1l) {
                    // 锁定成功
                    skuStock = true;
                    //TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
                    wareOrderTaskDetailEntity.setSkuId(skuId);
                    wareOrderTaskDetailEntity.setSkuNum(hasStock.getNum());
                    wareOrderTaskDetailEntity.setTaskId(wareOrderTaskDetailEntity.getId());
                    wareOrderTaskDetailEntity.setWareId(wareId);
                    wareOrderTaskDetailEntity.setLockStatus(1);
                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                    // rabbitMQ
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTo);
                    stockLockedTo.setDetailTo(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                }
            }
            if (!skuStock) {
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    // 解锁库存
    @Override
    public void unlockStock(StockLockedTo to) {

        Long id = to.getId();
        StockDetailTo detailTo = to.getDetailTo();
        Long detailToId = detailTo.getId();
        // 解锁
        // 1、查询数据库关于这个订单的锁定库存信息
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailToId);
        if (byId != null) {
            // 查到有信息 证明库存锁定成功，需查询订单再确定 是否 需要解锁
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();// 根据订单号查询订单状态
            R status = orderFeignService.getOrderStatus(orderSn);
            if (status.get("code").toString().equals("0")) {
                String s = JSON.toJSONString(status.get("order"));
                OrderVo orderVo = JSON.parseObject(s, OrderVo.class);
                if (orderVo == null || orderVo.getStatus() == 4) {
                    // 订单不存在 / 订单已取消，解锁库存
                    if (byId.getLockStatus() == 1) {
                        unLockStock(detailTo.getSkuId(), detailTo.getWareId(), detailTo.getSkuNum(), detailToId);
                    }
                }
            } else {
                throw new RuntimeException("远程服务失败");
            }
        }
    }
    // 订单修改 主动触发 库存解锁
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 查最新库存解锁状态，防止重复操作
        WareOrderTaskEntity task = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long id = task.getId();
        // 按照工作单找到所有，没解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> list = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity e:list){
            unLockStock(e.getSkuId(),e.getWareId(),e.getSkuNum(),e.getId());
        }

    }

    // 订单解锁方法
    private void unLockStock(Long skuId, Long wareId, Integer num, Long teskDetailId) {
        // 库存解锁
        wareSkuDao.unlockStock(skuId, wareId, num);
        // 更新库存工作单的状态
        WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
        wareOrderTaskDetailEntity.setId(teskDetailId);
        wareOrderTaskDetailEntity.setLockStatus(2);
        wareOrderTaskDetailService.updateById(wareOrderTaskDetailEntity);
    }

    @Data
    class SkuHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}