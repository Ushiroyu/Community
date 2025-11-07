package com.community.product.controller;

import com.community.common.util.ApiResponse;
import com.community.product.entity.Product;
import com.community.product.entity.ProductSku;
import com.community.product.mapper.ProductMapper;
import com.community.product.mapper.ProductSkuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class SkuQueryController {

    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;

    @GetMapping("/skus/{skuId}")
    public ApiResponse sku(@PathVariable Long skuId) {
        ProductSku s = skuMapper.selectById(skuId);
        return s == null ? ApiResponse.error("NOT_FOUND") : ApiResponse.ok().data("sku", s);
    }

    @GetMapping("/products/{id}/price")
    public ApiResponse price(@PathVariable Long id) {
        Product p = productMapper.selectById(id);
        return p == null ? ApiResponse.error("NOT_FOUND") : ApiResponse.ok().data("price", p.getPrice());
    }
}
