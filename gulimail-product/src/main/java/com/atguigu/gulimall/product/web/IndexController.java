package com.atguigu.gulimall.product.web;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.CateLog2Vo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Resource
    private CategoryService categoryService;

    @GetMapping({"/","index.html"})
    public String index(Model model){

        //TODO 查出所有一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categorys",categoryEntities);
        return "index";
    }

    // index/catalog.json
    @GetMapping("/index/catalog.json")
    @ResponseBody
    public Map<String, List<CateLog2Vo>> getCatelogJson() throws InterruptedException {

        Map<String, List<CateLog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }

}
