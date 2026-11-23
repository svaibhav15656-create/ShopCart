package com.shopcart.dto;

import java.math.BigDecimal;

public record CheckoutResponse(Long cartId, BigDecimal totalCharged, int unitsPurchased) {
}
