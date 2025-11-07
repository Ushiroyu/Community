package com.community.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    List<Product> searchFulltext(@Param("q") String q,
                                 @Param("categoryId") Long categoryId,
                                 @Param("size") int size,
                                 @Param("offset") int offset);
}
