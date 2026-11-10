
import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CustomerFrame extends JPanel {

    private final Color background = new Color(245, 247, 250);
    private final Color navy = new Color(25, 45, 75);
    private final Color blue = new Color(45, 105, 190);

    private JTable customerTable;
    private DefaultTableModel model;
    private JTextField searchField;

    private Connection getConnection() throws SQLException {
        return DbConfig.getConnection();
    }

    public CustomerFrame() {
        setLayout(new BorderLayout(15, 15));
        setBackground(background);
        setBorder(BorderFactory.createEmptyBorder(22, 25, 22, 25));

        createHeader();
        createTable();
        createBottomPanel();

        loadCustomers("");
    }

    private void createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 10));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("Customer Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(navy);

        JLabel subtitle = new JLabel(
            "Manage customer details and contact information"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(105, 115, 130));

        titlePanel.add(title);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitle);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setPreferredSize(new Dimension(220, 38));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(210, 218, 228)),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JButton searchButton = createButton("Search", blue);
        searchButton.addActionListener(e -> loadCustomers(searchField.getText()));
        searchField.addActionListener(e -> loadCustomers(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(searchPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);
    }

    private void createTable() {
        model = new DefaultTableModel(
            new String[]{"Phone Number", "Customer Name", "Email"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(model);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        customerTable.setRowHeight(35);
        customerTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );
        customerTable.setShowVerticalLines(false);
        customerTable.setGridColor(new Color(230, 234, 240));
        customerTable.setSelectionBackground(new Color(220, 234, 250));
        customerTable.setSelectionForeground(navy);
        customerTable.getTableHeader().setFont(
            new Font("Segoe UI", Font.BOLD, 13)
        );
        customerTable.getTableHeader().setBackground(navy);
        customerTable.getTableHeader().setForeground(Color.WHITE);
        customerTable.getTableHeader().setPreferredSize(new Dimension(0, 40));

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 232))
        );
        scrollPane.getViewport().setBackground(Color.WHITE);

        add(scrollPane, BorderLayout.CENTER);
    }

    private void createBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        bottom.setOpaque(false);

        JButton addButton = createButton("Add Customer", new Color(35, 145, 90));
        JButton editButton = createButton("Edit Customer", blue);
        JButton deleteButton = createButton("Delete Customer", new Color(195, 55, 55));
        JButton refreshButton = createButton("Refresh", navy);

        addButton.addActionListener(e -> addCustomer());
        editButton.addActionListener(e -> editCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadCustomers("");
        });

        bottom.add(addButton);
        bottom.add(editButton);
        bottom.add(deleteButton);
        bottom.add(refreshButton);

        add(bottom, BorderLayout.SOUTH);
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        return button;
    }

    private void loadCustomers(String search) {
        model.setRowCount(0);

        String sql = """
            SELECT phone, customer_name, email
            FROM customer
            WHERE customer_name LIKE ?
               OR phone LIKE ?
               OR COALESCE(email, '') LIKE ?
            ORDER BY customer_name
            """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String keyword = "%" + search.trim() + "%";
            ps.setString(1, keyword);
            ps.setString(2, keyword);
            ps.setString(3, keyword);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("phone"),
                        rs.getString("customer_name"),
                        rs.getString("email")
                    });
                }
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void addCustomer() {
        JTextField phoneField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();

        Object[] fields = {
            "Phone Number:", phoneField,
            "Customer Name:", nameField,
            "Email:", emailField
        };

        int result = JOptionPane.showConfirmDialog(
            this, fields, "Add Customer",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String phone = phoneField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();

        if (!validateCustomer(phone, name)) {
            return;
        }

        String sql = """
            INSERT INTO customer (phone, customer_name, email)
            VALUES (?, ?, ?)
            """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, phone);
            ps.setString(2, name);
            ps.setString(3, email.isEmpty() ? null : email);

            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Customer added successfully.");
            loadCustomers(searchField.getText());

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void editCustomer() {
        int row = customerTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to edit.");
            return;
        }

        String oldPhone = model.getValueAt(row, 0).toString();
        String oldName = model.getValueAt(row, 1).toString();
        Object emailValue = model.getValueAt(row, 2);
        String oldEmail = emailValue == null ? "" : emailValue.toString();

        JTextField phoneField = new JTextField(oldPhone);
        JTextField nameField = new JTextField(oldName);
        JTextField emailField = new JTextField(oldEmail);

        Object[] fields = {
            "Phone Number:", phoneField,
            "Customer Name:", nameField,
            "Email:", emailField
        };

        int result = JOptionPane.showConfirmDialog(
            this, fields, "Edit Customer",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newPhone = phoneField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();

        if (!validateCustomer(newPhone, name)) {
            return;
        }

        String sql = """
            UPDATE customer
            SET phone = ?, customer_name = ?, email = ?
            WHERE phone = ?
            """;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, newPhone);
            ps.setString(2, name);
            ps.setString(3, email.isEmpty() ? null : email);
            ps.setString(4, oldPhone);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Customer updated successfully.");
                loadCustomers(searchField.getText());
            } else {
                JOptionPane.showMessageDialog(this, "Customer not found.");
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private void deleteCustomer() {
        int row = customerTable.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to delete.");
            return;
        }

        String phone = model.getValueAt(row, 0).toString();
        String name = model.getValueAt(row, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete customer " + name + " (" + phone + ")?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM customer WHERE phone = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, phone);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Customer deleted successfully.");
            loadCustomers(searchField.getText());

        } catch (SQLException ex) {
            showDatabaseError(ex);
        }
    }

    private boolean validateCustomer(String phone, String name) {
        if (phone.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(
                this, "Phone number and customer name are required."
            );
            return false;
        }

        if (phone.length() > 15) {
            JOptionPane.showMessageDialog(
                this, "Phone number cannot exceed 15 characters."
            );
            return false;
        }

        return true;
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            "Database error:\n" + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
