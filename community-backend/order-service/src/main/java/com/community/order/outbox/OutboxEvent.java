package com.community.order.outbox;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("outbox_event")
public class OutboxEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String eventType;      // e.g. ORDER_CREATED
    private String aggregateType;  // ORDER
    private Long aggregateId;      // orderId
    private String payload;        // JSON
    private String status;         // NEW,SENT,FAIL
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
