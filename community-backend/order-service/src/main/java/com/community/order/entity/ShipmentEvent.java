package com.community.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("shipment_event")
public class ShipmentEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String status;    // SHIPPED / IN_TRANSIT / DELIVERED ...
    private String location;  // 位置(可选)
    private String remark;    // 备注
    private Date eventTime;   // 发生时间
}
