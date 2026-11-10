import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

class BillingFrame extends JPanel {

    private JTextField idField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField emailField;

    private JTextField productIdField;
    private JComboBox<String> productNameComboBox;
    private JTextField availableStockField;
    private JTextField sellingPriceField;
    private JTextField quantityField;
    private JTextField totalCostField;
    private JTextField subtotalField;
    private JTextField discountField;
    private JTextField discountamt;
    private JTextField taxField;
    private JTextField taxamt;
    private JTextField payableField;

    private JTable billTable;
    private DefaultTableModel model;
    private JTable recentBillsTable;
    private DefaultTableModel recentBillsModel;
    private String lastGeneratedBillText;

    public BillingFrame() {

        setLayout(null);
        setBackground(new Color(241, 245, 249));

        // Customer Details Card
        JPanel card1 = new JPanel(null);
        card1.setBounds(20, 50, 800, 140);
        card1.setBackground(Color.WHITE);
        card1.setBorder(BorderFactory.createLineBorder(
            new Color(220, 226, 235)
        ));

        JLabel heading = new JLabel("Customer Details");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading.setBounds(15, 10, 250, 30);
        card1.add(heading);

        JLabel idLabel = new JLabel("Customer ID");
        idLabel.setBounds(15, 50, 100, 25);
        card1.add(idLabel);

        idField = new JTextField();
        idField.setEditable(false);
        idField.setBounds(15, 80, 70, 30);
        card1.add(idField);

        JLabel nameLabel = new JLabel("Customer Name");
        nameLabel.setBounds(100, 50, 150, 25);
        card1.add(nameLabel);

        nameField = new JTextField();
        nameField.setBounds(100, 80, 150, 30);
        card1.add(nameField);

        JLabel phoneLabel = new JLabel("Phone Number");
        phoneLabel.setBounds(265, 50, 150, 25);
        card1.add(phoneLabel);

        phoneField = new JTextField();
        phoneField.setBounds(265, 80, 150, 30);
        card1.add(phoneField);

        JLabel emailLabel = new JLabel("Email");
        emailLabel.setBounds(430, 50, 150, 25);
        card1.add(emailLabel);

        emailField = new JTextField();
        emailField.setBounds(430, 80, 150, 30);
        card1.add(emailField);

        JButton addCustomerButton = new JButton("Add Customer");
        addCustomerButton.setBounds(620, 80, 130, 30);
        card1.add(addCustomerButton);

        add(card1);

        // NOW WE WILL BE ADDING THE AUTOFETCH FROM THE 

        phoneField.addActionListener(e -> fetchCustomerDetails());
        // Product Details Card
        JPanel card2 = new JPanel(null);
        card2.setBounds(20, 200, 800, 140);
        card2.setBackground(Color.WHITE);
        card2.setBorder(BorderFactory.createLineBorder(
            new Color(220, 226, 235)
        ));

        JLabel heading2 = new JLabel("Product Details");
        heading2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading2.setBounds(15, 10, 250, 30);
        card2.add(heading2);

        JLabel productIdLabel = new JLabel("Product ID");
        productIdLabel.setBounds(15, 40, 100, 25);
        card2.add(productIdLabel);

        productIdField = new JTextField();
        productIdField.setEditable(false);
        productIdField.setBounds(15, 70, 70, 30);
        card2.add(productIdField);

        JLabel productNameLabel = new JLabel("Product Name");
        productNameLabel.setBounds(100, 40, 120, 25);
        card2.add(productNameLabel);

        productNameComboBox = new JComboBox<>();
        productNameComboBox.setEditable(false);
        productNameComboBox.setBounds(100, 70, 130, 30);
        card2.add(productNameComboBox);
        loadProductNames();



        JLabel stockLabel = new JLabel("Available Stock");
        stockLabel.setBounds(240, 40, 110, 25);
        card2.add(stockLabel);

        availableStockField = new JTextField();
        availableStockField.setEditable(false);
        availableStockField.setBounds(240, 70, 100, 30);
        card2.add(availableStockField);

        JLabel priceLabel = new JLabel("Selling Price");
        priceLabel.setBounds(350, 40, 100, 25);
        card2.add(priceLabel);

        sellingPriceField = new JTextField();
        sellingPriceField.setEditable(false);
        sellingPriceField.setBounds(350, 70, 100, 30);
        card2.add(sellingPriceField);

        JLabel quantityLabel = new JLabel("Quantity");
        quantityLabel.setBounds(480, 40, 80, 25);
        card2.add(quantityLabel);

        quantityField = new JTextField();
        quantityField.setBounds(480, 70, 45, 30);
        card2.add(quantityField);

        JButton minusButton = new JButton("");
JButton plusButton = new JButton("");

minusButton.setBounds(470, 70, 10, 30);
plusButton.setBounds(524, 70, 10, 30);
card2.add(minusButton);
card2.add(plusButton);

minusButton.addActionListener(e -> {
    try {
        int quantity = Integer.parseInt(
            quantityField.getText().trim()
        );

        if (quantity > 1) {
            quantityField.setText(String.valueOf(quantity - 1));
        }
    } catch (NumberFormatException ex) {
        quantityField.setText("1");
    }
});

plusButton.addActionListener(e -> {
    try {
        int quantity = Integer.parseInt(
            quantityField.getText().trim()
        );

        quantityField.setText(String.valueOf(quantity + 1));

    } catch (NumberFormatException ex) {
        quantityField.setText("1");
    }
});

        JLabel totalLabel = new JLabel("Total Cost");
        totalLabel.setBounds(560, 40, 90, 25);
        card2.add(totalLabel);

        totalCostField = new JTextField();
        totalCostField.setEditable(false);
        totalCostField.setBounds(560, 70, 60, 30);
        totalCostField.setEditable(true);
        card2.add(totalCostField);

        JButton addProductButton = new JButton("Add Product");
        addProductButton.setBounds(645, 70, 140, 30);
        card2.add(addProductButton);

        add(card2);

        // Bill Items Card
        JPanel card3 = new JPanel(null);
        card3.setBounds(20, 350, 800, 310);
        card3.setBackground(Color.WHITE);
        card3.setBorder(BorderFactory.createLineBorder(
            new Color(220, 226, 235)
        ));

        JLabel heading3 = new JLabel("Bill Items");
        heading3.setFont(new Font("Segoe UI", Font.BOLD, 18));
        heading3.setBounds(15, 10, 250, 30);
        card3.add(heading3);

        String[] columns = {
            "Product ID",
            "Product Name",
            "Selling Price",
            "Quantity",
            "Total"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billTable = new JTable(model);
        billTable.setRowHeight(25);
        billTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        billTable.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );

        JScrollPane scrollPane = new JScrollPane(billTable);
        scrollPane.setBounds(15, 50, 770, 190);
        card3.add(scrollPane);

        JButton removeButton = new JButton("Remove Selected");
        removeButton.setBounds(15, 255, 160, 30);
        card3.add(removeButton);

        JLabel grandTotalText = new JLabel("Grand Total:");
        grandTotalText.setFont(new Font("Segoe UI", Font.BOLD, 16));
        grandTotalText.setBounds(450, 255, 120, 30);
        card3.add(grandTotalText);

        JLabel grandTotalLabel = new JLabel("Rs. 0.00");
        grandTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        grandTotalLabel.setForeground(new Color(0, 103, 184));
        grandTotalLabel.setBounds(575, 255, 200, 30);
        card3.add(grandTotalLabel);

        add(card3);

        javax.swing.event.DocumentListener listener =
    new javax.swing.event.DocumentListener() {

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            calculateItemTotal();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            calculateItemTotal();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            calculateItemTotal();
        }
    };

quantityField.getDocument().addDocumentListener(listener);
sellingPriceField.getDocument().addDocumentListener(listener);
productNameComboBox.addActionListener(e -> { if (!loadingProducts) loadSelectedProductDetails(); });
loadSelectedProductDetails();
        // Add product to bill
        addProductButton.addActionListener(e -> {

            try {
                int productId = Integer.parseInt(
                    productIdField.getText().trim()
                );

                Object selected =productNameComboBox.getSelectedItem();
                String productName = selected == null ? "" : selected.toString().trim();

                int availableStock = Integer.parseInt(
                    availableStockField.getText().trim()
                );

                double sellingPrice = Double.parseDouble(
                    sellingPriceField.getText().trim()
                );

                int quantity = Integer.parseInt(
                    quantityField.getText().trim()
                );

                if (productName.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        this, "Enter product name."
                    );
                    return;
                }

                if (availableStock < 0 || sellingPrice < 0) {
                    JOptionPane.showMessageDialog(
                        this, "Stock and price cannot be negative."
                    );
                    return;
                }

                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(
                        this, "Quantity must be greater than zero."
                    );
                    return;
                }

                int existingRow = -1;
                int alreadyAdded = 0;
                for (int row = 0; row < model.getRowCount(); row++) {
                    if (Integer.parseInt(model.getValueAt(row, 0).toString()) == productId) {
                        existingRow = row;
                        alreadyAdded = Integer.parseInt(model.getValueAt(row, 3).toString());
                        break;
                    }
                }
                if (quantity + alreadyAdded > availableStock) {
                    JOptionPane.showMessageDialog(this,
                        "Insufficient stock. Already in this bill: " + alreadyAdded + ". Available: " + availableStock + ".");
                    return;
                }

                double total = sellingPrice * quantity;
                if (existingRow >= 0) {
                    int combined = alreadyAdded + quantity;
                    model.setValueAt(combined, existingRow, 3);
                    model.setValueAt(sellingPrice * combined, existingRow, 4);
                } else {
                    model.addRow(new Object[] { productId, productName, sellingPrice, quantity, total });
                }

                calculateGrandTotal(grandTotalLabel);

                productIdField.setText("");
              productNameComboBox.setSelectedIndex(-1);
                availableStockField.setText("");
                sellingPriceField.setText("");
                quantityField.setText("");
                totalCostField.setText("");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Enter valid numeric values for ID, stock, price and quantity."
                );
            }
        });

        // Remove selected product
        removeButton.addActionListener(e -> {

            int selectedRow = billTable.getSelectedRow();

            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(
                    this, "Please select an item to remove."
                );
                return;
            }

            model.removeRow(selectedRow);
            calculateGrandTotal(grandTotalLabel);
        });

        addCustomerButton.addActionListener(e -> {
            String customerName = nameField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            if (customerName.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Customer name and phone number are required.");
                return;
            }
            try (Connection con = DbConfig.getConnection()) {
                try (PreparedStatement update = con.prepareStatement(
                        "UPDATE customer SET customer_name=?, email=? WHERE phone=?")) {
                    update.setString(1, customerName); update.setString(2, email); update.setString(3, phone);
                    int rows = update.executeUpdate();
                    if (rows == 0) {
                        try (PreparedStatement insert = con.prepareStatement(
                                "INSERT INTO customer (customer_name, phone, email) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                            insert.setString(1, customerName); insert.setString(2, phone); insert.setString(3, email);
                            insert.executeUpdate();
                            try (ResultSet keys = insert.getGeneratedKeys()) { if (keys.next()) idField.setText(String.valueOf(keys.getInt(1))); }
                        }
                    } else {
                        try (PreparedStatement query = con.prepareStatement("SELECT customer_id FROM customer WHERE phone=?")) {
                            query.setString(1, phone); try (ResultSet rs = query.executeQuery()) { if (rs.next()) idField.setText(String.valueOf(rs.getInt(1))); }
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "Customer details saved.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Could not save customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // adding the new card for showing the bill summary  the fields of discount, discount amt, tax, tax amt  then the grand total 


        JPanel billSummary=new JPanel();
        billSummary.setBounds(830,50,500,420);
        billSummary.setLayout(null);
        add(billSummary);
        billSummary.setBackground(Color.WHITE);

        JLabel billsummary=new JLabel("BILL SUMMARY");
        billsummary.setBounds(20,10,200,50);
        billsummary.setFont(new Font("Segoe UI", Font.BOLD, 18));
        billSummary.add(billsummary);


        JLabel subtotal=new JLabel("Sub Total (₹) :");
        subtotal.setBounds(20,80,100,20);
        subtotal.setFont(new Font("Segoe UI",Font.BOLD, 14));
        billSummary.add(subtotal);

        subtotalField = new JTextField("0.00");
        subtotalField.setBounds(130,80,200,30);
        subtotalField.setEditable(false);
        billSummary.add(subtotalField);

        JLabel discount=new JLabel("Discount %  :");
        discount.setBounds(20,120,100,20);
        discount.setFont(new Font("Segoe UI",Font.BOLD,14));
        billSummary.add(discount);

        discountField = new JTextField("0");
        discountField.setBounds(130,120,200,30);
        billSummary.add(discountField);

        JLabel damt=new JLabel("Amount:");
        damt.setBounds(340,120,100,30);
        damt.setFont(new Font("Segoe UI",Font.BOLD,14));
        billSummary.add(damt);

        discountamt = new JTextField("0.00");
        discountamt.setBounds(410,120,80,30);
        discountamt.setEditable(false);
        billSummary.add(discountamt);


        // ADDED THE TAX FIELD


        JLabel tax=new JLabel("Tax %           :");
        tax.setBounds(20,160,100,20);
        tax.setFont(new Font("Segoe UI",Font.BOLD,14));
        billSummary.add(tax);

        taxField = new JTextField("0");
        taxField.setBounds(130,160,200,30);
        billSummary.add(taxField);

        // GET THE TAX % FROM Taxfield and fill it in the taxamt

         JLabel tamt=new JLabel("Amount:");
        tamt.setBounds(340,160,100,30);
        tamt.setFont(new Font("Segoe UI",Font.BOLD,14));
        billSummary.add(tamt);

        taxamt = new JTextField("0.00");
        taxamt.setBounds(410,160,80,30);
        taxamt.setEditable(false);
        billSummary.add(taxamt);



        // for the total amt;


        JPanel tcpanel=new JPanel();
        tcpanel.setBounds(50,220,400,40);
        tcpanel.setBackground((new Color(252, 231, 243,225)));
        tcpanel.setLayout(null);
        billSummary.add(tcpanel);


        JLabel payableamt=new JLabel("Payable Amount:");
        payableamt.setFont(new Font("Segoe UI",Font.BOLD,16));
        payableamt.setBounds(2,5,190,30);
        tcpanel.add(payableamt);

        payableField = new JTextField("0.00");
        payableField.setBounds(200,5,190,30);
        payableField.setFont(new Font("Segoe UI",Font.BOLD,18));
        payableField.setForeground(new Color(22,163,74));
        payableField.setEditable(false);
        tcpanel.add(payableField);

        javax.swing.event.DocumentListener summaryListener = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateBillSummary(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateBillSummary(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateBillSummary(); }
        };
        discountField.getDocument().addDocumentListener(summaryListener);
        taxField.getDocument().addDocumentListener(summaryListener);
        updateBillSummary();


        //now we will be adding the generate bill button

        JButton generateBillButton=new JButton("Generate Bill");
         generateBillButton.setBounds(50,280,400,40);
        // generatebill.setBackground(Color.RED);
        // billSummary.add(generatebill);
        generateBillButton.setBackground(new Color(37, 99, 235));
        generateBillButton.setForeground(Color.WHITE);
        generateBillButton.setFont(new Font("Segoe UI", Font.BOLD, 18));
        generateBillButton.setFocusPainted(false);
        generateBillButton.setBorderPainted(false);
        generateBillButton.setOpaque(true);
        billSummary.add(generateBillButton);


        // adding the function for generating the bill

        generateBillButton.addActionListener(e -> generateBill());
        

        JButton downloadButton = new JButton("Download Bill");
        downloadButton.setBounds(50,340,400,40);
        downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        downloadButton.setBackground(new Color(22, 163, 74));
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setFocusPainted(false);
        downloadButton.setOpaque(true);
        downloadButton.setBorderPainted(false);
        billSummary.add(downloadButton);
        
        downloadButton.addActionListener(e -> exportLastBill());
        // Recent Bills Card
JPanel recentBillsPanel = new JPanel(new BorderLayout(5, 5));
recentBillsPanel.setBounds(830, 480, 500, 240);
recentBillsPanel.setBackground(Color.WHITE);
recentBillsPanel.setBorder(
    BorderFactory.createLineBorder(new Color(220, 226, 235))
);

JLabel recentBillsHeading = new JLabel("  Recent Bills");
recentBillsHeading.setFont(new Font("Segoe UI", Font.BOLD, 18));
recentBillsPanel.add(recentBillsHeading, BorderLayout.NORTH);

recentBillsModel = new DefaultTableModel(
    new String[]{"Bill No", "Customer Name", "Date", "Total (₹)"}, 0
) {
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
};

recentBillsTable = new JTable(recentBillsModel);
recentBillsTable.setRowHeight(25);
recentBillsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
recentBillsTable.getTableHeader().setFont(
    new Font("Segoe UI", Font.BOLD, 12)
);

recentBillsPanel.add(
    new JScrollPane(recentBillsTable),
    BorderLayout.CENTER
);

add(recentBillsPanel);

// Load previously saved bills
loadRecentBills();



JButton viewBillButton = new JButton("View Bill Details");
viewBillButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
viewBillButton.setBackground(new Color(37, 99, 235));
viewBillButton.setForeground(Color.WHITE);
viewBillButton.setFocusPainted(false);
viewBillButton.setBorderPainted(false);
viewBillButton.setOpaque(true);

JPanel lowerButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
lowerButtonPanel.setLayout(new FlowLayout());
lowerButtonPanel.setBackground(Color.WHITE);
lowerButtonPanel.add(viewBillButton);

JButton delete=new JButton("Delete bill");
lowerButtonPanel.add(delete,FlowLayout.LEFT);

recentBillsPanel.add(lowerButtonPanel,BorderLayout.SOUTH);
viewBillButton.addActionListener(e -> viewSelectedBillDetails());
delete.addActionListener(epp -> deleteSelectedBill());
    }



    
private boolean loadingProducts = false;

private void loadProductNames() {
    loadingProducts = true;
    productNameComboBox.removeAllItems();
    String sql = "SELECT DISTINCT product_name FROM stock WHERE quantity > 0 ORDER BY product_name";
    try (Connection conn = DbConfig.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) productNameComboBox.addItem(rs.getString("product_name"));
    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Unable to load products: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    } finally { loadingProducts = false; }
    if (productNameComboBox.getItemCount() > 0) productNameComboBox.setSelectedIndex(0);
}

private void loadSelectedProductDetails() {
    Object selected = productNameComboBox.getSelectedItem();
    if (selected == null || productIdField == null || availableStockField == null || sellingPriceField == null) return;
    String sql = "SELECT product_id, product_name, quantity, selling_price FROM stock WHERE product_name=? AND quantity > 0 ORDER BY product_id LIMIT 1";
    try (Connection conn = DbConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, selected.toString());
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                productIdField.setText(String.valueOf(rs.getInt("product_id")));
                availableStockField.setText(String.valueOf(rs.getInt("quantity")));
                sellingPriceField.setText(rs.getBigDecimal("selling_price").toPlainString());
                quantityField.setText("1"); calculateItemTotal();
            } else {
                productIdField.setText(""); availableStockField.setText("0"); sellingPriceField.setText(""); quantityField.setText(""); totalCostField.setText("");
            }
        }
    } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Could not load selected product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); }
}

private void exportLastBill() {
    if (lastGeneratedBillText == null || lastGeneratedBillText.isBlank()) { JOptionPane.showMessageDialog(this, "Generate a bill first, then use Download Bill."); return; }
    JFileChooser chooser = new JFileChooser(); chooser.setDialogTitle("Save bill text"); chooser.setSelectedFile(new java.io.File("inventory-bill.txt"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
    try { Files.writeString(chooser.getSelectedFile().toPath(), lastGeneratedBillText, StandardCharsets.UTF_8); JOptionPane.showMessageDialog(this, "Bill saved successfully."); }
    catch (Exception ex) { JOptionPane.showMessageDialog(this, "Could not save bill: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE); }
}




    public void fetchCustomerDetails() {
        String phone = phoneField.getText().trim();
        if (phone.isEmpty()) return;
        String sql = "SELECT customer_id, customer_name, phone, email FROM customer WHERE phone=?";
        try (Connection conn = DbConfig.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { idField.setText(String.valueOf(rs.getInt("customer_id"))); nameField.setText(rs.getString("customer_name")); emailField.setText(rs.getString("email")); }
                else { idField.setText(""); JOptionPane.showMessageDialog(this, "Customer not found. Enter the name and email, then select Add Customer."); }
            }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Could not fetch customer: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); }
    }


private static final class BillStockMovement {
    final int productId;
    final int quantity;
    final BigDecimal inventoryCost;
    BillStockMovement(int productId, int quantity, BigDecimal inventoryCost) {
        this.productId = productId; this.quantity = quantity; this.inventoryCost = inventoryCost;
    }
}

private void deleteSelectedBill() {
    int selectedRow = recentBillsTable.getSelectedRow();
    if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a bill from Recent Bills first.", "No Bill Selected", JOptionPane.WARNING_MESSAGE); return; }
    int modelRow = recentBillsTable.convertRowIndexToModel(selectedRow);
    String billNumber = recentBillsModel.getValueAt(modelRow, 0).toString();
    int billId = Integer.parseInt(billNumber.replace("B", ""));
    int choice = JOptionPane.showConfirmDialog(this,
            "Delete bill " + billNumber + "? Recorded stock deductions will be reversed to their original supplier lots, and the reversal will remain in stock movement history.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (choice != JOptionPane.YES_OPTION) return;
    try (Connection conn = DbConfig.getConnection()) {
        // Schema upgrades must happen before the transaction; MySQL DDL implicitly commits.
        DbConfig.ensureBillStockMovementTable(conn);
        StockMovementService.ensureSchema(conn);
        conn.setAutoCommit(false);
        int restoredProducts = 0;
        int restoredUnits = 0;
        try {
            // Lock the bill itself so two windows cannot reverse/delete it concurrently.
            try (PreparedStatement lockBill = conn.prepareStatement("SELECT bill_id FROM bills WHERE bill_id=? FOR UPDATE")) {
                lockBill.setInt(1, billId);
                try (ResultSet bill = lockBill.executeQuery()) {
                    if (!bill.next()) throw new SQLException("Bill was not found. Refresh the bill list and try again.");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM bill_stock_movements WHERE bill_id=? AND is_reversed=1")) {
                ps.setInt(1, billId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getLong(1) > 0) {
                        throw new SQLException("This bill has already-marked stock reversals but still exists. The bill was not deleted; inspect its movement history first.");
                    }
                }
            }
            java.util.List<BillStockMovement> movements = new java.util.ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT product_id, quantity, inventory_cost FROM bill_stock_movements WHERE bill_id=? AND is_reversed=0 FOR UPDATE")) {
                ps.setInt(1, billId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) movements.add(new BillStockMovement(rs.getInt("product_id"), rs.getInt("quantity"), rs.getBigDecimal("inventory_cost")));
                }
            }
            validateBillStockMovementCoverage(conn, billId, movements);
            for (BillStockMovement movement : movements) {
                int productId = movement.productId;
                int quantity = movement.quantity;
                StockMovementService.assertStockMatchesLots(conn, productId);
                BigDecimal currentLotValue = StockMovementService.recalculateProductStock(conn, productId);
                int currentQty;
                BigDecimal currentCost, currentTotal;
                try (PreparedStatement current = conn.prepareStatement(
                        "SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE")) {
                    current.setInt(1, productId);
                    try (ResultSet stock = current.executeQuery()) {
                        if (!stock.next()) throw new SQLException("Cannot restore stock for product ID " + productId + ". Its stock record is missing; restore the stock record before deleting this bill.");
                        currentQty = stock.getInt("quantity");
                        currentCost = stock.getBigDecimal("purchase_price");
                        currentTotal = stock.getBigDecimal("total");
                    }
                }
                if (currentCost == null) currentCost = BigDecimal.ZERO;
                if (currentLotValue != null) currentTotal = currentLotValue;
                if (currentTotal == null) currentTotal = currentCost.multiply(BigDecimal.valueOf(currentQty)).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal fallbackUnitCost = currentQty > 0
                        ? currentTotal.divide(BigDecimal.valueOf(currentQty), 8, java.math.RoundingMode.HALF_UP) : currentCost;
                StockMovementService.restoreLotsForBill(conn, billId, productId, quantity, fallbackUnitCost);
                BigDecimal restoredTotal = StockMovementService.recalculateProductStock(conn, productId);
                BigDecimal actualValueRestored = restoredTotal.subtract(currentTotal).setScale(2, java.math.RoundingMode.HALF_UP);
                StockMovementService.record(conn, productId, "SALE_REVERSAL", quantity, actualValueRestored,
                        "BILL_VOID", (long) billId, "Stock restored when bill " + billNumber + " was deleted");
                try (PreparedStatement mark = conn.prepareStatement(
                        "UPDATE bill_stock_movements SET inventory_cost=COALESCE(inventory_cost, ?), is_reversed=1 WHERE bill_id=? AND product_id=? AND is_reversed=0")) {
                    mark.setBigDecimal(1, actualValueRestored); mark.setInt(2, billId); mark.setInt(3, productId); mark.executeUpdate();
                }
                restoredProducts++;
                restoredUnits = Math.addExact(restoredUnits, quantity);
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bill_items WHERE bill_id=?")) { ps.setInt(1, billId); ps.executeUpdate(); }
            int deleted;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM bills WHERE bill_id=?")) { ps.setInt(1, billId); deleted = ps.executeUpdate(); }
            if (deleted == 0) throw new SQLException("Bill was not found in the database.");
            conn.commit();
            loadRecentBills();
            String message = "Bill " + billNumber + " deleted successfully.";
            if (restoredProducts > 0) message += "\nStock restored: " + restoredUnits + " unit(s) across " + restoredProducts + " product(s).";
            else message += "\nNo stock movement was recorded for this bill (likely an older bill), so no quantity was changed.";
            JOptionPane.showMessageDialog(this, message);
        } catch (Exception ex) {
            conn.rollback();
            if (ex instanceof SQLException) throw (SQLException) ex;
            throw new SQLException(ex.getMessage(), ex);
        } finally { conn.setAutoCommit(true); }
    } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Could not delete bill: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); }
}



    private void viewSelectedBillDetails() {
    int selectedRow = recentBillsTable.getSelectedRow();

    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(
            this,
            "Please select a bill from Recent Bills first."
        );
        return;
    }

    int billId = Integer.parseInt(
        recentBillsModel.getValueAt(selectedRow, 0)
            .toString().replace("B", "")
    );

    String sql = """
        SELECT product_name, selling_price, quantity, total
        FROM bill_items
        WHERE bill_id = ?
        """;

    StringBuilder details = new StringBuilder();

    details.append("BILL DETAILS\n");
    details.append("------------------------------------------\n");
    details.append("Bill No: B")
           .append(String.format("%03d", billId))
           .append("\n");

    details.append("Customer: ")
           .append(recentBillsModel.getValueAt(selectedRow, 1))
           .append("\n");

    details.append("Date: ")
           .append(recentBillsModel.getValueAt(selectedRow, 2))
           .append("\n\n");

    details.append(String.format(
        "%-18s %8s %5s %10s%n",
        "Product", "Price", "Qty", "Total"
    ));

    details.append("------------------------------------------\n");

    try (Connection conn = DbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setInt(1, billId);

        double grandTotal = 0;

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String productName = rs.getString("product_name");
                double price = rs.getDouble("selling_price");
                int quantity = rs.getInt("quantity");
                double total = rs.getDouble("total");

                details.append(String.format(
                    "%-18s %8.2f %5d %10.2f%n",
                    productName, price, quantity, total
                ));

                grandTotal += total;
            }
        }

        details.append("------------------------------------------\n");
        details.append(String.format(
            "ITEMS TOTAL: Rs. %.2f", grandTotal
        ));

        JTextArea textArea = new JTextArea(details.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JOptionPane.showMessageDialog(
            this,
            new JScrollPane(textArea),
            "Bill Details",
            JOptionPane.INFORMATION_MESSAGE
        );

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            "Could not load bill details: " + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}




private void loadRecentBills() {
    recentBillsModel.setRowCount(0);

    String sql = """
        SELECT bill_id, customer_name, bill_date, grand_total
        FROM bills
        ORDER BY bill_id DESC
        LIMIT 5
        """;

    try (Connection conn = DbConfig.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            recentBillsModel.addRow(new Object[]{
                "B" + String.format("%03d", rs.getInt("bill_id")),
                rs.getString("customer_name"),
                rs.getTimestamp("bill_date"),
                rs.getBigDecimal("grand_total")
            });
        }

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            "Unable to load recent bills: " + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }



    
}












































   private void calculateItemTotal() {
    try {
        double price = Double.parseDouble(
            sellingPriceField.getText().trim()
        );

        int quantity = Integer.parseInt(
            quantityField.getText().trim()
        );

        if (price >= 0 && quantity >= 0) {
            double total = price * quantity;
            totalCostField.setText(String.format("%.2f", total));
        } else {
            totalCostField.setText("");
        }

    } catch (NumberFormatException e) {
        totalCostField.setText("");
    }
}

    private double getSubtotal() {
        double subtotal = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            subtotal += Double.parseDouble(model.getValueAt(i, 4).toString());
        }
        return subtotal;
    }

    private double readPercent(JTextField field) {
        String value = field.getText().trim();
        if (value.isEmpty()) return 0;
        return Double.parseDouble(value);
    }

    private void updateBillSummary() {
        if (subtotalField == null || discountField == null || taxField == null ||
            discountamt == null || taxamt == null || payableField == null || model == null) {
            return;
        }
        double subtotal = getSubtotal();
        subtotalField.setText(String.format("%.2f", subtotal));
        try {
            double discountPercent = readPercent(discountField);
            double taxPercent = readPercent(taxField);
            if (discountPercent < 0 || discountPercent > 100 || taxPercent < 0 || taxPercent > 100) {
                discountamt.setText("0.00");
                taxamt.setText("0.00");
                payableField.setText("Invalid %");
                return;
            }
            double discountAmount = subtotal * discountPercent / 100.0;
            double taxableAmount = subtotal - discountAmount;
            double taxAmount = taxableAmount * taxPercent / 100.0;
            double payableAmount = taxableAmount + taxAmount;
            discountamt.setText(String.format("%.2f", discountAmount));
            taxamt.setText(String.format("%.2f", taxAmount));
            payableField.setText(String.format("%.2f", payableAmount));
        } catch (NumberFormatException ex) {
            discountamt.setText("0.00");
            taxamt.setText("0.00");
            payableField.setText("Enter valid %");
        }
    }

    private void calculateGrandTotal(JLabel grandTotalLabel) {
        double subtotal = getSubtotal();
        grandTotalLabel.setText(String.format("Rs. %.2f", subtotal));
        updateBillSummary();
    }

    private void generateBill() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Please add products before generating the bill.");
            return;
        }
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter customer name.");
            return;
        }

        double subtotal = getSubtotal();
        double discountPercent;
        double taxPercent;
        try {
            discountPercent = readPercent(discountField);
            taxPercent = readPercent(taxField);
            if (discountPercent < 0 || discountPercent > 100 || taxPercent < 0 || taxPercent > 100) {
                JOptionPane.showMessageDialog(this, "Discount and tax must be between 0 and 100.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric discount and tax percentages.");
            return;
        }

        double discountAmount = subtotal * discountPercent / 100.0;
        double taxableAmount = subtotal - discountAmount;
        double taxAmount = taxableAmount * taxPercent / 100.0;
        double payableAmount = taxableAmount + taxAmount;

        StringBuilder bill = new StringBuilder();
        bill.append("       INVENTORY MANAGEMENT SYSTEM\n");
        bill.append("----------------------------------------\n");
        bill.append("                 CUSTOMER BILL\n");
        bill.append("----------------------------------------\n");
        bill.append("Customer Name: ").append(nameField.getText()).append("\n");
        bill.append("Phone: ").append(phoneField.getText()).append("\n");
        bill.append("----------------------------------------\n");

        for (int i = 0; i < model.getRowCount(); i++) {
            String productName = model.getValueAt(i, 1).toString();
            double price = Double.parseDouble(model.getValueAt(i, 2).toString());
            int quantity = Integer.parseInt(model.getValueAt(i, 3).toString());
            double total = Double.parseDouble(model.getValueAt(i, 4).toString());
            bill.append(productName).append(" | ₹").append(String.format("%.2f", price))
                .append(" x ").append(quantity).append(" = ₹")
                .append(String.format("%.2f", total)).append("\n");
        }

        bill.append("----------------------------------------\n");
        bill.append(String.format("SUBTOTAL: ₹%.2f%n", subtotal));
        bill.append(String.format("DISCOUNT (%.2f%%): -₹%.2f%n", discountPercent, discountAmount));
        bill.append(String.format("TAX (%.2f%%): ₹%.2f%n", taxPercent, taxAmount));
        bill.append(String.format("PAYABLE AMOUNT: ₹%.2f%n", payableAmount));
        bill.append("----------------------------------------\n");
        bill.append("         Thank you for shopping!");

        lastGeneratedBillText = bill.toString();
        JTextArea billArea = new JTextArea(bill.toString());
        billArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        billArea.setEditable(false);
        int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(billArea),
            "Generated Bill", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        try {
            int billId = saveBillToDatabase(payableAmount);
            loadRecentBills();
            JOptionPane.showMessageDialog(this, "Bill saved successfully! Bill ID: " + billId);
            showBillScreen(bill.toString());
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save bill: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

private void validateBillStockMovementCoverage(Connection conn, int billId,
        java.util.List<BillStockMovement> movements) throws SQLException {
    // Legacy bills have no movement rows and are intentionally not guessed/re-stocked.
    if (movements.isEmpty()) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM stock_movement_history WHERE movement_type='SALE' AND reference_type='BILL' AND reference_id=?")) {
            ps.setLong(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong(1) > 0) {
                    throw new SQLException("This bill has a SALE movement in the audit log but no active bill_stock_movements row. The bill was not deleted because stock restoration cannot be verified.");
                }
            }
        }
        // A modern bill must have both the summary movement and its lot-allocation details.
        // If detail rows remain but the summary has vanished, do not treat it as a legacy bill;
        // otherwise deletion could permanently lose deducted stock.
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM bill_stock_lot_movements WHERE bill_id=?")) {
            ps.setInt(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getLong(1) > 0) {
                    throw new SQLException("This bill has supplier-lot allocations but its summary stock movement is missing. The bill was not deleted; reconcile the bill movement records first.");
                }
            }
        }
        return;
    }
    java.util.Map<Integer, Integer> expected = new java.util.TreeMap<>();
    try (PreparedStatement ps = conn.prepareStatement(
            "SELECT product_id, quantity FROM bill_items WHERE bill_id=? FOR UPDATE")) {
        ps.setInt(1, billId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int productId = rs.getInt("product_id");
                if (rs.wasNull()) continue; // legacy non-stock line
                int quantity = rs.getInt("quantity");
                if (quantity <= 0) throw new SQLException("Bill " + billId + " contains a non-positive item quantity; it cannot be safely reversed.");
                expected.merge(productId, quantity, Math::addExact);
            }
        }
    }
    java.util.Map<Integer, Integer> actual = new java.util.TreeMap<>();
    for (BillStockMovement movement : movements) {
        if (movement.quantity <= 0) throw new SQLException("Bill " + billId + " has an invalid stock movement quantity.");
        actual.merge(movement.productId, movement.quantity, Math::addExact);
    }
    if (!expected.equals(actual)) {
        throw new SQLException("Bill " + billId + " has incomplete or inconsistent stock movement records. The bill was not deleted and no stock was changed. Reconcile the bill records first.");
    }
    validateBillLotMovementCoverage(conn, billId, actual);
}

private void validateBillLotMovementCoverage(Connection conn, int billId,
        java.util.Map<Integer, Integer> expectedSummary) throws SQLException {
    java.util.Map<Integer, Integer> detailTotals = new java.util.TreeMap<>();
    java.util.Map<Integer, Integer> activeDetailTotals = new java.util.TreeMap<>();
    boolean foundDetails = false;
    try (PreparedStatement ps = conn.prepareStatement(
            "SELECT product_id, quantity, is_reversed FROM bill_stock_lot_movements WHERE bill_id=? FOR UPDATE")) {
        ps.setInt(1, billId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                foundDetails = true;
                int productId = rs.getInt("product_id");
                int quantity = rs.getInt("quantity");
                if (quantity <= 0) throw new SQLException("Bill " + billId + " has an invalid lot-allocation quantity.");
                detailTotals.merge(productId, quantity, Math::addExact);
                if (rs.getBoolean("is_reversed")) {
                    throw new SQLException("Bill " + billId + " contains already-reversed lot allocations while the bill still exists. The bill was not deleted.");
                }
                activeDetailTotals.merge(productId, quantity, Math::addExact);
            }
        }
    }
    // Bills created before lot tracking have no detail rows at all and can use the documented legacy fallback.
    if (!foundDetails) return;
    if (!detailTotals.equals(expectedSummary) || !activeDetailTotals.equals(expectedSummary)) {
        throw new SQLException("Bill " + billId + " has incomplete supplier-lot allocation details. The bill was not deleted and no stock was changed.");
    }
}

private int saveBillToDatabase(double grandTotal) throws SQLException {
    String billSql = """
        INSERT INTO bills
        (customer_name, phone, email, grand_total)
        VALUES (?, ?, ?, ?)
        """;

    String itemSql = """
        INSERT INTO bill_items
        (bill_id, product_id, product_name,
         selling_price, quantity, total)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    if (model.getRowCount() == 0) throw new SQLException("Cannot save a bill without at least one product.");
    try (Connection conn = DbConfig.getConnection()) {
        DbConfig.ensureBillStockMovementTable(conn);
        StockMovementService.ensureSchema(conn);
        conn.setAutoCommit(false);

        try {
            int billId;

            try (PreparedStatement billStmt =
                     conn.prepareStatement(
                         billSql,
                         Statement.RETURN_GENERATED_KEYS)) {

                billStmt.setString(1, nameField.getText().trim());
                billStmt.setString(2, phoneField.getText().trim());

                String email = emailField.getText().trim();
                billStmt.setString(3, email);
                billStmt.setDouble(4, grandTotal);

                billStmt.executeUpdate();

                try (ResultSet keys = billStmt.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Bill ID was not generated.");
                    }
                    billId = keys.getInt(1);
                }
            }

            try (PreparedStatement itemStmt =
                     conn.prepareStatement(itemSql)) {

                for (int i = 0; i < model.getRowCount(); i++) {
                    itemStmt.setInt(1, billId);

                    Object productId =
                        model.getValueAt(i, 0);

                    if (productId == null ||
                        productId.toString().trim().isEmpty()) {
                        itemStmt.setNull(2, java.sql.Types.INTEGER);
                    } else {
                        itemStmt.setInt(
                            2, Integer.parseInt(productId.toString())
                        );
                    }

                    itemStmt.setString(
                        3, model.getValueAt(i, 1).toString()
                    );
                    itemStmt.setDouble(
                        4, Double.parseDouble(
                            model.getValueAt(i, 2).toString()
                        )
                    );
                    itemStmt.setInt(
                        5, Integer.parseInt(
                            model.getValueAt(i, 3).toString()
                        )
                    );
                    itemStmt.setDouble(
                        6, Double.parseDouble(
                            model.getValueAt(i, 4).toString()
                        )
                    );

                    itemStmt.addBatch();
                }

                itemStmt.executeBatch();
            }

            java.util.Map<Integer, Integer> quantitiesByProduct = new java.util.TreeMap<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object rawProductId = model.getValueAt(i, 0);
                if (rawProductId == null || rawProductId.toString().trim().isEmpty())
                    throw new SQLException("Every bill item must reference a valid inventory product.");
                int productId = Integer.parseInt(rawProductId.toString());
                int quantity = Integer.parseInt(model.getValueAt(i, 3).toString());
                if (productId <= 0 || quantity <= 0) throw new SQLException("Bill product IDs and quantities must be positive.");
                quantitiesByProduct.merge(productId, quantity, Math::addExact);
            }
            try (PreparedStatement stockRead = conn.prepareStatement(
                    "SELECT quantity, purchase_price, total FROM stock WHERE product_id=? FOR UPDATE");
                 PreparedStatement movementInsert = conn.prepareStatement(
                    "INSERT INTO bill_stock_movements (bill_id, product_id, quantity, inventory_cost, is_reversed) VALUES (?, ?, ?, ?, 0)")) {
                // Stable product-ID order lowers deadlock risk when two checkout windows sell overlapping products.
                for (java.util.Map.Entry<Integer, Integer> entry : quantitiesByProduct.entrySet()) {
                    int productId = entry.getKey();
                    int quantity = entry.getValue();
                    StockMovementService.assertStockMatchesLots(conn, productId);
                    BigDecimal synchronizedInventoryTotal = StockMovementService.recalculateProductStock(conn, productId);
                    int available;
                    BigDecimal unitCost, inventoryTotal;
                    stockRead.setInt(1, productId);
                    try (ResultSet stock = stockRead.executeQuery()) {
                        if (!stock.next()) throw new SQLException("Product ID " + productId + " has no stock record. Refresh products and try again.");
                        available = stock.getInt("quantity");
                        unitCost = stock.getBigDecimal("purchase_price");
                        inventoryTotal = synchronizedInventoryTotal != null ? synchronizedInventoryTotal : stock.getBigDecimal("total");
                    }
                    if (available < quantity) throw new SQLException("Insufficient stock for product ID " + productId + ". Available: " + available + ", requested: " + quantity + ".");
                    if (unitCost == null) unitCost = BigDecimal.ZERO;
                    if (inventoryTotal == null) inventoryTotal = unitCost.multiply(BigDecimal.valueOf(available)).setScale(2, java.math.RoundingMode.HALF_UP);
                    StockMovementService.consumeLotsForBill(conn, billId, productId, quantity);
                    BigDecimal newInventoryTotal = StockMovementService.recalculateProductStock(conn, productId);
                    BigDecimal removedValue = inventoryTotal.subtract(newInventoryTotal).setScale(2, java.math.RoundingMode.HALF_UP);
                    if (removedValue.signum() < 0) throw new SQLException("Inventory valuation increased during a sale for product ID " + productId + ". The transaction was cancelled.");
                    movementInsert.setInt(1, billId); movementInsert.setInt(2, productId);
                    movementInsert.setInt(3, quantity); movementInsert.setBigDecimal(4, removedValue);
                    movementInsert.executeUpdate();
                    StockMovementService.record(conn, productId, "SALE", -quantity, removedValue.negate(),
                            "BILL", (long) billId, "Sold on bill B" + billId);
                }
            }

            conn.commit();
            loadProductNames();
            return billId;

        } catch (SQLException | RuntimeException ex) {
            conn.rollback();
            throw ex;
        }
    }
}

private void showBillScreen(String billText) {

    JFrame billFrame = new JFrame("Generated Bill");
    billFrame.setSize(650, 720);
    billFrame.setLocationRelativeTo(this);
    billFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    billFrame.setLayout(new BorderLayout(15, 15));
    billFrame.getContentPane().setBackground(
        new Color(241, 245, 249)
    );

    // Header
    JPanel header = new JPanel(new GridLayout(2, 1));
    header.setBackground(new Color(153, 27, 27));
    header.setBorder(
        BorderFactory.createEmptyBorder(15, 20, 15, 20)
    );

    JLabel title = new JLabel(
        "INVENTORY MANAGEMENT SYSTEM",
        SwingConstants.CENTER
    );
    title.setFont(new Font("Segoe UI", Font.BOLD, 22));
    title.setForeground(Color.WHITE);

    JLabel subtitle = new JLabel(
        "CUSTOMER INVOICE",
        SwingConstants.CENTER
    );
    subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
    subtitle.setForeground(Color.WHITE);

    header.add(title);
    header.add(subtitle);

    // Receipt area
    JTextArea receipt = new JTextArea(billText);
    receipt.setFont(new Font("Monospaced", Font.PLAIN, 14));
    receipt.setEditable(false);
    receipt.setBackground(Color.WHITE);
    receipt.setMargin(new Insets(20, 20, 20, 20));
    receipt.setLineWrap(false);

    JScrollPane scrollPane = new JScrollPane(receipt);
    scrollPane.setBorder(
        BorderFactory.createEmptyBorder(15, 20, 5, 20)
    );

    // Buttons
    JPanel buttonPanel = new JPanel(
        new FlowLayout(FlowLayout.CENTER, 20, 15)
    );
    buttonPanel.setBackground(new Color(241, 245, 249));

    JButton printButton = new JButton("Print Bill");
    printButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
    printButton.setBackground(new Color(220, 38, 38));
    printButton.setForeground(Color.WHITE);
    printButton.setFocusPainted(false);
    printButton.setOpaque(true);
    printButton.setBorderPainted(false);

    JButton closeButton = new JButton("Close");
    JButton downloadButton = new JButton("Download Bill");
    downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
    downloadButton.setBackground(new Color(22, 163, 74));
    downloadButton.setForeground(Color.WHITE);
    downloadButton.setFocusPainted(false);
    downloadButton.setOpaque(true);
    downloadButton.setBorderPainted(false);
    closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));

    JButton whatsappButton = new JButton("Share on WhatsApp");
JButton emailButton = new JButton("Share via Email");

whatsappButton.setFont(
    new Font("Segoe UI", Font.BOLD, 14)
);
whatsappButton.setBackground(new Color(22, 163, 74));
whatsappButton.setForeground(Color.WHITE);
whatsappButton.setFocusPainted(false);

emailButton.setFont(
    new Font("Segoe UI", Font.BOLD, 14)
);

    printButton.addActionListener(e -> {
        try {
            receipt.print();
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(
                billFrame,
                "Printing failed: " + ex.getMessage()
            );
        }
    });

    closeButton.addActionListener(e -> billFrame.dispose());
    downloadButton.addActionListener(e -> {
    JFileChooser chooser = new JFileChooser();
    chooser.setDialogTitle("Download Bill");
    chooser.setSelectedFile(
        new java.io.File("Customer_Bill.txt")
    );




    whatsappButton.addActionListener(epp -> {

    String phone = phoneField.getText().trim();

    if (phone.isEmpty()) {
        JOptionPane.showMessageDialog(
            billFrame,
            "Please enter customer phone number."
        );
        return;
    }

    phone = phone.replaceAll("[^0-9]", "");

    if (phone.length() == 10) {
        phone = "91" + phone;
    }

    try {
        String message = java.net.URLEncoder.encode(
            billText,
            java.nio.charset.StandardCharsets.UTF_8
        );

        String url = "https://wa.me/" + phone + "?text=" + message;

        java.awt.Desktop.getDesktop().browse(
            new java.net.URI(url)
        );

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
            billFrame,
            "Unable to open WhatsApp: " + ex.getMessage()
        );
    }
});
    int result = chooser.showSaveDialog(billFrame);

    if (result == JFileChooser.APPROVE_OPTION) {
        java.io.File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".txt")) {
            file = new java.io.File(
                file.getAbsolutePath() + ".txt"
            );
        }

        if (file.exists()) {
            int choice = JOptionPane.showConfirmDialog(
                billFrame,
                "File already exists. Replace it?",
                "Confirm Save",
                JOptionPane.YES_NO_OPTION
            );

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }

        try (java.io.BufferedWriter writer =
                 new java.io.BufferedWriter(
                     new java.io.FileWriter(file))) {

            writer.write(billText);

            JOptionPane.showMessageDialog(
                billFrame,
                "Bill downloaded successfully!\n" +
                file.getAbsolutePath()
            );

        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(
                billFrame,
                "Error saving bill: " + ex.getMessage()
            );
        }
    }
});


buttonPanel.add(downloadButton);
buttonPanel.add(whatsappButton);
buttonPanel.add(emailButton);
buttonPanel.add(printButton);
buttonPanel.add(closeButton);

    billFrame.add(header, BorderLayout.NORTH);
    billFrame.add(scrollPane, BorderLayout.CENTER);
    billFrame.add(buttonPanel, BorderLayout.SOUTH);

    billFrame.setVisible(true);
}

}
