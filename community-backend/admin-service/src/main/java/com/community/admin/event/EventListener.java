package com.community.admin.event;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.util.SimpleMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.community.admin.config.EventConsumerConfig.ADMIN_Q;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final ConsumeLogMapper logMapper;

    @RabbitListener(queues = ADMIN_Q)
    public void onEvent(Message message) {
        String msgId = message.getMessageProperties().getMessageId();
        String eventType = message.getMessageProperties().getReceivedRoutingKey();
        if (msgId == null || msgId.isBlank()) {
            msgId = java.util.UUID.randomUUID().toString();
        }
        // 幂等去重
        ConsumeLog exist = logMapper.selectOne(new LambdaQueryWrapper<ConsumeLog>()
                .eq(ConsumeLog::getMessageId, msgId));
        if (exist != null) {
            log.info("dup message ignored id={}", msgId);
            return;
        }
        ConsumeLog rec = new ConsumeLog();
        rec.setConsumer("admin-service");
        rec.setMessageId(msgId);
        rec.setEventType(eventType);
        rec.setStatus("DONE");
        rec.setRetryCount(0);
        rec.setCreatedAt(java.time.LocalDateTime.now());
        rec.setUpdatedAt(rec.getCreatedAt());
        logMapper.insert(rec);
        SimpleMetrics.inc("event.consumed");
        log.info("consume event {} msgId={}", eventType, msgId);
        // TODO: 在此更新后台报表缓存/通知等
    }
}
