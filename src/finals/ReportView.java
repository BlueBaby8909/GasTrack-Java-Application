package finals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportView extends JPanel {
    private Connection conn;
    private JLabel statusLabel;
    private String exportPath;

    public ReportView(Connection connection) {
        this.conn = connection;
        this.exportPath = System.getProperty("user.dir") + "/src/finals/exports/";
        
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel title = new JLabel("Export Reports");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(title);

        // Buttons Panel
        JPanel buttonsPanel = new JPanel(new GridLayout(5, 1, 0, 10));
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JButton customersButton = createExportButton("Export Customers Data", "customers");
        JButton productsButton = createExportButton("Export Products Data", "products");
        JButton salesButton = createExportButton("Export Sales Data", "sales");
        JButton saleItemsButton = createExportButton("Export Sale Items Data", "sale_items");
        JButton exportAllButton = createExportButton("Export All Data", "all");

        buttonsPanel.add(customersButton);
        buttonsPanel.add(productsButton);
        buttonsPanel.add(salesButton);
        buttonsPanel.add(saleItemsButton);
        buttonsPanel.add(exportAllButton);

        // Status Panel
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Ready to export data");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusPanel.add(statusLabel);

        // Main Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(buttonsPanel, BorderLayout.NORTH);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(titlePanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JButton createExportButton(String text, String type) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(200, 40));
        button.addActionListener((ActionEvent e) -> {
            button.setEnabled(false);
            statusLabel.setText("Exporting " + type + " data...");
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        if (type.equals("all")) {
                            exportTable("customers");
                            exportTable("products");
                            exportTable("sales");
                            exportTable("sale_items");
                        } else {
                            exportTable(type);
                        }
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(
                                ReportView.this,
                                "Error exporting data: " + ex.getMessage(),
                                "Export Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        });
                    }
                    return null;
                }

                @Override
                protected void done() {
                    button.setEnabled(true);
                    statusLabel.setText("Export completed successfully!");
                }
            };
            worker.execute();
        });
        return button;
    }

    private void exportTable(String tableName) throws SQLException, IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = exportPath + tableName + "_" + timestamp + ".csv";

        String query = "SELECT * FROM " + tableName;
        try (PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery();
             FileWriter writer = new FileWriter(fileName)) {

            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();

            // Write header
            for (int i = 1; i <= columnCount; i++) {
                writer.append(metadata.getColumnName(i));
                if (i < columnCount) {
                    writer.append(",");
                }
            }
            writer.append("\n");

            // Write data
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    // Handle null values and escape special characters
                    if (value == null) {
                        value = "";
                    } else {
                        value = value.replace("\"", "\"\""); // Escape quotes
                        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                            value = "\"" + value + "\"";
                        }
                    }
                    writer.append(value);
                    if (i < columnCount) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
            writer.flush();
        }
    }

    public void refresh() {
        statusLabel.setText("Ready to export data");
    }
}
