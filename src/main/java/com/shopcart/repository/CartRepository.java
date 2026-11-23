package com.shopcart.repository;

import com.shopcart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("select c from Cart c left join fetch c.items where c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") Long id);
}
