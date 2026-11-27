BUG 1: VIEW CART FAILS WITH "SOMETHING WENT WRONG"
=================================================

SYMPTOM
-------
GET /api/carts/{cartId} always returns a 500 error with the message
"Something went wrong". The response right after adding an item works fine.

ROOT CAUSE
----------
LazyInitializationException. The cart's items list is lazy loaded, and it
was read after the Hibernate session had already closed.

File: src/main/java/com/shopcart/service/CartService.java
Method: findCart (used by getCart)

WHAT HAPPENED STEP BY STEP
--------------------------
1. CartController.get() calls service.getCart(cartId, userId).
2. getCart calls cartRepository.findById(cartId).
   - getCart has no @Transactional, so the repository call opens its own
     short transaction and closes it as soon as it returns.
   - Only the cart row is loaded. The items list (@OneToMany) is lazy, so
     it is just an empty placeholder (a proxy).
3. getCart checks ownership using cart.getOwner(). This works because
   owner is a @ManyToOne, which is EAGER by default and was already loaded.
4. getCart returns the Cart to the controller.
5. The controller calls CartMapper.toResponse(cart), which reads
   cart.getItems().
6. Hibernate tries to load the items now, but there is no open session.
   It throws LazyInitializationException.
7. GlobalExceptionHandler has no specific handler for it, so the catch-all
   Exception handler returns 500 "Something went wrong".

Why addItem works: it is @Transactional and builds the response inside the
service method, so the session is still open when the items are read.
Also, spring.jpa.open-in-view=false means Spring does not keep the session
open for the rest of the web request.

CONCEPTS USED
-------------
1. Lazy vs eager loading
   - EAGER: related data is loaded together with the parent.
     Default for @ManyToOne and @OneToOne.
   - LAZY: related data is loaded only when first accessed.
     Default for @OneToMany and @ManyToMany.
   - A lazy field holds a proxy until it is accessed.

2. Hibernate session and transaction
   - Lazy data can only be loaded while the session is open.
   - A session lasts as long as the transaction around it.
   - @Transactional keeps the session open until the method returns.
   - Without @Transactional, each repository call opens and closes its
     own session.

3. LazyInitializationException
   - Thrown when a lazy field is accessed after the session is closed.
   - The cause is almost always: data was not loaded before the session
     ended.

4. Open Session In View (spring.jpa.open-in-view)
   - When true, the session stays open for the whole web request.
   - When false, the session closes with the transaction.
   - Setting it to true hides the problem and holds database connections
     open during JSON rendering, so it is not a good fix.

5. JOIN FETCH
   - "left join fetch c.items" loads the parent and its children in a
     single SQL query.
   - The items are real objects in memory, so reading them later needs no
     session.

THE FIX
-------
In CartService.findCart, use the repository method that fetches the items
together with the cart.

Before:
    return cartRepository.findById(cartId)
            .orElseThrow(...);

After:
    return cartRepository.findByIdWithItems(cartId)
            .orElseThrow(...);

CartRepository already defines it:
    @Query("select c from Cart c left join fetch c.items where c.id = :id")
    Optional<Cart> findByIdWithItems(@Param("id") Long id);

Why this works: the items are loaded in the same query as the cart, so the
mapper in the controller only reads ordinary in-memory lists.

ALTERNATIVE FIX
---------------
Make getCart @Transactional(readOnly = true), build the CartResponse inside
the service, and return it. The controller then returns it directly.
This needs changes in two files.

Note: adding @Transactional to getCart by itself does NOT fix the bug. The
transaction ends when the method returns, and the items would still be
unloaded. The items must be read while the transaction is open, or loaded
before it ends.

KEY TAKEAWAYS
-------------
- @Transactional does not load data. It only keeps the session open.
- Fix lazy errors by loading data before the session closes (JOIN FETCH,
  EntityGraph) or by reading it inside the transaction.
- A 500 error right after a successful database call is a hint that
  something is accessed later than it should be.
- The console log shows the real exception even when the API response
  hides it.

HOW TO VERIFY
-------------
1. Start the app.
2. POST /api/carts/1/items with {"productId":1,"quantity":2}  -> 200
3. GET /api/carts/1 -> should be 200 with the items.
   Before the fix this returned 500.
4. Check the SQL log: the select now contains a left outer join on
   cart_items.