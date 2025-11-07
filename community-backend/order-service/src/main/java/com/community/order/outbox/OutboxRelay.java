package com.community.order.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.common.util.SimpleMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventMapper mapper;
    private final RabbitTemplate rabbit;

    public static final String EVENT_EX = "event.bus.ex";

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    public void publish() {
        List<OutboxEvent> list = mapper.selectList(new LambdaQueryWrapper<OutboxEvent>()
                .eq(OutboxEvent::getStatus, "NEW").last("limit 100"));
        for (OutboxEvent e : list) {
            try {
                rabbit.convertAndSend(EVENT_EX, e.getEventType(), e.getPayload(),
                        m -> {
                            m.getMessageProperties().setMessageId(String.valueOf(e.getId()));
                            return m;
                        });
                e.setStatus("SENT");
                e.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(e);
                SimpleMetrics.inc("outbox.sent");
            } catch (Exception ex) {
                log.warn("outbox publish fail id={}", e.getId(), ex);
                e.setStatus("FAIL");
                e.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(e);
                SimpleMetrics.inc("outbox.fail");
            }
        }
    }
}
