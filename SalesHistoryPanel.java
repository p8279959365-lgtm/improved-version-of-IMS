
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class SalesHistoryPanel extends JPanel {

    private final Color background = new Color(242, 246, 255);
    private final Color navy = new Color(30, 41, 80);
    private final Color blue = new Color(59, 91, 219);
    private final Color purple = new Color(124, 77, 255);
    private final Color green = new Color(16, 160, 105);
    private final Color muted = new Color(105, 116, 135);

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;

    private JLabel totalSalesLabel;
    private JLabel totalBillsLabel;
    private JLabel averageBillLabel;

    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));

    public SalesHistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(background);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBackground(background);

        JLabel heading = new JLabel("Sales History");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(navy);

        JLabel subtitle = new JLabel(
                "Review transactions and monitor your sales performance");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(muted);

        main.add(heading);
        main.add(Box.createVerticalStrut(5));
        main.add(subtitle);
        main.add(Box.createVerticalStrut(22));

        JPanel stats = new JPanel(new GridLayout(1, 3, 14, 0));
        stats.setBackground(background);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        totalSalesLabel = new JLabel("₹0.00");
        totalBillsLabel = new JLabel("0");
        averageBillLabel = new JLabel("₹0.00");

        stats.add(createCard("TOTAL SALES", totalSalesLabel, blue, "₹"));
        stats.add(createCard("TOTAL BILLS", totalBillsLabel, purple, "#"));
        stats.add(createCard("AVERAGE BILL", averageBillLabel, green, "₹"));

        main.add(stats);
        main.add(Box.createVerticalStrut(18));

        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 228, 243)),
        new EmptyBorder(12, 14, 12, 14)));
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel searchLabel = new JLabel("Search bills");
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLabel.setForeground(navy);

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 238)),
                new EmptyBorder(7, 10, 7, 10)));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setBackground(blue);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(new EmptyBorder(9, 16, 9, 16));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(refreshButton, BorderLayout.EAST);

        main.add(searchPanel);
        main.add(Box.createVerticalStrut(18));

        JPanel recordsPanel = new JPanel(new BorderLayout(0, 12));
        recordsPanel.setBackground(Color.WHITE);
        recordsPanel.setBorder(new EmptyBorder(16, 16, 12, 16));
        recordsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel recordsHeading = new JLabel("Transaction Records");
        recordsHeading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recordsHeading.setForeground(navy);
        recordsPanel.add(recordsHeading, BorderLayout.NORTH);

        String[] columns = {
                "BILL ID", "CUSTOMER NAME", "PHONE",
                "EMAIL", "DATE & TIME", "TOTAL AMOUNT"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setForeground(new Color(45, 55, 75));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(230, 235, 245));
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setFillsViewportHeight(true);

        table.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 10));
        table.getTableHeader().setBackground(navy);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.getTableHeader().setReorderingAllowed(false);

        int[] widths = {65, 125, 95, 125, 145, 95};

        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i)
                    .setPreferredWidth(widths[i]);
        }

        DefaultTableCellRenderer centerRenderer =
                new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        table.getColumnModel().getColumn(0)
                .setCellRenderer(centerRenderer);

        DefaultTableCellRenderer amountRenderer =
                new DefaultTableCellRenderer();
        amountRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        amountRenderer.setForeground(green);

        table.getColumnModel().getColumn(5)
                .setCellRenderer(amountRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(
                new Color(225, 231, 243)));
        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(Color.WHITE);

        recordsPanel.add(scrollPane, BorderLayout.CENTER);
      recordsPanel.setMaximumSize(
    new Dimension(Integer.MAX_VALUE, 500)
);
recordsPanel.setPreferredSize(
    new Dimension(1800, 420)
);
recordsPanel.setMinimumSize(
    new Dimension(0, 300)
);

main.add(recordsPanel);
table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

scrollPane.setHorizontalScrollBarPolicy(
    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
);
        add(main, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    public void insertUpdate(
                            javax.swing.event.DocumentEvent e) {
                        loadBills();
                    }

                    public void removeUpdate(
                            javax.swing.event.DocumentEvent e) {
                        loadBills();
                    }

                    public void changedUpdate(
                            javax.swing.event.DocumentEvent e) {
                        loadBills();
                    }
                });

        refreshButton.addActionListener(e -> loadBills());

        loadBills();
    }

    private JPanel createCard(
            String title, JLabel value, Color accent, String symbol) {

        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(12, 14, 12, 12)));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLabel.setForeground(muted);

        value.setFont(new Font("Segoe UI", Font.BOLD, 23));
        value.setForeground(accent);

        JLabel description = new JLabel(
                title.equals("TOTAL SALES") ? "Total value of matching bills"
                : title.equals("TOTAL BILLS") ? "Number of matching bills"
                : "Average amount per bill");
        description.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        description.setForeground(muted);

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(value);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(description);

        JLabel icon = new JLabel(symbol, SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
        icon.setForeground(accent);
        icon.setOpaque(true);
        icon.setBackground(new Color(
                Math.min(255, accent.getRed() + 190),
                Math.min(255, accent.getGreen() + 190),
                Math.min(255, accent.getBlue() + 190)));
        icon.setPreferredSize(new Dimension(42, 55));

        card.add(textPanel, BorderLayout.CENTER);
        card.add(icon, BorderLayout.EAST);
        return card;
    }

    private void loadBills() {
        String sql = """
                SELECT bill_id, customer_name, phone, email,
                       bill_date, grand_total
                FROM bills
                WHERE CAST(bill_id AS CHAR) LIKE ?
                   OR COALESCE(customer_name, '') LIKE ?
                   OR COALESCE(phone, '') LIKE ?
                ORDER BY bill_date DESC, bill_id DESC
                """;

        model.setRowCount(0);

        double totalSales = 0;
        int totalBills = 0;
        String search = searchField.getText().trim();

        try (Connection conn = DbConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String pattern = "%" + search + "%";

            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int billId = rs.getInt("bill_id");
                    String billCode = String.format("B%03d", billId);
                    String customer = rs.getString("customer_name");
                    String phone = rs.getString("phone");
                    String email = rs.getString("email");
                    Timestamp date = rs.getTimestamp("bill_date");
                    double amount = rs.getDouble("grand_total");

                    model.addRow(new Object[]{
                            billCode,
                            customer == null ? "" : customer,
                            phone == null ? "" : phone,
                            email == null ? "" : email,
                            date == null ? "" : date.toString(),
                            currency.format(amount)
                    });

                    totalSales += amount;
                    totalBills++;
                }
            }

            totalSalesLabel.setText(currency.format(totalSales));
            totalBillsLabel.setText(String.valueOf(totalBills));
            averageBillLabel.setText(currency.format(
                    totalBills == 0 ? 0 : totalSales / totalBills));

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load sales history.\n"
                    + "Check your MySQL connection and password setting.\n\n"
                    + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
