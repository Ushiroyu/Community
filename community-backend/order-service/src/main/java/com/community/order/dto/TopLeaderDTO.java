package com.community.order.dto;

import java.math.BigDecimal;

public record TopLeaderDTO(Long leaderId, Long orders, BigDecimal gmv) {
}
