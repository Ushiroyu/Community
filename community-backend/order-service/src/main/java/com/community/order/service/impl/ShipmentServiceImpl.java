package com.community.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.order.entity.ShipmentEvent;
import com.community.order.mapper.ShipmentEventMapper;
import com.community.order.service.ShipmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShipmentServiceImpl
        extends ServiceImpl<ShipmentEventMapper, ShipmentEvent>
        implements ShipmentService {

    @Override
    public void addEvent(ShipmentEvent event) {
        this.save(event);
    }

    @Override
    public List<ShipmentEvent> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<ShipmentEvent>()
                .eq(ShipmentEvent::getOrderId, orderId)
                .orderByAsc(ShipmentEvent::getEventTime));
    }
}
