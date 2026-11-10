import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class InventoryFrame extends JFrame {
    private static final Color NAVY = new Color(15, 27, 49);
    private static final Color NAVY_LIGHT = new Color(29, 45, 70);
    private static final Color BLUE = new Color(47, 105, 235);
    private static final Color INK = new Color(23, 37, 63);
    private static final Color MUTED = new Color(105, 119, 141);
    private final JPanel pageHost = new JPanel(new BorderLayout());
    private final JLabel pageTitle = new JLabel("Dashboard");
    private final JLabel connectionStatus = new JLabel("●  DATABASE CONFIGURATION REQUIRED");
    private JButton activeButton;
    private JButton dashboardNavButton;
    private JButton themeToggleButton;

    public InventoryFrame() {
        setTitle("Inventory Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1370, 820));
        setSize(1660, 1000);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 251));

        add(createSidebar(), BorderLayout.WEST);
        add(createWorkspace(), BorderLayout.CENTER);
        if (dashboardNavButton != null) setActive(dashboardNavButton);
        showPage("Dashboard", new DashboardPanel(), null);
        refreshConnectionStatus();
        applyCurrentTheme();
    }

    private JComponent createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(225, 0));
        sidebar.setBackground(NAVY);
        sidebar.setBorder(new EmptyBorder(20, 12, 16, 12));

        JPanel brand = new JPanel(new BorderLayout(10, 0)); brand.setOpaque(false); brand.setBorder(new EmptyBorder(2, 8, 24, 4));
        JLabel mark = new JLabel("IM"); mark.setHorizontalAlignment(SwingConstants.CENTER); mark.setFont(new Font("Segoe UI", Font.BOLD, 18)); mark.setForeground(Color.WHITE); mark.setOpaque(true); mark.setBackground(BLUE); mark.setPreferredSize(new Dimension(42, 42));
        JPanel brandText = new JPanel(); brandText.setOpaque(false); brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("StockPilot"); name.setFont(new Font("Segoe UI", Font.BOLD, 17)); name.setForeground(Color.WHITE);
        JLabel small = new JLabel("INVENTORY SUITE"); small.setFont(new Font("Segoe UI", Font.BOLD, 9)); small.setForeground(new Color(148, 163, 184));
        brandText.add(name); brandText.add(Box.createVerticalStrut(4)); brandText.add(small); brand.add(mark, BorderLayout.WEST); brand.add(brandText, BorderLayout.CENTER); sidebar.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel(); nav.setOpaque(false); nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        JLabel section = new JLabel("WORKSPACE"); section.setFont(new Font("Segoe UI", Font.BOLD, 10)); section.setForeground(new Color(125, 145, 173)); section.setBorder(new EmptyBorder(0, 11, 10, 0)); section.setAlignmentX(Component.LEFT_ALIGNMENT); nav.add(section);
        addNav(nav, "▦", "Dashboard", () -> showPage("Dashboard", new DashboardPanel(), null));
        addDivider(nav);
        addNav(nav, "▤", "Products", () -> showPage("Products", new ProductFrame(), null));
        addNav(nav, "▣", "Stock", () -> showPage("Stock Management", new StockManagementPanel(), null));
        addNav(nav, "↗", "Purchases", () -> showPage("Purchases", new PurchaseManagementPanel(), null));
        addNav(nav, "♙", "Customers", () -> showPage("Customers", new CustomerFrame(), null));
        addDivider(nav);
        addNav(nav, "₹", "Billing / POS", () -> showPage("Billing / POS", new BillingFrame(), null));
        addNav(nav, "▧", "Bill History", () -> showPage("Bill History", new BillHistoryFrame(), null));
        addNav(nav, "▥", "Sales Analytics", () -> showPage("Sales Analytics", new SalesHistoryPanel(), null));
        sidebar.add(nav, BorderLayout.CENTER);

        JPanel footer = new JPanel(); footer.setOpaque(false); footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        JSeparator line = new JSeparator(); line.setForeground(new Color(51, 65, 85)); line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1)); footer.add(line); footer.add(Box.createVerticalStrut(12));
        JLabel footerMain = new JLabel("Inventory Management"); footerMain.setFont(new Font("Segoe UI", Font.BOLD, 11)); footerMain.setForeground(new Color(226, 232, 240));
        JLabel footerSub = new JLabel("Java Swing  ·  MySQL"); footerSub.setFont(new Font("Segoe UI", Font.PLAIN, 10)); footerSub.setForeground(new Color(148, 163, 184));
        footer.add(footerMain); footer.add(Box.createVerticalStrut(4)); footer.add(footerSub); sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private void addDivider(JPanel nav) {
        JSeparator separator = new JSeparator(); separator.setForeground(new Color(45, 61, 85)); separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setBorder(new EmptyBorder(7, 8, 7, 8)); separator.setAlignmentX(Component.LEFT_ALIGNMENT); nav.add(Box.createVerticalStrut(9)); nav.add(separator); nav.add(Box.createVerticalStrut(9));
    }

    private void addNav(JPanel nav, String glyph, String title, Runnable action) {
        JButton button = new JButton("  " + glyph + "     " + title);
        button.setHorizontalAlignment(SwingConstants.LEFT); button.setFont(new Font("Segoe UI", Font.PLAIN, 13)); button.setForeground(new Color(203, 213, 225)); button.setBackground(NAVY); button.setFocusPainted(false); button.setBorder(new EmptyBorder(12, 11, 12, 8)); button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44)); button.setPreferredSize(new Dimension(190, 44)); button.setAlignmentX(Component.LEFT_ALIGNMENT); button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); button.setOpaque(true);
        if ("Dashboard".equals(title)) dashboardNavButton = button;
        button.addActionListener(e -> { setActive(button); action.run(); });
        nav.add(button);
    }

    private JComponent createWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout()); workspace.setBackground(new Color(245, 247, 251));
        JPanel header = new JPanel(new BorderLayout(15, 0)); header.setBackground(Color.WHITE); header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 241)), new EmptyBorder(18, 26, 18, 26)));
        JPanel labels = new JPanel(); labels.setOpaque(false); labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 21)); pageTitle.setForeground(INK);
        JLabel sub = new JLabel("Operations overview  /  Manage your inventory with confidence"); sub.setFont(new Font("Segoe UI", Font.PLAIN, 11)); sub.setForeground(MUTED);
        labels.add(pageTitle); labels.add(Box.createVerticalStrut(4)); labels.add(sub);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0)); statusPanel.setOpaque(false);
        connectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 10)); connectionStatus.setForeground(new Color(180, 83, 9));
        JButton logout = new JButton("Sign out"); logout.setFont(new Font("Segoe UI", Font.BOLD, 11)); logout.setForeground(INK); logout.setBackground(new Color(248, 250, 252)); logout.setFocusPainted(false); logout.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(226,232,241)), new EmptyBorder(7, 12, 7, 12)));
        logout.addActionListener(e -> { dispose(); main.createLoginPage(); });
        themeToggleButton = new JButton();
        themeToggleButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        themeToggleButton.setForeground(INK);
        themeToggleButton.setBackground(new Color(248, 250, 252));
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        themeToggleButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(226, 232, 241)),
            new EmptyBorder(7, 12, 7, 12)));
        themeToggleButton.addActionListener(e -> {
            ThemeManager.toggle();
            applyCurrentTheme();
        });
        updateThemeToggleText();
        statusPanel.add(connectionStatus); statusPanel.add(themeToggleButton); statusPanel.add(logout);
        header.add(labels, BorderLayout.WEST); header.add(statusPanel, BorderLayout.EAST);
        pageHost.setBackground(new Color(245, 247, 251));
        workspace.add(header, BorderLayout.NORTH); workspace.add(pageHost, BorderLayout.CENTER);
        return workspace;
    }

    private void setActive(JButton button) {
        if (activeButton != null) { activeButton.setBackground(NAVY); activeButton.setForeground(new Color(203, 213, 225)); }
        activeButton = button; activeButton.setBackground(NAVY_LIGHT); activeButton.setForeground(Color.WHITE);
    }

    private void showPage(String title, JComponent component, JButton source) {
        pageTitle.setText(title);
        pageHost.removeAll();
        JPanel page = new JPanel(new BorderLayout()); page.setOpaque(false); page.setBorder(new EmptyBorder(0, 0, 0, 0));
        if (component instanceof BillingFrame) {
            component.setPreferredSize(new Dimension(1360, 780));
            JScrollPane scroll = new JScrollPane(component);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getViewport().setBackground(new Color(245, 247, 251));
            page.add(scroll, BorderLayout.CENTER);
        } else {
            page.add(component, BorderLayout.CENTER);
        }
        pageHost.add(page, BorderLayout.CENTER); pageHost.revalidate(); pageHost.repaint();
        if (source != null) setActive(source);
        applyCurrentTheme();
        refreshConnectionStatus();
    }

    private void updateThemeToggleText() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isDarkMode() ? "☀ Light mode" : "☾ Dark mode");
            themeToggleButton.setToolTipText(ThemeManager.isDarkMode()
                ? "Switch to light appearance" : "Switch to dark appearance");
        }
    }

    private void applyCurrentTheme() {
        updateThemeToggleText();
        ThemeManager.apply(getContentPane());
        if (activeButton != null) {
            activeButton.setBackground(NAVY_LIGHT);
            activeButton.setForeground(Color.WHITE);
        }
        getContentPane().setBackground(ThemeManager.isDarkMode()
            ? new Color(15, 23, 42) : new Color(245, 247, 251));
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void refreshConnectionStatus() {
        SwingUtilities.invokeLater(() -> {
            try (Connection ignored = DbConfig.getConnection()) {
                connectionStatus.setText("●  MYSQL CONNECTED"); connectionStatus.setForeground(new Color(21, 128, 61));
            } catch (SQLException ex) {
                connectionStatus.setText("●  MYSQL NOT CONNECTED"); connectionStatus.setForeground(new Color(185, 28, 28));
                connectionStatus.setToolTipText(ex.getMessage());
            }
        });
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new InventoryFrame().setVisible(true)); }
}
