package com.shopcart.service;

import com.shopcart.dto.CartResponse;
import com.shopcart.dto.CheckoutResponse;
import com.shopcart.entity.Cart;
import com.shopcart.entity.CartItem;
import com.shopcart.entity.Product;
import com.shopcart.exception.InsufficientStockException;
import com.shopcart.exception.ResourceNotFoundException;
import com.shopcart.exception.UnauthorizedException;
import com.shopcart.repository.CartItemRepository;
import com.shopcart.repository.CartRepository;
import com.shopcart.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
    }

    public Cart getCart(Long cartId, Long userId) {
        Cart cart = findCart(cartId);
        checkOwner(cart, userId);
        return cart;
    }

    @Transactional
    public CartResponse addItem(Long cartId, Long productId, int quantity, Long userId) {
        Cart cart = findCart(cartId);
        checkOwner(cart, userId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        cartItemRepository.save(item);

        cart.getItems().add(item);
        return CartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(Long cartId, Long itemId, Long userId) {
        Cart cart = findCart(cartId);
        checkOwner(cart, userId);

        boolean removed = cart.getItems().removeIf(i -> i.getId().equals(itemId));
        if (!removed) {
            throw new ResourceNotFoundException("Item not found in cart: " + itemId);
        }
        return CartMapper.toResponse(cart);
    }

    public CheckoutResponse checkout(Long cartId, Long userId) {
        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));

        BigDecimal total = BigDecimal.ZERO;
        int units = 0;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            if (product.getStock() < item.getQuantity()) {
                throw new InsufficientStockException("Not enough stock for " + product.getName());
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            units += item.getQuantity();
        }

        cart.getItems().clear();
        cartRepository.save(cart);

        return new CheckoutResponse(cart.getId(), total, units);
    }

    @Transactional(readOnly =  true)
    private Cart findCart(Long cartId) {
        return cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
    }
    //return cartRepository.findById(cartId)


    private void checkOwner(Cart cart, Long userId) {
        if (!cart.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You do not own this cart");
        }
    }
}
