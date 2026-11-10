# StockPilot — Inventory Management System

A desktop inventory application built with Java Swing, JDBC and MySQL. This upgraded version keeps the original product, stock, customer, billing and sales-history modules while adding a dashboard, low-stock alerts, searchable stock, CSV stock export, and a purchase register that updates stock transactionally.

### Supplier purchase totals and opening stock

The supplier accounts screen totals receipt units from `purchase_history`. New receipt totals are calculated as `quantity × unit purchase cost`, stored as `total_cost`, then summed across all products and receipts. Amount due is the recorded receipt total less payments recorded in `supplier_payments` (floored at zero). Incoming stock added through Stock or Purchases creates a receipt, so these totals refresh from the same ledger. Unit prices are normalized to two decimal places before the receipt total is calculated so the displayed unit cost and invoice amount do not drift due to rounding.

When stock tracking is first initialized by any screen (or `database-upgrade.sql` is run), legacy stock rows without a matching receipt are imported as an opening-stock estimate. The opening receipt total uses the legacy `stock.total` inventory value when available (otherwise quantity × stored unit purchase cost), and the stored unit cost remains an estimate. This makes older stock visible in Units Received and Purchase Amount. The app also creates an opening stock lot based on the old `stock.supplier` value, because original supplier-specific remaining quantities cannot be rebuilt with certainty from the old schema. **Those opening values will also increase Amount Due** unless a payment is recorded. If that stock had already been paid for before using this ledger, record the historical payment(s) against the supplier so the displayed balance is accurate. Existing purchases with receipt history are not imported again.

## Included improvements

- Modern navy sidebar, dashboard metrics, low-stock alerts, and a header toggle for persistent light/dark mode.
- Contrast-safe controls in both themes: explicit Swing text-field/combo-box delegates, readable light/dark button labels, and a dedicated table-header renderer so white-on-white headings cannot reappear. Cards, labels, selections, and borders also follow the active palette.
- Responsive stock screen with product search, form validation, stock totals and CSV export.
- Purchase register with supplier accounts: current stock quantity from the live stock table, units received from saved purchase receipts, total purchase value, amount paid, outstanding amount due, per-supplier product drill-down, Record Payment, and Remove/Restore Supplier actions. A supplier with linked stock, purchases or payments is archived rather than hard-deleted so accounting history remains available. Enable Show archived suppliers to inspect archived suppliers or record outstanding payments.
- Incoming stock recorded from either Purchases or Stock → Add Stock creates a purchase receipt, supplier-specific stock lot, stock movement entry and supplier payable in the same database transaction. Current inventory valuation is recalculated from remaining lot quantities × their receipt costs; the stock table stores a weighted-average unit cost for the existing UI.
- Supplier-specific stock is lot tracked. Each new receipt creates a supplier lot, sales consume the oldest available lots (FIFO), and the Purchases screen reports remaining units by supplier lot instead of assigning the entire product quantity to only the latest supplier name. Legacy inventory is migrated as one opening lot under the supplier recorded in the old stock row; its original source supplier cannot be reconstructed if the old database did not track lots.
- Central MySQL connection settings via `DbConfig.java`; credentials are no longer embedded in active Java code.
- Product selection in billing fills the product ID, available units and selling price from stock.
- Billing prevents adding more than available stock, locks the current stock row during checkout, applies the stock deduction, records the inventory value removed at weighted-average cost, and saves bill items and stock movement in one transaction. Any failure rolls the whole operation back.
- Deleting a bill restores the exact supplier lots consumed by that bill and recalculates stock quantity/value in one transaction. The reversal is kept in the audit ledger and both bill movement records are marked reversed to prevent a second restoration. A pre-lot legacy bill with a recorded stock deduction restores into a `Legacy / Unassigned` lot because its original supplier allocation is unknown; a bill without any stock movement rows is deleted without guessing the quantity to restore.
- Stock adjustments support signed quantity changes with a required reason. Decreases consume lots using FIFO; increases create an explicitly unassigned adjustment lot. Both update stock quantity/value and create an audit record but deliberately do not create supplier payable.
- Stock Management includes **Reconcile Stock**. It compares each stock-table quantity with the supplier-lot ledger and, after confirmation plus a required reason, aligns lots to the stock-table count inside one transaction. Missing lot units are assigned to `Legacy / Unassigned`; excess lot units are removed FIFO. This is an inventory correction only and never invents supplier purchases or changes supplier payables. If a mismatch represents a real delivery, use Purchases instead of reconciliation.
- Direct stock/product deletion is blocked where quantities or purchase/sale/movement history exist, preventing later bill reversals from pointing at a missing stock row. Product metadata edits sync to stock without overwriting on-hand quantity or valuation.
- Stock movement history records opening balances on first upgrade and subsequent purchases, sales, sale reversals, and manual adjustments. Stock quantity and supplier-lot quantity are checked before stock-affecting transactions; on mismatch, the operation is rejected instead of silently compounding the discrepancy. The legacy opening snapshot is the point from which movement history begins; it cannot reconstruct earlier movements that were not logged.
- Login supports existing legacy passwords and PBKDF2-hashed passwords. After a successful legacy login, the app attempts to upgrade the stored value to a PBKDF2 hash.
- Searchable bill history and sales analytics remain available.

## Requirements

- JDK 17 or newer (the source uses Java text blocks).
- MySQL Server and the existing `Inventory` database.
- The required MySQL Connector/J JAR is bundled in `library/`.

## Configure the database

1. Open this folder in VS Code.
2. Copy `config.properties.example` to a new file named `config.properties`.
3. Edit `config.properties` and set your local MySQL username and password. `config.properties` is ignored by Git and should not be shared.
4. Confirm that the `Inventory` database and the existing tables are present.
5. Run `database-upgrade.sql` in MySQL Workbench with the `Inventory` schema selected. This widens the authentication password column to support password hashes and prepares the purchase, supplier payment, and supplier directory tables. It does not drop existing records.

The app expects these existing tables/columns from the original project:

- `authentication(user_id, password)`
- `product(PRODUCT_ID, PRODUCT_NAME, SUPPLIER, PURCHASE_PRICE, SELLING_PRICE)`
- `stock(product_id, product_name, supplier, purchase_price, selling_price, quantity, total)`
- `customer(customer_id, customer_name, phone, email)`
- `bills(bill_id, customer_name, phone, email, bill_date, grand_total)`
- `bill_items(bill_id, product_id, product_name, selling_price, quantity, total)`
- `purchase_history(purchase_id, product_id, product_name, supplier, quantity, purchase_price, total_cost, purchase_date)`
- `supplier_payments(payment_id, supplier, amount, payment_date, note)`
- `supplier_directory(supplier_key, supplier_name, is_active, created_at, updated_at)`, `stock_movement_history(...)`, and `bill_stock_movements(bill_id, product_id, quantity, inventory_cost, is_reversed)`

If your local schema uses different column names or types, adjust the SQL to match your schema before running the application. The code cannot be integration-tested against your live database from this ZIP alone because the database itself was not included.

## Appearance

Use the **Dark mode / Light mode** toggle in the application header. Your preference is remembered on the computer for the next launch. Text fields, password/formatted fields, dropdown values, buttons, and table headings use explicit foreground/background colors in both modes. The header renderer and standard button UI are enforced to avoid low-contrast native Windows Swing painting.

## Run from Windows

Double-click `run.bat`, or open the project folder in VS Code and run `main.java`. The script compiles the Java files into `out/` and opens the sign-in screen.

For a manual build:

```powershell
javac -encoding UTF-8 -cp "library/*" -d out *.java
java -cp "out;library/*" main
```

## Password storage note

After a successful sign-in with a legacy plaintext password, the app attempts to replace it with a PBKDF2 hash. Run the included `database-upgrade.sql` first so the `authentication.password` column can store the hash. If the migration cannot be applied, sign-in still works for legacy data but the hash upgrade may not be saved.

## Supplier removal / archive

The Supplier Accounts table provides a Remove Supplier action. When a supplier has stock, purchase receipts, or supplier payment records, the app archives it instead of deleting it; this preserves historical purchases and any outstanding payable. Archived suppliers are hidden from the default active list. Enable Show archived suppliers to inspect them, record old payments, or restore a supplier. A supplier directory entry with no linked stock, purchases, or payments can be permanently deleted. A new purchase recorded against an archived supplier restores it automatically.

## Supplier payables and unit cost

The unit purchase cost should not be removed: it is the cost per unit on a particular receipt. New receipt totals are calculated as `quantity × unit purchase cost`; the stored receipt `total_cost` is used for payable summaries so historic invoice totals stay auditable even if an old row had rounding differences. **Units Received** is the sum of receipt quantities, including a one-time opening-stock estimate for legacy products that had no matching receipt. **Amount Due** is total purchase value less recorded supplier payments, with the displayed due floored at zero. The unit cost remains on purchase history as an audit record; the stock table stores a weighted-average cost for current inventory valuation. Use **Record Payment** whenever a payment is actually made so the balance stays accurate. When opening stock is imported, its estimated value is added to the supplier's payable because the application cannot know whether those older items were paid for previously. If they were already paid, record those prior payments to avoid showing a false outstanding balance. New receipts are tracked in supplier-specific FIFO stock lots, so sales and stock adjustments update each supplier's remaining units. For legacy on-hand stock, supplier attribution is only an estimate from the old `stock.supplier` field because the old schema did not retain per-receipt lot history.

## Data-safety notes

- Purchase receipts are stored in `purchase_history`.
- Billing stock deductions are recorded in `bill_stock_movements` so a newly created bill can be voided without losing stock.
- Stock rows with quantities or transaction history are retained; use Adjust Stock to record verified losses/count corrections instead of deleting rows. This preserves links needed by bills and purchase history.
- Test on a copy of your database before using this application with business-critical data.
#   i m p r o v e d - v e r s i o n - o f - I M S 
 

 
### how to run it 

 java -cp ".;library\mysql-connector-j-26.7.0.jar" main.java  
                 
