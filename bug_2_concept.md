# ShopCart - Concepts Behind the Bugs

A learning log for the debugging challenge. Each section explains the concept
behind a bug, not just the fix. Fill in the remaining rows as you solve more bugs.

## 1. The data model: why a Cart holds CartItems, not Products

| Entity | What it is | Example |
|--------|------------|---------|
| Product | Something the store sells | Mouse, 25.50, stock 100 |
| Cart | One shopper's basket | alice's cart |
| CartItem | One line: a product + a quantity inside a cart | 2 x Mouse |

A Cart never holds Products directly. It holds CartItems, and each CartItem points to a Product.
To ask "is this product in the cart?" you must look through the lines:

    Cart -> getItems() -> CartItem -> getProduct() -> Product -> getId()

## 2. How a request travels (trace this when debugging)

    Controller -> Service -> Repository -> Database
                     |
        GlobalExceptionHandler (exception -> HTTP status + JSON)

Debugging method: reproduce, read the console logs and SQL, trace the layers,
fix the root cause, retest everything.

## 3. Bug 1 - Viewing the cart returned 500

**Concept: lazy loading and transaction boundaries.**
- `Cart.items` is a `@OneToMany`, which Hibernate loads lazily (only when first touched).
- Lazy loading only works while the Hibernate session is open. Here the session lives
  inside a transaction (`spring.jpa.open-in-view=false`).
- The "add" path worked because it ran inside `@Transactional`, so touching the items was safe.
- The "view" path returned the entity out of the service, then the controller touched
  `getItems()` after the transaction had ended -> `LazyInitializationException` -> caught by
  the catch-all handler -> 500 "Something went wrong".

**Takeaways**
- A 500 with a generic message hides the real exception. Read the server console.
- When two code paths do the same thing and one works, diff what is different (here: the transaction).
- Map entities to DTOs inside the transaction, or fetch what you need before it ends.

## 4. Bug 2 - Same product added twice made two lines

**Concept: find-or-update (an "upsert" pattern).**
- "Add to cart" must ask first: does a line for this product already exist?
  - Yes -> raise its quantity.
  - No -> create a new line.
- Inside a `@Transactional` method, changing a loaded entity (`line.setQuantity(...)`) is enough.
  Hibernate notices the change (dirty checking) and writes an `UPDATE` when the transaction commits.
  No explicit `save` is needed.
- Compare `Long` ids with `.equals`, not `==`.
- Always check that the database matches the response (`select * from cart_items;`).

## 5. General patterns to watch for in a debugging round

1. API says success but the data is wrong or missing -> check the SQL log for the expected insert/update/delete.
2. A sibling method works and this one does not -> diff the two.
3. Silent failures (no error) -> ask what could fail quietly.
4. Wrong status codes -> trace the exception to its handler.
5. Annotations are labels: they only work with the right trigger and context.

## 6. Bug log (fill in as you go)

| # | Symptom | Root cause (file / why) | Fix |
|---|---------|-------------------------|-----|
| 1 | View cart returns 500 | Lazy collection read after the transaction ended | (your fix) |
| 2 | Duplicate lines for one product | addItem never checked for an existing line | find-or-update |
| 3 | Total too low with quantity > 1 | (fill in after you fix it) | |
| 4 | | | |
| 5 | | | |
| 6 | | | |
| 7 | | | |
| 8 | | | |