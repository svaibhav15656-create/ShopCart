package com.shopcart.dto;

import java.math.BigDecimal;

public record CartItemDto(Long itemId, Long productId, String productName, BigDecimal price, int quantity) {
}
