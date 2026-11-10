import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(main::createLoginPage);
    }

    static void createLoginPage() {

        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("StockPilot | Inventory Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setMinimumSize(new Dimension(800, 540));
        frame.setLocationRelativeTo(null);
        frame.setLayout(new GridLayout(1, 2));

        // Left Branding Panel
        JPanel brand = new JPanel(new BorderLayout());
        brand.setBackground(new Color(15, 27, 49));
        brand.setBorder(new EmptyBorder(60, 48, 50, 44));

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("IM  /  STOCKPILOT");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        logo.setForeground(new Color(147, 197, 253));

        JLabel heading = new JLabel(
            "<html>Inventory,<br>under control.</html>"
        );
        heading.setFont(new Font("Segoe UI", Font.BOLD, 34));
        heading.setForeground(Color.WHITE);

        JLabel description = new JLabel(
            "<html>Keep products organized, monitor stock levels,<br>" +
            "and manage sales from one workspace.</html>"
        );
        description.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        description.setForeground(new Color(203, 213, 225));

        brandText.add(logo);
        brandText.add(Box.createVerticalStrut(58));
        brandText.add(heading);
        brandText.add(Box.createVerticalStrut(18));
        brandText.add(description);

        brand.add(brandText, BorderLayout.CENTER);

        JLabel footer = new JLabel("JAVA SWING  •  MYSQL");
        footer.setFont(new Font("Segoe UI", Font.BOLD, 10));
        footer.setForeground(new Color(100, 116, 139));

        brand.add(footer, BorderLayout.SOUTH);

        // Right Login Panel
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(new Color(248, 250, 252));
        right.setBorder(new EmptyBorder(36, 36, 36, 36));

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(226, 232, 241)
                ),
                new EmptyBorder(30, 28, 30, 28)
            )
        );

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 9, 0);

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(new Color(23, 37, 63));

        c.gridy = 0;
        card.add(welcome, c);

        JLabel subtitle = new JLabel(
            "Sign in to your inventory workspace"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(105, 119, 141));

        c.gridy = 1;
        c.insets = new Insets(0, 0, 28, 0);
        card.add(subtitle, c);

        JLabel userLabel = new JLabel("User ID");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userLabel.setForeground(new Color(51, 65, 85));

        c.gridy = 2;
        c.insets = new Insets(0, 0, 7, 0);
        card.add(userLabel, c);

        JTextField user = new JTextField();
        styleInput(user);

        c.gridy = 3;
        c.insets = new Insets(0, 0, 17, 0);
        card.add(user, c);

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(new Color(51, 65, 85));

        c.gridy = 4;
        c.insets = new Insets(0, 0, 7, 0);
        card.add(passLabel, c);

        JPasswordField pass = new JPasswordField();
        styleInput(pass);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 22, 0);
        card.add(pass, c);

        JButton login = new JButton("Sign in");
        login.setFont(new Font("Segoe UI", Font.BOLD, 14));
        login.setForeground(Color.WHITE);
        login.setBackground(new Color(37, 99, 235));
        login.setFocusPainted(false);
        login.setBorderPainted(false);
        login.setContentAreaFilled(true);
        login.setOpaque(true);
        login.setBorder(new EmptyBorder(13, 12, 13, 12));
        login.setCursor(new Cursor(Cursor.HAND_CURSOR));
        login.setPreferredSize(new Dimension(310, 48));

// Hover Effect
login.addMouseListener(new java.awt.event.MouseAdapter() {

    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
        login.setBackground(new Color(29, 78, 216));
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {
        login.setBackground(new Color(37, 99, 235));
    }
});


        c.gridy = 6;
        c.insets = new Insets(0, 0, 12, 0);
        card.add(login, c);

        JLabel tip = new JLabel(
            "Configure MySQL in config.properties before signing in."
        );
        tip.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        tip.setForeground(new Color(105, 119, 141));

        c.gridy = 7;
        card.add(tip, c);

        right.add(card);

        frame.add(brand);
        frame.add(right);

        // Authentication Logic
        Runnable authenticate = () -> {

            String userText = user.getText().trim();
            char[] password = pass.getPassword();

            if (userText.isEmpty() || password.length == 0) {
                JOptionPane.showMessageDialog(
                    frame,
                    "Enter your User ID and password.",
                    "Missing details",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            final int userId;

            try {
                userId = Integer.parseInt(userText);

                if (userId <= 0) {
                    throw new NumberFormatException();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(
                    frame,
                    "User ID must be a positive number.",
                    "Invalid User ID",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            try (
                Connection con = DbConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(
                    "SELECT password FROM authentication WHERE user_id=?"
                )
            ) {

                ps.setInt(1, userId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "User ID was not found.",
                            "Sign in failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    String stored = rs.getString("password");

                    if (!PasswordUtil.verify(password, stored)) {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Incorrect User ID or password.",
                            "Sign in failed",
                            JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    // Migrate Legacy Password to Hashed Password
                    if (PasswordUtil.isLegacy(stored)) {

                        try (
                            PreparedStatement update = con.prepareStatement(
                                "UPDATE authentication SET password=? " +
                                "WHERE user_id=? AND password=?"
                            )
                        ) {

                            update.setString(
                                1,
                                PasswordUtil.hash(password)
                            );

                            update.setInt(2, userId);
                            update.setString(3, stored);

                            update.executeUpdate();

                        } catch (SQLException migrationEx) {

                            System.err.println(
                                "Password verified, but its hash could not " +
                                "be saved. Run the authentication password-column " +
                                "migration in database-upgrade.sql."
                            );
                        }
                    }
                }

                frame.dispose();
                new InventoryFrame().setVisible(true);

            } catch (SQLException ex) {

                JOptionPane.showMessageDialog(
                    frame,
                    "Could not connect to MySQL. Check config.properties " +
                    "and confirm the Inventory database exists.\n\n" +
                    ex.getMessage(),
                    "Database connection",
                    JOptionPane.ERROR_MESSAGE
                );

            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        };

        // Login Button and Enter Key Actions
        login.addActionListener(e -> authenticate.run());
        pass.addActionListener(e -> authenticate.run());

        frame.getRootPane().setDefaultButton(login);
        frame.setVisible(true);
    }

    // Input Field Styling
    private static void styleInput(JTextField field) {

        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(310, 42));
        field.setBackground(Color.WHITE);
        field.setForeground(new Color(30, 41, 59));
        field.setCaretColor(new Color(30, 41, 59));
        field.setSelectionColor(new Color(37, 99, 235));
        field.setSelectedTextColor(Color.WHITE);
        field.setOpaque(true);

        field.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                    new Color(148, 163, 184),
                    1
                ),
                new EmptyBorder(8, 10, 8, 10)
            )
        );
    }
}