-- Safe, non-destructive upgrade for the existing Inventory database.
-- Run this once in MySQL Workbench after selecting the Inventory schema.
-- Existing product, stock, customer, bills and bill_items records are not deleted.

ALTER TABLE authentication MODIFY COLUMN password VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS bill_stock_movements (
    bill_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    inventory_cost DECIMAL(14,2) NULL,
    is_reversed TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (bill_id, product_id)
) ENGINE=InnoDB;

-- Ongoing stock change audit. The application also creates this table automatically,
-- captures a one-time opening balance for existing stock, and logs new movements.
CREATE TABLE IF NOT EXISTS stock_movement_history (
    movement_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    movement_type VARCHAR(40) NOT NULL,
    quantity_change INT NOT NULL,
    inventory_value_change DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    reference_type VARCHAR(40) NULL,
    reference_id BIGINT NULL,
    note VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_stock_movement_product_date (product_id, created_at),
    INDEX idx_stock_movement_reference (reference_type, reference_id),
    UNIQUE KEY uq_stock_movement_reference (movement_type, reference_type, reference_id, product_id)
) ENGINE=InnoDB;

-- Supplier-specific stock lots. New purchases create a lot per supplier receipt;
-- sales consume oldest lots first (FIFO). The application inserts opening lots
-- from current on-hand stock where no lot history exists.
CREATE TABLE IF NOT EXISTS stock_lots (
    lot_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    supplier VARCHAR(200) NOT NULL,
    purchase_id BIGINT NULL,
    received_qty INT NOT NULL,
    remaining_qty INT NOT NULL,
    unit_cost DECIMAL(18,8) NOT NULL DEFAULT 0.00000000,
    lot_type VARCHAR(40) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500) NULL,
    INDEX idx_stock_lot_fifo (product_id, remaining_qty, received_at, lot_id),
    INDEX idx_stock_lot_supplier (supplier, remaining_qty),
    UNIQUE KEY uq_stock_lot_purchase (purchase_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS bill_stock_lot_movements (
    movement_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    bill_id INT NOT NULL,
    product_id INT NOT NULL,
    lot_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    inventory_cost DECIMAL(18,8) NOT NULL DEFAULT 0.00000000,
    is_reversed TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_bill_lot_movement (bill_id, product_id, is_reversed),
    INDEX idx_lot_bill_movement (lot_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS purchase_history (
    purchase_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    supplier VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    purchase_price DECIMAL(12,2) NOT NULL,
    total_cost DECIMAL(14,2) NOT NULL,
    purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Supplier-level payment ledger. Outstanding payable is calculated as total
-- purchase receipts minus payments recorded against the normalized supplier name.
CREATE TABLE IF NOT EXISTS supplier_payments (
    payment_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supplier VARCHAR(200) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500) NULL,
    INDEX idx_supplier_payments_supplier (supplier)
) ENGINE=InnoDB;

-- Supplier directory supports archiving/removing suppliers without deleting accounting history.
CREATE TABLE IF NOT EXISTS supplier_directory (
    supplier_key VARCHAR(200) NOT NULL PRIMARY KEY,
    supplier_name VARCHAR(200) NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
-- One-time opening-stock import for legacy products that have no purchase receipt yet.
-- Each opening line uses the legacy stock inventory value when available; unit cost is the stored average.
-- The anti-duplicate check keeps this idempotent when the script is run again.
INSERT INTO purchase_history
    (product_id, product_name, supplier, quantity, purchase_price, total_cost)
SELECT s.product_id, s.product_name, TRIM(s.supplier), s.quantity,
       COALESCE(s.purchase_price, 0), ROUND(COALESCE(NULLIF(s.total, 0), COALESCE(s.purchase_price, 0) * s.quantity), 2)
FROM stock s
LEFT JOIN (
    SELECT product_id, LOWER(TRIM(supplier)) AS supplier_key
    FROM purchase_history
    GROUP BY product_id, LOWER(TRIM(supplier))
) ph ON ph.product_id = s.product_id
    AND ph.supplier_key = LOWER(TRIM(s.supplier))
WHERE s.supplier IS NOT NULL AND TRIM(s.supplier) <> '' AND s.quantity > 0
  AND ph.product_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM stock_lots existing_lot WHERE existing_lot.product_id = s.product_id);

-- Seed supplier directory using existing data. INSERT IGNORE preserves an existing Archived status.
INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active)
SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1
FROM purchase_history WHERE supplier IS NOT NULL AND TRIM(supplier) <> ''
GROUP BY LOWER(TRIM(supplier));

INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active)
SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1
FROM stock WHERE supplier IS NOT NULL AND TRIM(supplier) <> ''
GROUP BY LOWER(TRIM(supplier));

INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active)
SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1
FROM supplier_payments WHERE supplier IS NOT NULL AND TRIM(supplier) <> ''
GROUP BY LOWER(TRIM(supplier));
