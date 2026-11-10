import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JFormattedTextField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicFormattedTextFieldUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.plaf.basic.BasicTextFieldUI;

/**
 * Centralized, contrast-safe light/dark themes for StockPilot.
 * Uses Basic Swing delegates for controls whose native Windows delegates may ignore
 * explicit foreground/background colors, and a dedicated table-header renderer.
 */
public final class ThemeManager {
    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);
    private static boolean darkMode = PREFS.getBoolean("darkMode", false);

    private static final String ORIGINAL_BG = "stockpilot.original.background";
    private static final String ORIGINAL_FG = "stockpilot.original.foreground";
    private static final String ORIGINAL_BORDER = "stockpilot.original.border";
    private static final String ORIGINAL_BUTTON_UI = "stockpilot.original.buttonUI";
    private static final String ORIGINAL_COMBO_RENDERER = "stockpilot.original.comboRenderer";

    // Light palette: nearly-white canvas, white input controls, dark readable text.
    private static final Color LIGHT_CANVAS = new Color(245, 247, 251);
    private static final Color LIGHT_SURFACE = Color.WHITE;
    private static final Color LIGHT_SURFACE_ALT = new Color(241, 245, 249);
    private static final Color LIGHT_TEXT = new Color(23, 37, 63);
    private static final Color LIGHT_MUTED = new Color(71, 85, 105);
    private static final Color LIGHT_INPUT = new Color(255, 255, 255);
    private static final Color LIGHT_BORDER = new Color(203, 213, 225);
    private static final Color LIGHT_SELECTION = new Color(219, 234, 254);

    // Dark palette: deep navy canvas, raised slate cards, dark inputs, bright text.
    private static final Color DARK_BG = new Color(15, 23, 42);
    private static final Color DARK_SURFACE = new Color(30, 41, 59);
    private static final Color DARK_SURFACE_ALT = new Color(39, 52, 73);
    private static final Color DARK_TEXT = new Color(241, 245, 249);
    private static final Color DARK_MUTED = new Color(203, 213, 225);
    private static final Color DARK_INPUT = new Color(11, 20, 36);
    private static final Color DARK_BORDER = new Color(100, 116, 139);
    private static final Color DARK_SELECTION = new Color(37, 80, 145);

    private ThemeManager() { }

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void toggle() {
        setDarkMode(!darkMode);
    }

    public static void setDarkMode(boolean enabled) {
        darkMode = enabled;
        PREFS.putBoolean("darkMode", enabled);
        updateLookAndFeelDefaults();
    }

    /** Applies the current palette to the complete Swing tree. Call after changing pages. */
    public static void apply(Component root) {
        updateLookAndFeelDefaults();
        if (root == null) return;
        applyTo(root);
        if (root instanceof JComponent jc) {
            jc.revalidate();
            jc.repaint();
        }
    }

    private static Color canvas() { return darkMode ? DARK_BG : LIGHT_CANVAS; }
    private static Color surface() { return darkMode ? DARK_SURFACE : LIGHT_SURFACE; }
    private static Color surfaceAlt() { return darkMode ? DARK_SURFACE_ALT : LIGHT_SURFACE_ALT; }
    private static Color text() { return darkMode ? DARK_TEXT : LIGHT_TEXT; }
    private static Color muted() { return darkMode ? DARK_MUTED : LIGHT_MUTED; }
    private static Color input() { return darkMode ? DARK_INPUT : LIGHT_INPUT; }
    private static Color border() { return darkMode ? DARK_BORDER : LIGHT_BORDER; }
    private static Color selection() { return darkMode ? DARK_SELECTION : LIGHT_SELECTION; }

    private static void updateLookAndFeelDefaults() {
        Color panel = surface();
        Color foreground = text();
        UIManager.put("Panel.background", panel);
        UIManager.put("OptionPane.background", panel);
        UIManager.put("OptionPane.messageForeground", foreground);
        UIManager.put("TextField.background", input());
        UIManager.put("TextField.foreground", foreground);
        UIManager.put("TextField.caretForeground", foreground);
        UIManager.put("PasswordField.background", input());
        UIManager.put("PasswordField.foreground", foreground);
        UIManager.put("PasswordField.caretForeground", foreground);
        UIManager.put("FormattedTextField.background", input());
        UIManager.put("FormattedTextField.foreground", foreground);
        UIManager.put("TextArea.background", input());
        UIManager.put("TextArea.foreground", foreground);
        UIManager.put("ComboBox.background", input());
        UIManager.put("ComboBox.foreground", foreground);
        UIManager.put("ComboBox.selectionBackground", selection());
        UIManager.put("ComboBox.selectionForeground", darkMode ? Color.WHITE : LIGHT_TEXT);
        UIManager.put("ComboBox.buttonBackground", surfaceAlt());
        UIManager.put("ComboBox.buttonShadow", border());
        UIManager.put("ComboBox.buttonDarkShadow", border());
        UIManager.put("ComboBox.buttonHighlight", surface());
        UIManager.put("List.background", input());
        UIManager.put("List.foreground", foreground);
        UIManager.put("List.selectionBackground", selection());
        UIManager.put("List.selectionForeground", darkMode ? Color.WHITE : LIGHT_TEXT);
        UIManager.put("Table.background", surface());
        UIManager.put("Table.foreground", foreground);
        UIManager.put("Table.selectionBackground", selection());
        UIManager.put("Table.selectionForeground", darkMode ? Color.WHITE : LIGHT_TEXT);
        UIManager.put("TableHeader.background", surfaceAlt());
        UIManager.put("TableHeader.foreground", foreground);
        UIManager.put("TableHeader.cellBorder", BorderFactory.createMatteBorder(0, 0, 1, 1, border()));
        UIManager.put("Button.background", surfaceAlt());
        UIManager.put("Button.foreground", foreground);
        UIManager.put("Button.select", selection());
        UIManager.put("Component.borderColor", border());
        UIManager.put("Separator.foreground", border());
        UIManager.put("Label.foreground", foreground);
    }

    private static void applyTo(Component component) {
        if (component instanceof JComponent jc) {
            rememberOriginals(jc);
            Color originalBg = (Color) jc.getClientProperty(ORIGINAL_BG);
            Color originalFg = (Color) jc.getClientProperty(ORIGINAL_FG);

            if (jc instanceof JTextComponent textComponent) {
                styleTextComponent(textComponent);
            } else if (jc instanceof JComboBox<?> combo) {
                styleComboBox(combo);
            } else if (jc instanceof JTable table) {
                styleTable(table);
            } else if (jc instanceof JTableHeader header) {
                styleTableHeader(header);
            } else if (jc instanceof JViewport viewport) {
                viewport.setBackground(darkMode ? DARK_BG : LIGHT_SURFACE);
                viewport.setOpaque(true);
            } else if (jc instanceof JScrollPane scroll) {
                scroll.setBackground(canvas());
                scroll.getViewport().setBackground(darkMode ? DARK_BG : LIGHT_SURFACE);
                scroll.setBorder(BorderFactory.createLineBorder(border()));
                scroll.setOpaque(true);
            } else if (jc instanceof JButton button) {
                styleButton(button, originalBg, originalFg);
            } else if (jc instanceof JLabel label) {
                styleLabel(label, originalBg, originalFg);
            } else {
                styleContainer(jc, originalBg, originalFg);
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) applyTo(child);
        }
    }

    private static void rememberOriginals(JComponent component) {
        if (component.getClientProperty(ORIGINAL_BG) == null)
            component.putClientProperty(ORIGINAL_BG, component.getBackground());
        if (component.getClientProperty(ORIGINAL_FG) == null)
            component.putClientProperty(ORIGINAL_FG, component.getForeground());
        if (component.getClientProperty(ORIGINAL_BORDER) == null && component.getBorder() != null)
            component.putClientProperty(ORIGINAL_BORDER, component.getBorder());
        if (component instanceof JButton button && component.getClientProperty(ORIGINAL_BUTTON_UI) == null)
            component.putClientProperty(ORIGINAL_BUTTON_UI, button.getUI());
        if (component instanceof JComboBox<?> combo && component.getClientProperty(ORIGINAL_COMBO_RENDERER) == null)
            component.putClientProperty(ORIGINAL_COMBO_RENDERER, combo.getRenderer());
    }

    private static void styleTextComponent(JTextComponent field) {
        // Force Basic UI delegates so Windows native delegates cannot paint low-contrast text.
        if (field instanceof JPasswordField password) password.setUI(new BasicPasswordFieldUI());
        else if (field instanceof JFormattedTextField formatted) formatted.setUI(new BasicFormattedTextFieldUI());
        else if (field instanceof JTextArea area) area.setUI(new BasicTextAreaUI());
        else if (field instanceof javax.swing.JTextField textField) textField.setUI(new BasicTextFieldUI());
        field.setFont(field.getFont() == null ? new Font("Segoe UI", Font.PLAIN, 13) : field.getFont());
        field.setBackground(input());
        field.setForeground(text());
        field.setCaretColor(text());
        field.setSelectionColor(selection());
        field.setSelectedTextColor(darkMode ? Color.WHITE : LIGHT_TEXT);
        field.setDisabledTextColor(muted());
        field.setOpaque(true);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border(), 1), new EmptyBorder(7, 10, 7, 10)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void styleComboBox(JComboBox combo) {
        combo.setUI(new BasicComboBoxUI());
        combo.setBackground(input());
        combo.setForeground(text());
        combo.setOpaque(true);
        combo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border(), 1), new EmptyBorder(2, 6, 2, 6)));
        combo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, cellHasFocus);
                label.setOpaque(true);
                label.setFont(combo.getFont() == null ? new Font("Segoe UI", Font.PLAIN, 13) : combo.getFont());
                label.setBackground(selected ? selection() : input());
                label.setForeground(selected && darkMode ? Color.WHITE : text());
                label.setBorder(new EmptyBorder(4, 6, 4, 6));
                return label;
            }
        });
        combo.setMaximumRowCount(12);
    }

    private static void styleTable(JTable table) {
        table.setBackground(surface());
        table.setForeground(text());
        table.setGridColor(border());
        table.setSelectionBackground(selection());
        table.setSelectionForeground(darkMode ? Color.WHITE : LIGHT_TEXT);
        table.setFillsViewportHeight(true);
        table.setOpaque(true);
        table.setRowHeight(Math.max(table.getRowHeight(), 30));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        JTableHeader header = table.getTableHeader();
        if (header != null) styleTableHeader(header);
    }

    private static void styleTableHeader(JTableHeader header) {
        header.setUI(new BasicTableHeaderUI());
        header.setBackground(surfaceAlt());
        header.setForeground(text());
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setOpaque(true);
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border()));
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
                label.setOpaque(true);
                label.setBackground(surfaceAlt());
                label.setForeground(text());
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, border()),
                    new EmptyBorder(8, 9, 8, 9)));
                return label;
            }
        };
        header.setDefaultRenderer(renderer);
        for (int i = 0; i < header.getColumnModel().getColumnCount(); i++) {
            header.getColumnModel().getColumn(i).setHeaderRenderer(renderer);
        }
    }

    private static void styleButton(JButton button, Color originalBg, Color originalFg) {
        // BasicButtonUI honors explicit colors on standard buttons. Keep arrow-button
        // delegates intact so spinner/combo arrows are still painted by their own UI.
        boolean arrowControl = button.getClass().getName().toLowerCase().contains("arrowbutton");
        if (!arrowControl) button.setUI(new BasicButtonUI());
        boolean brandSurface = originalBg != null && brightness(originalBg) < 0.20;
        boolean accent = isAccent(originalBg);
        Color bg;
        Color fg;
        if (accent) {
            bg = originalBg;
            fg = hasGoodContrast(Color.WHITE, bg) ? Color.WHITE : LIGHT_TEXT;
        } else if (brandSurface) {
            bg = originalBg;
            fg = darkMode ? DARK_TEXT : (isLight(originalFg) ? Color.WHITE : LIGHT_TEXT);
        } else {
            bg = surfaceAlt();
            fg = text();
        }
        button.setBackground(bg);
        button.setForeground(fg);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        if (arrowControl) {
            button.setBorder((Border) button.getClientProperty(ORIGINAL_BORDER));
            return;
        }
        if (accent) {
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darken(originalBg, 0.12)), new EmptyBorder(8, 12, 8, 12)));
        } else {
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(brandSurface ? new Color(51, 65, 85) : border()),
                new EmptyBorder(7, 11, 7, 11)));
        }
    }

    private static void styleLabel(JLabel label, Color originalBg, Color originalFg) {
        if (label.isOpaque()) label.setBackground(mapBackground(originalBg));
        if (originalFg == null) {
            label.setForeground(text());
            return;
        }
        if (isAccent(originalFg)) {
            label.setForeground(originalFg);
            return;
        }
        Color effective = label.isOpaque() ? label.getBackground() : nearestOpaqueBackground(label);
        if (isLight(originalFg)) {
            // White text is only retained over dark/colored surfaces; on pale cards it turns dark.
            label.setForeground(brightness(effective) > 0.62 ? LIGHT_TEXT : (darkMode ? DARK_TEXT : Color.WHITE));
        } else if (brightness(originalFg) < 0.34) {
            label.setForeground(darkMode ? DARK_TEXT : LIGHT_TEXT);
        } else {
            label.setForeground(darkMode ? DARK_MUTED : LIGHT_MUTED);
        }
    }

    private static void styleContainer(JComponent component, Color originalBg, Color originalFg) {
        if (component.isOpaque()) component.setBackground(mapBackground(originalBg));
        if (originalFg != null && !isAccent(originalFg) && brightness(originalFg) < 0.34)
            component.setForeground(text());
        else if (originalFg != null && !isAccent(originalFg) && brightness(originalFg) >= 0.34)
            component.setForeground(muted());
    }

    private static Color nearestOpaqueBackground(Component component) {
        Container parent = component.getParent();
        while (parent != null) {
            if (parent instanceof JComponent jc && jc.isOpaque()) return jc.getBackground();
            parent = parent.getParent();
        }
        return canvas();
    }

    private static Color mapBackground(Color original) {
        if (original == null) return darkMode ? DARK_BG : LIGHT_CANVAS;
        if (!darkMode) return original;
        if (brightness(original) < 0.20) return original; // Preserve navy brand surfaces.
        if (brightness(original) > 0.90) return DARK_SURFACE;
        if (brightness(original) > 0.72) return DARK_BG;
        return original;
    }

    private static boolean isAccent(Color color) {
        if (color == null) return false;
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return hsb[1] > 0.38f && hsb[2] > 0.25f && hsb[2] < 0.96f;
    }

    private static boolean isLight(Color color) {
        return color != null && brightness(color) > 0.78;
    }

    private static double brightness(Color color) {
        if (color == null) return 1.0;
        return (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
    }

    private static boolean hasGoodContrast(Color foreground, Color background) {
        return contrastRatio(foreground, background) >= 4.5;
    }

    private static double contrastRatio(Color a, Color b) {
        double la = relativeLuminance(a), lb = relativeLuminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double relativeLuminance(Color c) {
        double r = linear(c.getRed() / 255.0), g = linear(c.getGreen() / 255.0), b = linear(c.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double linear(double value) {
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static Color darken(Color c, double factor) {
        return new Color((int) Math.max(0, c.getRed() * (1 - factor)),
            (int) Math.max(0, c.getGreen() * (1 - factor)),
            (int) Math.max(0, c.getBlue() * (1 - factor)));
    }
}
