import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.AbstractCellEditor;
import javax.swing.plaf.basic.BasicButtonUI;

/** Supplier payable ledger and incoming purchase register. */
public class PurchaseManagementPanel extends JPanel {
    private final JTextField id = new JTextField(), name = new JTextField(), supplier = new JTextField();
    private final JTextField cost = new JTextField(), sell = new JTextField(), qty = new JTextField();

    private final DefaultTableModel supplierModel = new DefaultTableModel(
            new String[]{"Supplier", "Status", "Current Stock (Lots)", "Units Received", "Purchase Amount (₹)", "Paid (₹)", "Amount Due (₹)", "Products", "Record Payment", "Remove Supplier"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return column >= 7; }
        @Override public Class<?> getColumnClass(int column) { return column == 2 || column == 3 ? Integer.class : String.class; }
    };
    private final JCheckBox showArchived = new JCheckBox("Show archived suppliers");
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Purchase ID", "Date", "Product ID", "Product", "Supplier", "Qty", "Unit Cost (₹)", "Total (₹)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable supplierTable = new JTable(supplierModel);
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel suppliersValue = new JLabel("0");
    private final JLabel purchasesValue = new JLabel("₹0.00");
    private final JLabel dueValue = new JLabel("₹0.00");

    private final Color ink = new Color(23, 37, 63), blue = new Color(37, 99, 235);

    public PurchaseManagementPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(new Color(245, 247, 251));
        setBorder(new EmptyBorder(20, 22, 22, 22));
        ensureLedgerTables();
        add(header(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);
        main.add(form(), BorderLayout.WEST);
        main.add(createTabs(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        refreshAll();
    }

    private void ensureLedgerTables() {
        String history = "CREATE TABLE IF NOT EXISTS purchase_history (" +
                "purchase_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "product_id INT NOT NULL, product_name VARCHAR(200) NOT NULL," +
                "supplier VARCHAR(200) NOT NULL, quantity INT NOT NULL," +
                "purchase_price DECIMAL(12,2) NOT NULL, total_cost DECIMAL(14,2) NOT NULL," +
                "purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB";
        String payments = "CREATE TABLE IF NOT EXISTS supplier_payments (" +
                "payment_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "supplier VARCHAR(200) NOT NULL, amount DECIMAL(14,2) NOT NULL," +
                "payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "note VARCHAR(500) NULL, INDEX idx_supplier_payments_supplier (supplier)) ENGINE=InnoDB";
        String supplierDirectory = "CREATE TABLE IF NOT EXISTS supplier_directory (" +
                "supplier_key VARCHAR(200) NOT NULL PRIMARY KEY," +
                "supplier_name VARCHAR(200) NOT NULL," +
                "is_active TINYINT(1) NOT NULL DEFAULT 1," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB";
        // Legacy inventory may exist in `stock` without any receipt in purchase_history.
        // Import that opening quantity once per product/supplier using the legacy stock
        // valuation as its estimated receipt total. This makes legacy stock visible in units received and payables,
        // rather than displaying zero for suppliers whose stock predates the receipt ledger.
        String openingStock = "INSERT INTO purchase_history " +
                "(product_id, product_name, supplier, quantity, purchase_price, total_cost) " +
                "SELECT s.product_id, s.product_name, TRIM(s.supplier), s.quantity, " +
                "COALESCE(s.purchase_price,0), ROUND(COALESCE(NULLIF(s.total,0), COALESCE(s.purchase_price,0) * s.quantity), 2) " +
                "FROM stock s LEFT JOIN (SELECT product_id, LOWER(TRIM(supplier)) AS supplier_key " +
                "FROM purchase_history GROUP BY product_id, LOWER(TRIM(supplier))) ph " +
                "ON ph.product_id=s.product_id AND ph.supplier_key=LOWER(TRIM(s.supplier)) " +
                "WHERE s.supplier IS NOT NULL AND TRIM(s.supplier)<>'' AND s.quantity>0 AND ph.product_id IS NULL";
        try (Connection con = DbConfig.getConnection(); Statement st = con.createStatement()) {
            st.execute(history);
            st.execute(payments);
            st.execute(supplierDirectory);
            st.executeUpdate(openingStock);
            seedSupplierDirectory(con);
            StockMovementService.ensureSchema(con);
            DbConfig.ensureBillStockMovementTable(con);
        } catch (SQLException ex) {
            error("Could not prepare purchase/payables tables", ex);
        }
    }

    private void seedSupplierDirectory(Connection con) throws SQLException {
        String[] seedSql = {
                "INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active) " +
                "SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1 FROM purchase_history " +
                "WHERE supplier IS NOT NULL AND TRIM(supplier)<>'' GROUP BY LOWER(TRIM(supplier))",
                "INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active) " +
                "SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1 FROM stock " +
                "WHERE supplier IS NOT NULL AND TRIM(supplier)<>'' GROUP BY LOWER(TRIM(supplier))",
                "INSERT IGNORE INTO supplier_directory (supplier_key, supplier_name, is_active) " +
                "SELECT LOWER(TRIM(supplier)), MIN(TRIM(supplier)), 1 FROM supplier_payments " +
                "WHERE supplier IS NOT NULL AND TRIM(supplier)<>'' GROUP BY LOWER(TRIM(supplier))"
        };
        try (Statement st = con.createStatement()) {
            for (String sql : seedSql) st.executeUpdate(sql);
        }
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Purchases & Supplier Payables");
        title.setFont(new Font("Segoe UI", Font.BOLD, 27));
        title.setForeground(ink);
        JLabel sub = new JLabel("Track incoming products, supplier balances, payments and product-wise purchase history.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(105, 119, 141));
        titles.add(title);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);
        JButton refresh = button("Refresh", blue);
        refresh.addActionListener(e -> refreshAll());
        p.add(titles, BorderLayout.WEST);
        p.add(refresh, BorderLayout.EAST);
        return p;
    }

    private JComponent createTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Supplier Accounts", createSupplierAccountsTab());
        tabs.addTab("Purchase History", createHistoryTab());
        return tabs;
    }

    private JPanel createSupplierAccountsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("ACTIVE SUPPLIERS", suppliersValue, new Color(37, 99, 235)));
        stats.add(statCard("TOTAL PURCHASES", purchasesValue, new Color(124, 58, 237)));
        stats.add(statCard("OUTSTANDING PAYABLE", dueValue, new Color(220, 38, 38)));
        panel.add(stats, BorderLayout.NORTH);

        JPanel tableCard = new JPanel(new BorderLayout(0, 8));
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel title = new JLabel("Supplier accounts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(ink);
        JLabel hint = new JLabel("Current stock is remaining supplier-lot quantity (FIFO); legacy stock uses the old stock supplier as an estimate. Units received/purchase value come from receipts; amount due = purchases − payments.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(105, 119, 141));
        JPanel captions = new JPanel();
        captions.setOpaque(false);
        captions.setLayout(new BoxLayout(captions, BoxLayout.Y_AXIS));
        captions.add(title);
        captions.add(Box.createVerticalStrut(4));
        captions.add(hint);
        showArchived.setOpaque(false);
        showArchived.setForeground(ink);
        showArchived.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        showArchived.setToolTipText("Archived suppliers remain available here so old purchases and outstanding payments are not lost.");
        showArchived.addActionListener(e -> refreshSupplierAccounts());
        captions.add(Box.createVerticalStrut(5));
        captions.add(showArchived);
        tableCard.add(captions, BorderLayout.NORTH);

        prepareTable(supplierTable);
        supplierTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] supplierWidths = {100, 75, 120, 95, 115, 80, 105, 95, 115, 120};
        for (int i = 0; i < supplierWidths.length; i++) supplierTable.getColumnModel().getColumn(i).setPreferredWidth(supplierWidths[i]);
        supplierTable.getColumnModel().getColumn(7).setCellRenderer(new ActionButtonRenderer("View Products", blue));
        supplierTable.getColumnModel().getColumn(7).setCellEditor(new SupplierActionEditor(supplierTable, "View Products", blue, this::showSupplierProducts));
        supplierTable.getColumnModel().getColumn(8).setCellRenderer(new ActionButtonRenderer("Record Payment", new Color(5, 150, 105)));
        supplierTable.getColumnModel().getColumn(8).setCellEditor(new SupplierActionEditor(supplierTable, "Record Payment", new Color(5, 150, 105), this::recordSupplierPayment));
        supplierTable.getColumnModel().getColumn(9).setCellRenderer(new ActionButtonRenderer("Remove Supplier", new Color(220, 38, 38)));
        supplierTable.getColumnModel().getColumn(9).setCellEditor(new SupplierActionEditor(supplierTable, "Remove Supplier", new Color(220, 38, 38), this::removeOrRestoreSupplier));
        rightAlign(supplierTable, 2, 3, 4, 5, 6);
        tableCard.add(new JScrollPane(supplierTable), BorderLayout.CENTER);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHistoryTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel title = new JLabel("Recent purchase receipts");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(ink);
        JLabel hint = new JLabel("Unit cost is retained for audit and valuation; each receipt total is quantity × unit cost.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(105, 119, 141));
        JPanel captions = new JPanel();
        captions.setOpaque(false);
        captions.setLayout(new BoxLayout(captions, BoxLayout.Y_AXIS));
        captions.add(title);
        captions.add(Box.createVerticalStrut(4));
        captions.add(hint);
        panel.add(captions, BorderLayout.NORTH);
        prepareTable(historyTable);
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {85, 150, 85, 180, 150, 65, 115, 115};
        for (int i = 0; i < widths.length; i++) historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        rightAlign(historyTable, 2, 5, 6, 7);
        panel.add(new JScrollPane(historyTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel statCard(String labelText, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(12, 13, 12, 13)));
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(new Color(105, 119, 141));
        value.setFont(new Font("Segoe UI", Font.BOLD, 22));
        value.setForeground(accent);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel form() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setPreferredSize(new Dimension(290, 0));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(0, 0, 8, 0);
        JLabel heading = new JLabel("Receive products / purchase");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 17));
        heading.setForeground(ink);
        p.add(heading, c);
        JLabel note = new JLabel("Creates a receipt and increases supplier payable.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        note.setForeground(new Color(105, 119, 141));
        c.gridy = 1; c.insets = new Insets(0, 0, 12, 0); p.add(note, c);

        field(p, c, "Product ID", id, 2);
        field(p, c, "Product name", name, 4);
        field(p, c, "Supplier", supplier, 6);
        field(p, c, "Unit purchase cost (₹ per unit)", cost, 8);
        field(p, c, "Selling price (₹ per unit)", sell, 10);
        field(p, c, "Quantity received", qty, 12);
        JLabel total = new JLabel("Receipt total = unit cost × quantity");
        total.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        total.setForeground(new Color(105, 119, 141));
        c.gridy = 14; c.insets = new Insets(4, 0, 4, 0); p.add(total, c);
        c.gridy = 15; c.insets = new Insets(12, 0, 7, 0);
        JButton add = button("Record Purchase", blue);
        add.addActionListener(e -> recordPurchase());
        p.add(add, c);
        c.gridy = 16;
        JButton clear = button("Clear Form", new Color(71, 85, 105));
        clear.addActionListener(e -> clearForm());
        p.add(clear, c);
        c.gridy = 17; c.weighty = 1; p.add(Box.createVerticalGlue(), c);
        return p;
    }

    private void field(JPanel p, GridBagConstraints c, String label, JTextField input, int row) {
        c.gridy = row;
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(new Color(105, 119, 141));
        p.add(l, c);
        c.gridy = row + 1;
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setPreferredSize(new Dimension(250, 34));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 222, 233)), new EmptyBorder(5, 9, 5, 9)));
        p.add(input, c);
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(10, 12, 10, 12));
        return b;
    }

    private void prepareTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(226, 232, 241));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 250));
        table.getTableHeader().setForeground(ink);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);
        table.setFillsViewportHeight(true);
    }

    private void rightAlign(JTable table, int... columns) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int column : columns) table.getColumnModel().getColumn(column).setCellRenderer(renderer);
    }

    private void clearForm() {
        id.setText(""); name.setText(""); supplier.setText(""); cost.setText(""); sell.setText(""); qty.setText("");
    }

    private void recordPurchase() {
        try {
            int productId = Integer.parseInt(id.getText().trim());
            int quantity = Integer.parseInt(qty.getText().trim());
            String productName = name.getText().trim(), vendor = supplier.getText().trim();
            BigDecimal unitCost = new BigDecimal(cost.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellPrice = new BigDecimal(sell.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            if (productId <= 0 || quantity <= 0 || productName.isBlank() || vendor.isBlank() ||
                    unitCost.signum() < 0 || sellPrice.signum() < 0) throw new IllegalArgumentException();
            BigDecimal receiptTotal = unitCost.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);

            try (Connection con = DbConfig.getConnection()) {
                con.setAutoCommit(false);
                try {
                    upsertProduct(con, productId, productName, vendor, unitCost, sellPrice);
                    upsertSupplierDirectory(con, vendor);
                    int existingQty = 0;
                    BigDecimal oldStockCost = BigDecimal.ZERO;
                    BigDecimal oldStockValue = BigDecimal.ZERO;
                    boolean hasStock = false;
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
                        ps.setInt(1, productId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                hasStock = true;
                                existingQty = rs.getInt("quantity");
                                oldStockCost = rs.getBigDecimal("purchase_price");
                                oldStockValue = rs.getBigDecimal("total");
                            }
                        }
                    }
                    if (hasStock) {
                        StockMovementService.assertStockMatchesLots(con, productId);
                        oldStockValue = StockMovementService.recalculateProductStock(con, productId);
                    }
                    int newQty = Math.addExact(existingQty, quantity);
                    if (oldStockValue == null) oldStockValue = (oldStockCost == null ? BigDecimal.ZERO : oldStockCost).multiply(BigDecimal.valueOf(existingQty));
                    BigDecimal newStockValue = oldStockValue.add(receiptTotal).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal averageCost = newStockValue.divide(BigDecimal.valueOf(newQty), 2, RoundingMode.HALF_UP);
                    if (hasStock) {
                        try (PreparedStatement ps = con.prepareStatement(
                                "UPDATE stock SET product_name=?, supplier=?, purchase_price=?, selling_price=?, quantity=?, total=? WHERE product_id=?")) {
                            ps.setString(1, productName); ps.setString(2, vendor); ps.setBigDecimal(3, averageCost);
                            ps.setBigDecimal(4, sellPrice); ps.setInt(5, newQty); ps.setBigDecimal(6, newStockValue); ps.setInt(7, productId);
                            ps.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ps = con.prepareStatement(
                                "INSERT INTO stock (product_id,product_name,supplier,purchase_price,selling_price,quantity,total) VALUES (?,?,?,?,?,?,?)")) {
                            ps.setInt(1, productId); ps.setString(2, productName); ps.setString(3, vendor);
                            ps.setBigDecimal(4, unitCost); ps.setBigDecimal(5, sellPrice); ps.setInt(6, quantity); ps.setBigDecimal(7, receiptTotal);
                            ps.executeUpdate();
                        }
                    }
                    long purchaseId = insertPurchaseReceipt(con, productId, productName, vendor, quantity, unitCost, receiptTotal);
                    StockMovementService.createLot(con, productId, vendor, purchaseId, quantity, unitCost,
                            "PURCHASE", "Purchase receipt from supplier " + vendor);
                    BigDecimal recalculatedValue = StockMovementService.recalculateProductStock(con, productId);
                    BigDecimal actualValueChange = recalculatedValue.subtract(oldStockValue).setScale(2, RoundingMode.HALF_UP);
                    StockMovementService.record(con, productId, "PURCHASE", quantity, actualValueChange,
                            "PURCHASE", purchaseId, "Stock received from supplier " + vendor);
                    con.commit();
                } catch (Exception ex) {
                    con.rollback();
                    throw ex;
                }
            }
            JOptionPane.showMessageDialog(this, "Purchase recorded. Stock was updated and the supplier payable increased by " + money(receiptTotal) + ".");
            clearForm();
            refreshAll();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Product ID, quantity and prices must be numeric.", "Check values", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive product ID and quantity, required text fields and valid non-negative prices.", "Check values", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            error("Could not record purchase", ex);
        }
    }

    private void upsertProduct(Connection con, int productId, String productName, String vendor,
            BigDecimal unitCost, BigDecimal sellPrice) throws SQLException {
        boolean exists;
        try (PreparedStatement ps = con.prepareStatement("SELECT PRODUCT_ID FROM product WHERE PRODUCT_ID=?")) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (exists) {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE product SET PRODUCT_NAME=?, SUPPLIER=?, PURCHASE_PRICE=?, SELLING_PRICE=? WHERE PRODUCT_ID=?")) {
                ps.setString(1, productName); ps.setString(2, vendor); ps.setBigDecimal(3, unitCost); ps.setBigDecimal(4, sellPrice); ps.setInt(5, productId); ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO product (PRODUCT_ID, PRODUCT_NAME, SUPPLIER, PURCHASE_PRICE, SELLING_PRICE) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, productId); ps.setString(2, productName); ps.setString(3, vendor); ps.setBigDecimal(4, unitCost); ps.setBigDecimal(5, sellPrice); ps.executeUpdate();
            }
        }
    }

    private long insertPurchaseReceipt(Connection con, int productId, String productName, String vendor,
            int quantity, BigDecimal unitCost, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO purchase_history (product_id,product_name,supplier,quantity,purchase_price,total_cost) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, productId); ps.setString(2, productName); ps.setString(3, vendor);
            ps.setInt(4, quantity); ps.setBigDecimal(5, unitCost); ps.setBigDecimal(6, total); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Purchase receipt ID was not generated.");
                return keys.getLong(1);
            }
        }
    }

    private void refreshAll() {
        refreshSupplierAccounts();
        refreshPurchaseHistory();
    }

    private void refreshSupplierAccounts() {
        supplierModel.setRowCount(0);
        String sql = "SELECT sup.supplier_name, sup.is_active, COALESCE(st.current_stock,0) AS current_stock, " +
                "COALESCE(p.units_received,0) AS units_received, COALESCE(p.purchase_total,0) AS purchase_total, " +
                "COALESCE(pay.amount_paid,0) AS amount_paid, " +
                "GREATEST(COALESCE(p.purchase_total,0)-COALESCE(pay.amount_paid,0),0) AS amount_due " +
                "FROM supplier_directory sup " +
                "LEFT JOIN (SELECT LOWER(TRIM(supplier)) AS supplier_key, SUM(remaining_qty) AS current_stock " +
                "FROM stock_lots WHERE remaining_qty>0 AND lot_type IN ('PURCHASE','OPENING_BALANCE') " +
                "AND supplier IS NOT NULL AND TRIM(supplier)<>'' GROUP BY LOWER(TRIM(supplier))) st " +
                "ON st.supplier_key=sup.supplier_key " +
                "LEFT JOIN (SELECT LOWER(TRIM(supplier)) AS supplier_key, SUM(quantity) AS units_received, " +
                "SUM(total_cost) AS purchase_total " +
                "FROM purchase_history GROUP BY LOWER(TRIM(supplier))) p ON p.supplier_key=sup.supplier_key " +
                "LEFT JOIN (SELECT LOWER(TRIM(supplier)) AS supplier_key, SUM(amount) AS amount_paid " +
                "FROM supplier_payments GROUP BY LOWER(TRIM(supplier))) pay ON pay.supplier_key=sup.supplier_key " +
                (showArchived.isSelected() ? "" : "WHERE sup.is_active=1 ") +
                "ORDER BY amount_due DESC, sup.is_active DESC, sup.supplier_name";
        try (Connection con = DbConfig.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String supplierName = rs.getString("supplier_name");
                boolean active = rs.getBoolean("is_active");
                int currentStock = rs.getInt("current_stock");
                int unitsReceived = rs.getInt("units_received");
                BigDecimal purchased = nz(rs.getBigDecimal("purchase_total"));
                BigDecimal paid = nz(rs.getBigDecimal("amount_paid"));
                BigDecimal due = nz(rs.getBigDecimal("amount_due"));
                supplierModel.addRow(new Object[]{supplierName, active ? "Active" : "Archived", currentStock, unitsReceived,
                        money(purchased), money(paid), money(due), "View Products", "Record Payment",
                        active ? "Remove Supplier" : "Restore Supplier"});
            }

            // KPI totals always cover active and archived suppliers; hiding archived rows must not hide liabilities.
            String totalSql = "SELECT COALESCE(SUM(p.purchase_total),0) AS total_purchases, " +
                    "COALESCE(SUM(pay.amount_paid),0) AS total_paid, " +
                    "COALESCE(SUM(GREATEST(COALESCE(p.purchase_total,0)-COALESCE(pay.amount_paid,0),0)),0) AS total_due " +
                    "FROM (SELECT LOWER(TRIM(supplier)) AS supplier_key FROM purchase_history " +
                    "UNION SELECT LOWER(TRIM(supplier)) AS supplier_key FROM supplier_payments) all_sup " +
                    "LEFT JOIN (SELECT LOWER(TRIM(supplier)) AS supplier_key, SUM(total_cost) AS purchase_total " +
                    "FROM purchase_history GROUP BY LOWER(TRIM(supplier))) p ON p.supplier_key=all_sup.supplier_key " +
                    "LEFT JOIN (SELECT LOWER(TRIM(supplier)) AS supplier_key, SUM(amount) AS amount_paid " +
                    "FROM supplier_payments GROUP BY LOWER(TRIM(supplier))) pay ON pay.supplier_key=all_sup.supplier_key";
            try (Statement st = con.createStatement(); ResultSet totals = st.executeQuery(totalSql)) {
                if (totals.next()) {
                    purchasesValue.setText(money(nz(totals.getBigDecimal("total_purchases"))));
                    dueValue.setText(money(nz(totals.getBigDecimal("total_due"))));
                    dueValue.setToolTipText("Recorded purchases " + purchasesValue.getText() +
                            " less recorded payments " + money(nz(totals.getBigDecimal("total_paid"))) +
                            ". Totals include archived suppliers.");
                }
            }
            try (Statement st = con.createStatement(); ResultSet count = st.executeQuery(
                    "SELECT COUNT(*) AS active_count FROM supplier_directory WHERE is_active=1")) {
                if (count.next()) suppliersValue.setText(Integer.toString(count.getInt("active_count")));
            }
        } catch (SQLException ex) {
            error("Could not load supplier accounts", ex);
        }
    }

    private void upsertSupplierDirectory(Connection con, String supplierName) throws SQLException {
        String normalized = supplierName.trim().toLowerCase(Locale.ROOT);
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO supplier_directory (supplier_key,supplier_name,is_active) VALUES (?,?,1) " +
                "ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name), is_active=1")) {
            ps.setString(1, normalized);
            ps.setString(2, supplierName.trim());
            ps.executeUpdate();
        }
    }

    private void removeOrRestoreSupplier(String supplierName) {
        try (Connection con = DbConfig.getConnection()) {
            String key = supplierName.trim().toLowerCase(Locale.ROOT);
            boolean active = true;
            try (PreparedStatement ps = con.prepareStatement("SELECT is_active FROM supplier_directory WHERE supplier_key=?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Supplier record was not found. Refresh the page and try again.", "Supplier not found", JOptionPane.WARNING_MESSAGE);
                        refreshSupplierAccounts();
                        return;
                    }
                    active = rs.getBoolean("is_active");
                }
            }
            if (!active) {
                int answer = JOptionPane.showConfirmDialog(this,
                        "Restore supplier '" + supplierName + "'? It will appear in the active supplier list again.",
                        "Restore supplier", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (answer != JOptionPane.YES_OPTION) return;
                try (PreparedStatement ps = con.prepareStatement("UPDATE supplier_directory SET is_active=1 WHERE supplier_key=?")) {
                    ps.setString(1, key); ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, supplierName + " has been restored.", "Supplier restored", JOptionPane.INFORMATION_MESSAGE);
                refreshSupplierAccounts();
                return;
            }

            int stockUnits = 0, stockRows = 0, purchaseCount = 0, paymentCount = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(DISTINCT product_id) AS stock_rows, COALESCE(SUM(remaining_qty),0) AS stock_units FROM stock_lots WHERE LOWER(TRIM(supplier))=LOWER(TRIM(?)) AND remaining_qty>0 AND lot_type IN ('PURCHASE','OPENING_BALANCE')")) {
                ps.setString(1, supplierName); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) { stockRows = rs.getInt("stock_rows"); stockUnits = rs.getInt("stock_units"); } }
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM purchase_history WHERE LOWER(TRIM(supplier))=LOWER(TRIM(?))")) {
                ps.setString(1, supplierName); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) purchaseCount = rs.getInt(1); }
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM supplier_payments WHERE LOWER(TRIM(supplier))=LOWER(TRIM(?))")) {
                ps.setString(1, supplierName); try (ResultSet rs = ps.executeQuery()) { if (rs.next()) paymentCount = rs.getInt(1); }
            }
            BigDecimal due = getOutstanding(con, supplierName);
            boolean hasRecords = stockRows > 0 || purchaseCount > 0 || paymentCount > 0;
            String message;
            if (hasRecords) {
                message = "Supplier: " + supplierName + "\n\n" +
                        "Current units in stock: " + stockUnits + "\n" +
                        "Stock product records: " + stockRows + "\n" +
                        "Purchase receipts: " + purchaseCount + "\n" +
                        "Payment records: " + paymentCount + "\n" +
                        "Amount due: " + money(due) + "\n\n" +
                        "This supplier has linked records, so it will be ARCHIVED rather than permanently deleted. " +
                        "Past purchases and payments will be preserved. Turn on 'Show archived suppliers' to view it or record a remaining payment.\n\nContinue?";
            } else {
                message = "Remove supplier '" + supplierName + "'? This supplier has no stock, purchases or payment records, so its directory entry can be deleted.";
            }
            int answer = JOptionPane.showConfirmDialog(this, message, hasRecords ? "Archive supplier" : "Remove supplier",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) return;

            if (hasRecords) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE supplier_directory SET is_active=0 WHERE supplier_key=?")) {
                    ps.setString(1, key); ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, supplierName + " has been archived. Historical data was kept.", "Supplier archived", JOptionPane.INFORMATION_MESSAGE);
            } else {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM supplier_directory WHERE supplier_key=?")) {
                    ps.setString(1, key); ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, supplierName + " has been removed.", "Supplier removed", JOptionPane.INFORMATION_MESSAGE);
            }
            refreshAll();
        } catch (SQLException ex) {
            error("Could not remove or restore supplier", ex);
        }
    }

    private void refreshPurchaseHistory() {
        historyModel.setRowCount(0);
        String sql = "SELECT purchase_id, purchase_date, product_id, product_name, supplier, quantity, purchase_price, " +
                "total_cost AS calculated_total FROM purchase_history ORDER BY purchase_id DESC LIMIT 500";
        try (Connection con = DbConfig.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                historyModel.addRow(new Object[]{rs.getInt("purchase_id"), rs.getTimestamp("purchase_date"), rs.getInt("product_id"),
                        rs.getString("product_name"), rs.getString("supplier"), rs.getInt("quantity"),
                        money(nz(rs.getBigDecimal("purchase_price"))), money(nz(rs.getBigDecimal("calculated_total")))});
            }
        } catch (SQLException ex) {
            error("Could not load purchase history", ex);
        }
    }

    private void showSupplierProducts(String supplierName) {
        DefaultTableModel products = new DefaultTableModel(
                new String[]{"Product ID", "Product", "Units Received", "Units Remaining from Supplier*", "Unit Cost (₹)", "Purchase Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        String historySql = "SELECT ph.product_id, ph.product_name, SUM(ph.quantity) AS bought_qty, " +
                "COALESCE((SELECT SUM(l.remaining_qty) FROM stock_lots l WHERE l.product_id=ph.product_id " +
                "AND LOWER(TRIM(l.supplier))=LOWER(TRIM(ph.supplier)) AND l.lot_type IN ('PURCHASE','OPENING_BALANCE')),0) AS current_qty, " +
                "(SELECT ph2.purchase_price FROM purchase_history ph2 WHERE ph2.product_id=ph.product_id " +
                "AND LOWER(TRIM(ph2.supplier))=LOWER(TRIM(ph.supplier)) ORDER BY ph2.purchase_date DESC, ph2.purchase_id DESC LIMIT 1) AS latest_cost, " +
                "SUM(ph.total_cost) AS purchase_value " +
                "FROM purchase_history ph WHERE LOWER(TRIM(ph.supplier))=LOWER(TRIM(?)) " +
                "GROUP BY ph.product_id, ph.product_name, ph.supplier ORDER BY ph.product_name";
        String stockOnlySql = "SELECT s.product_id, s.product_name, SUM(l.received_qty) AS received_qty, " +
                "SUM(l.remaining_qty) AS remaining_qty, MAX(l.unit_cost) AS unit_cost, " +
                "SUM(ROUND(l.received_qty * l.unit_cost,2)) AS lot_value FROM stock_lots l " +
                "JOIN stock s ON s.product_id=l.product_id " +
                "WHERE LOWER(TRIM(l.supplier))=LOWER(TRIM(?)) AND l.lot_type IN ('PURCHASE','OPENING_BALANCE') " +
                "AND NOT EXISTS (SELECT 1 FROM purchase_history ph WHERE ph.product_id=l.product_id AND LOWER(TRIM(ph.supplier))=LOWER(TRIM(l.supplier))) " +
                "GROUP BY s.product_id, s.product_name ORDER BY s.product_name";
        try (Connection con = DbConfig.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(historySql)) {
                ps.setString(1, supplierName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) products.addRow(new Object[]{rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getInt("bought_qty"), rs.getInt("current_qty"), money(nz(rs.getBigDecimal("latest_cost"))),
                            money(nz(rs.getBigDecimal("purchase_value")))});
                }
            }
            try (PreparedStatement ps = con.prepareStatement(stockOnlySql)) {
                ps.setString(1, supplierName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) products.addRow(new Object[]{rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getInt("received_qty"), rs.getInt("remaining_qty"), money(nz(rs.getBigDecimal("unit_cost"))),
                            money(nz(rs.getBigDecimal("lot_value")))});
                }
            }
            JTable table = new JTable(products);
            prepareTable(table);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            int[] widths = {80, 190, 170, 120, 120, 130};
            for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(820, Math.max(115, Math.min(390, 75 + products.getRowCount() * 34))));
            JPanel content = new JPanel(new BorderLayout(0, 8));
            content.add(scroll, BorderLayout.CENTER);
            JLabel note = new JLabel("*Remaining units are tracked by supplier lot (FIFO) for movements recorded after lot tracking was enabled. Legacy stock is assigned to the supplier recorded in the old stock row because its original source lots cannot be reconstructed reliably.");
            note.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            note.setForeground(new Color(105, 119, 141));
            content.add(note, BorderLayout.SOUTH);
            String title = "Products supplied by " + supplierName;
            JOptionPane.showMessageDialog(this, products.getRowCount() == 0 ? new JLabel("No products or purchase records found for this supplier.") : content,
                    title, JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            error("Could not load products for " + supplierName, ex);
        }
    }

    private void recordSupplierPayment(String supplierName) {
        BigDecimal outstanding;
        try (Connection con = DbConfig.getConnection()) {
            outstanding = getOutstanding(con, supplierName);
        } catch (SQLException ex) {
            error("Could not calculate supplier balance", ex);
            return;
        }
        if (outstanding.signum() <= 0) {
            JOptionPane.showMessageDialog(this, supplierName + " has no outstanding payable. Current balance: " + money(outstanding), "No payment due", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextField amount = new JTextField(outstanding.setScale(2, RoundingMode.HALF_UP).toPlainString(), 14);
        JTextField note = new JTextField(20);
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(5, 5, 5, 5); c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Supplier"), c); c.gridx = 1; form.add(new JLabel(supplierName), c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Current amount due"), c); c.gridx = 1; form.add(new JLabel(money(outstanding)), c);
        c.gridx = 0; c.gridy = 2; form.add(new JLabel("Payment amount (₹)"), c); c.gridx = 1; form.add(amount, c);
        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Note / reference"), c); c.gridx = 1; form.add(note, c);
        int result = JOptionPane.showConfirmDialog(this, form, "Record supplier payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;
        try {
            BigDecimal paid = new BigDecimal(amount.getText().trim()).setScale(2, RoundingMode.HALF_UP);
            if (paid.signum() <= 0) throw new IllegalArgumentException("Payment amount must be greater than zero.");
            try (Connection con = DbConfig.getConnection()) {
                con.setAutoCommit(false);
                try {
                    String supplierKey = supplierName.trim().toLowerCase(Locale.ROOT);
                    try (PreparedStatement lockSupplier = con.prepareStatement(
                            "SELECT supplier_key FROM supplier_directory WHERE supplier_key=? FOR UPDATE")) {
                        lockSupplier.setString(1, supplierKey);
                        try (ResultSet rs = lockSupplier.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Supplier account was not found. Refresh the supplier list and try again.");
                        }
                    }
                    BigDecimal currentDue = getOutstanding(con, supplierName);
                    if (paid.compareTo(currentDue) > 0) throw new IllegalArgumentException("Payment cannot exceed the current outstanding amount of " + money(currentDue) + ".");
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO supplier_payments (supplier, amount, note) VALUES (?,?,?)")) {
                        ps.setString(1, supplierName); ps.setBigDecimal(2, paid);
                        String n = note.getText().trim(); if (n.isEmpty()) ps.setNull(3, Types.VARCHAR); else ps.setString(3, n);
                        ps.executeUpdate();
                    }
                    con.commit();
                } catch (Exception ex) {
                    con.rollback(); throw ex;
                }
            }
            JOptionPane.showMessageDialog(this, "Payment of " + money(paid) + " recorded for " + supplierName + ".");
            refreshSupplierAccounts();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid payment amount.", "Check amount", JOptionPane.WARNING_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Check payment", JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            error("Could not record supplier payment", ex);
        }
    }

    private BigDecimal getOutstanding(Connection con, String supplierName) throws SQLException {
        String sql = "SELECT " +
                "GREATEST(COALESCE((SELECT SUM(total_cost) FROM purchase_history WHERE LOWER(TRIM(supplier))=LOWER(TRIM(?))),0) - " +
                "COALESCE((SELECT SUM(amount) FROM supplier_payments WHERE LOWER(TRIM(supplier))=LOWER(TRIM(?))),0), 0) AS amount_due";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, supplierName); ps.setString(2, supplierName);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? nz(rs.getBigDecimal("amount_due")) : BigDecimal.ZERO; }
        }
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private String money(BigDecimal amount) { return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private void error(String message, Exception ex) {
        JOptionPane.showMessageDialog(this, message + ".\n" + ex.getMessage(), "Purchase error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ActionButtonRenderer extends JButton implements TableCellRenderer {
        private final Color baseColor;
        ActionButtonRenderer(String text, Color background) {
            this.baseColor = background;
            // Use BasicButtonUI instead of the Windows native renderer, which can ignore
            // JButton background/foreground settings inside JTable cells.
            setUI(new BasicButtonUI());
            setText(text);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setForeground(Color.WHITE);
            setBackground(background);
            setOpaque(true);
            setContentAreaFilled(true);
            setBorderPainted(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(background.darker()),
                    new EmptyBorder(6, 8, 6, 8)));
            setFocusPainted(false);
            setRolloverEnabled(false);
            setEnabled(true);
        }
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
            setText(value == null ? "" : value.toString());
            Color actionColor = "Restore Supplier".equals(getText()) ? new Color(5, 150, 105) : baseColor;
            setBackground(actionColor);
            setForeground(Color.WHITE);
            setOpaque(true);
            setContentAreaFilled(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(actionColor.darker()),
                    new EmptyBorder(6, 8, 6, 8)));
            return this;
        }
    }

    private static class SupplierActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JTable table;
        private final JButton button = new JButton();
        private final Color background;
        private final Consumer<String> action;
        private String supplierName;
        private String text;

        SupplierActionEditor(JTable table, String label, Color background, Consumer<String> action) {
            this.table = table; this.text = label; this.background = background; this.action = action;
            // Force a UI delegate that paints the explicit action colors consistently.
            button.setUI(new BasicButtonUI());
            button.setText(label);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(background);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(background.darker()),
                    new EmptyBorder(6, 8, 6, 8)));
            button.setFocusPainted(false);
            button.setRolloverEnabled(false);
            button.addActionListener(e -> {
                String target = supplierName;
                fireEditingStopped();
                if (target != null) SwingUtilities.invokeLater(() -> this.action.accept(target));
            });
        }
        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean selected, int row, int column) {
            this.supplierName = table.getValueAt(row, 0).toString();
            this.text = value == null ? "" : value.toString();
            button.setText(text);
            Color actionColor = "Restore Supplier".equals(text) ? new Color(5, 150, 105) : background;
            button.setBackground(actionColor);
            button.setForeground(Color.WHITE);
            button.setOpaque(true);
            button.setContentAreaFilled(true);
            button.setBorderPainted(true);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(actionColor.darker()),
                    new EmptyBorder(6, 8, 6, 8)));
            return button;
        }
        @Override public Object getCellEditorValue() { return text; }
    }
}
