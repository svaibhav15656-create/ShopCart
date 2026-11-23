# ShopCart - Cart & Checkout (Backend Debugging Challenge #2)

## 1. About the project

ShopCart is the backend of a small online store. Users browse products, put them in a
shopping cart, and check out. This repository contains the **Cart** feature:

| Operation   | What it should do |
|-------------|-------------------|
| View cart   | Show every item in the cart and the cart total |
| Add item    | Put a product in the cart with a quantity |
| Remove item | Take an item out of the cart |
| Checkout    | Buy everything in the cart: reduce product stock, empty the cart, report the amount charged |

**Tech stack:** Java 17, Spring Boot 3.2, Spring Data JPA / Hibernate, Bean Validation, H2 in-memory database, Maven.

**Architecture (follow the request through every layer when debugging):**

    CartController  ->  CartService (+ CartMapper)  ->  Repositories  ->  H2 database
                                    |
                      GlobalExceptionHandler (turns exceptions into HTTP responses)

**Entities:** `User`, `Product` (name, price, stock), `Cart` (belongs to a user), `CartItem` (a product + quantity inside a cart).

## 2. The situation

The Cart feature has been released but is **not working reliably**. Customer support has
collected the complaints below. Your job is to find out why, and fix it.

> This is a debugging assessment, not a new development project.
> Fix the existing code. Do not rewrite the application.

## 3. Problems users are facing

These are the symptoms reported by users and testers. The causes are NOT listed;
finding them is the exercise.

**Viewing the cart**
- "I can't open my cart. Every time I view it the app says *Something went wrong*."
  (Tip: the response you get right after adding an item still shows the cart contents, so you can use that while investigating.)

**Adding items**
- "I added the same mouse twice and now it shows up as two separate lines instead of quantity 2."
- "The cart total looks too low. I put 2 laptops in my cart and the total only shows the price of one."

**Removing items**
- "I removed an item and the app said it worked, but the item came back later when I reopened my cart."

**Checkout**
- "My checkout failed because one product was out of stock, but the stock of the other products in my cart still went down."
- "After a successful checkout my cart still had the items in it."
- "I could check out somebody else's cart by using their cart ID."

**Error messages**
- "When I ask for a cart / product / item that does not exist, the API says *bad request* instead of *not found*."
- "When there isn't enough stock, the app just says *Something went wrong* (a server error) instead of clearly saying there is a stock conflict."

## 4. Your task

You must make all of these work correctly:

1. **View cart**: returns all items and a correct total (price x quantity, summed).
2. **Add item**: the item appears in the cart; adding a product already in the cart increases its quantity instead of creating a second line. Quantity must be at least 1.
3. **Remove item**: the item is removed from the cart permanently.
4. **Checkout**: stock goes down by the purchased quantities, the cart is emptied, the response shows the amount charged. If any product lacks stock, the whole checkout fails and **no** stock changes.
5. **Validation and errors**: unknown cart/product/item -> 404; someone else's cart -> 403 (for every operation); not enough stock -> 409 Conflict.

## 5. Expected behaviour

| Operation | Expected result |
|-----------|-----------------|
| View cart | 200 with items and correct total |
| Add item | 200, item in cart; same product again = quantity increases |
| Add item, quantity 0 or negative | 400 |
| Remove item | 200, item gone for good |
| Checkout | 200, stock reduced, cart empty |
| Checkout, not enough stock | 409, nothing changed |
| Unknown cart / product / item | 404 |
| Someone else's cart (any operation) | 403 |

## 6. Rules

- Treat this as debugging. Understand the code first.
- Trace each request: Controller -> Service -> Repository -> Database.
- Identify all bugs before changing code, then fix the root causes (not just the symptoms).
- Keep the existing architecture. Avoid unnecessary changes to working code.
- Retest everything after each fix, not only the part you changed.

## 7. Run it

Open PowerShell in THIS folder (the one containing `pom.xml`):

    .\mvnw.cmd spring-boot:run

App runs on http://localhost:8080. The database resets to the seed data on every restart.
Needs Java 17+ and internet access the first time (to download Maven and dependencies).

## 8. Seed data

| Users | |
|-------|--|
| 1 | alice |
| 2 | bob |

| Products | Price | Stock |
|----------|-------|-------|
| 1 Laptop | 999.99 | 5 |
| 2 Mouse | 25.50 | 100 |
| 3 Keyboard | 75.00 | 2 |
| 4 Headphones | 59.90 | 1 |

Carts: 1 = alice's (empty), 2 = bob's (empty).
Every request needs the header `X-User-Id: <userId>` (stand-in for real authentication).

## 9. API reference

| Operation | Request |
|-----------|---------|
| View cart | `GET /api/carts/{cartId}` |
| Add item | `POST /api/carts/{cartId}/items` body `{"productId":1,"quantity":2}` |
| Remove item | `DELETE /api/carts/{cartId}/items/{itemId}` |
| Checkout | `POST /api/carts/{cartId}/checkout` |

## 10. How to test (Windows PowerShell)

PowerShell breaks quotes in `-d "..."`, so write the JSON to a file first and use `curl.exe`
(plain `curl` is a different command in PowerShell):

    '{"productId":1,"quantity":2}' | Set-Content -Encoding ascii add.json
    curl.exe -i -X POST http://localhost:8080/api/carts/1/items -H "X-User-Id: 1" -H "Content-Type: application/json" -d "@add.json"
    curl.exe -i http://localhost:8080/api/carts/1 -H "X-User-Id: 1"
    curl.exe -i -X DELETE http://localhost:8080/api/carts/1/items/1 -H "X-User-Id: 1"
    curl.exe -i -X POST http://localhost:8080/api/carts/1/checkout -H "X-User-Id: 1"

Make a body file per scenario, for example:

    '{"productId":999,"quantity":1}' | Set-Content -Encoding ascii bad-product.json
    '{"productId":1,"quantity":0}'  | Set-Content -Encoding ascii zero-qty.json

**Inspect the database:** open http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:shopcart`, user `sa`, empty password) and run:

    select * from products;
    select * from cart_items;

**Read the SQL log** in the console where the app runs (`show-sql` is on). After a write,
check that the `insert` / `update` / `delete` you expect really appears.

## 11. Tips for testing

- Build a table: one row per operation and per failure case, with the status code you get vs. the one expected.
- Test the unhappy paths: unknown IDs, another user's cart, quantity 0, not enough stock.
- Test multi-step flows: add several items, then remove, then view, then check out, then view again.
- After "success" responses, check the database. A 200 does not prove the data was saved.
- Restart the app between experiments to reset the data.

## 12. When you finish

For every bug send:
1. The **symptom** you observed
2. The **root cause** (file / method and why it happens)
3. Your **fix**

and I will review it like a backend debugging interviewer.
