import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared inventory movement ledger and supplier-specific stock-lot service.
 * All quantity changes are made to lots and the stock summary in the same DB transaction.
 */
public final class StockMovementService {
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private StockMovementService() { }

    public static void ensureSchema(Connection con) throws SQLException {
        String movements = "CREATE TABLE IF NOT EXISTS stock_movement_history (" +
                "movement_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "product_id INT NOT NULL," +
                "movement_type VARCHAR(40) NOT NULL," +
                "quantity_change INT NOT NULL," +
                "inventory_value_change DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                "reference_type VARCHAR(40) NULL," +
                "reference_id BIGINT NULL," +
                "note VARCHAR(500) NULL," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_stock_movement_product_date (product_id, created_at)," +
                "INDEX idx_stock_movement_reference (reference_type, reference_id)," +
                "UNIQUE KEY uq_stock_movement_reference (movement_type, reference_type, reference_id, product_id)" +
                ") ENGINE=InnoDB";
        String lots = "CREATE TABLE IF NOT EXISTS stock_lots (" +
                "lot_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "product_id INT NOT NULL," +
                "supplier VARCHAR(200) NOT NULL," +
                "purchase_id BIGINT NULL," +
                "received_qty INT NOT NULL," +
                "remaining_qty INT NOT NULL," +
                "unit_cost DECIMAL(18,8) NOT NULL DEFAULT 0.00000000," +
                "lot_type VARCHAR(40) NOT NULL," +
                "received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "note VARCHAR(500) NULL," +
                "INDEX idx_stock_lot_fifo (product_id, remaining_qty, received_at, lot_id)," +
                "INDEX idx_stock_lot_supplier (supplier, remaining_qty)," +
                "UNIQUE KEY uq_stock_lot_purchase (purchase_id)" +
                ") ENGINE=InnoDB";
        String billLots = "CREATE TABLE IF NOT EXISTS bill_stock_lot_movements (" +
                "movement_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "bill_id INT NOT NULL," +
                "product_id INT NOT NULL," +
                "lot_id BIGINT NOT NULL," +
                "quantity INT NOT NULL," +
                "inventory_cost DECIMAL(18,8) NOT NULL DEFAULT 0.00000000," +
                "is_reversed TINYINT(1) NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_bill_lot_movement (bill_id, product_id, is_reversed)," +
                "INDEX idx_lot_bill_movement (lot_id)" +
                ") ENGINE=InnoDB";
        try (Statement st = con.createStatement()) {
            st.execute(movements);
            st.execute(lots);
            st.execute(billLots);
            // Ensure the receipt ledger exists and import legacy opening stock before any screen
            // (including Billing) can sell inventory. Otherwise a first sale could reduce stock
            // before the old on-hand quantity is captured for supplier payables.
            st.execute("CREATE TABLE IF NOT EXISTS purchase_history (" +
                    "purchase_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, " +
                    "product_name VARCHAR(200) NOT NULL, supplier VARCHAR(200) NOT NULL, quantity INT NOT NULL, " +
                    "purchase_price DECIMAL(12,2) NOT NULL, total_cost DECIMAL(14,2) NOT NULL, " +
                    "purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            String openingPurchases = "INSERT INTO purchase_history " +
                    "(product_id, product_name, supplier, quantity, purchase_price, total_cost) " +
                    "SELECT s.product_id, s.product_name, TRIM(s.supplier), s.quantity, " +
                    "COALESCE(s.purchase_price,0), ROUND(COALESCE(NULLIF(s.total,0), COALESCE(s.purchase_price,0)*s.quantity),2) " +
                    "FROM stock s LEFT JOIN (SELECT product_id, LOWER(TRIM(supplier)) AS supplier_key " +
                    "FROM purchase_history GROUP BY product_id, LOWER(TRIM(supplier))) ph " +
                    "ON ph.product_id=s.product_id AND ph.supplier_key=LOWER(TRIM(s.supplier)) " +
                    "WHERE s.supplier IS NOT NULL AND TRIM(s.supplier)<>'' AND s.quantity>0 AND ph.product_id IS NULL " +
                    "AND NOT EXISTS (SELECT 1 FROM stock_lots existing_lot WHERE existing_lot.product_id=s.product_id)";
            st.executeUpdate(openingPurchases);
            // One-time migration for legacy stock: use the actual on-hand balance as an opening lot.
            // Supplier attribution of legacy stock is an estimate from the old stock.supplier field;
            // all new receipts and sales are supplier/lot tracked.
            String openingLots = "INSERT INTO stock_lots " +
                    "(product_id, supplier, purchase_id, received_qty, remaining_qty, unit_cost, lot_type, note) " +
                    "SELECT s.product_id, COALESCE(NULLIF(TRIM(s.supplier),''),'Legacy / Unassigned'), NULL, " +
                    "s.quantity, s.quantity, " +
                    "CASE WHEN s.quantity>0 THEN ROUND(COALESCE(NULLIF(s.total,0), COALESCE(s.purchase_price,0)*s.quantity)/s.quantity,8) " +
                    "ELSE COALESCE(s.purchase_price,0) END, 'OPENING_BALANCE', " +
                    "'Legacy opening balance; original source lot could not be reconstructed' " +
                    "FROM stock s WHERE s.quantity>0 AND NOT EXISTS " +
                    "(SELECT 1 FROM stock_lots l WHERE l.product_id=s.product_id)";
            st.executeUpdate(openingLots);
            // Capture the pre-upgrade quantity as audit baseline only if the product has no movement yet.
            String openingMovement = "INSERT INTO stock_movement_history " +
                    "(product_id, movement_type, quantity_change, inventory_value_change, reference_type, reference_id, note) " +
                    "SELECT s.product_id, 'OPENING_BALANCE', s.quantity, " +
                    "ROUND(COALESCE(NULLIF(s.total,0), COALESCE(s.purchase_price,0) * s.quantity),2), 'SYSTEM', s.product_id, " +
                    "'Opening balance captured when stock movement tracking was enabled' " +
                    "FROM stock s WHERE NOT EXISTS " +
                    "(SELECT 1 FROM stock_movement_history m WHERE m.product_id=s.product_id) " +
                    "ON DUPLICATE KEY UPDATE movement_id=movement_id";
            st.executeUpdate(openingMovement);
        }
    }

    public static long createLot(Connection con, int productId, String supplier, Long purchaseId,
            int quantity, BigDecimal unitCost, String lotType, String note) throws SQLException {
        if (quantity <= 0) throw new SQLException("A stock lot must have positive quantity.");
        String sql = "INSERT INTO stock_lots " +
                "(product_id, supplier, purchase_id, received_qty, remaining_qty, unit_cost, lot_type, note) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId);
            ps.setString(2, supplier == null || supplier.isBlank() ? "Legacy / Unassigned" : supplier.trim());
            if (purchaseId == null) ps.setNull(3, java.sql.Types.BIGINT); else ps.setLong(3, purchaseId);
            ps.setInt(4, quantity);
            ps.setInt(5, quantity);
            ps.setBigDecimal(6, (unitCost == null ? BigDecimal.ZERO : unitCost).setScale(8, ROUNDING));
            ps.setString(7, lotType);
            ps.setString(8, note);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Stock lot ID was not generated.");
                return keys.getLong(1);
            }
        }
    }

    /** Consumes oldest lots first and saves the exact lot allocation for a future bill reversal. */
    public static void consumeLotsForBill(Connection con, int billId, int productId, int quantity) throws SQLException {
        if (quantity <= 0) throw new SQLException("Sale quantity must be positive.");
        assertStockMatchesLots(con, productId);
        List<LotBalance> lots = lockAvailableLots(con, productId);
        int available = 0;
        for (LotBalance lot : lots) available = Math.addExact(available, lot.remainingQty);
        if (available < quantity) throw new SQLException("Supplier stock lots are out of sync or insufficient for product ID " + productId + ". Lots available: " + available + ". Refresh/reconcile stock before billing.");
        int remainingToConsume = quantity;
        for (LotBalance lot : lots) {
            if (remainingToConsume == 0) break;
            int taken = Math.min(remainingToConsume, lot.remainingQty);
            int newLotQty = lot.remainingQty - taken;
            BigDecimal cost = lot.unitCost.multiply(BigDecimal.valueOf(taken)).setScale(8, ROUNDING);
            try (PreparedStatement ps = con.prepareStatement("UPDATE stock_lots SET remaining_qty=? WHERE lot_id=? AND remaining_qty=?")) {
                ps.setInt(1, newLotQty); ps.setLong(2, lot.lotId); ps.setInt(3, lot.remainingQty);
                if (ps.executeUpdate() != 1) throw new SQLException("Stock lot changed concurrently; retry the bill.");
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO bill_stock_lot_movements (bill_id, product_id, lot_id, quantity, inventory_cost, is_reversed) VALUES (?,?,?,?,?,0)")) {
                ps.setInt(1, billId); ps.setInt(2, productId); ps.setLong(3, lot.lotId); ps.setInt(4, taken); ps.setBigDecimal(5, cost); ps.executeUpdate();
            }
            remainingToConsume -= taken;
        }
        if (remainingToConsume != 0) throw new SQLException("Could not allocate all sold units across supplier lots.");
    }

    /** Decreases FIFO lots for a manual loss/count correction (not a supplier purchase). */
    public static void decreaseLotsForAdjustment(Connection con, int productId, int quantity) throws SQLException {
        if (quantity <= 0) throw new SQLException("Adjustment quantity must be positive.");
        assertStockMatchesLots(con, productId);
        List<LotBalance> lots = lockAvailableLots(con, productId);
        int available = 0;
        for (LotBalance lot : lots) available = Math.addExact(available, lot.remainingQty);
        if (available < quantity) throw new SQLException("Adjustment would make supplier lots negative. Current lot quantity: " + available + ".");
        int remaining = quantity;
        for (LotBalance lot : lots) {
            if (remaining == 0) break;
            int taken = Math.min(remaining, lot.remainingQty);
            try (PreparedStatement ps = con.prepareStatement("UPDATE stock_lots SET remaining_qty=? WHERE lot_id=? AND remaining_qty=?")) {
                ps.setInt(1, lot.remainingQty - taken); ps.setLong(2, lot.lotId); ps.setInt(3, lot.remainingQty);
                if (ps.executeUpdate() != 1) throw new SQLException("Stock lot changed concurrently; retry the adjustment.");
            }
            remaining -= taken;
        }
    }

    public static void assertStockMatchesLots(Connection con, int productId) throws SQLException {
        int stockQty;
        try (PreparedStatement ps = con.prepareStatement("SELECT quantity FROM stock WHERE product_id=? FOR UPDATE")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Stock record for product ID " + productId + " is missing.");
                stockQty = rs.getInt(1);
            }
        }
        int lotQty;
        try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(remaining_qty),0) FROM stock_lots WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); lotQty = rs.getInt(1); }
        }
        if (stockQty != lotQty) throw new SQLException("Stock quantity and supplier lots differ for product ID " + productId + " (stock=" + stockQty + ", lots=" + lotQty + "). The operation was cancelled to avoid making the mismatch worse.");
    }

    /** Recalculates the stock summary from remaining lots. Return value is the rounded inventory value. */
    public static BigDecimal recalculateProductStock(Connection con, int productId) throws SQLException {
        BigDecimal previousCost;
        try (PreparedStatement ps = con.prepareStatement("SELECT purchase_price FROM stock WHERE product_id=? FOR UPDATE")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Stock record for product ID " + productId + " is missing.");
                previousCost = rs.getBigDecimal(1);
            }
        }
        int quantity;
        BigDecimal value;
        try (PreparedStatement ps = con.prepareStatement("SELECT COALESCE(SUM(remaining_qty),0) AS qty, ROUND(COALESCE(SUM(remaining_qty * unit_cost),0),2) AS value FROM stock_lots WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next(); quantity = rs.getInt("qty"); value = rs.getBigDecimal("value");
            }
        }
        if (value == null) value = BigDecimal.ZERO;
        value = value.setScale(2, ROUNDING);
        BigDecimal average = quantity > 0 ? value.divide(BigDecimal.valueOf(quantity), 2, ROUNDING)
                : (previousCost == null ? BigDecimal.ZERO : previousCost);
        try (PreparedStatement ps = con.prepareStatement("UPDATE stock SET quantity=?, total=?, purchase_price=? WHERE product_id=?")) {
            ps.setInt(1, quantity); ps.setBigDecimal(2, value); ps.setBigDecimal(3, average); ps.setInt(4, productId);
            if (ps.executeUpdate() != 1) throw new SQLException("Could not update stock summary for product ID " + productId + ".");
        }
        return value;
    }

    /** Restore original lots for a modern bill. If it is a pre-lot legacy bill, creates an explicitly unassigned lot. */
    public static int restoreLotsForBill(Connection con, int billId, int productId, int quantity,
            BigDecimal fallbackUnitCost) throws SQLException {
        List<BillLotMovement> details = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT movement_id, lot_id, quantity, inventory_cost FROM bill_stock_lot_movements WHERE bill_id=? AND product_id=? AND is_reversed=0 FOR UPDATE")) {
            ps.setInt(1, billId); ps.setInt(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) details.add(new BillLotMovement(rs.getLong("movement_id"), rs.getLong("lot_id"), rs.getInt("quantity"), rs.getBigDecimal("inventory_cost")));
            }
        }
        if (details.isEmpty()) {
            // No details means a pre-lot legacy bill only if no allocation rows ever existed.
            // Never create a fallback lot if prior detail rows were already reversed or corrupted.
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM bill_stock_lot_movements WHERE bill_id=? AND product_id=?")) {
                ps.setInt(1, billId); ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getLong(1) > 0) {
                        throw new SQLException("Bill lot allocations already exist but none are active for bill " + billId + ", product ID " + productId + ". Refusing a duplicate stock restoration.");
                    }
                }
            }
            BigDecimal cost = fallbackUnitCost == null ? BigDecimal.ZERO : fallbackUnitCost;
            createLot(con, productId, "Legacy / Unassigned", null, quantity, cost, "LEGACY_BILL_RESTORATION",
                    "Restored from a bill created before supplier-lot tracking was enabled");
            return 0;
        }
        int restored = 0;
        for (BillLotMovement detail : details) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE stock_lots SET remaining_qty=remaining_qty+? WHERE lot_id=? AND product_id=?")) {
                ps.setInt(1, detail.quantity); ps.setLong(2, detail.lotId); ps.setInt(3, productId);
                if (ps.executeUpdate() != 1) throw new SQLException("Original stock lot " + detail.lotId + " is missing; cannot safely reverse this bill.");
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE bill_stock_lot_movements SET is_reversed=1 WHERE movement_id=? AND is_reversed=0")) {
                ps.setLong(1, detail.movementId); ps.executeUpdate();
            }
            restored = Math.addExact(restored, detail.quantity);
        }
        if (restored != quantity) throw new SQLException("Bill lot allocation quantity does not match the bill stock movement for product ID " + productId + ".");
        return details.size();
    }

    /**
     * Reconcile lot balances to the stock table's on-hand quantity after a verified physical count.
     * This is an inventory correction only: it does not create a purchase receipt or supplier payable.
     * Call within a transaction. The stock row is treated as the confirmed quantity; any added units are
     * assigned to an unallocated reconciliation lot, and any removed units are taken FIFO from existing lots.
     */
    public static ReconciliationResult reconcileLotsToStock(Connection con, int productId, String reason) throws SQLException {
        int stockQty;
        BigDecimal stockCost;
        BigDecimal storedTotal;
        BigDecimal auditBaselineTotal;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Stock record for product ID " + productId + " is missing.");
                stockQty = rs.getInt("quantity");
                stockCost = rs.getBigDecimal("purchase_price");
                storedTotal = rs.getBigDecimal("total");
            }
        }
        if (stockQty < 0) throw new SQLException("Stock quantity is negative for product ID " + productId + ". Correct the stock row before reconciliation.");
        if (stockCost == null) stockCost = BigDecimal.ZERO;
        auditBaselineTotal = storedTotal == null ? BigDecimal.ZERO.setScale(2, ROUNDING) : storedTotal.setScale(2, ROUNDING);
        BigDecimal valuationTarget = storedTotal;
        if (valuationTarget == null || (valuationTarget.signum() == 0 && stockQty > 0)) {
            valuationTarget = stockCost.multiply(BigDecimal.valueOf(stockQty)).setScale(2, ROUNDING);
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM stock_lots WHERE product_id=? AND remaining_qty<0")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong(1) > 0) throw new SQLException("Negative supplier-lot quantity found for product ID " + productId + ". Manual database review is required.");
            }
        }

        int lotQty;
        BigDecimal lotValue;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(remaining_qty),0) AS qty, " +
                "ROUND(COALESCE(SUM(remaining_qty * unit_cost),0),2) AS value FROM stock_lots WHERE product_id=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                lotQty = rs.getInt("qty");
                lotValue = rs.getBigDecimal("value");
            }
        }
        if (lotQty < 0) throw new SQLException("Supplier-lot quantity is negative for product ID " + productId + ". Manual database review is required.");
        if (lotValue == null) lotValue = BigDecimal.ZERO;

        int quantityChange = Math.subtractExact(stockQty, lotQty);
        if (quantityChange > 0) {
            // Preserve the known stored inventory value where possible. If it cannot support a positive
            // unit cost for the missing units, use the current average cost instead.
            BigDecimal residualValue = valuationTarget.subtract(lotValue);
            BigDecimal adjustmentCost;
            if (residualValue.signum() > 0) {
                adjustmentCost = residualValue.divide(BigDecimal.valueOf(quantityChange), 8, ROUNDING);
            } else if (stockCost.signum() > 0) {
                adjustmentCost = stockCost.setScale(8, ROUNDING);
            } else if (lotQty > 0) {
                adjustmentCost = lotValue.divide(BigDecimal.valueOf(lotQty), 8, ROUNDING);
            } else {
                adjustmentCost = BigDecimal.ZERO.setScale(8, ROUNDING);
            }
            createLot(con, productId, "Legacy / Unassigned", null, quantityChange, adjustmentCost,
                    "RECONCILIATION_IN", "Unassigned units added by stock reconciliation: " + safeNote(reason));
        } else if (quantityChange < 0) {
            int toRemove = Math.negateExact(quantityChange);
            List<LotBalance> lots = lockAvailableLots(con, productId);
            int remaining = toRemove;
            for (LotBalance lot : lots) {
                if (remaining == 0) break;
                int removed = Math.min(remaining, lot.remainingQty);
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE stock_lots SET remaining_qty=? WHERE lot_id=? AND remaining_qty=?")) {
                    ps.setInt(1, lot.remainingQty - removed);
                    ps.setLong(2, lot.lotId);
                    ps.setInt(3, lot.remainingQty);
                    if (ps.executeUpdate() != 1) throw new SQLException("Stock lot changed during reconciliation; retry.");
                }
                remaining -= removed;
            }
            if (remaining != 0) throw new SQLException("Could not reconcile all excess lot quantity for product ID " + productId + ".");
        }

        BigDecimal newValue = recalculateProductStock(con, productId);
        BigDecimal valueChange = newValue.subtract(auditBaselineTotal).setScale(2, ROUNDING);
        if (quantityChange != 0 || valueChange.signum() != 0) {
            String note = "Reconciled lots to stock-table quantity " + stockQty + ". Quantity difference before repair: " + quantityChange + ". Reason: " + safeNote(reason);
            record(con, productId, "STOCK_RECONCILIATION", quantityChange, valueChange,
                    "RECONCILIATION", null, note);
        }
        assertStockMatchesLots(con, productId);
        return new ReconciliationResult(productId, lotQty, stockQty, quantityChange, valueChange);
    }

    private static String safeNote(String value) {
        if (value == null || value.isBlank()) return "No reason supplied";
        String trimmed = value.trim();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) : trimmed;
    }

    public static final class ReconciliationResult {
        public final int productId;
        public final int previousLotQuantity;
        public final int stockQuantity;
        public final int quantityChange;
        public final BigDecimal inventoryValueChange;
        public ReconciliationResult(int productId, int previousLotQuantity, int stockQuantity,
                int quantityChange, BigDecimal inventoryValueChange) {
            this.productId = productId;
            this.previousLotQuantity = previousLotQuantity;
            this.stockQuantity = stockQuantity;
            this.quantityChange = quantityChange;
            this.inventoryValueChange = inventoryValueChange == null ? BigDecimal.ZERO : inventoryValueChange;
        }
        public boolean changed() { return quantityChange != 0 || inventoryValueChange.signum() != 0; }
    }

    public static void record(Connection con, int productId, String movementType,
            int quantityChange, BigDecimal valueChange, String referenceType,
            Long referenceId, String note) throws SQLException {
        String sql = "INSERT INTO stock_movement_history " +
                "(product_id, movement_type, quantity_change, inventory_value_change, reference_type, reference_id, note) " +
                "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setString(2, movementType);
            ps.setInt(3, quantityChange);
            ps.setBigDecimal(4, (valueChange == null ? BigDecimal.ZERO : valueChange).setScale(2, ROUNDING));
            if (referenceType == null || referenceType.isBlank()) ps.setNull(5, java.sql.Types.VARCHAR);
            else ps.setString(5, referenceType);
            if (referenceId == null) ps.setNull(6, java.sql.Types.BIGINT);
            else ps.setLong(6, referenceId);
            ps.setString(7, note);
            ps.executeUpdate();
        }
    }

    private static List<LotBalance> lockAvailableLots(Connection con, int productId) throws SQLException {
        List<LotBalance> lots = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT lot_id, remaining_qty, unit_cost FROM stock_lots WHERE product_id=? AND remaining_qty>0 ORDER BY received_at ASC, lot_id ASC FOR UPDATE")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lots.add(new LotBalance(rs.getLong("lot_id"), rs.getInt("remaining_qty"), rs.getBigDecimal("unit_cost")));
            }
        }
        return lots;
    }

    private static final class LotBalance {
        final long lotId; final int remainingQty; final BigDecimal unitCost;
        LotBalance(long lotId, int remainingQty, BigDecimal unitCost) { this.lotId=lotId; this.remainingQty=remainingQty; this.unitCost=unitCost == null ? BigDecimal.ZERO : unitCost; }
    }
    private static final class BillLotMovement {
        final long movementId, lotId; final int quantity; final BigDecimal inventoryCost;
        BillLotMovement(long movementId, long lotId, int quantity, BigDecimal inventoryCost) { this.movementId=movementId; this.lotId=lotId; this.quantity=quantity; this.inventoryCost=inventoryCost; }
    }
}
