package com.community.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.community.common.util.ApiResponse;
import com.community.product.entity.Product;
import com.community.product.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class SearchController {

    private final ProductMapper productMapper;

    @GetMapping("/search")
    public ApiResponse search(@RequestParam String q,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size) {
        int offset = Math.max(0, (page - 1) * size);
        List<Product> items;
        try {
            items = productMapper.searchFulltext(q, categoryId, size, offset);
            if (items == null || items.isEmpty()) throw new RuntimeException("no ft result");
        } catch (Exception e) {
            QueryWrapper<Product> qw = new QueryWrapper<Product>().like("name", q);
            if (categoryId != null) qw.eq("category_id", categoryId);
            qw.last("limit " + size + " offset " + offset);
            items = productMapper.selectList(qw);
        }
        return ApiResponse.ok().data("items", items);
    }
}
