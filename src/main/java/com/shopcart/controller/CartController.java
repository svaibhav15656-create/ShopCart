package com.shopcart.controller;

import com.shopcart.dto.AddItemRequest;
import com.shopcart.dto.CartResponse;
import com.shopcart.dto.CheckoutResponse;
import com.shopcart.service.CartMapper;
import com.shopcart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping("/{cartId}")
    public ResponseEntity<CartResponse> get(@RequestHeader("X-User-Id") Long userId,
                                            @PathVariable Long cartId) {
        return ResponseEntity.ok(CartMapper.toResponse(service.getCart(cartId, userId)));
    }

    @PostMapping("/{cartId}/items")
    public ResponseEntity<CartResponse> addItem(@RequestHeader("X-User-Id") Long userId,
                                                @PathVariable Long cartId,
                                                @Valid @RequestBody AddItemRequest request) {
        return ResponseEntity.ok(service.addItem(cartId, request.productId(), request.quantity(), userId));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(@RequestHeader("X-User-Id") Long userId,
                                                   @PathVariable Long cartId,
                                                   @PathVariable Long itemId) {
        return ResponseEntity.ok(service.removeItem(cartId, itemId, userId));
    }

    @PostMapping("/{cartId}/checkout")
    public ResponseEntity<CheckoutResponse> checkout(@RequestHeader("X-User-Id") Long userId,
                                                     @PathVariable Long cartId) {
        return ResponseEntity.ok(service.checkout(cartId, userId));
    }
}
