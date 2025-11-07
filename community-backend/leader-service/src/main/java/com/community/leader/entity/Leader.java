package com.community.leader.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("leader")
public class Leader {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long communityId;

    private String realName;   // 新增：申请人姓名
    private String phone;      // 新增：申请人电话

    private String status;     // PENDING / APPROVED / REJECTED

    @TableField(value = "create_time")
    private Date createTime;   // 新增：申请时间

    @TableField(value = "update_time")
    private Date updateTime;   // 新增：更新时间
}
