package com.shopcart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long id, Long ownerId, List<CartItemDto> items, BigDecimal total) {
}
