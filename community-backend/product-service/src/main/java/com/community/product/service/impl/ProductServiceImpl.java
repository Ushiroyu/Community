package com.community.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.product.entity.Category;
import com.community.product.entity.Product;
import com.community.product.mapper.CategoryMapper;
import com.community.product.mapper.ProductMapper;
import com.community.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    private final CategoryMapper categoryMapper;

    @Override
    @Cacheable(cacheNames = "prod:list:cat", key = "#categoryId")
    public List<Product> listByCategory(Long categoryId) {
        return this.list(new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, categoryId));
    }

    @Override
    public List<Category> listAllCategories() {
        return categoryMapper.selectList(null);
    }

    @Override
    @Transactional
    public void decreaseStock(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) return;
        // 乐观做法：直接原子扣减，确保库存 >= quantity
        UpdateWrapper<Product> uw = new UpdateWrapper<>();
        uw.setSql("stock = stock - " + quantity)
                .eq("id", productId)
                .ge("stock", quantity);
        int rows = this.baseMapper.update(null, uw);
        // rows==0 表示库存不足或商品不存在，可按需抛业务异常/记录日志
    }
}
