package com.community.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    List<Product> searchFulltext(@Param("q") String q,
                                 @Param("categoryId") Long categoryId,
                                 @Param("size") int size,
                                 @Param("offset") int offset);

    /**
     * 原子扣减库存：仅当剩余库存充足时才扣减
     */
    @Update("UPDATE product SET stock = stock - #{qty} WHERE id = #{productId} AND stock >= #{qty}")
    int deductStock(@Param("productId") Long productId, @Param("qty") int qty);
}
