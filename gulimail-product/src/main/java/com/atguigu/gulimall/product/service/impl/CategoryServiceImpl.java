package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.CateLog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;


import javax.annotation.Resource;

@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redisson;


    // 可以用baseMapper 范型指定了的
//    @Resource
//    private CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        // 2、组装成父子树形结构
        // 1、找到所有一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity -> {
            return categoryEntity.getParentCid() == 0;
        })).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // TODO 1、检查当前删除菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);
        log.info("impl内容{}", Arrays.asList(parentPath.toArray(new Long[0])));
        return parentPath.toArray(new Long[0]);
    }

    /**
     * 级联跟新所有关联数据
     *
     * @param category
     */
    @Override
    public void updateCasecade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }


    // TODO 产生堆外内存溢出：OutofdirectMemoryError
    // 溢出原因
    @Override
    @Cacheable(value = "catelog")  // 表示当前方法需要缓存，如果缓存中没有会调用方法，最后将方法的结果放入缓存
    public Map<String, List<CateLog2Vo>> getCatelogJson() {
        // 缓存放入JSON字符串，用时还需逆转为能用的对象「序列化与反序列化」
        // 优化 引入缓存，将三级分类放入缓存中。

        /**
         * 1、空结果缓存，解决缓存穿透
         * 2、设置过期时间（添加随机值）：解决缓存雪崩
         * 3、加锁，解决缓存击穿
         */
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if ("null".equals(catalogJSON) || "".equals(catalogJSON)) {
            // 缓存中没有，查询数据库
            Map<String, List<CateLog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedissonLock();
            // 将查到的数据再放入缓存，将对象转为json放在缓存中。
            // 将以下语句移动到数据库查询方法中，查完数据库直接保存在缓存中。
            /*String s = JSON.toJSONString(catelogJsonFromDb);
            stringRedisTemplate.opsForValue().set("catalogJSON", s,1, TimeUnit.HOURS);*/
            return catelogJsonFromDb;
        }
        // 转为指定类型的对象
        Map<String, List<CateLog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<CateLog2Vo>>>() {
        });

        return result;

    }

    // 从数据库查询并分装分类数据(redisson锁)
    public Map<String, List<CateLog2Vo>> getCatelogJsonFromDbWithRedissonLock() {
        /**
         * 优化 使用分布式锁Redisson
         * 1、将数据库的多次查询变为一次
         */
        // 获取分布式锁(锁的名字是去redis占坑的，锁的粒度越细，越快)
        RLock lock = redisson.getLock("catelogJSON-lock");
        lock.lock(); // 上锁
        Map<String, List<CateLog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock(); // 解锁
        }
        return dataFromDb;

    }

    // 从数据库查询并分装分类数据(本地锁)
    public Map<String, List<CateLog2Vo>> getCatelogJsonFromDbWithLocalLock() {
        /**
         * 优化 将反复数据库查询，抽取出，变为一次查询，遍历需要的数据。
         * 1、将数据库的多次查询变为一次
         */
        // 加锁（本地锁）
        synchronized (this) {
            List<CategoryEntity> selectList = baseMapper.selectList(null);

            // 1、查出所有分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0l);
            // 2、封装数据
            Map<String, List<CateLog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
                    , v -> {
                        // 每一个的一级分类，查出这个一级分类的二级分类
                        List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                        // 封装上面的结果
                        List<CateLog2Vo> cateLog2Vos = null;
                        if (categoryEntities != null) {
                            cateLog2Vos = categoryEntities.stream().map(l2 -> {
                                CateLog2Vo cateLog2Vo = new CateLog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                                // 给二级分类查找三级分类
                                List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                                if (level3Catelog != null) {
                                    List<CateLog2Vo.CataLog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                        // 分装成指定格式
                                        CateLog2Vo.CataLog3Vo cataLog3Vo = new CateLog2Vo.CataLog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                        return cataLog3Vo;
                                    }).collect(Collectors.toList());
                                    cateLog2Vo.setCatalog3List(collect);
                                }
                                return cateLog2Vo;
                            }).collect(Collectors.toList());
                        }
                        return cateLog2Vos;
                    }));
            // 将数据库查询完的结果保存在缓存中。
            String s = JSON.toJSONString(parent_cid);
            stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.HOURS);
            return parent_cid;
        }

    }

    // 从数据库查询并分装分类数据(redis锁)
    public Map<String, List<CateLog2Vo>> getCatelogJsonFromDbWithRedisLock() throws InterruptedException {

        // 1、占分布式锁,去redis占位,同时设置过期时间，防止突然出现的问题导致死锁。
        String id = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", id, 30, TimeUnit.SECONDS);
        if (lock) {
            // 占位成功,执行业务
            Map<String, List<CateLog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            } finally {
                String lockValue = stringRedisTemplate.opsForValue().get("lock");
            /*if (id.equals(lockValue)){
                stringRedisTemplate.delete("lock"); // 删除锁
            }*/
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(), Arrays.asList("lock"), id);
            }
            return dataFromDb;
        } else {
            // 占位失败，重试
            // 休眠100ms重试
            Thread.sleep(100);
            return getCatelogJsonFromDbWithRedisLock(); // 自旋方式重试
        }
    }

    @Nullable
    private Map<String, List<CateLog2Vo>> getDataFromDb() {
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 1、查出所有分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0l);
        // 2、封装数据
        Map<String, List<CateLog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
                , v -> {
                    // 每一个的一级分类，查出这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                    // 封装上面的结果
                    List<CateLog2Vo> cateLog2Vos = null;
                    if (categoryEntities != null) {
                        cateLog2Vos = categoryEntities.stream().map(l2 -> {
                            CateLog2Vo cateLog2Vo = new CateLog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 给二级分类查找三级分类
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                            if (level3Catelog != null) {
                                List<CateLog2Vo.CataLog3Vo> collect = level3Catelog.stream().map(l3 -> {
                                    // 分装成指定格式
                                    CateLog2Vo.CataLog3Vo cataLog3Vo = new CateLog2Vo.CataLog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                    return cataLog3Vo;
                                }).collect(Collectors.toList());
                                cateLog2Vo.setCatalog3List(collect);
                            }
                            return cateLog2Vo;
                        }).collect(Collectors.toList());
                    }
                    return cateLog2Vos;
                }));
        // 将数据库查询完的结果保存在缓存中。
        String s = JSON.toJSONString(parent_cid);
        stringRedisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.HOURS);
        return parent_cid;
    }


    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parentCid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parentCid).collect(Collectors.toList());
        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        // 收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

    // 递归查找所有菜单子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid().equals(root.getCatId());
        }).map(categoryEntity -> {
            // 1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            // 2、菜单排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

}