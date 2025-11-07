package com.community.order.dto;

import java.math.BigDecimal;

public record CartItemDTO(Long productId, Integer quantity, BigDecimal price) {
}
