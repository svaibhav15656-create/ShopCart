package com.shopcart.service;

import com.shopcart.dto.CartItemDto;
import com.shopcart.dto.CartResponse;
import com.shopcart.entity.Cart;

import java.math.BigDecimal;
import java.util.List;

public final class CartMapper {

    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(i -> new CartItemDto(i.getId(), i.getProduct().getId(), i.getProduct().getName(),
                        i.getProduct().getPrice(), i.getQuantity()))
                .toList();

        BigDecimal total = cart.getItems().stream()
                .map(i -> i.getProduct().getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(cart.getId(), cart.getOwner().getId(), items, total);
    }
}
