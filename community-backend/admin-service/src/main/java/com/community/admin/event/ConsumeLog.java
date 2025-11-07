package com.community.admin.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("event_consume_log")
public class ConsumeLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String consumer;      // admin-service
    private String messageId;     // from message header
    private String eventType;
    private String status;        // DONE/FAIL
    private int retryCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
