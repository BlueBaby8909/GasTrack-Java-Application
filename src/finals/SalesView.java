package finals;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SalesView extends JPanel {
    private static final long serialVersionUID = 1L;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JComboBox<String> customerCombo;
    private JLabel customerStatusLabel;
    private JComboBox<String> productCombo;
    private JSpinner quantitySpinner;
    private JLabel totalLabel;
    private JTextField amountField;
    private JLabel changeLabel;
    private JLabel profitLabel;
    private JRadioButton onSiteRadio;
    private JRadioButton deliveryRadio;
    private JLabel dateLabel;
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
    
    // Stats labels
    private JLabel todaySalesLabel;
    private JLabel todayProfitLabel;
    private JLabel totalOrdersLabel;
    private JLabel pendingDeliveriesLabel;
    
    private Connection conn;
    private double total = 0.0;
    private double profit = 0.0;
    private LocalDate currentDate = LocalDate.now();
    
    // Store cart items
    private List<CartItem> cartItems = new ArrayList<>();
    
    class CartItem {
        String sku;
        String product;
        int quantity;
        double price;
        double cost;
        
        CartItem(String sku, String product, int quantity, double price, double cost) {
            this.sku = sku;
            this.product = product;
            this.quantity = quantity;
            this.price = price;
            this.cost = cost;
        }
    }

    public SalesView(Connection connection) {
        this.conn = connection;
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Top Panel with Stats
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        todaySalesLabel = addStatCard(statsPanel, "Today's Sales", currencyFormat.format(0));
        todayProfitLabel = addStatCard(statsPanel, "Today's Profit", currencyFormat.format(0));
        totalOrdersLabel = addStatCard(statsPanel, "Today's Orders", "0");
        pendingDeliveriesLabel = addStatCard(statsPanel, "Pending Deliveries", "0");
        updateDailyStats(); // Initialize stats

        // Main Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        
        // Left Panel - Selection Controls
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

        // Date Display
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateLabel = new JLabel("Date: " + currentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        dateLabel.setFont(new Font("Arial", Font.BOLD, 14));
        datePanel.add(dateLabel);
        leftPanel.add(datePanel);
        leftPanel.add(Box.createVerticalStrut(10));

        // Customer Selection with Status
        JPanel customerPanel = new JPanel(new BorderLayout(5, 5));
        JPanel customerTopPanel = new JPanel(new BorderLayout(5, 5));
        customerTopPanel.add(new JLabel("Customer:"), BorderLayout.WEST);
        customerCombo = new JComboBox<>();
        customerCombo.addActionListener(e -> updateCustomerStatus());
        customerTopPanel.add(customerCombo);
        JButton addCustomerBtn = new JButton("+");
        addCustomerBtn.addActionListener(e -> addNewCustomer());
        customerTopPanel.add(addCustomerBtn, BorderLayout.EAST);
        customerPanel.add(customerTopPanel, BorderLayout.NORTH);
        
        // Customer Status
        customerStatusLabel = new JLabel("Status: -");
        customerStatusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        customerPanel.add(customerStatusLabel, BorderLayout.SOUTH);
        
        leftPanel.add(customerPanel);
        leftPanel.add(Box.createVerticalStrut(10));

        // Product Selection
        JPanel productPanel = new JPanel(new BorderLayout(5, 5));
        productPanel.add(new JLabel("Product:"), BorderLayout.WEST);
        productCombo = new JComboBox<>();
        loadProducts();
        productPanel.add(productCombo);
        leftPanel.add(productPanel);
        leftPanel.add(Box.createVerticalStrut(10));

        // Quantity Selection
        JPanel quantityPanel = new JPanel(new BorderLayout(5, 5));
        quantityPanel.add(new JLabel("Quantity:"), BorderLayout.WEST);
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        quantityPanel.add(quantitySpinner);
        leftPanel.add(quantityPanel);
        leftPanel.add(Box.createVerticalStrut(10));

        // Add to Cart Button
        JButton addToCartBtn = new JButton("Add to Cart");
        addToCartBtn.addActionListener(e -> addToCart());
        leftPanel.add(addToCartBtn);
        leftPanel.add(Box.createVerticalStrut(20));

        // Delivery Options
        JPanel deliveryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        deliveryPanel.setBorder(BorderFactory.createTitledBorder("Delivery Option"));
        ButtonGroup deliveryGroup = new ButtonGroup();
        onSiteRadio = new JRadioButton("On-site", true);
        deliveryRadio = new JRadioButton("Delivery");
        deliveryGroup.add(onSiteRadio);
        deliveryGroup.add(deliveryRadio);
        deliveryPanel.add(onSiteRadio);
        deliveryPanel.add(deliveryRadio);
        leftPanel.add(deliveryPanel);

        // Right Panel - Cart and Payment
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        
        // Cart Table
        String[] columns = {"Product", "Quantity", "Price", "Subtotal"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        cartTable = new JTable(cartModel);
        cartTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(cartTable);
        rightPanel.add(scrollPane, BorderLayout.CENTER);

        // Payment Panel
        JPanel paymentPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        paymentPanel.setBorder(BorderFactory.createTitledBorder("Payment Details"));
        
        totalLabel = new JLabel(currencyFormat.format(0));
        profitLabel = new JLabel(currencyFormat.format(0));
        amountField = new JTextField();
        changeLabel = new JLabel(currencyFormat.format(0));
        
        paymentPanel.add(new JLabel("Total:"));
        paymentPanel.add(totalLabel);
        paymentPanel.add(new JLabel("Amount:"));
        paymentPanel.add(amountField);
        paymentPanel.add(new JLabel("Change:"));
        paymentPanel.add(changeLabel);
        paymentPanel.add(new JLabel("Profit:"));
        paymentPanel.add(profitLabel);
        
        // Add amount field listener
        amountField.addActionListener(e -> calculateChange());
        
        rightPanel.add(paymentPanel, BorderLayout.SOUTH);

        // Complete Sale Button
        JButton completeSaleBtn = new JButton("Complete Sale");
        completeSaleBtn.addActionListener(e -> completeSale());
        
        // Add panels to content
        contentPanel.add(leftPanel, BorderLayout.WEST);
        contentPanel.add(rightPanel, BorderLayout.CENTER);
        contentPanel.add(completeSaleBtn, BorderLayout.SOUTH);

        // Add all panels to main panel
        add(statsPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        loadCustomers();
        checkAndResetDailyStats();
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

    private void loadCustomers() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Name FROM customers ORDER BY Name");
            customerCombo.removeAllItems();
            while (rs.next()) {
                customerCombo.addItem(rs.getString("Name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
        }
    }

    private void loadProducts() {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Product FROM products WHERE Quantity > 0 ORDER BY Product");
            productCombo.removeAllItems();
            while (rs.next()) {
                productCombo.addItem(rs.getString("Product"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage());
        }
    }

    private void addNewCustomer() {
        JDialog dialog = new JDialog((Frame)SwingUtilities.getWindowAncestor(this), "Add New Customer", true);
        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField();
        JTextField contactField = new JTextField();
        JTextField addressField = new JTextField();

        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Contact:"));
        panel.add(contactField);
        panel.add(new JLabel("Address:"));
        panel.add(addressField);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(e -> {
            try {
                String sql = "INSERT INTO customers (Name, Contact, Address, Orders, TotalSpent, Balance) VALUES (?, ?, ?, 0, 0, 0)";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, nameField.getText().trim());
                pst.setString(2, contactField.getText().trim());
                pst.setString(3, addressField.getText().trim());
                pst.executeUpdate();
                
                loadCustomers();
                customerCombo.setSelectedItem(nameField.getText().trim());
                dialog.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Error adding customer: " + ex.getMessage());
            }
        });

        panel.add(saveBtn);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addToCart() {
        String productName = (String) productCombo.getSelectedItem();
        if (productName == null) {
            JOptionPane.showMessageDialog(this, "Please select a product");
            return;
        }

        try {
            PreparedStatement pst = conn.prepareStatement(
                "SELECT SKU, Quantity, Price, ProductCost FROM products WHERE Product = ?"
            );
            pst.setString(1, productName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                int availableQty = rs.getInt("Quantity");
                int requestedQty = (Integer) quantitySpinner.getValue();
                
                if (requestedQty > availableQty) {
                    JOptionPane.showMessageDialog(this, 
                        "Not enough stock. Available: " + availableQty);
                    return;
                }

                double price = rs.getDouble("Price");
                double cost = rs.getDouble("ProductCost");
                String sku = rs.getString("SKU");
                
                // Add to cart items
                cartItems.add(new CartItem(sku, productName, requestedQty, price, cost));
                
                // Update cart table
                Vector<Object> row = new Vector<>();
                row.add(productName);
                row.add(requestedQty);
                row.add(currencyFormat.format(price));
                row.add(currencyFormat.format(price * requestedQty));
                cartModel.addRow(row);

                // Update totals
                total += (price * requestedQty);
                profit += ((price - cost) * requestedQty);
                totalLabel.setText(currencyFormat.format(total));
                profitLabel.setText(currencyFormat.format(profit));

                // Reset quantity spinner
                quantitySpinner.setValue(1);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding to cart: " + e.getMessage());
        }
    }

    private void calculateChange() {
        try {
            double amount = Double.parseDouble(amountField.getText());
            double change = amount - total;
            changeLabel.setText(currencyFormat.format(change));
        } catch (NumberFormatException e) {
            changeLabel.setText(currencyFormat.format(0));
        }
    }

    private void updateCustomerStatus() {
        String customerName = (String) customerCombo.getSelectedItem();
        if (customerName != null) {
            try {
                PreparedStatement pst = conn.prepareStatement(
                    "SELECT Balance, TotalSpent FROM customers WHERE Name = ?"
                );
                pst.setString(1, customerName);
                ResultSet rs = pst.executeQuery();
                
                if (rs.next()) {
                    double balance = rs.getDouble("Balance");
                    double totalSpent = rs.getDouble("TotalSpent");
                    
                    StringBuilder status = new StringBuilder("Status: ");
                    if (balance > 0) {
                        status.append("With Balance (").append(currencyFormat.format(balance)).append(")");
                        customerStatusLabel.setForeground(new Color(220, 53, 69)); // Red
                    } else {
                        status.append("Fully Paid");
                        customerStatusLabel.setForeground(new Color(40, 167, 69)); // Green
                    }
                    status.append("\nTotal Spent: ").append(currencyFormat.format(totalSpent));
                    
                    customerStatusLabel.setText(status.toString());
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error checking customer status: " + e.getMessage());
            }
        }
    }

    private void checkAndResetDailyStats() {
        LocalDate today = LocalDate.now();
        if (!today.equals(currentDate)) {
            // It's a new day, reset the stats
            currentDate = today;
            updateDailyStats();
            dateLabel.setText("Date: " + currentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        }
    }

    private void updateDailyStats() {
        try {
            String dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + "%";
            
            // Get today's sales statistics
            PreparedStatement pst = conn.prepareStatement(
                "SELECT " +
                "SUM(Total) as TotalSales, " +
                "SUM(Profit) as TotalProfit, " +
                "COUNT(*) as OrderCount, " +
                "SUM(Balance) as TotalBalance " +
                "FROM sales WHERE Date LIKE ?"
            );
            pst.setString(1, dateStr);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                double totalSales = rs.getDouble("TotalSales");
                double totalProfit = rs.getDouble("TotalProfit");
                int orderCount = rs.getInt("OrderCount");
                double totalBalance = rs.getDouble("TotalBalance");
                
                todaySalesLabel.setText(currencyFormat.format(totalSales));
                todayProfitLabel.setText(currencyFormat.format(totalProfit));
                totalOrdersLabel.setText(String.valueOf(orderCount));
            }

            // Get pending deliveries
            pst = conn.prepareStatement(
                "SELECT COUNT(*) as PendingCount FROM sales " +
                "WHERE Date LIKE ? AND DeliveryType = 'Delivery'"
            );
            pst.setString(1, dateStr);
            rs = pst.executeQuery();
            
            if (rs.next()) {
                int pendingCount = rs.getInt("PendingCount");
                pendingDeliveriesLabel.setText(String.valueOf(pendingCount));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating statistics: " + e.getMessage());
        }
    }

    private void completeSale() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!");
            return;
        }

        String customerName = (String) customerCombo.getSelectedItem();
        if (customerName == null) {
            JOptionPane.showMessageDialog(this, "Please select a customer");
            return;
        }

        try {
            // First, get the customer's current balance
            String balanceQuery = "SELECT Balance FROM customers WHERE Name = ?";
            PreparedStatement balancePst = conn.prepareStatement(balanceQuery);
            balancePst.setString(1, customerName);
            ResultSet balanceRs = balancePst.executeQuery();
            
            double currentBalance = 0.0;
            if (balanceRs.next()) {
                currentBalance = balanceRs.getDouble("Balance");
            }

            double amount = Double.parseDouble(amountField.getText());
            double totalWithBalance = total + currentBalance;
            double change = 0.0;
            double newBalance = 0.0;
            
            // If payment is less than total + current balance, there will be a new balance
            if (amount < totalWithBalance) {
                newBalance = totalWithBalance - amount;
                
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Amount is less than total due.\n\n" +
                    "Purchase Total: " + currencyFormat.format(total) + "\n" +
                    "Current Balance: " + currencyFormat.format(currentBalance) + "\n" +
                    "Total Due: " + currencyFormat.format(totalWithBalance) + "\n" +
                    "Amount Paid: " + currencyFormat.format(amount) + "\n" +
                    "New Balance will be: " + currencyFormat.format(newBalance) + 
                    "\n\nRecord as partial payment?",
                    "Confirm Partial Payment",
                    JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) {
                    return;
                }
            } else {
                // Payment is sufficient to cover total and balance
                change = amount - totalWithBalance;
                newBalance = 0.0;
            }

            // Start transaction
            conn.setAutoCommit(false);
            try {
                // 1. Create sale record
                String saleSQL = "INSERT INTO sales (Date, Customer, Total, AmountPaid, Balance, Profit, DeliveryType) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement salePst = conn.prepareStatement(saleSQL, Statement.RETURN_GENERATED_KEYS);
                LocalDateTime now = LocalDateTime.now();
                salePst.setString(1, now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                salePst.setString(2, customerName);
                salePst.setDouble(3, total);
                salePst.setDouble(4, amount);
                salePst.setDouble(5, newBalance);
                salePst.setDouble(6, profit);
                salePst.setString(7, onSiteRadio.isSelected() ? "On-site" : "Delivery");
                salePst.executeUpdate();

                // Get the generated sale ID and store sale items
                ResultSet generatedKeys = salePst.getGeneratedKeys();
                if (generatedKeys.next()) {
                    long saleId = generatedKeys.getLong(1);
                    
                    // 2. Insert sale items
                    String saleItemSQL = "INSERT INTO sale_items (sale_id, product_sku, quantity, price, cost) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement saleItemPst = conn.prepareStatement(saleItemSQL);
                    
                    for (CartItem item : cartItems) {
                        saleItemPst.setLong(1, saleId);
                        saleItemPst.setString(2, item.sku);
                        saleItemPst.setInt(3, item.quantity);
                        saleItemPst.setDouble(4, item.price);
                        saleItemPst.setDouble(5, item.cost);
                        saleItemPst.executeUpdate();
                    }
                }

                // 3. Update product quantities
                String updateProductSQL = "UPDATE products SET Quantity = Quantity - ? WHERE SKU = ?";
                PreparedStatement productPst = conn.prepareStatement(updateProductSQL);
                for (CartItem item : cartItems) {
                    productPst.setInt(1, item.quantity);
                    productPst.setString(2, item.sku);
                    productPst.executeUpdate();
                }

                // 4. Update customer record
                String updateCustomerSQL = "UPDATE customers SET " +
                    "Orders = Orders + 1, " +
                    "TotalSpent = TotalSpent + ?, " +
                    "Balance = ? " +
                    "WHERE Name = ?";
                PreparedStatement customerPst = conn.prepareStatement(updateCustomerSQL);
                customerPst.setDouble(1, total);
                customerPst.setDouble(2, newBalance);
                customerPst.setString(3, customerName);
                customerPst.executeUpdate();

                conn.commit();
                
                // Show sale summary
                StringBuilder summary = new StringBuilder();
                summary.append("Sale completed successfully!\n\n");
                summary.append("Purchase Total: ").append(currencyFormat.format(total)).append("\n");
                if (currentBalance > 0) {
                    summary.append("Previous Balance: ").append(currencyFormat.format(currentBalance)).append("\n");
                    summary.append("Total Due: ").append(currencyFormat.format(totalWithBalance)).append("\n");
                }
                summary.append("Amount Paid: ").append(currencyFormat.format(amount)).append("\n");
                
                if (change > 0) {
                    summary.append("Change: ").append(currencyFormat.format(change)).append("\n");
                }
                if (newBalance > 0) {
                    summary.append("Remaining Balance: ").append(currencyFormat.format(newBalance)).append("\n");
                }
                
                JOptionPane.showMessageDialog(this, summary.toString(), "Sale Summary", JOptionPane.INFORMATION_MESSAGE);
                
                updateDailyStats();
                updateCustomerStatus();
                clearSale();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid amount");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error completing sale: " + e.getMessage());
        }
    }

    private void clearSale() {
        cartItems.clear();
        cartModel.setRowCount(0);
        total = 0;
        profit = 0;
        totalLabel.setText(currencyFormat.format(0));
        profitLabel.setText(currencyFormat.format(0));
        amountField.setText("");
        changeLabel.setText(currencyFormat.format(0));
        quantitySpinner.setValue(1);
        onSiteRadio.setSelected(true);
        loadProducts(); // Refresh product list with updated quantities
    }

    public void refresh() {
        loadProducts();
        loadCustomers();
        updateDailyStats();
        clearSale();
    }
}
