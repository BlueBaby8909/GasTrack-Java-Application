package finals;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Toolkit;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class App {

	private JFrame frame;
	private JPanel panel;
	private JPanel panel_1;
	private JPanel panel_2;
	private JPanel panel_3;
	
	private JPanel ind;
	private JPanel ind_1;
	private JPanel ind_2;
	private JPanel ind_3;
	
	private JPanel[] panels;
	private JPanel[] inds;
	
	private JPanel home;
	private Connection conn;
	
	// Store references to views
	private CustomerView customerView;
	private ProductsView productsView;
	private SalesView salesView;
	private ReportView reportView;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
//		System.setProperty("sun.java2d.uiScale", "1");
	    try {
	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    EventQueue.invokeLater(new Runnable() {
	        public void run() {
	            try {
	                App window = new App();
	                window.frame.setVisible(true);
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    });
	}

	public App() {
		// Initialize database connection first
		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("JDBC:sqlite:src/finals/DataBase/inventory.sqlite");
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Database Connection Error: " + e.getMessage());
			System.exit(1);
		}

		initialize();
		panels = new JPanel[]{panel, panel_1, panel_2, panel_3};
		inds = new JPanel[] {ind, ind_1, ind_2, ind_3};

		// Add shutdown hook to close database connection
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}));
	}
	
	public void setColor(JPanel panel, JPanel ind, JPanel[] panels, JPanel[] inds) {
			for(JPanel p : panels) {
				p.setBackground(new Color(221, 102, 74));
			}
			
			for(JPanel i: inds) {
				i.setOpaque(false);
			}
			
			ind.setOpaque(true);
			panel.setBackground(new Color(194,143,133));
	}

	private void initialize() {
		frame = new JFrame("GasTrack");
		frame.setIconImage(Toolkit.getDefaultToolkit().getImage(App.class.getResource("/finals/icons/Jacutin_LPG_Logo.png")));
		frame.setBounds(100, 100, 854, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel background = new JPanel();
		frame.getContentPane().add(background, BorderLayout.CENTER);
		background.setLayout(null);
		
		home = new JPanel(new CardLayout());
		home.setBounds(205, 0, 635, 443);
		background.add(home);
		
        customerView = new CustomerView(conn);
        productsView = new ProductsView(conn);
        salesView = new SalesView(conn);
        reportView = new ReportView(conn);

        home.add(customerView, "CUSTOMER");
        home.add(productsView, "PRODUCTS");
        home.add(salesView, "SALES");
        home.add(reportView, "REPORT");

        ((CardLayout)home.getLayout()).show(home, "CUSTOMER");
		
		JPanel sideBar = new JPanel();
		sideBar.setBounds(0, 0, 205, 443);
		sideBar.setBackground(new Color(221, 102, 74));
		background.add(sideBar);
		sideBar.setLayout(null);
		
		panel = new JPanel();
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setColor(panel, ind, panels, inds);
				customerView.refresh();
				((CardLayout)home.getLayout()).show(home, "CUSTOMER");
			}
		});
		panel.setBackground(new Color(221, 102, 74));
		panel.setBounds(0, 113, 205, 31);
		sideBar.add(panel);
		panel.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setIcon(new ImageIcon(App.class.getResource("/finals/icons/2.png")));
		lblNewLabel.setBackground(new Color(103, 61, 197));
		lblNewLabel.setBounds(15, 0, 31, 31);
		panel.add(lblNewLabel);
		
		ind = new JPanel();
		ind.setBackground(new Color(255, 255, 255));
		ind.setOpaque(false);
		ind.setBounds(0, 0, 5, 31);
		panel.add(ind);
		
		JLabel lblCustomer = new JLabel("Customer");
		lblCustomer.setHorizontalAlignment(SwingConstants.LEFT);
		lblCustomer.setForeground(Color.WHITE);
		lblCustomer.setFont(new Font("Century Gothic", Font.PLAIN, 12));
		lblCustomer.setBounds(70, 7, 64, 16);
		panel.add(lblCustomer);
		
		JLabel lblName = new JLabel("GasTrack");
		lblName.setHorizontalAlignment(SwingConstants.LEFT);
		lblName.setForeground(new Color(255, 255, 255));
		lblName.setFont(new Font("Century Gothic", Font.BOLD, 18));
		lblName.setBounds(58, 20, 88, 24);
		sideBar.add(lblName);
		
		JLabel lblInterface = new JLabel("Interface");
		lblInterface.setHorizontalAlignment(SwingConstants.LEFT);
		lblInterface.setForeground(Color.WHITE);
		lblInterface.setFont(new Font("Century Gothic", Font.PLAIN, 10));
		lblInterface.setBounds(79, 47, 46, 16);
		sideBar.add(lblInterface);
		
		panel_1 = new JPanel();
		panel_1.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setColor(panel_1, ind_1, panels, inds);
				productsView.refresh();
				((CardLayout)home.getLayout()).show(home, "PRODUCTS");
			}
		});
		panel_1.setLayout(null);
		panel_1.setBackground(new Color(221, 102, 74));
		panel_1.setBounds(0, 154, 205, 31);
		sideBar.add(panel_1);
		
		JLabel lblNewLabel_2 = new JLabel("");
		lblNewLabel_2.setIcon(new ImageIcon(App.class.getResource("/finals/icons/1.png")));
		lblNewLabel_2.setBackground(new Color(103, 61, 197));
		lblNewLabel_2.setBounds(15, 0, 31, 31);
		panel_1.add(lblNewLabel_2);
		
		ind_1 = new JPanel();
		ind_1.setBackground(new Color(255, 255, 255));
		ind_1.setOpaque(false);
		ind_1.setBounds(0, 0, 5, 31);
		panel_1.add(ind_1);
		
		JLabel lblProducts = new JLabel("Products");
		lblProducts.setHorizontalAlignment(SwingConstants.LEFT);
		lblProducts.setForeground(Color.WHITE);
		lblProducts.setFont(new Font("Century Gothic", Font.PLAIN, 12));
		lblProducts.setBounds(70, 7, 88, 16);
		panel_1.add(lblProducts);
		
		panel_2 = new JPanel();
		panel_2.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setColor(panel_2, ind_2, panels, inds);
				salesView.refresh();
				((CardLayout)home.getLayout()).show(home, "SALES");
			}
		});
		panel_2.setLayout(null);
		panel_2.setBackground(new Color(221, 102, 74));
		panel_2.setBounds(0, 195, 205, 31);
		sideBar.add(panel_2);
		
		JLabel lblNewLabel_3 = new JLabel("");
		lblNewLabel_3.setIcon(new ImageIcon(App.class.getResource("/finals/icons/4.png")));
		lblNewLabel_3.setBackground(new Color(103, 61, 197));
		lblNewLabel_3.setBounds(15, 0, 31, 31);
		panel_2.add(lblNewLabel_3);
		
		ind_2 = new JPanel();
		ind_2.setBackground(new Color(255, 255, 255));
		ind_2.setOpaque(false);
		ind_2.setBounds(0, 0, 5, 31);
		panel_2.add(ind_2);
		
		JLabel lblSales = new JLabel("Sales");
		lblSales.setHorizontalAlignment(SwingConstants.LEFT);
		lblSales.setForeground(Color.WHITE);
		lblSales.setFont(new Font("Century Gothic", Font.PLAIN, 12));
		lblSales.setBounds(70, 7, 88, 16);
		panel_2.add(lblSales);
		
		panel_3 = new JPanel();
		panel_3.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				setColor(panel_3, ind_3, panels, inds);
				((CardLayout)home.getLayout()).show(home, "REPORT");
			}
		});
		panel_3.setLayout(null);
		panel_3.setBackground(new Color(221, 102, 74));
		panel_3.setBounds(0, 236, 205, 31);
		sideBar.add(panel_3);
		
		JLabel lblNewLabel_3_1 = new JLabel("");
		lblNewLabel_3_1.setIcon(new ImageIcon(App.class.getResource("/finals/icons/3.png")));
		lblNewLabel_3_1.setBackground(new Color(103, 61, 197));
		lblNewLabel_3_1.setBounds(15, 0, 31, 31);
		panel_3.add(lblNewLabel_3_1);
		
		ind_3 = new JPanel();
		ind_3.setBackground(new Color(255, 255, 255));
		ind_3.setOpaque(false);
		ind_3.setBounds(0, 0, 5, 31);
		panel_3.add(ind_3);
		
		JLabel lblReport = new JLabel("Report");
		lblReport.setHorizontalAlignment(SwingConstants.LEFT);
		lblReport.setForeground(Color.WHITE);
		lblReport.setFont(new Font("Century Gothic", Font.PLAIN, 12));
		lblReport.setBounds(70, 7, 88, 16);
		panel_3.add(lblReport);
		
		JLabel lblVersion = new JLabel("V1.0");
		lblVersion.setHorizontalAlignment(SwingConstants.LEFT);
		lblVersion.setForeground(Color.WHITE);
		lblVersion.setFont(new Font("Century Gothic", Font.PLAIN, 12));
		lblVersion.setBounds(144, 27, 30, 16);
		sideBar.add(lblVersion);
	}
}
