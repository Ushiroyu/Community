package com.community.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.product.entity.Product;
import com.community.product.entity.Category;

import java.util.List;

public interface ProductService extends IService<Product> {
    List<Product> listByCategory(Long categoryId);

    List<Category> listAllCategories();

    void decreaseStock(Long productId, Integer quantity);
}
