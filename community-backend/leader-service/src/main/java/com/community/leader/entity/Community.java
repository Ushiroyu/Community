package com.community.leader.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("community")
public class Community {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String address;
    // 数据库表中没有 leader_id 列，运行时由业务查询补全
    @TableField(exist = false)
    private Long leaderId;
    // 运行时补充的团长用户ID（订单按用户ID筛选）
    @TableField(exist = false)
    private Long leaderUserId;

    // 新增：地理位置（可为空）
    private Double lat;
    private Double lng;
}
