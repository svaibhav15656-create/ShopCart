# Bug 3 - Cart Total Ignored Quantity

## Symptom
A shopper adds 2 Mice (25.50 each). The cart shows the right items, but the total is 25.50
instead of 51.00. The same wrong total appears in the "add item" response and in "view cart".

## Where the total is calculated
Not in the controller or the service. Both endpoints build their response with the same method,
`CartMapper.toResponse`, which is why both showed the same wrong value.

## Root cause
The total is built in two stages:

1. `map`: turn each cart line into one number
2. `reduce`: add those numbers together, starting from zero

The bug was in stage 1. Each line was mapped to the product's unit price only:

    .map(i -> i.getProduct().getPrice())

So a line of 2 Mice counted as 25.50 instead of 51.00. The quantity was never used.
The `reduce` was correct; it just added whatever numbers it was given.

## Fix
Map each line to its line total: unit price x quantity.

    .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
    .reduce(BigDecimal.ZERO, BigDecimal::add)

## Concepts behind the fix

**map and reduce**
- `map` transforms each element into something else (line -> line total).
- `reduce` folds many values into one (sum of the line totals).
- Debug each stage separately: what goes into the `map`, and what comes out?

**BigDecimal for money**
- `double` cannot represent most decimals exactly (0.1 + 0.2 = 0.30000000000000004).
  `BigDecimal` is exact, so it is used for money.
- `BigDecimal` is an object, so there is no `*` or `+`. Use `multiply` and `add`.
- `multiply` accepts only another `BigDecimal`. `getQuantity()` returns an `int`, so it is
  converted at the moment of multiplying with `BigDecimal.valueOf(...)`.
- Do not change the type of the `quantity` field itself. A quantity is a whole number, and the
  conversion belongs only in the money calculation.

## How to verify
Restart the app (the data resets), then:
- 2 x Mouse (25.50)             -> total 51.00
- add 1 x Laptop (999.99)       -> total 1050.99
- Run `.\mvnw.cmd test`: `total_isPriceTimesQuantitySummed` should pass.

## Interview answer in one line
"The total summed unit prices and ignored quantity, so I changed the mapping to price times
quantity, converting the int quantity to BigDecimal for the multiplication, and kept the reduce
that sums the line totals."

## Takeaways
- When a calculated value is wrong, find where it is computed and check which inputs it actually uses.
- Two endpoints showing the same wrong value usually means they share one calculation.
- Verify with numbers you can check by hand.
- A failing test that turns green after the fix is proof the fix works.