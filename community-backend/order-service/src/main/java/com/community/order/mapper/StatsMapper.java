package com.community.order.mapper;

import com.community.order.dto.TopLeaderDTO;
import com.community.order.dto.TopProductDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatsMapper {

    @Select("""
            
              SELECT IFNULL(SUM(total_amount),0) FROM `order`
              WHERE create_time BETWEEN #{fromTs} AND #{toTs}
            """)
    java.math.BigDecimal gmv(@Param("fromTs") java.util.Date from, @Param("toTs") java.util.Date to);

    @Select("""
            
              SELECT COUNT(*) FROM `order` WHERE create_time BETWEEN #{from} AND #{to}
            """)
    long total(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    @Select("""
            
              SELECT COUNT(*) FROM `order` WHERE status='PAID' AND create_time BETWEEN #{from} AND #{to}
            """)
    long paid(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    @Select("""
            
              SELECT COUNT(*) FROM `order` WHERE status='SHIPPED' AND create_time BETWEEN #{from} AND #{to}
            """)
    long shipped(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    @Select("""
            
              SELECT COUNT(*) FROM `order` WHERE status='DELIVERED' AND create_time BETWEEN #{from} AND #{to}
            """)
    long delivered(@Param("from") java.util.Date from, @Param("to") java.util.Date to);

    @Select("""
            
              SELECT oi.product_id AS productId, SUM(oi.quantity) AS quantity
            
              FROM order_item oi JOIN `order` o ON oi.order_id = o.id
            
              WHERE o.create_time BETWEEN #{from} AND #{to}
            
              GROUP BY oi.product_id
            
              ORDER BY quantity DESC
            
              LIMIT #{limit}
            
            """)
    List<TopProductDTO> topProducts(@Param("from") java.util.Date from, @Param("to") java.util.Date to, @Param("limit") int limit);

    @Select("""
            
              SELECT o.leader_id AS leaderId, COUNT(*) AS orders, IFNULL(SUM(o.total_amount),0) AS gmv
            
              FROM `order` o
            
              WHERE o.create_time BETWEEN #{from} AND #{to}
            
              GROUP BY o.leader_id
            
              ORDER BY gmv DESC
            
              LIMIT #{limit}
            
            """)
    List<TopLeaderDTO> topLeaders(@Param("from") java.util.Date from, @Param("to") java.util.Date to, @Param("limit") int limit);
}
