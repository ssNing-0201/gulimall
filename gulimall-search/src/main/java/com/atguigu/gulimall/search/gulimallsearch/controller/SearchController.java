package com.atguigu.gulimall.search.gulimallsearch.controller;

import com.atguigu.gulimall.search.gulimallsearch.services.MallSearchService;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;

@Controller
public class SearchController {

    @Resource
    private MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model){

        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);
        return "list";
    }

}
