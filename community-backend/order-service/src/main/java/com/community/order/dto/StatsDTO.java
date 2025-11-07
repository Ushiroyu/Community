package com.community.order.dto;

import java.math.BigDecimal;

public record StatsDTO(long totalOrders, long paidOrders, long shippedOrders, long deliveredOrders, BigDecimal gmv) {
}
