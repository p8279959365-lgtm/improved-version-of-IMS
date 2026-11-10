import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel {
    private final Color ink = new Color(23, 37, 63);
    private final Color muted = new Color(105, 119, 141);
    private final Color blue = new Color(37, 99, 235);
    private final JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
    private final DefaultTableModel lowStockModel = new DefaultTableModel(
            new String[]{"Product ID", "Product", "Supplier", "Available Qty"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel status = new JLabel("Ready");

    public DashboardPanel() {
        setLayout(new BorderLayout(18, 18));
        setBackground(new Color(245, 247, 251));
        setBorder(new EmptyBorder(22, 24, 24, 24));

        JPanel top = new JPanel(new BorderLayout(8, 6));
        top.setOpaque(false);
        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(ink);
        JLabel sub = new JLabel("A live overview of products, stock and sales.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(muted);
        titleStack.add(title);
        titleStack.add(Box.createVerticalStrut(5));
        titleStack.add(sub);
        JButton refresh = new JButton("Refresh data");
        refresh.setFocusPainted(false);
        refresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refresh.setForeground(Color.WHITE);
        refresh.setBackground(blue);
        refresh.setBorder(new EmptyBorder(10, 16, 10, 16));
        refresh.addActionListener(e -> loadData());
        top.add(titleStack, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);

        metrics.setOpaque(false);
        JPanel metricArea = new JPanel(new BorderLayout());
        metricArea.setOpaque(false);
        metricArea.add(metrics, BorderLayout.NORTH);

        JPanel lowStockCard = new JPanel(new BorderLayout(10, 12));
        lowStockCard.setBackground(Color.WHITE);
        lowStockCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 241)),
                new EmptyBorder(18, 18, 18, 18)));
        JPanel lowHead = new JPanel(new BorderLayout());
        lowHead.setOpaque(false);
        JLabel lowTitle = new JLabel("Low stock alerts");
        lowTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lowTitle.setForeground(ink);
        JLabel lowSub = new JLabel("Products with 5 or fewer units remaining");
        lowSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lowSub.setForeground(muted);
        JPanel lowLabels = new JPanel();
        lowLabels.setOpaque(false);
        lowLabels.setLayout(new BoxLayout(lowLabels, BoxLayout.Y_AXIS));
        lowLabels.add(lowTitle);
        lowLabels.add(Box.createVerticalStrut(4));
        lowLabels.add(lowSub);
        lowHead.add(lowLabels, BorderLayout.WEST);
        lowStockCard.add(lowHead, BorderLayout.NORTH);

        JTable table = new JTable(lowStockModel);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(ink);
        table.setGridColor(new Color(235, 239, 245));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(224, 236, 255));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(241, 245, 250));
        table.getTableHeader().setForeground(ink);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        lowStockCard.add(new JScrollPane(table), BorderLayout.CENTER);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.setForeground(muted);
        lowStockCard.add(status, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(0, 18));
        center.setOpaque(false);
        center.add(metricArea, BorderLayout.NORTH);
        center.add(lowStockCard, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        loadData();
    }

    private JPanel metricCard(String label, String value, String hint, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 241)),
                        new EmptyBorder(15, 15, 15, 15))));
        JLabel name = new JLabel(label);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(muted);
        JLabel number = new JLabel(value);
        number.setFont(new Font("Segoe UI", Font.BOLD, 26));
        number.setForeground(ink);
        JLabel small = new JLabel(hint);
        small.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        small.setForeground(muted);
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(name);
        text.add(Box.createVerticalStrut(8));
        text.add(number);
        text.add(Box.createVerticalStrut(4));
        text.add(small);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private void loadData() {
        metrics.removeAll();
        lowStockModel.setRowCount(0);
        try (Connection con = DbConfig.getConnection()) {
            long products = scalar(con, "SELECT COUNT(*) FROM product");
            long stockUnits = scalar(con, "SELECT COALESCE(SUM(quantity),0) FROM stock");
            double inventoryValue = scalarDouble(con, "SELECT COALESCE(SUM(total),0) FROM stock");
            long bills = scalar(con, "SELECT COUNT(*) FROM bills");
            metrics.add(metricCard("PRODUCTS", String.valueOf(products), "Catalog items", new Color(37, 99, 235)));
            metrics.add(metricCard("UNITS IN STOCK", String.valueOf(stockUnits), "Across all stock records", new Color(8, 145, 178)));
            metrics.add(metricCard("INVENTORY VALUE", money(inventoryValue), "Current recorded stock cost", new Color(22, 163, 74)));
            metrics.add(metricCard("BILLS CREATED", String.valueOf(bills), "All saved bills", new Color(124, 58, 237)));
            String sql = "SELECT product_id, product_name, supplier, quantity FROM stock WHERE quantity <= 5 ORDER BY quantity ASC, product_name ASC LIMIT 100";
            try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lowStockModel.addRow(new Object[]{rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4)});
            }
            status.setText(lowStockModel.getRowCount() == 0 ? "All stocked products are above the low-stock threshold." : lowStockModel.getRowCount() + " product(s) need attention.");
        } catch (SQLException ex) {
            metrics.add(metricCard("DATABASE", "Unavailable", "Configure MySQL connection", new Color(220, 38, 38)));
            status.setText("Could not load live data: " + ex.getMessage());
        }
        metrics.revalidate();
        metrics.repaint();
    }

    private long scalar(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getLong(1); }
    }
    private double scalarDouble(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getDouble(1); }
    }
    private String money(double value) { return String.format("₹%,.2f", value); }
}
