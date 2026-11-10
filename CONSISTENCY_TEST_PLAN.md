# Inventory consistency test plan

Run these tests on a copy of the `Inventory` database first. Do not use production records for test sales, voids, or stock adjustments.

## 1. Purchase receipt

- Record a receipt for an existing product: quantity `5`, unit purchase cost `₹20`, supplier `Supplier Test`.
- Expected: current stock rises by `5`; purchase history gains a row for `₹100`; supplier Units Received rises by `5`; Purchase Amount and Amount Due each rise by `₹100` (assuming no payment).
- Record a payment of `₹40`.
- Expected: stock quantity is unchanged; Paid rises by `₹40`; Amount Due becomes `₹60`.

## 2. Sale

- Create a bill selling `2` units of that product.
- Expected: `stock.quantity` and the sum of `stock_lots.remaining_qty` both decrease by `2`; bill items, the bill stock summary, lot allocation, and movement audit are committed together. Supplier Units Received, Purchase Amount, and Amount Due do not decrease on sale.
- Attempt to sell more than available stock.
- Expected: the bill transaction rolls back and stock, lots, and bill records remain unchanged.

## 3. Bill reversal

- Delete the newly created test bill from Recent Bills.
- Expected: the exact FIFO lots used by that bill are restored once; the stock row is recalculated; movement rows are marked reversed; the bill and bill items are removed in the same transaction.
- Try deleting an inconsistent/corrupted test bill where stock movement summary or lot details are missing.
- Expected: deletion is blocked rather than guessing how much stock to restore.

## 4. Manual adjustment

- Use **Adjust Stock (+ / −)** with a negative quantity and a reason.
- Expected: lot quantity and stock quantity both decrease by that amount; supplier purchase amount and payable remain unchanged; movement history contains the reason.
- Apply a positive correction.
- Expected: stock increases through an unassigned adjustment lot, with no supplier payable created.

## 5. Reconciliation

- On the test database only, create a known mismatch by changing a stock quantity directly in SQL without updating `stock_lots`.
- Run **Reconcile Stock**, provide a reason, and refresh.
- Expected: `stock.quantity` equals the sum of `stock_lots.remaining_qty`; a `STOCK_RECONCILIATION` audit entry appears; supplier payables remain unchanged.

## 6. Supplier and product history

- Archive a supplier that has transactions.
- Expected: purchase history, payment records, outstanding balance, and lots remain intact; the supplier can be viewed/restored.
- Try permanently deleting a product with stock or transaction history.
- Expected: deletion is blocked so bill reversals and audit links are not broken.

## Database check query

```sql
SELECT s.product_id, s.product_name, s.quantity AS stock_quantity,
       COALESCE(SUM(l.remaining_qty), 0) AS lot_quantity,
       s.quantity - COALESCE(SUM(l.remaining_qty), 0) AS difference
FROM stock s
LEFT JOIN stock_lots l ON l.product_id = s.product_id
GROUP BY s.product_id, s.product_name, s.quantity
HAVING s.quantity <> COALESCE(SUM(l.remaining_qty), 0);
```

The query should return no rows after reconciliation. Supplier-lot history for old inventory is an estimate where the previous database did not track per-receipt supplier lots.
