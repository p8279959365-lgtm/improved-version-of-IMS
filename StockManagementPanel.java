import java.awt.*;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public class StockManagementPanel extends JPanel {
    private final Color ink = new Color(23, 37, 63), muted = new Color(105, 119, 141), blue = new Color(37, 99, 235);
    private final JTextField search = new JTextField();
    private final JTextField id = new JTextField(), name = new JTextField(), supplier = new JTextField();
    private final JTextField purchasePrice = new JTextField(), sellingPrice = new JTextField(), quantity = new JTextField();
    private final JLabel totalLabel = new JLabel("Inventory value: ₹0.00");
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Product ID", "Product Name", "Supplier", "Purchase Price", "Selling Price", "Quantity", "Total Cost"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private Integer selectedId;

    public StockManagementPanel() {
        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(245, 247, 251));
        setBorder(new EmptyBorder(20, 22, 22, 22));
        ensurePurchaseLedgerTables();
        add(createHeader(), BorderLayout.NORTH);
        add(createContent(), BorderLayout.CENTER);
        loadStock();
    }

    private void ensurePurchaseLedgerTables() {
        String history = "CREATE TABLE IF NOT EXISTS purchase_history (" +
                "purchase_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, product_id INT NOT NULL, " +
                "product_name VARCHAR(200) NOT NULL, supplier VARCHAR(200) NOT NULL, quantity INT NOT NULL, " +
                "purchase_price DECIMAL(12,2) NOT NULL, total_cost DECIMAL(14,2) NOT NULL, " +
                "purchase_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB";
        String payments = "CREATE TABLE IF NOT EXISTS supplier_payments (" +
                "payment_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, supplier VARCHAR(200) NOT NULL, " +
                "amount DECIMAL(14,2) NOT NULL, payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "note VARCHAR(500) NULL, INDEX idx_supplier_payments_supplier (supplier)) ENGINE=InnoDB";
        String suppliers = "CREATE TABLE IF NOT EXISTS supplier_directory (" +
                "supplier_key VARCHAR(200) NOT NULL PRIMARY KEY, supplier_name VARCHAR(200) NOT NULL, " +
                "is_active TINYINT(1) NOT NULL DEFAULT 1, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB";
        try (Connection con = DbConfig.getConnection(); Statement st = con.createStatement()) {
            st.execute(history);
            st.execute(payments);
            st.execute(suppliers);
            StockMovementService.ensureSchema(con);
            DbConfig.ensureBillStockMovementTable(con);
        } catch (SQLException ex) {
            showError("Could not prepare supplier purchase ledger", ex);
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 8)); header.setOpaque(false);
        JPanel titles = new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Stock Management"); title.setFont(new Font("Segoe UI", Font.BOLD, 27)); title.setForeground(ink);
        JLabel sub = new JLabel("Track quantity on hand, supplier and stock cost."); sub.setFont(new Font("Segoe UI", Font.PLAIN, 13)); sub.setForeground(muted);
        titles.add(title); titles.add(Box.createVerticalStrut(4)); titles.add(sub);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); tools.setOpaque(false);
        search.setPreferredSize(new Dimension(230, 38)); search.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        search.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(214, 222, 233)), new EmptyBorder(7, 10, 7, 10)));
        search.putClientProperty("JTextField.placeholderText", "Search stock...");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); } public void removeUpdate(DocumentEvent e) { filter(); } public void changedUpdate(DocumentEvent e) { filter(); }
        });
        JButton export = button("Export CSV", new Color(71, 85, 105)); export.addActionListener(e -> exportCsv());
        JButton reconcile = button("Reconcile Stock", new Color(124, 58, 237)); reconcile.addActionListener(e -> reconcileStock());
        JButton movements = button("Movement History", new Color(71, 85, 105)); movements.addActionListener(e -> viewMovementHistory());
        JButton refresh = button("Refresh", blue); refresh.addActionListener(e -> loadStock());
        tools.add(search); tools.add(reconcile); tools.add(movements); tools.add(export); tools.add(refresh);
        header.add(titles, BorderLayout.WEST); header.add(tools, BorderLayout.EAST); return header;
    }

    private JComponent createContent() {
        JPanel content = new JPanel(new BorderLayout(16, 0)); content.setOpaque(false);
        JPanel tableCard = card(); tableCard.setLayout(new BorderLayout(0, 12));
        JPanel tableHead = new JPanel(new BorderLayout()); tableHead.setOpaque(false);
        JLabel tableTitle = new JLabel("Current Stock"); tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 17)); tableTitle.setForeground(ink);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 13)); totalLabel.setForeground(new Color(22, 101, 52));
        tableHead.add(tableTitle, BorderLayout.WEST); tableHead.add(totalLabel, BorderLayout.EAST); tableCard.add(tableHead, BorderLayout.NORTH);
        table.setRowHeight(34); table.setFont(new Font("Segoe UI", Font.PLAIN, 12)); table.setForeground(ink); table.setGridColor(new Color(235, 239, 245)); table.setShowVerticalLines(false); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.setRowSorter(sorter);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12)); table.getTableHeader().setBackground(new Color(241, 245, 250)); table.getTableHeader().setForeground(ink); table.getTableHeader().setPreferredSize(new Dimension(0, 38)); table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillForm(); });
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 241))); tableCard.add(scroll, BorderLayout.CENTER);
        JLabel hint = new JLabel("Tip: selecting a row loads it into the form. Add stock increases the existing quantity for the same product ID."); hint.setFont(new Font("Segoe UI", Font.PLAIN, 11)); hint.setForeground(muted); tableCard.add(hint, BorderLayout.SOUTH);
        content.add(tableCard, BorderLayout.CENTER); content.add(createForm(), BorderLayout.EAST); return content;
    }

    private JPanel createForm() {
        JPanel form = card(); form.setPreferredSize(new Dimension(310, 0)); form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1; c.insets = new Insets(0, 0, 7, 0);
        JLabel title = new JLabel("Stock details"); title.setFont(new Font("Segoe UI", Font.BOLD, 17)); title.setForeground(ink); c.gridy = 0; form.add(title, c);
        JLabel desc = new JLabel("Add new stock or edit selected item"); desc.setFont(new Font("Segoe UI", Font.PLAIN, 11)); desc.setForeground(muted); c.gridy = 1; form.add(desc, c);
        c.gridwidth = 1; c.insets = new Insets(6, 0, 3, 7); c.weightx = 0.35;
        addFieldLabel(form, c, "Product ID", 2); addField(form, c, id, 2);
        addFieldLabel(form, c, "Product name", 4); addField(form, c, name, 4);
        addFieldLabel(form, c, "Supplier", 6); addField(form, c, supplier, 6);
        addFieldLabel(form, c, "Unit purchase cost for new stock (₹)", 8); addField(form, c, purchasePrice, 8);
        addFieldLabel(form, c, "Selling price (₹)", 10); addField(form, c, sellingPrice, 10);
        addFieldLabel(form, c, "Quantity", 12); addField(form, c, quantity, 12);
        c.gridx = 0; c.gridy = 14; c.gridwidth = 2; c.insets = new Insets(14, 0, 6, 0); c.weightx = 1;
        JButton add = button("＋  Add Stock", blue); add.addActionListener(e -> addStock()); form.add(add, c);
        c.gridy = 15; JButton update = button("Save Changes", new Color(15, 118, 110)); update.addActionListener(e -> updateStock()); form.add(update, c);
        c.gridy = 16; JButton adjust = button("Adjust Stock (+ / −)", new Color(124, 58, 237)); adjust.addActionListener(e -> adjustStock()); form.add(adjust, c);
        c.gridy = 17; JButton delete = button("Remove Stock Record", new Color(185, 28, 28)); delete.addActionListener(e -> deleteStock()); form.add(delete, c);
        c.gridy = 18; JButton clear = button("Clear Form", new Color(71, 85, 105)); clear.addActionListener(e -> clearForm()); form.add(clear, c);
        c.gridx = 0; c.gridy = 19; c.gridwidth = 2; c.weightx = 1; c.weighty = 0; c.insets = new Insets(8, 0, 4, 0);
        JLabel ledgerHint = new JLabel("<html>Purchases increase stock and supplier payables. Billing deducts sold units. Use Adjust Stock for damage, loss or count corrections; it does not create a supplier bill.</html>");
        ledgerHint.setFont(new Font("Segoe UI", Font.PLAIN, 10)); ledgerHint.setForeground(muted); form.add(ledgerHint, c);
        c.gridy = 20; c.weighty = 1; form.add(Box.createVerticalGlue(), c);
        return form;
    }

    private void addFieldLabel(JPanel panel, GridBagConstraints c, String label, int row) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1; c.insets = new Insets(8, 0, 3, 0);
        JLabel l = new JLabel(label); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(muted); panel.add(l, c);
    }
    private void addField(JPanel panel, GridBagConstraints c, JTextField field, int row) {
        c.gridx = 0; c.gridy = row + 1; c.gridwidth = 2; c.insets = new Insets(0, 0, 3, 0); c.weightx = 1;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13)); field.setPreferredSize(new Dimension(250, 34)); field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(214, 222, 233)), new EmptyBorder(5, 9, 5, 9))); panel.add(field, c);
    }

    private JPanel card() { JPanel p = new JPanel(); p.setBackground(Color.WHITE); p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226, 232, 241)), new EmptyBorder(16, 16, 16, 16))); return p; }
    private JButton button(String text, Color color) { JButton b = new JButton(text); b.setFont(new Font("Segoe UI", Font.BOLD, 12)); b.setForeground(Color.WHITE); b.setBackground(color); b.setFocusPainted(false); b.setBorder(new EmptyBorder(9, 12, 9, 12)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b; }
    private void filter() { String q = search.getText().trim(); sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q))); }

    private void loadStock() {
        model.setRowCount(0);
        String sql = "SELECT product_id, product_name, supplier, purchase_price, selling_price, quantity, total FROM stock ORDER BY product_name";
        BigDecimal total = BigDecimal.ZERO;
        try (Connection con = DbConfig.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) { model.addRow(new Object[]{rs.getInt("product_id"), rs.getString("product_name"), rs.getString("supplier"), rs.getBigDecimal("purchase_price"), rs.getBigDecimal("selling_price"), rs.getInt("quantity"), rs.getBigDecimal("total")}); if (rs.getBigDecimal("total") != null) total = total.add(rs.getBigDecimal("total")); }
            totalLabel.setText("Inventory value: " + money(total));
        } catch (SQLException ex) { showError("Could not load stock", ex); }
        clearForm();
    }

    private void fillForm() {
        int viewRow = table.getSelectedRow(); if (viewRow < 0) return;
        int row = table.convertRowIndexToModel(viewRow);
        selectedId = Integer.valueOf(model.getValueAt(row, 0).toString());
        id.setText(selectedId.toString()); name.setText(str(row, 1)); supplier.setText(str(row, 2)); purchasePrice.setText(str(row, 3)); sellingPrice.setText(str(row, 4)); quantity.setText(str(row, 5));
    }
    private String str(int row, int col) { Object v = model.getValueAt(row, col); return v == null ? "" : v.toString(); }

    private StockValues validateFormValues() {
        try {
            int productId = Integer.parseInt(id.getText().trim());
            String productName = name.getText().trim(), vendor = supplier.getText().trim();
            BigDecimal cost = new BigDecimal(purchasePrice.getText().trim()).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal price = new BigDecimal(sellingPrice.getText().trim()).setScale(2, java.math.RoundingMode.HALF_UP);
            int qty = Integer.parseInt(quantity.getText().trim());
            if (productId <= 0 || productName.isBlank() || vendor.isBlank() || cost.signum() < 0 || price.signum() < 0 || qty < 0) throw new IllegalArgumentException();
            return new StockValues(productId, productName, vendor, cost, price, qty);
        } catch (RuntimeException ex) { JOptionPane.showMessageDialog(this, "Enter a positive product ID, product name/supplier, non-negative quantity, and valid non-negative prices.", "Check stock details", JOptionPane.WARNING_MESSAGE); return null; }
    }

    private void addStock() {
        StockValues v = validateFormValues(); if (v == null) return;
        if (selectedId == null && v.quantity <= 0) {
            JOptionPane.showMessageDialog(this, "Incoming quantity must be greater than zero.", "Check quantity", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (selectedId != null) {
            if (v.id != selectedId) {
                JOptionPane.showMessageDialog(this, "Product ID is fixed for the selected record. Clear the form to add a different product.");
                return;
            }
            String addition = JOptionPane.showInputDialog(this,
                    "How many additional units should be received for " + v.name + "?\nThis receipt will also increase the supplier payable.", "1");
            if (addition == null) return;
            int incoming;
            try {
                incoming = Integer.parseInt(addition.trim());
                if (incoming <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Additional quantity must be a positive whole number.", "Check quantity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String costInput = JOptionPane.showInputDialog(this,
                    "Enter the actual unit purchase cost on this supplier invoice (₹ per unit).\nCurrent stock uses weighted-average cost ₹" + v.cost.toPlainString() + ".",
                    v.cost.toPlainString());
            if (costInput == null) return;
            BigDecimal invoiceUnitCost;
            try {
                invoiceUnitCost = new BigDecimal(costInput.trim()).setScale(2, java.math.RoundingMode.HALF_UP);
                if (invoiceUnitCost.signum() < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Unit purchase cost must be a valid non-negative amount.", "Check purchase cost", JOptionPane.WARNING_MESSAGE);
                return;
            }
            v = new StockValues(v.id, v.name, v.supplier, invoiceUnitCost, v.price, incoming);
        }
        BigDecimal receiptCost = v.cost.multiply(BigDecimal.valueOf(v.quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
        try (Connection con = DbConfig.getConnection()) {
            // Do all idempotent DDL before starting the transaction (MySQL DDL may implicitly commit).
            StockMovementService.ensureSchema(con);
            DbConfig.ensureBillStockMovementTable(con);
            ensureSupplierDirectoryTable(con);
            con.setAutoCommit(false);
            try {
                upsertProduct(con, v);
                upsertSupplierDirectory(con, v.supplier);
                int existingQty = -1;
                BigDecimal existingUnitCost = BigDecimal.ZERO;
                BigDecimal existingTotal = BigDecimal.ZERO;
                BigDecimal previousInventoryValue = BigDecimal.ZERO;
                try (PreparedStatement ps = con.prepareStatement("SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
                    ps.setInt(1, v.id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingQty = rs.getInt("quantity");
                            existingUnitCost = rs.getBigDecimal("purchase_price");
                            existingTotal = rs.getBigDecimal("total");
                        }
                    }
                }
                if (existingQty >= 0) {
                    StockMovementService.assertStockMatchesLots(con, v.id);
                    existingTotal = StockMovementService.recalculateProductStock(con, v.id);
                    if (existingTotal == null) existingTotal = (existingUnitCost == null ? BigDecimal.ZERO : existingUnitCost).multiply(BigDecimal.valueOf(existingQty));
                    previousInventoryValue = existingTotal;
                    int newQty = Math.addExact(existingQty, v.quantity);
                    BigDecimal newTotal = existingTotal.add(receiptCost).setScale(2, java.math.RoundingMode.HALF_UP);
                    BigDecimal weightedAverage = newTotal.divide(BigDecimal.valueOf(newQty), 2, java.math.RoundingMode.HALF_UP);
                    try (PreparedStatement ps = con.prepareStatement("UPDATE stock SET product_name=?, supplier=?, purchase_price=?, selling_price=?, quantity=?, total=? WHERE product_id=?")) {
                        ps.setString(1, v.name); ps.setString(2, v.supplier); ps.setBigDecimal(3, weightedAverage);
                        ps.setBigDecimal(4, v.price); ps.setInt(5, newQty); ps.setBigDecimal(6, newTotal); ps.setInt(7, v.id); ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO stock (product_id, product_name, supplier, purchase_price, selling_price, quantity, total) VALUES (?,?,?,?,?,?,?)")) {
                        ps.setInt(1, v.id); ps.setString(2, v.name); ps.setString(3, v.supplier); ps.setBigDecimal(4, v.cost);
                        ps.setBigDecimal(5, v.price); ps.setInt(6, v.quantity); ps.setBigDecimal(7, receiptCost); ps.executeUpdate();
                    }
                }
                long purchaseId;
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO purchase_history (product_id, product_name, supplier, quantity, purchase_price, total_cost) VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, v.id); ps.setString(2, v.name); ps.setString(3, v.supplier); ps.setInt(4, v.quantity);
                    ps.setBigDecimal(5, v.cost); ps.setBigDecimal(6, receiptCost); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Purchase receipt ID was not generated.");
                        purchaseId = keys.getLong(1);
                    }
                }
                StockMovementService.createLot(con, v.id, v.supplier, purchaseId, v.quantity, v.cost,
                        "PURCHASE", "Purchase receipt from supplier " + v.supplier);
                BigDecimal recalculatedValue = StockMovementService.recalculateProductStock(con, v.id);
                BigDecimal actualValueChange = recalculatedValue.subtract(previousInventoryValue).setScale(2, java.math.RoundingMode.HALF_UP);
                StockMovementService.record(con, v.id, "PURCHASE", v.quantity, actualValueChange,
                        "PURCHASE", purchaseId, "Stock received from supplier " + v.supplier);
                con.commit();
                JOptionPane.showMessageDialog(this, "Stock received successfully. Purchase of " + money(receiptCost) + " was added to " + v.supplier + "'s payable ledger.");
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (Exception ex) { showError("Could not add stock / record supplier purchase", ex); return; }
        loadStock();
    }

    private void updateStock() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a stock record first."); return; }
        StockValues v = validateFormValues(); if (v == null) return;
        if (v.id != selectedId) { JOptionPane.showMessageDialog(this, "Product ID is fixed for an existing stock record. To create a new product, clear the form and add it separately."); return; }
        try (Connection con = DbConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                int currentQty;
                BigDecimal currentCost, currentTotal;
                try (PreparedStatement ps = con.prepareStatement("SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
                    ps.setInt(1, selectedId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Stock record no longer exists.");
                        currentQty = rs.getInt("quantity");
                        currentCost = rs.getBigDecimal("purchase_price");
                        currentTotal = rs.getBigDecimal("total");
                    }
                }
                if (v.quantity != currentQty) {
                    throw new SQLException("Quantity cannot be edited through Save Changes. Use Add Stock, Billing, or Adjust Stock so the change is recorded in the movement history.");
                }
                if (currentCost == null) currentCost = BigDecimal.ZERO;
                if (v.cost.compareTo(currentCost) != 0) {
                    throw new SQLException("Purchase cost here is the current weighted-average inventory cost. Do not edit it directly; record an actual purchase or use Adjust Stock.");
                }
                if (currentTotal == null) currentTotal = currentCost.multiply(BigDecimal.valueOf(currentQty)).setScale(2, java.math.RoundingMode.HALF_UP);
                try (PreparedStatement ps = con.prepareStatement("UPDATE stock SET product_name=?, supplier=?, selling_price=? WHERE product_id=?")) {
                    ps.setString(1, v.name); ps.setString(2, v.supplier); ps.setBigDecimal(3, v.price); ps.setInt(4, v.id);
                    if (ps.executeUpdate() == 0) throw new SQLException("Stock record no longer exists.");
                }
                updateProductMetadata(con, v);
                con.commit();
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (Exception ex) { showError("Could not update stock", ex); return; }
        JOptionPane.showMessageDialog(this, "Product details saved. Quantity and inventory valuation were preserved."); loadStock();
    }

    private void updateProductMetadata(Connection con, StockValues v) throws SQLException {
        boolean exists;
        try (PreparedStatement ps = con.prepareStatement("SELECT PRODUCT_ID FROM product WHERE PRODUCT_ID=?")) {
            ps.setInt(1, v.id); try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (exists) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE product SET PRODUCT_NAME=?, SUPPLIER=?, SELLING_PRICE=? WHERE PRODUCT_ID=?")) {
                ps.setString(1, v.name); ps.setString(2, v.supplier); ps.setBigDecimal(3, v.price); ps.setInt(4, v.id); ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO product (PRODUCT_ID, PRODUCT_NAME, SUPPLIER, PURCHASE_PRICE, SELLING_PRICE) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, v.id); ps.setString(2, v.name); ps.setString(3, v.supplier); ps.setBigDecimal(4, v.cost); ps.setBigDecimal(5, v.price); ps.executeUpdate();
            }
        }
    }

    private void ensureSupplierDirectoryTable(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS supplier_directory (" +
                    "supplier_key VARCHAR(200) NOT NULL PRIMARY KEY, supplier_name VARCHAR(200) NOT NULL, " +
                    "is_active TINYINT(1) NOT NULL DEFAULT 1, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB");
        }
    }

    private void upsertSupplierDirectory(Connection con, String supplierName) throws SQLException {
        String normalized = supplierName.trim().toLowerCase(java.util.Locale.ROOT);
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO supplier_directory (supplier_key, supplier_name, is_active) VALUES (?,?,1) " +
                "ON DUPLICATE KEY UPDATE supplier_name=VALUES(supplier_name), is_active=1")) {
            ps.setString(1, normalized); ps.setString(2, supplierName.trim()); ps.executeUpdate();
        }
    }

    private void upsertProduct(Connection con, StockValues v) throws SQLException {
        boolean exists;
        try (PreparedStatement ps = con.prepareStatement("SELECT PRODUCT_ID FROM product WHERE PRODUCT_ID=?")) { ps.setInt(1, v.id); try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); } }
        if (exists) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE product SET PRODUCT_NAME=?, SUPPLIER=?, PURCHASE_PRICE=?, SELLING_PRICE=? WHERE PRODUCT_ID=?")) {
                ps.setString(1, v.name); ps.setString(2, v.supplier); ps.setBigDecimal(3, v.cost); ps.setBigDecimal(4, v.price); ps.setInt(5, v.id); ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO product (PRODUCT_ID, PRODUCT_NAME, SUPPLIER, PURCHASE_PRICE, SELLING_PRICE) VALUES (?,?,?,?,?)")) {
                ps.setInt(1, v.id); ps.setString(2, v.name); ps.setString(3, v.supplier); ps.setBigDecimal(4, v.cost); ps.setBigDecimal(5, v.price); ps.executeUpdate();
            }
        }
    }

    private void adjustStock() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a stock record first."); return; }
        String amountText = JOptionPane.showInputDialog(this,
                "Enter a signed quantity change for " + name.getText().trim() + ".\nPositive = stock count increase; negative = damage/loss/count decrease.\nThis adjustment will not create supplier payable.", "1");
        if (amountText == null) return;
        int delta;
        try {
            delta = Integer.parseInt(amountText.trim());
            if (delta == 0 || delta == Integer.MIN_VALUE) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a non-zero whole number such as 5 or -2.", "Invalid adjustment", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String reason = JOptionPane.showInputDialog(this, "Reason for this adjustment (required):", delta < 0 ? "Damage / loss" : "Physical count correction");
        if (reason == null) return;
        reason = reason.trim();
        if (reason.isEmpty()) { JOptionPane.showMessageDialog(this, "A reason is required for the stock audit log."); return; }
        final String adjustmentReason = reason;
        try (Connection con = DbConfig.getConnection()) {
            StockMovementService.ensureSchema(con);
            con.setAutoCommit(false);
            try {
                int currentQty;
                BigDecimal currentCost, currentTotal;
                try (PreparedStatement ps = con.prepareStatement("SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
                    ps.setInt(1, selectedId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Stock record no longer exists.");
                        currentQty = rs.getInt("quantity"); currentCost = rs.getBigDecimal("purchase_price"); currentTotal = rs.getBigDecimal("total");
                    }
                }
                if (currentCost == null) currentCost = BigDecimal.ZERO;
                if (currentTotal == null) currentTotal = currentCost.multiply(BigDecimal.valueOf(currentQty)).setScale(2, java.math.RoundingMode.HALF_UP);
                StockMovementService.assertStockMatchesLots(con, selectedId);
                currentTotal = StockMovementService.recalculateProductStock(con, selectedId);
                int newQty = Math.addExact(currentQty, delta);
                if (newQty < 0) throw new SQLException("Adjustment would make stock negative. Current quantity: " + currentQty + ".");
                if (delta > 0) {
                    BigDecimal adjustmentUnitCost = currentQty > 0
                            ? currentTotal.divide(BigDecimal.valueOf(currentQty), 8, java.math.RoundingMode.HALF_UP)
                            : currentCost;
                    StockMovementService.createLot(con, selectedId, "Inventory Adjustment", null, delta,
                            adjustmentUnitCost, "ADJUSTMENT_IN", adjustmentReason);
                } else {
                    StockMovementService.decreaseLotsForAdjustment(con, selectedId, -delta);
                }
                BigDecimal newTotal = StockMovementService.recalculateProductStock(con, selectedId);
                BigDecimal actualValueChange = newTotal.subtract(currentTotal).setScale(2, java.math.RoundingMode.HALF_UP);
                StockMovementService.record(con, selectedId, "MANUAL_ADJUSTMENT", delta, actualValueChange,
                        "STOCK_ADJUSTMENT", null, adjustmentReason);
                con.commit();
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (Exception ex) { showError("Could not adjust stock", ex); return; }
        JOptionPane.showMessageDialog(this, "Stock adjusted and movement logged. Supplier payable was not changed.");
        loadStock();
    }

    private void deleteStock() {
        if (selectedId == null) { JOptionPane.showMessageDialog(this, "Select a stock record first."); return; }
        int answer = JOptionPane.showConfirmDialog(this, "Remove this stock record only if its quantity is zero and it has no transaction history? Historical stock records are kept for audit and bill restoration.", "Confirm removal", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try (Connection con = DbConfig.getConnection()) {
            StockMovementService.ensureSchema(con);
            DbConfig.ensureBillStockMovementTable(con);
            con.setAutoCommit(false);
            try {
                int currentQty;
                try (PreparedStatement ps = con.prepareStatement("SELECT quantity FROM stock WHERE product_id=? FOR UPDATE")) {
                    ps.setInt(1, selectedId); try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Stock record no longer exists.");
                        currentQty = rs.getInt(1);
                    }
                }
                if (currentQty != 0) throw new SQLException("This stock record has " + currentQty + " units. Use Adjust Stock to record a real quantity change; records with stock on hand cannot be deleted.");
                if (hasLinkedStockHistory(con, selectedId)) throw new SQLException("This product has purchase, sale or stock movement history. The zero-quantity stock row is retained so history stays consistent.");
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM stock WHERE product_id=? AND quantity=0")) {
                    ps.setInt(1, selectedId); if (ps.executeUpdate() == 0) throw new SQLException("Stock record was changed by another operation; refresh and try again.");
                }
                con.commit();
            } catch (Exception ex) { con.rollback(); throw ex; }
        } catch (Exception ex) { showError("Could not remove stock record", ex); return; }
        loadStock();
    }

    private boolean hasLinkedStockHistory(Connection con, int productId) throws SQLException {
        String[] checks = {
                "SELECT COUNT(*) FROM purchase_history WHERE product_id=?",
                "SELECT COUNT(*) FROM bill_items WHERE product_id=?",
                "SELECT COUNT(*) FROM bill_stock_movements WHERE product_id=?",
                "SELECT COUNT(*) FROM bill_stock_lot_movements WHERE product_id=?",
                "SELECT COUNT(*) FROM stock_lots WHERE product_id=? AND received_qty>0",
                "SELECT COUNT(*) FROM stock_movement_history WHERE product_id=? AND movement_type<>'OPENING_BALANCE'"
        };
        for (String sql : checks) {
            try (PreparedStatement ps = con.prepareStatement(sql)) { ps.setInt(1, productId); try (ResultSet rs = ps.executeQuery()) { if (rs.next() && rs.getLong(1) > 0) return true; } }
        }
        return false;
    }

    private void reconcileStock() {
        int answer = JOptionPane.showConfirmDialog(this,
                "This checks every stock record against the supplier-lot ledger. If a mismatch exists, the stock-table quantity is treated as the confirmed count. " +
                "Missing lot units are added as Legacy / Unassigned; excess lot units are removed FIFO. This does NOT create supplier purchases or change supplier payables.\n\n" +
                "If the mismatch was caused by a real supplier delivery, cancel and record it in Purchases instead. Continue?",
                "Reconcile stock ledger", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        String reason = JOptionPane.showInputDialog(this,
                "Enter the reason / reference for this inventory reconciliation:", "Physical count / ledger correction");
        if (reason == null) return;
        reason = reason.trim();
        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "A reason is required so the reconciliation can be audited.", "Reason required", JOptionPane.WARNING_MESSAGE);
            return;
        }
        final String auditReason = reason;
        int checked = 0, changed = 0, unitsAdjusted = 0;
        BigDecimal valueAdjusted = BigDecimal.ZERO;
        try (Connection con = DbConfig.getConnection()) {
            // Schema setup/migration must run before beginning the transaction because MySQL DDL auto-commits.
            StockMovementService.ensureSchema(con);
            con.setAutoCommit(false);
            try {
                java.util.List<Integer> productIds = new java.util.ArrayList<>();
                try (PreparedStatement ps = con.prepareStatement("SELECT product_id FROM stock ORDER BY product_id FOR UPDATE");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) productIds.add(rs.getInt(1));
                }
                for (int productId : productIds) {
                    StockMovementService.ReconciliationResult result =
                            StockMovementService.reconcileLotsToStock(con, productId, auditReason);
                    checked++;
                    if (result.changed()) {
                        changed++;
                        unitsAdjusted = Math.addExact(unitsAdjusted, Math.abs(result.quantityChange));
                        valueAdjusted = valueAdjusted.add(result.inventoryValueChange);
                    }
                }
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        } catch (Exception ex) {
            showError("Stock reconciliation failed", ex);
            return;
        }
        loadStock();
        if (changed == 0) {
            JOptionPane.showMessageDialog(this, "Stock ledger is consistent. Checked " + checked + " product(s); no corrections were needed.",
                    "Reconciliation complete", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Reconciliation complete.\nProducts checked: " + checked + "\nProducts corrected: " + changed +
                    "\nUnits adjusted: " + unitsAdjusted + "\nNet inventory value change: ₹" + valueAdjusted.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() +
                    "\n\nAll corrections are recorded in Movement History. Supplier purchases and payables were not changed.",
                    "Reconciliation complete", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void viewMovementHistory() {
        DefaultTableModel movementModel = new DefaultTableModel(new String[]{"Date", "Product ID", "Movement", "Qty Change", "Inventory Value Change (₹)", "Reference", "Reason / Note"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        String sql = "SELECT created_at, product_id, movement_type, quantity_change, inventory_value_change, " +
                "CONCAT(COALESCE(reference_type,''), CASE WHEN reference_id IS NULL THEN '' ELSE CONCAT(' #',reference_id) END) AS reference_text, note " +
                "FROM stock_movement_history ORDER BY movement_id DESC LIMIT 500";
        try (Connection con = DbConfig.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) movementModel.addRow(new Object[]{rs.getTimestamp("created_at"), rs.getInt("product_id"), rs.getString("movement_type"),
                    rs.getInt("quantity_change"), rs.getBigDecimal("inventory_value_change"), rs.getString("reference_text"), rs.getString("note")});
            JTable movements = new JTable(movementModel);
            movements.setRowHeight(28);
            movements.setAutoCreateRowSorter(true);
            JScrollPane scroll = new JScrollPane(movements);
            scroll.setPreferredSize(new Dimension(1000, 420));
            JOptionPane.showMessageDialog(this, scroll, "Stock Movement History (latest 500)", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) { showError("Could not load stock movement history", ex); }
    }

    private void clearForm() { selectedId = null; table.clearSelection(); id.setText(""); name.setText(""); supplier.setText(""); purchasePrice.setText(""); sellingPrice.setText(""); quantity.setText(""); }
    private void exportCsv() {
        JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Export stock to CSV"); chooser.setSelectedFile(new java.io.File("inventory-stock.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (BufferedWriter writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) writer.write(','); writer.write(csv(model.getColumnName(c))); } writer.newLine();
            for (int r = 0; r < model.getRowCount(); r++) { for (int c = 0; c < model.getColumnCount(); c++) { if (c > 0) writer.write(','); writer.write(csv(str(r, c))); } writer.newLine(); }
            JOptionPane.showMessageDialog(this, "Stock exported successfully.");
        } catch (Exception ex) { showError("Could not export stock", ex); }
    }
    private String csv(String value) { return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\""; }
    private String money(BigDecimal amount) { return "₹" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private void showError(String heading, Exception ex) { JOptionPane.showMessageDialog(this, heading + ".\n" + ex.getMessage(), "Inventory error", JOptionPane.ERROR_MESSAGE); }
    private static class StockValues {
        final int id, quantity; final String name, supplier; final BigDecimal cost, price;
        StockValues(int id, String name, String supplier, BigDecimal cost, BigDecimal price, int quantity) { this.id=id; this.name=name; this.supplier=supplier; this.cost=cost; this.price=price; this.quantity=quantity; }
    }
}
