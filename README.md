# StockPilot — Inventory Management System

**A Desktop-Based Inventory Management Application**

Built using **Java Swing, JDBC, and MySQL**

---

## 1. Project Overview

StockPilot is a desktop-based inventory management application developed using Java Swing, JDBC, and MySQL. It provides a centralized platform for managing products, stock, customers, purchases, billing, and sales history.

The upgraded version retains the original application modules while introducing a modern dashboard, low-stock alerts, searchable inventory, CSV stock export, supplier account management, and a transactional purchase register.

The application focuses on maintaining accurate inventory records, supplier payables, and stock movement history.

## 2. Key Features and Improvements

### 2.1 Modern User Interface

- Modern navy-themed sidebar and dashboard.
- Dashboard metrics and low-stock alerts.
- Persistent Dark Mode / Light Mode toggle.
- Contrast-safe text fields, dropdowns, buttons, and table headings.
- Theme-aware cards, labels, selections, and borders.
- Consistent styling across application screens.

### 2.2 Stock Management

- Searchable stock table.
- Product and stock management.
- Stock totals and inventory valuation.
- Low-stock alerts.
- Stock adjustment with mandatory reason.
- CSV stock export.
- Stock reconciliation functionality.
- Supplier-specific stock lot tracking.

### 2.3 Purchase Register and Supplier Management

- Supplier account management.
- Purchase history and receipt tracking.
- Units received and purchase amount calculations.
- Amount paid and outstanding amount due.
- Supplier-wise product drill-down.
- Record Payment functionality.
- Remove and Restore Supplier actions.
- Archived supplier visibility.

### 2.4 Billing and Sales Management

- Product selection automatically fills product ID, available quantity, and selling price.
- Prevents billing quantities from exceeding available stock.
- Locks stock records during checkout.
- Records inventory value removed during sales.
- Saves bill items and stock movements in one database transaction.
- Supports bill deletion with stock restoration.
- Maintains an audit trail for reversed transactions.

### 2.5 Security and Configuration

- Centralized database configuration through `DbConfig.java`.
- Database credentials are not embedded in active Java source code.
- Supports legacy passwords and PBKDF2 password hashes.
- Automatically attempts to upgrade legacy passwords after successful login.

---

## 3. Supplier Purchase Totals and Opening Stock

The supplier accounts screen calculates receipt units from the `purchase_history` table.

For every new purchase receipt:

**Purchase Amount = Quantity × Unit Purchase Cost**

The calculated amount is stored in `total_cost` and used for supplier purchase summaries.

**Amount Due = Total Purchase Value − Recorded Payments**

The displayed amount due is floored at zero.

### 3.1 Opening Stock Migration

When stock tracking is initialized for the first time, legacy stock records without matching purchase receipts are imported as opening-stock estimates.

The opening receipt value is calculated using:

1. The existing `stock.total` inventory value, when available.
2. Otherwise, `quantity × stored unit purchase cost`.

The existing supplier value from the old stock record is used to create an opening stock lot.

**Important:** The original supplier-specific remaining quantities cannot be reconstructed with certainty if the previous database did not track individual stock lots.

Opening-stock estimates also increase the supplier's Amount Due. If the inventory was already paid for before introducing this ledger, record the historical payment against the supplier to correct the outstanding balance.

Existing purchases with receipt history are not imported again.

### 3.2 Supplier Accounting

| Metric | Calculation |
|---|---|
| Units Received | Sum of purchase receipt quantities |
| Purchase Amount | Sum of recorded receipt totals |
| Amount Paid | Sum of supplier payments |
| Amount Due | Purchase Amount − Amount Paid |
| Current Stock | Live quantity from the stock table |

Current stock is displayed separately from total units received.

### 3.3 Supplier Removal and Archiving

A supplier with linked stock, purchase receipts, or payment records is archived instead of permanently deleted.

Archived suppliers can be inspected by enabling **Show archived suppliers**. Users can also record outstanding payments or restore archived suppliers.

A supplier with no linked records can be permanently deleted.

A new purchase recorded against an archived supplier automatically restores that supplier.

---

## 4. Inventory Tracking and Stock Transactions

### 4.1 Supplier-Specific Stock Lots

Every new purchase receipt creates:

- A purchase receipt record.
- A supplier-specific stock lot.
- A stock movement entry.
- A supplier payable.

These operations are performed within the same database transaction.

### 4.2 FIFO Stock Consumption

Stock is maintained in supplier-specific lots.

When a sale occurs, the application consumes the oldest available stock lots first using the **First In, First Out (FIFO)** method.

The Purchases screen displays the remaining quantity for each supplier lot rather than assigning the entire product quantity to the latest supplier.

### 4.3 Inventory Valuation

The current inventory valuation is recalculated using:

**Inventory Value = Sum of (Remaining Lot Quantity × Receipt Unit Cost)**

The stock table maintains a weighted-average unit cost for compatibility with the existing application interface.

### 4.4 Stock Adjustments

Stock adjustments support both positive and negative quantity changes.

- Decreases consume available stock using FIFO.
- Increases create an explicitly unassigned adjustment lot.
- Every adjustment requires a reason.
- All adjustments update stock quantity and inventory value.
- Each adjustment creates an audit record.
- Manual adjustments do not create supplier payables.

### 4.5 Stock Reconciliation

The Stock Management module includes a **Reconcile Stock** feature.

It compares the quantity stored in the stock table with the total quantity available in supplier stock lots.

After confirmation and entry of a mandatory reason, the application aligns the lot quantities with the stock-table quantity within one transaction.

- Missing lot quantities are assigned to `Legacy / Unassigned`.
- Excess lot quantities are removed using FIFO.
- Reconciliation does not create supplier purchases.
- Supplier payables remain unchanged.

**Important:** If a mismatch represents an actual delivery, record it through the Purchases module instead of using reconciliation.

---

## 5. Billing, Stock Deduction, and Reversal

### 5.1 Transactional Billing

During checkout, the application:

1. Locks the current stock row.
2. Validates available inventory.
3. Checks supplier-lot quantities.
4. Deducts the sold quantity.
5. Records the inventory value removed at weighted-average cost.
6. Saves bill items and stock movement records.
7. Commits the transaction.

If any operation fails, the entire transaction is rolled back.

### 5.2 Deleting a Bill

When a bill is deleted, the application restores the exact supplier stock lots consumed by that bill.

It then recalculates stock quantity and inventory value within a single transaction.

The reversal is recorded in the audit ledger, and both bill movement records are marked as reversed to prevent duplicate restoration.

For older bills without supplier-lot records:

- If a stock deduction record exists, the quantity is restored to `Legacy / Unassigned`.
- If no stock movement record exists, the bill is deleted without guessing the quantity to restore.

### 5.3 Stock and Product Deletion Protection

Direct stock or product deletion is blocked when quantities or transaction history exist.

This prevents bill reversals and purchase records from referencing missing stock records.

Product metadata changes are synchronized with stock without overwriting existing on-hand quantities or inventory valuation.

---

## 6. Stock Movement History and Data Consistency

The application maintains a stock movement history containing:

- Opening balances.
- Purchase receipts.
- Sales deductions.
- Sale reversals.
- Manual stock adjustments.

Stock quantity and supplier-lot quantity are validated before stock-affecting transactions.

If a mismatch is detected, the operation is rejected instead of silently increasing the discrepancy.

The opening-stock snapshot represents the starting point of movement history. Earlier movements that were never recorded cannot be reconstructed.

---

## 7. Database Configuration

Database connection settings are centralized in `DbConfig.java`.

The application uses the following database connection:

```properties
db.url=jdbc:mysql://localhost:3306/Inventory?useSSL=false&serverTimezone=Asia/Kolkata
db.user=root
db.password=PUT_YOUR_LOCAL_MYSQL_PASSWORD_HERE
```

### 7.1 Configure the Database

1. Open the project folder in VS Code.
2. Copy `config.properties.example`.
3. Create a new file named `config.properties`.
4. Enter your local MySQL username and password.
5. Confirm that the `Inventory` database and its existing tables are present.
6. Open MySQL Workbench.
7. Select the `Inventory` schema.
8. Run the included `database-upgrade.sql` script.

The migration:

- Widens the authentication password column to support password hashes.
- Prepares purchase and supplier payment tables.
- Prepares the supplier directory.
- Adds stock movement and bill movement tracking.
- Does not drop existing records.

**Security:** The `config.properties` file is ignored by Git and should not be shared publicly.

---

## 8. Database Requirements

The application expects the following existing tables and columns:

| Table | Required Columns |
|---|---|
| `authentication` | `user_id`, `password` |
| `product` | `PRODUCT_ID`, `PRODUCT_NAME`, `SUPPLIER`, `PURCHASE_PRICE`, `SELLING_PRICE` |
| `stock` | `product_id`, `product_name`, `supplier`, `purchase_price`, `selling_price`, `quantity`, `total` |
| `customer` | `customer_id`, `customer_name`, `phone`, `email` |
| `bills` | `bill_id`, `customer_name`, `phone`, `email`, `bill_date`, `grand_total` |
| `bill_items` | `bill_id`, `product_id`, `product_name`, `selling_price`, `quantity`, `total` |
| `purchase_history` | `purchase_id`, `product_id`, `product_name`, `supplier`, `quantity`, `purchase_price`, `total_cost`, `purchase_date` |
| `supplier_payments` | `payment_id`, `supplier`, `amount`, `payment_date`, `note` |
| `supplier_directory` | `supplier_key`, `supplier_name`, `is_active`, `created_at`, `updated_at` |
| `stock_movement_history` | Stock movement audit records |
| `bill_stock_movements` | `bill_id`, `product_id`, `quantity`, `inventory_cost`, `is_reversed` |

If your local database schema uses different column names or data types, adjust the SQL migration to match your schema before running the application.

**Database integration note:** The source ZIP does not include the live MySQL database. Therefore, the code cannot be integration-tested against your local database from the ZIP alone.

---

## 9. Technology Stack

| Technology | Purpose |
|---|---|
| Java | Core application development |
| Java Swing | Desktop graphical user interface |
| JDBC | Java-to-MySQL connectivity |
| MySQL | Relational database management |
| MySQL Connector/J | JDBC driver |
| HTML, CSS, JavaScript | Not required for the Java Swing application interface |

---

## 10. System Requirements

- **Operating System:** Windows
- **Java:** JDK 17 or newer
- **Database:** MySQL Server
- **Database Management Tool:** MySQL Workbench
- **IDE:** Visual Studio Code
- **JDBC Driver:** MySQL Connector/J
- **Build Output Directory:** `out/`

The source code uses Java text blocks, so JDK 17 or newer is required.

The required MySQL Connector/J JAR is bundled in the `library/` directory.

---

## 11. Running the Application

### 11.1 Using the Windows Batch File

1. Configure the database.
2. Ensure MySQL Server is running.
3. Open the project folder.
4. Double-click `run.bat`.
5. The script compiles the Java files into `out/` and launches the sign-in screen.

### 11.2 Running Through VS Code

Open the project folder in VS Code and run `main.java`.

### 11.3 Manual Compilation

Open the VS Code terminal or PowerShell in the project directory.

**Compile:**

```powershell
javac -encoding UTF-8 -cp "library/*" -d out *.java
```

**Run:**

```powershell
java -cp "out;library/*" main
```

---

## 12. Appearance and Theme Settings

StockPilot supports both Dark Mode and Light Mode.

Use the theme toggle in the application header to switch between the two modes.

The selected preference is remembered on the computer for future application launches.

The application uses explicit foreground and background colors for:

- Text fields.
- Password fields.
- Formatted fields.
- Dropdown values.
- Buttons.
- Table headings.
- Table selections.
- Cards and borders.

Dedicated Swing UI delegates and table-header renderers help prevent low-contrast native Windows painting.

---

## 13. Password Storage and Authentication

StockPilot supports both existing legacy passwords and PBKDF2-hashed passwords.

After a successful login using a legacy plaintext password, the application attempts to replace the stored password with a PBKDF2 hash.

The included `database-upgrade.sql` must be executed first so the `authentication.password` column can accommodate the hash.

If the migration cannot be applied, legacy login can still work, but the password hash upgrade may not be saved.

---

## 14. Supplier Payables and Unit Purchase Cost

The unit purchase cost is retained because it represents the cost per unit for a particular purchase receipt.

New receipt totals are calculated using:

**Receipt Total = Quantity × Unit Purchase Cost**

The stored `total_cost` is used for supplier payable summaries so that historical invoice totals remain auditable, even if an older row contains rounding differences.

The stock table stores a weighted-average cost for current inventory valuation, while purchase history preserves the original receipt-level unit cost.

### Important Accounting Notes

- Record payments whenever an actual supplier payment is made.
- Opening stock is imported using an estimated value.
- Opening stock increases supplier Amount Due.
- If the opening inventory was already paid for, record the historical payment to avoid an incorrect outstanding balance.
- Legacy supplier attribution is estimated using the old `stock.supplier` field.
- New purchases are tracked through supplier-specific FIFO stock lots.

---

## 15. Data Safety and Transaction Integrity

The application follows several data-safety practices:

1. Purchase receipts are stored in `purchase_history`.
2. Billing deductions are recorded in `bill_stock_movements`.
3. Bill deletion restores stock using the recorded movement history.
4. Stock rows with quantities or transaction history are retained.
5. Verified stock losses and corrections should be recorded through Adjust Stock.
6. Stock and lot quantities are validated before stock-affecting operations.
7. Purchases, sales, and their associated stock changes are performed transactionally.
8. Failed transactions are rolled back to prevent partial updates.

**Recommendation:** Test the application using a copy of your database before using it with business-critical inventory data.

---

## 16. Conclusion

StockPilot provides a centralized desktop solution for inventory, purchase, supplier, customer, and billing management.

Its upgraded architecture introduces supplier-specific stock tracking, FIFO consumption, transactional billing, supplier payables, stock reconciliation, and audit-based movement history.

By combining a modern Java Swing interface with JDBC and MySQL, the application aims to improve inventory accuracy, financial visibility, and data consistency while retaining the functionality of the original inventory management system.


how to run it 

by calling the sql connector from the terminal
java -cp".;library\mysql-connector-j-26.7.0.jar" main.java
---

**Project Name:** StockPilot — Inventory Management System  
**Technologies:** Java, Java Swing, JDBC, MySQL  
**Java Requirement:** JDK 17 or newer  
**Database:** Inventory  
**Application Type:** Desktop-Based Inventory Management System
