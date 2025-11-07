package com.community.leader.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
    private Long leaderId;

    // 新增：地理位置（可为空）
    private Double lat;
    private Double lng;
}
