
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BillHistoryFrame extends JPanel {

    private JTable billTable;
    private DefaultTableModel model;
    private JTextField searchField;

    public BillHistoryFrame() {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Bill History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        add(title, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBackground(Color.WHITE);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setToolTipText("Search by bill ID or customer name");

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");
        JButton viewButton = new JButton("View Bill");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setBackground(Color.WHITE);
        buttons.add(searchButton);
        buttons.add(refreshButton);
        buttons.add(viewButton);

        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(buttons, BorderLayout.EAST);

        model = new DefaultTableModel(
            new String[]{
                "Bill ID", "Customer Name", "Phone",
                "Bill Date", "Grand Total"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        billTable = new JTable(model);
        billTable.setRowHeight(30);
        billTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        billTable.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );

        JScrollPane scrollPane = new JScrollPane(billTable);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        searchButton.addActionListener(e -> loadBills(searchField.getText().trim()));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadBills("");
        });
        viewButton.addActionListener(e -> viewSelectedBill());

        loadBills("");
    }

    private Connection getConnection() throws SQLException {
        return DbConfig.getConnection();
    }

    private void loadBills(String search) {
        model.setRowCount(0);

        String sql = """
            SELECT bill_id, customer_name, phone,
                   bill_date, grand_total
            FROM bills
            WHERE CAST(bill_id AS CHAR) LIKE ?
               OR customer_name LIKE ?
            ORDER BY bill_id DESC
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + search + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("bill_id"),
                        rs.getString("customer_name"),
                        rs.getString("phone"),
                        rs.getTimestamp("bill_date"),
                        rs.getBigDecimal("grand_total")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not load bill history:\n" + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void viewSelectedBill() {
        int row = billTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(
                this, "Please select a bill first."
            );
            return;
        }

        int billId = Integer.parseInt(
            model.getValueAt(row, 0).toString()
        );

        String sql = """
            SELECT product_name, selling_price, quantity, total
            FROM bill_items
            WHERE bill_id = ?
            """;

        StringBuilder details = new StringBuilder();
        details.append("Bill ID: ").append(billId).append("\n\n");

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, billId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    details.append(rs.getString("product_name"))
                        .append(" | Price: ")
                        .append(rs.getBigDecimal("selling_price"))
                        .append(" | Qty: ")
                        .append(rs.getInt("quantity"))
                        .append(" | Total: ")
                        .append(rs.getBigDecimal("total"))
                        .append("\n");
                }
            }

            JOptionPane.showMessageDialog(
                this,
                new JScrollPane(new JTextArea(details.toString(), 12, 45)),
                "Bill Details",
                JOptionPane.INFORMATION_MESSAGE
            );

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                this, "Could not load bill details: " + ex.getMessage()
            );
        }
    }
}
