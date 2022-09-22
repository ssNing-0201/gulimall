package com.atguigu.gulimall.search.gulimallsearch.controller;

import com.atguigu.gulimall.search.gulimallsearch.services.MallSearchService;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class SearchController {

    @Resource
    private MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){

        String queryString = request.getQueryString();
        param.setQueryString(queryString);
        SearchResult result = mallSearchService.search(param);
        List<SearchResult.NavVo> navs = result.getNavs();
        model.addAttribute("result",result);
        return "list";
    }

}
