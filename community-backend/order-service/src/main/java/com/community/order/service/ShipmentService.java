package com.community.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.order.entity.ShipmentEvent;

import java.util.List;

public interface ShipmentService extends IService<ShipmentEvent> {
    void addEvent(ShipmentEvent event);

    List<ShipmentEvent> listByOrderId(Long orderId);
}
