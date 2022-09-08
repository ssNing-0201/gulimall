package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.CateLog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @Override
    public Map<String, List<CateLog2Vo>> getCatelogJson() {

        /**
         * 优化 将反复数据库查询，抽取出，变为一次查询，遍历需要的数据。
         * 1、将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);


        // 1、查出所有分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList,0l);
        // 2、封装数据
        Map<String, List<CateLog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString()
                , v -> {
                    // 每一个的一级分类，查出这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList,v.getCatId());
                    // 封装上面的结果
                    List<CateLog2Vo> cateLog2Vos = null;
                    if (categoryEntities != null) {
                        cateLog2Vos = categoryEntities.stream().map(l2 -> {
                            CateLog2Vo cateLog2Vo = new CateLog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                            // 给二级分类查找三级分类
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList,l2.getCatId());
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