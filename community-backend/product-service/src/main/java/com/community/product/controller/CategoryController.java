package com.community.product.controller;

import com.community.common.util.ApiResponse;
import com.community.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.community.product.entity.Category;
import com.community.product.mapper.CategoryMapper;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final ProductService productService;
    private final CategoryMapper categoryMapper;

    @GetMapping
    public ApiResponse list() {
        return ApiResponse.ok().data("categories", productService.listAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse add(@RequestBody Category c) {
        categoryMapper.insert(c);
        return ApiResponse.ok().data("id", c.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse update(@PathVariable Long id, @RequestBody Category c) {
        c.setId(id);
        categoryMapper.updateById(c);
        return ApiResponse.ok("分类已更新");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return ApiResponse.ok("分类已删除");
    }
}
