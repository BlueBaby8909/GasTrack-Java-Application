package finals;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.text.NumberFormat;
import javax.swing.table.TableCellEditor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CustomerView extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private DefaultTableModel tableModel;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    
    private JLabel totalCustomersLabel;
    private JLabel withBalanceLabel;
    private JLabel totalRevenueLabel;
    private JLabel totalOrdersLabel;
    
    Connection conn = null;
    Statement stmt;
    PreparedStatement pst;
    ResultSet rs;
    
    static int totalCustomers = 0;
    static int withBalance = 0;
    static double totalRevenue = 0;
    static int totalOrders = 0;

    public CustomerView(Connection connection) {
        this.conn = connection;
        initialize();
        refreshTable();
    }

    public void refresh() {
        refreshTable();
    }

    private void refreshTable() {
        try {
            String query = "SELECT * FROM customers";
            pst = conn.prepareStatement(query);
            rs = pst.executeQuery();
            resultSetToTable(rs);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Table refresh failed: " + e.getMessage());
        }
    }

    private void resultSetToTable(ResultSet rs) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        totalCustomers = 0;
        withBalance = 0;
        totalRevenue = 0;
        totalOrders = 0;

        try {
            while (rs.next()) {
                totalCustomers++;
                double balance = rs.getDouble("Balance");
                if (balance > 0) withBalance++;
                
                int orders = rs.getInt("Orders");
                totalOrders += orders;
                
                double spent = rs.getDouble("TotalSpent");
                totalRevenue += spent;

                Object[] row = {
                    rs.getString("Name"),
                    rs.getString("Contact"),
                    rs.getString("Address"),
                    rs.getString("Orders"),
                    currencyFormat.format(spent),
                    balance > 0 ? "With Balance" : "Paid",
                    "" // placeholder for buttons
                };
                model.addRow(row);
            }

            // Update stat cards
            totalCustomersLabel.setText(String.valueOf(totalCustomers));
            withBalanceLabel.setText(String.valueOf(withBalance));
            totalRevenueLabel.setText(currencyFormat.format(totalRevenue));
            totalOrdersLabel.setText(String.valueOf(totalOrders));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "ResultSet Exception -> " + e);
        }
    }

    public void initialize() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setOpaque(false);

        totalCustomersLabel = addStatCard(statsPanel, "Total Customers", "0");
        withBalanceLabel = addStatCard(statsPanel, "With Balance", "0");
        totalRevenueLabel = addStatCard(statsPanel, "Total Revenue", currencyFormat.format(0));
        totalOrdersLabel = addStatCard(statsPanel, "Total Orders", "0");

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText().trim();
                String status = statusFilter.getSelectedItem().toString();

                try {
                    StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE 1=1");
                    
                    if (!text.isEmpty()) {
                        sql.append(" AND Name LIKE ?");
                    }
                    
                    if (!status.equals("All")) {
                        if (status.equals("With Balance")) {
                            sql.append(" AND Balance > 0");
                        } else {
                            sql.append(" AND Balance = 0");
                        }
                    }

                    PreparedStatement pst = conn.prepareStatement(sql.toString());
                    if (!text.isEmpty()) {
                        pst.setString(1, "%" + text + "%");
                    }

                    ResultSet rs = pst.executeQuery();
                    resultSetToTable(rs);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Search failed: " + ex.getMessage());
                }
            }
        });
        
        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField);
        controlPanel.add(Box.createHorizontalStrut(10));

        statusFilter = new JComboBox<>(new String[]{"All", "Paid", "With Balance"});
        statusFilter.setPreferredSize(new Dimension(120, 30));
        statusFilter.addActionListener(e -> {
            String text = searchField.getText().trim();
            String status = statusFilter.getSelectedItem().toString();

            try {
                StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE 1=1");
                
                if (!text.isEmpty()) {
                    sql.append(" AND Name LIKE ?");
                }
                
                if (!status.equals("All")) {
                    if (status.equals("With Balance")) {
                        sql.append(" AND Balance > 0");
                    } else {
                        sql.append(" AND Balance = 0");
                    }
                }

                PreparedStatement pst = conn.prepareStatement(sql.toString());
                if (!text.isEmpty()) {
                    pst.setString(1, "%" + text + "%");
                }

                ResultSet rs = pst.executeQuery();
                resultSetToTable(rs);
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Filter failed: " + ex.getMessage());
            }
        });
        
        controlPanel.add(new JLabel("Status:"));
        controlPanel.add(statusFilter);
        controlPanel.add(Box.createHorizontalStrut(20));

        JButton addButton = new JButton("Add Customer");
        addButton.setPreferredSize(new Dimension(120, 30));
        addButton.addActionListener(e -> {
            openCustomerDialog(-1); // -1 indicates new customer
        });
        controlPanel.add(addButton);

        // Table
        String[] columnNames = {
            "Customer", "Contact", "Address", "Orders", "Total Spent", "Status", "Actions"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only make Actions column editable
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(50);
        table.setShowGrid(true);
        table.setGridColor(new Color(229, 231, 235));

        // Set up the action buttons column
        table.getColumnModel().getColumn(6).setCellRenderer(new ButtonsRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ButtonsEditor(table));
        table.getColumnModel().getColumn(6).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setMinWidth(80);
        table.getColumnModel().getColumn(6).setMaxWidth(80);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        // Layout assembly
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setOpaque(false);
        northPanel.add(statsPanel, BorderLayout.NORTH);
        northPanel.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
        northPanel.add(controlPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JLabel addStatCard(JPanel container, String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(107, 114, 128));
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(new Color(17, 24, 39));
        valueLabel.setFont(new Font("Arial", Font.BOLD, 18));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        container.add(card);
        
        return valueLabel;
    }

    class ButtonsRenderer implements TableCellRenderer {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private JButton payButton;

        public ButtonsRenderer() {
            panel = new JPanel(new GridLayout(3, 1, 0, 2));
            editButton = new JButton("Edit");
            deleteButton = new JButton("Delete");
            payButton = new JButton("Pay Balance");
            
            Dimension btnSize = new Dimension(70, 20);
            editButton.setPreferredSize(btnSize);
            deleteButton.setPreferredSize(btnSize);
            payButton.setPreferredSize(btnSize);
            editButton.setMargin(new Insets(0, 0, 0, 0));
            deleteButton.setMargin(new Insets(0, 0, 0, 0));
            payButton.setMargin(new Insets(0, 0, 0, 0));
            
            panel.add(editButton);
            panel.add(deleteButton);
            panel.add(payButton);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            
            // Only show Pay Balance button if customer has balance
            String status = (String) table.getValueAt(row, 5); // Status column
            payButton.setVisible(status.equals("With Balance"));
            
            return panel;
        }
    }

    class ButtonsEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private JButton payButton;
        private JTable table;

        public ButtonsEditor(JTable table) {
            this.table = table;
            panel = new JPanel(new GridLayout(3, 1, 0, 2));
            editButton = new JButton("Edit");
            deleteButton = new JButton("Delete");
            payButton = new JButton("Pay Balance");
            
            Dimension btnSize = new Dimension(70, 20);
            editButton.setPreferredSize(btnSize);
            deleteButton.setPreferredSize(btnSize);
            payButton.setPreferredSize(btnSize);
            editButton.setMargin(new Insets(0, 0, 0, 0));
            deleteButton.setMargin(new Insets(0, 0, 0, 0));
            payButton.setMargin(new Insets(0, 0, 0, 0));
            
            editButton.addActionListener(e -> {
                int row = table.getSelectedRow();
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                editCustomer(row);
            });
            
            deleteButton.addActionListener(e -> {
                int row = table.getSelectedRow();
                deleteCustomer(row);
            });
            
            payButton.addActionListener(e -> {
                int row = table.getSelectedRow();
                payBalance(row);
            });
            
            panel.add(editButton);
            panel.add(deleteButton);
            panel.add(payButton);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            panel.setBackground(table.getSelectionBackground());
            
            // Only show Pay Balance button if customer has balance
            String status = (String) table.getValueAt(row, 5); // Status column
            payButton.setVisible(status.equals("With Balance"));
            
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private void editCustomer(int row) {
        openCustomerDialog(row);
    }

    private void openCustomerDialog(int row) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(CustomerView.this);
        boolean isEdit = (row >= 0);
        String dialogTitle = isEdit ? "Edit Customer" : "Add New Customer";
        String saveButtonText = isEdit ? "Save Changes" : "Save";

        JDialog dlg = new JDialog(parentFrame, dialogTitle, true);
        JPanel content = new JPanel(new GridLayout(5, 2, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JTextField nameField = new JTextField(20);
        JTextField contactField = new JTextField(20);
        JTextField addressField = new JTextField(20);

        final String existingCustomerName = isEdit ? (String) table.getValueAt(row, 0) : null; // assuming Name is column 0

        if (isEdit) {
            try {
                String query = "SELECT * FROM customers WHERE Name = ?";
                PreparedStatement pst = conn.prepareStatement(query);
                pst.setString(1, existingCustomerName);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    nameField.setText(rs.getString("Name"));
                    contactField.setText(rs.getString("Contact"));
                    addressField.setText(rs.getString("Address"));
                } else {
                    JOptionPane.showMessageDialog(this, "Customer not found.");
                    dlg.dispose();
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error fetching customer details: " + e.getMessage());
                dlg.dispose();
                return;
            }
        }

        content.add(new JLabel("Customer Name:")); content.add(nameField);
        content.add(new JLabel("Contact:")); content.add(contactField);
        content.add(new JLabel("Address:")); content.add(addressField);

        JButton saveButton = new JButton(saveButtonText);
        saveButton.addActionListener(evt -> {
            String name = nameField.getText().trim();
            String contact = contactField.getText().trim();
            String address = addressField.getText().trim();

            if (name.isEmpty() || contact.isEmpty() || address.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "All fields are required!");
                return;
            }

            try {
                if (isEdit) {
                    String updateSql = "UPDATE customers SET Name=?, Contact=?, Address=? WHERE Name=?";
                    PreparedStatement updatePst = conn.prepareStatement(updateSql);
                    updatePst.setString(1, name);
                    updatePst.setString(2, contact);
                    updatePst.setString(3, address);
                    updatePst.setString(4, existingCustomerName);
                    updatePst.executeUpdate();
                    JOptionPane.showMessageDialog(dlg, "Customer updated successfully!");
                } else {
                    String insertSql = "INSERT INTO customers (Name, Contact, Address, Orders, TotalSpent, Balance) VALUES (?, ?, ?, 0, 0, 0)";
                    PreparedStatement insertPst = conn.prepareStatement(insertSql);
                    insertPst.setString(1, name);
                    insertPst.setString(2, contact);
                    insertPst.setString(3, address);
                    insertPst.executeUpdate();
                    JOptionPane.showMessageDialog(dlg, "Customer added successfully!");
                }

                dlg.dispose();
                refreshTable();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dlg, "Error saving customer: " + ex.getMessage());
            }
        });
        content.add(saveButton);

        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(parentFrame);
        dlg.setVisible(true);
    }

    private void deleteCustomer(int row) {
        if (row < 0) return; // If no row is selected, do nothing
        
        String customerName = (String) table.getValueAt(row, 0);
        
        // Stop table editing before showing dialog
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the customer: " + customerName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM customers WHERE Name = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, customerName);
                pst.executeUpdate();
                
                // Instead of refreshing the whole table, just remove the row from the model
                SwingUtilities.invokeLater(() -> {
                    ((DefaultTableModel)table.getModel()).removeRow(row);
                    JOptionPane.showMessageDialog(this, "Customer deleted successfully!");
                });
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting customer: " + e.getMessage());
            }
        }
    }

    private void payBalance(int row) {
        if (row < 0) return;
        
        String customerName = (String) table.getValueAt(row, 0);
        
        try {
            // Get current balance
            String balanceQuery = "SELECT Balance FROM customers WHERE Name = ?";
            PreparedStatement pst = conn.prepareStatement(balanceQuery);
            pst.setString(1, customerName);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                double balance = rs.getDouble("Balance");
                
                // Create payment dialog
                JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Pay Balance", true);
                JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
                panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                
                JLabel balanceLabel = new JLabel("Current Balance:");
                JLabel balanceAmount = new JLabel(currencyFormat.format(balance));
                JLabel paymentLabel = new JLabel("Payment Amount:");
                JTextField paymentField = new JTextField();
                JButton payButton = new JButton("Pay");
                
                panel.add(balanceLabel);
                panel.add(balanceAmount);
                panel.add(paymentLabel);
                panel.add(paymentField);
                panel.add(new JLabel()); // Spacer
                panel.add(payButton);
                
                payButton.addActionListener(e -> {
                    try {
                        double payment = Double.parseDouble(paymentField.getText());
                        if (payment <= 0) {
                            JOptionPane.showMessageDialog(dialog, "Please enter a valid payment amount.");
                            return;
                        }
                        if (payment > balance) {
                            JOptionPane.showMessageDialog(dialog, "Payment amount cannot be greater than balance.");
                            return;
                        }
                        
                        // Update customer balance
                        String updateSql = "UPDATE customers SET Balance = Balance - ? WHERE Name = ?";
                        PreparedStatement updatePst = conn.prepareStatement(updateSql);
                        updatePst.setDouble(1, payment);
                        updatePst.setString(2, customerName);
                        updatePst.executeUpdate();
                        
                        dialog.dispose();
                        JOptionPane.showMessageDialog(this, 
                            "Payment of " + currencyFormat.format(payment) + " processed successfully!\n" +
                            "Remaining balance: " + currencyFormat.format(balance - payment));
                        
                        refreshTable();
                        
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(dialog, "Please enter a valid number.");
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error processing payment: " + ex.getMessage());
                    }
                });
                
                dialog.add(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this);
                dialog.setVisible(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error getting customer balance: " + e.getMessage());
        }
    }
}
