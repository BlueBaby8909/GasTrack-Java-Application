package finals;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import javax.swing.table.TableCellEditor;
import javax.swing.AbstractCellEditor;

public class ProductsView extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JTable table;
    private DefaultTableModel tableModel;
    
    private JLabel totalProductsLabel;
    private JLabel inStockLabel;
    private JLabel outOfStockLabel;
    private JLabel lowStockLabel;
    
    Connection conn = null;
    Statement stmt;
    PreparedStatement pst;
    ResultSet rs;
    
    static int totalProduct = 0;
	static int inStock = 0;
	static int outOfStock = 0;
	static int lowStock = 0;
    
	public ProductsView(Connection connection) {
		this.conn = connection;
		initialize();
		refreshTable();
	}
	
	private void resultSetToTable(ResultSet rs) {
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.setRowCount(0);
		totalProduct = 0;
		inStock = 0;
		outOfStock = 0;
		lowStock = 0;

		try {
		    while (rs.next()) {
		        int qty = Integer.parseInt(rs.getString("Quantity"));
		        totalProduct += qty;
		        if (qty > 0) inStock++;
		        else outOfStock++;
		        
		        if (qty < 20) lowStock++;

		        Object[] row = {
		            rs.getString("Product"),
		            rs.getString("SKU"),
		            rs.getString("Category"),
		            rs.getString("Quantity"),
		            rs.getString("ProductCost"),
		            rs.getString("Price"),
		            "" // placeholder for buttons
		        };
		        model.addRow(row);
		    }

		    // Update stat card values here
		    totalProductsLabel.setText(String.valueOf(totalProduct));
		    inStockLabel.setText(String.valueOf(inStock));
		    outOfStockLabel.setText(String.valueOf(outOfStock));
		    lowStockLabel.setText(String.valueOf(lowStock));

		} catch (Exception e) {
		    JOptionPane.showMessageDialog(null, "ResultSet Exception -> " + e);
		}
	}
	
	private void refreshTable() {
	    try {
	        String query = "SELECT * FROM products";
	        pst = conn.prepareStatement(query);
	        rs = pst.executeQuery();
	        resultSetToTable(rs);
	    } catch (Exception e) {
	        e.printStackTrace();
	        JOptionPane.showMessageDialog(null, "Table refresh failed: " + e.getMessage());
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
        
        totalProductsLabel = addStatCard(statsPanel, "Total Products", "0");
        inStockLabel = addStatCard(statsPanel, "In Stock", "0");
        outOfStockLabel = addStatCard(statsPanel, "Out of Stock", "0");
        lowStockLabel = addStatCard(statsPanel, "Low Stock", "0");
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setOpaque(false);
        
        searchField = new JTextField(20);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String text = searchField.getText().trim();

                String sql = "SELECT * FROM products"
                           + (text.isEmpty()
                              ? "" 
                              : " WHERE Product LIKE ?");

                try {
                    PreparedStatement pst = conn.prepareStatement(sql);
                    if (!text.isEmpty()) {
                        pst.setString(1, "%" + text + "%");
                    }

                    ResultSet rs = pst.executeQuery();
                    resultSetToTable(rs);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                        ProductsView.this,
                        "Search failed: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });
        searchField.setPreferredSize(new Dimension(200, 30));
        controlPanel.add(new JLabel("Search:"));
        controlPanel.add(searchField);
        controlPanel.add(Box.createHorizontalStrut(10));

        statusFilter = new JComboBox<>(new String[]{"All", "In Stock", "Low Stock", "Out of Stock"});
        statusFilter.addActionListener(evt -> {
            String text   = searchField.getText().trim();
            String status = statusFilter.getSelectedItem().toString();

            // 1) Build dynamic WHERE clauses
            ArrayList<String> clauses = new ArrayList<>();
            if (!text.isEmpty()) {
                clauses.add("Product LIKE ?");
            }
            switch (status) {
                case "In Stock":
                    clauses.add("Quantity > 0");
                    break;
                case "Low Stock":
                    clauses.add("Quantity < 20");
                    break;
                case "Out of Stock":
                    clauses.add("Quantity = 0");
                    break;
                // "All" adds nothing
            }

            // 2) Assemble SQL
            String sql = "SELECT * FROM products";
            if (!clauses.isEmpty()) {
                sql += " WHERE " + String.join(" AND ", clauses);
            }

            try (PreparedStatement pst = conn.prepareStatement(sql)) {
                // 3) Bind the search parameter if needed
                int idx = 1;
                if (!text.isEmpty()) {
                    pst.setString(idx++, "%" + text + "%");
                }

                // 4) Execute & refresh
                try (ResultSet rs = pst.executeQuery()) {
                    resultSetToTable(rs);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(
                    ProductsView.this,
                    "Filter failed: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
        statusFilter.setPreferredSize(new Dimension(120, 30));
        controlPanel.add(new JLabel("Status:"));
        controlPanel.add(statusFilter);
        controlPanel.add(Box.createHorizontalStrut(20));

        JButton addButton = new JButton("Add Product");
        addButton.setPreferredSize(new Dimension(120, 30));
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openProductDialog(-1);
            }
        });
        controlPanel.add(addButton);
	
        
        String[] columnNames = {
                "Product", "SKU", "Category", "Quantity", "Product Cost", "Price", "Actions"
            };
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 6; // Only make the Actions column editable
                }
            };
            table = new JTable(tableModel);
            table.setRowHeight(50);
            table.setShowGrid(true);
            table.setGridColor(new Color(229, 231, 235));
            
            // Set up the Actions column
            table.getColumnModel().getColumn(6).setCellRenderer(new ActionRenderer());
            table.getColumnModel().getColumn(6).setCellEditor(new ActionEditor(table));
            
            // Set smaller column width for Actions column
            table.getColumnModel().getColumn(6).setPreferredWidth(80);
            table.getColumnModel().getColumn(6).setMinWidth(80);
            table.getColumnModel().getColumn(6).setMaxWidth(80);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
    	JPanel northPanel = new JPanel(new BorderLayout());
    	northPanel.setOpaque(false);
    	northPanel.add(statsPanel, BorderLayout.NORTH);
        northPanel.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
        northPanel.add(controlPanel, BorderLayout.SOUTH);
    	
    	add(northPanel, BorderLayout.NORTH);
    	add(scrollPane, BorderLayout.CENTER);

        // TODO: add tables, forms, controls for customer
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

    // Replace the ButtonRenderer and ButtonEditor classes with this implementation
    class ButtonPanel extends JPanel {
        private JButton editBtn;
        private JButton deleteBtn;

        public ButtonPanel() {
            setLayout(new GridLayout(2, 1, 0, 2)); // 2 rows, 1 column, 2px vertical gap
            editBtn = new JButton("Edit");
            deleteBtn = new JButton("Delete");
            
            // Make buttons smaller and more compact
            Dimension btnSize = new Dimension(70, 20);
            editBtn.setPreferredSize(btnSize);
            deleteBtn.setPreferredSize(btnSize);
            editBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
            deleteBtn.setMargin(new java.awt.Insets(0, 0, 0, 0));
            
            add(editBtn);
            add(deleteBtn);
        }

        public JButton getEditButton() {
            return editBtn;
        }

        public JButton getDeleteButton() {
            return deleteBtn;
        }
    }

    class ActionRenderer implements TableCellRenderer {
        private ButtonPanel panel;

        public ActionRenderer() {
            panel = new ButtonPanel();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return panel;
        }
    }

    class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private ButtonPanel panel;

        public ActionEditor(JTable table) {
            panel = new ButtonPanel();
            
            panel.getEditButton().addActionListener(e -> {
                int row = table.getSelectedRow();
                if (table.isEditing()) {
                    table.getCellEditor().stopCellEditing();
                }
                editProduct(row);
            });
            
            panel.getDeleteButton().addActionListener(e -> {
                int row = table.getSelectedRow();
                deleteProduct(row);
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private void editProduct(int row) {
        openProductDialog(row);
    }

    private void openProductDialog(int row) {
        JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(ProductsView.this);
        boolean isEdit = (row >= 0);
        String dialogTitle = isEdit ? "Edit Product" : "Add New Product";
        String saveButtonText = isEdit ? "Save Changes" : "Save";
        final String existingSku = isEdit ? (String) table.getValueAt(row, 1) : null; // SKU is in column 1

        JDialog dlg = new JDialog(parentFrame, dialogTitle, true);
        JPanel content = new JPanel(new GridLayout(7, 2, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Form fields
        JTextField nameField = new JTextField(20);
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"LPG", "LPG Parts", "Other"});
        JTextField quantityField = new JTextField(20);
        JTextField costField = new JTextField(20);
        JTextField priceField = new JTextField(20);

        if (isEdit) {
            try {
                String query = "SELECT * FROM products WHERE SKU = ?";
                PreparedStatement pst = conn.prepareStatement(query);
                pst.setString(1, existingSku);
                ResultSet rs = pst.executeQuery();

                if (rs.next()) {
                    nameField.setText(rs.getString("Product"));
                    categoryCombo.setSelectedItem(rs.getString("Category"));
                    quantityField.setText(rs.getString("Quantity"));
                    costField.setText(rs.getString("ProductCost"));
                    priceField.setText(rs.getString("Price"));
                } else {
                    JOptionPane.showMessageDialog(this, "Product not found.");
                    dlg.dispose();
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error fetching product details: " + e.getMessage());
                dlg.dispose();
                return;
            }
        }

        // Add labels & fields
        content.add(new JLabel("Product Name:")); content.add(nameField);
        if (isEdit) {
            content.add(new JLabel("SKU:")); content.add(new JLabel(existingSku)); // SKU is read-only for edit
        }
        content.add(new JLabel("Category:")); content.add(categoryCombo);
        content.add(new JLabel("Quantity:")); content.add(quantityField);
        content.add(new JLabel("Product Cost:")); content.add(costField);
        content.add(new JLabel("Price:")); content.add(priceField);

        JButton saveButton = new JButton(saveButtonText);
        saveButton.addActionListener(evt -> {
            try {
                String name = nameField.getText().trim();
                String category = categoryCombo.getSelectedItem().toString();
                int quantity = Integer.parseInt(quantityField.getText().trim());
                double cost = Double.parseDouble(costField.getText().trim());
                double price = Double.parseDouble(priceField.getText().trim());

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(dlg, "Product name is required!");
                    return;
                }

                if (isEdit) {
                    String updateSql = "UPDATE products SET Product=?, Category=?, Quantity=?, ProductCost=?, Price=? WHERE SKU=?";
                    PreparedStatement updatePst = conn.prepareStatement(updateSql);
                    updatePst.setString(1, name);
                    updatePst.setString(2, category);
                    updatePst.setInt(3, quantity);
                    updatePst.setDouble(4, cost);
                    updatePst.setDouble(5, price);
                    updatePst.setString(6, existingSku);
                    updatePst.executeUpdate();
                    JOptionPane.showMessageDialog(dlg, "Product updated successfully!");
                } else {
                    // Generate a new SKU for new products
                    String sku = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
                    String insertSql = "INSERT INTO products (Product, SKU, Category, Quantity, ProductCost, Price) VALUES (?, ?, ?, ?, ?, ?)";
                    PreparedStatement insertPst = conn.prepareStatement(insertSql);
                    insertPst.setString(1, name);
                    insertPst.setString(2, sku);
                    insertPst.setString(3, category);
                    insertPst.setInt(4, quantity);
                    insertPst.setDouble(5, cost);
                    insertPst.setDouble(6, price);
                    insertPst.executeUpdate();
                    JOptionPane.showMessageDialog(dlg, "Product added successfully!\nGenerated SKU: " + sku);
                }

                dlg.dispose();
                refreshTable();

            } catch (SQLException ex) {
                if (ex.getMessage().toLowerCase().contains("constraint")) {
                    JOptionPane.showMessageDialog(dlg, "Product already exists!", "Duplicate Product", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(dlg, "Error saving product: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dlg, "Please enter valid numbers for Quantity, Cost and Price.", "Input Error", JOptionPane.WARNING_MESSAGE);
            }
        });
        content.add(saveButton);

        dlg.getContentPane().add(content);
        dlg.pack();
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(parentFrame);
        dlg.setVisible(true);
    }

    private void deleteProduct(int row) {
        if (row < 0) return; // If no row is selected, do nothing
        
        String sku = (String) table.getValueAt(row, 1);
        String productName = (String) table.getValueAt(row, 0);
        
        // Stop table editing before showing dialog
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete the product: " + productName + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM products WHERE SKU = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, sku);
                pst.executeUpdate();
                
                // Instead of refreshing the whole table, just remove the row from the model
                SwingUtilities.invokeLater(() -> {
                    ((DefaultTableModel)table.getModel()).removeRow(row);
                    JOptionPane.showMessageDialog(this, "Product deleted successfully!");
                });
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage());
            }
        }
    }

    public void refresh() {
        refreshTable();
    }
}