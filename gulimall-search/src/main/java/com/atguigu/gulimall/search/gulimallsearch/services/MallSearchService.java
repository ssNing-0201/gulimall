package com.atguigu.gulimall.search.gulimallsearch.services;

import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;

public interface MallSearchService {

    SearchResult search(SearchParam param);
}
