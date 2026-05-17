package com.barpay.pos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import com.barpay.pos.model.OpenOrder;
import com.barpay.pos.model.OpenOrderItem;
import com.barpay.pos.service.BillingService;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;

@SuppressWarnings("serial")
public class TablesPanel extends JPanel {

	private static final String APP_SOURCE = "TablesPanel";

	private BillingService billingService;
	private JTabbedPane mainTabs;

	// --- NEW: Reference to main panel ---
	private POSPanel posPanel; 

	private JPanel pnlTableButtons; 
	private JPanel pnlTablesCenter; 

	private JTextArea orderDetailArea;
	private JLabel lblFinalTableTotal;
	private OpenOrder currentlySelectedOrder = null; 

	// --- FIXED CONSTRUCTOR FOR RESPONSIVENESS ---
	public TablesPanel(BillingService billingService, JTabbedPane mainTabs, POSPanel posPanel) {
	    this.billingService = billingService;
	    this.mainTabs = mainTabs;
	    this.posPanel = posPanel;

	    this.setLayout(new BorderLayout(10, 10));

	    // Get screen dimensions to calculate relative sizes
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    int screenWidth = screenSize.width;

	    // NORTH: Title and Refresh Button
	    JPanel north = new JPanel(new BorderLayout()); 
	    north.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); 

	    JLabel lblTitle = new JLabel("Current Open Tables", SwingConstants.CENTER);
	    lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
	    north.add(lblTitle, BorderLayout.CENTER);

	    JButton btnRefresh = new JButton("Refresh Tables");
	    btnRefresh.setBackground(new Color(50, 150, 250));
	    btnRefresh.setForeground(Color.WHITE);
	    btnRefresh.addActionListener(e -> loadOpenTables());

	    JPanel pnlButtonWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	    pnlButtonWrapper.add(btnRefresh);
	    north.add(pnlButtonWrapper, BorderLayout.EAST);
	    this.add(north, BorderLayout.NORTH); 

	    // WEST: Table Grid View (FIXED: Uses percentage width, not fixed 650)
	    // Small screens (like 1366px) get 35% width, large screens get 45%
	    double sidebarWeight = (screenWidth < 1400) ? 0.45 : 0.40;
	    int sidebarWidth = (int)(screenWidth * sidebarWeight);
	    
	    // FIXED: Dynamic Columns (3 for small screens, 5 for large)
	    int columns = (screenWidth < 1200) ? 3 : 5;
	    pnlTableButtons = new JPanel(new GridLayout(0, columns, 10, 10));
	    
	    JScrollPane scrTableButtons = new JScrollPane(pnlTableButtons);
	    scrTableButtons.setPreferredSize(new Dimension(sidebarWidth, 0)); 
	    scrTableButtons.getVerticalScrollBar().setUnitIncrement(16); // Smoother scrolling

	    // CENTER: Details Area (FIXED: Wrapped in JScrollPane)
	    pnlTablesCenter = new JPanel(new BorderLayout());
	    pnlTablesCenter.setBorder(BorderFactory.createTitledBorder("Selected Table Order Details"));
	    pnlTablesCenter.add(new JLabel("Select a table from the grid to manage.", SwingConstants.CENTER), BorderLayout.CENTER);

	    // This is the "Safety Net" - if the screen is too short, the user can scroll to see buttons
	    JScrollPane scrDetails = new JScrollPane(pnlTablesCenter);
	    scrDetails.setBorder(null);

	    this.add(scrTableButtons, BorderLayout.WEST);
	    this.add(scrDetails, BorderLayout.CENTER);

	    mainTabs.addChangeListener(e -> {
	        if (mainTabs.getSelectedComponent() == TablesPanel.this) {
	            loadOpenTables();
	        }
	    });

	    loadOpenTables();
	}

	void loadOpenTables() {
		pnlTableButtons.removeAll(); 
		try {
			List<OpenOrder> orders = billingService.getAllOpenOrders();

			pnlTableButtons.add(createNewOrderButton());

			if (orders.isEmpty()) {
				pnlTableButtons.add(new JLabel("No open tables currently."));
			}

			for (OpenOrder order : orders) {
				pnlTableButtons.add(createTableButton(order));
			}

		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Failed to load open tables: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Error loading open tables: " + ex.getMessage());
		}
		pnlTableButtons.revalidate();
		pnlTableButtons.repaint();
	}

	private JPanel createTableButton(OpenOrder order) {
	    // 1. Create a Custom JPanel with Rounded Corners
	    JPanel tableCard = new JPanel(new BorderLayout(0, 0)) {
	        @Override
	        protected void paintComponent(java.awt.Graphics g) {
	            super.paintComponent(g);
	            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g;
	            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
	            g2.setColor(getBackground());
	            // 15 is the arc width/height for the curve
	            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
	        }
	    };
	    
	    tableCard.setOpaque(false); // Important: lets the rounded corners show
	    tableCard.setPreferredSize(new Dimension(110, 155)); 
	    tableCard.setBackground(new Color(2, 202, 58)); 
	    // Remove standard border and use a custom rounded one if desired
	    tableCard.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

	    // 2. Info Area (Clickable)
	    String htmlContent = String.format(
	            "<html><center>" +
	            "<div style='color: black; font-family: sans-serif; padding: 5px;'>" +
	            "  <b style='font-size: 13px;'>TABLE %d</b><br>" +
	            "  <span style='font-size: 9px; color: #222;'>Manager: %s</span><br><br>" +
	            "  <span style='font-size: 10px;'>TOTAL</span><br>" +
	            "  <b style='font-size: 16px;'>₹%.2f</b><br>" +
	            "  <i style='font-size: 8px;'>Time: %s</i>" +
	            "</div></center></html>",
	            order.getTableNo(), order.getManagerName(), order.getTotalAmount(),
	            order.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))
	    );

	    JButton btnSelect = new JButton(htmlContent);
	    btnSelect.setContentAreaFilled(false);
	    btnSelect.setBorderPainted(false);
	    btnSelect.setFocusPainted(false);
	    btnSelect.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
	    btnSelect.addActionListener(e -> loadOrderDetails(order));

	    // 3. Action Area: Rounded Print Bill Button
	    // We create a custom button to match the bottom curve of the panel
	    JButton btnPrintQuick = new JButton("<html><center>🖨️<br><b style='font-size:10px;'>PRINT BILL</b></center></html>") {
	        @Override
	        protected void paintComponent(java.awt.Graphics g) {
	            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
	            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
	            g2.setColor(getBackground());
	            // Only round the bottom corners (last two parameters)
	            // This is a simplified version; using fillRect for top and fillRoundRect for base
	            g2.fillRoundRect(0, -10, getWidth(), getHeight() + 10, 15, 15);
	            g2.dispose();
	            super.paintComponent(g);
	        }
	    };

	    btnPrintQuick.setOpaque(false);
	    btnPrintQuick.setContentAreaFilled(false);
	    btnPrintQuick.setPreferredSize(new Dimension(0, 50)); 
	    btnPrintQuick.setBackground(Color.WHITE);
	    btnPrintQuick.setForeground(new Color(0, 102, 204));
	    btnPrintQuick.setFocusable(false);
	    btnPrintQuick.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 100, 0)));
	    btnPrintQuick.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
	    
	    btnPrintQuick.addActionListener(e -> {
	        try {
	            List<OpenOrderItem> items = billingService.getOrderItems(order.getId());
	            printFinalBill(items, 0, order.getTotalAmount(), "UNPAID", order.getTableNo());
	        } catch (Exception ex) {
	            JOptionPane.showMessageDialog(this, "Print Error: " + ex.getMessage());
	        }
	    });

	    // 4. Assembly
	    tableCard.add(btnSelect, BorderLayout.CENTER);
	    tableCard.add(btnPrintQuick, BorderLayout.SOUTH);

	    return tableCard;
	}

	private JButton createNewOrderButton() {
		JButton btn = new JButton("<html><center>+ New Order<br>(Switch to Billing)</center></html>");
		btn.setPreferredSize(new Dimension(100, 100));
		btn.setBackground(new Color(200, 200, 200)); 
		btn.setForeground(Color.BLACK);
		btn.setFont(new Font("Arial", Font.PLAIN, 12));

		btn.addActionListener(e -> {
			// This action is handled by the main POSPanel,
			// but we simulate it by switching tabs.
			JOptionPane.showMessageDialog(this, "Switched to 'Billing & Ordering'. Cart is ready for a NEW order.");
			mainTabs.setSelectedIndex(0);
		});
		return btn;
	}

	private void loadOrderDetails(OpenOrder order) {
		// Save the selected order to state
		this.currentlySelectedOrder = order; 

		pnlTablesCenter.removeAll();

		// --- FONT DEFINITIONS ---
		Font titleFont = new Font("Arial", Font.BOLD, 12);
		Font buttonFont = new Font("Arial", Font.BOLD, 14);

		// --- 1. TITLE BORDER FIX ---
		javax.swing.border.TitledBorder tb = BorderFactory.createTitledBorder("Selected Table Order Details");
		tb.setTitleFont(titleFont); // Apply Bold Size 12 to the border title
		pnlTablesCenter.setBorder(tb);
		// -------------------------

		// 2. Order Summary
		JPanel summary = new JPanel(new GridLayout(0, 1));
		summary.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		summary.add(new JLabel("Table: " + order.getTableNo() + " | Manager: " + order.getManagerName()));
		summary.add(new JLabel("Start Time: " + order.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))));

		// 3. Item List
		orderDetailArea = new JTextArea();
		orderDetailArea.setEditable(false);
		JScrollPane itemScroll = new JScrollPane(orderDetailArea);

		// 4. Actions Panel (Buttons)
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));

		// --- BUTTON MODIFICATIONS (Size 14 Bold) ---

		JButton btnAddItem = new JButton("Add More Items");
		btnAddItem.setFont(buttonFont); 
		btnAddItem.setBackground(new Color(2, 92, 202));
		btnAddItem.setForeground(Color.WHITE);

		JButton btnRemoveItem = new JButton("Remove Item/Qty");
		btnRemoveItem.setFont(buttonFont); 
		btnRemoveItem.setBackground(new Color(233, 70, 70));
		btnRemoveItem.setForeground(Color.WHITE);

		JButton btnCheckout = new JButton("Final Checkout");
		btnCheckout.setFont(buttonFont); 
		btnCheckout.setForeground(Color.WHITE);
		btnCheckout.setBackground(new Color(2, 202, 58));

		// ------------------------------------------

		actions.add(btnAddItem);
		actions.add(btnRemoveItem); 
		actions.add(btnCheckout);

		// --- BUG FIX: ACTION LISTENER UPDATED ---
		btnAddItem.addActionListener(e -> {
			// FIX: Pass active table to POSPanel so it remembers!
			posPanel.setActiveOrderForBilling(order);

			JOptionPane.showMessageDialog(this, "Switched to 'Billing & Ordering'. Cart is ready for Table " + order.getTableNo());
			mainTabs.setSelectedIndex(0);
		});
		btnRemoveItem.addActionListener(e -> removeItemFromOrder(order));
		btnCheckout.addActionListener(e -> closeOrder(order));

		// Fixed total label
		lblFinalTableTotal = new JLabel("TOTAL: Loading...", SwingConstants.CENTER); 
		lblFinalTableTotal.setFont(new Font("Arial", Font.BOLD, 24)); 
		lblFinalTableTotal.setForeground(Color.WHITE); 
		lblFinalTableTotal.setBackground(new Color(0, 102, 204)); 
		lblFinalTableTotal.setOpaque(true);
		lblFinalTableTotal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); 

		JPanel pnlOrderSouth = new JPanel(new BorderLayout());
		pnlOrderSouth.add(lblFinalTableTotal, BorderLayout.NORTH); 
		pnlOrderSouth.add(actions, BorderLayout.CENTER); 

		pnlTablesCenter.add(summary, BorderLayout.NORTH);
		pnlTablesCenter.add(itemScroll, BorderLayout.CENTER);
		pnlTablesCenter.add(pnlOrderSouth, BorderLayout.SOUTH);

		pnlTablesCenter.revalidate();
		pnlTablesCenter.repaint();

		refreshOrderItems(order);
	}

	private void refreshOrderItems(OpenOrder order) {
		orderDetailArea.setText("");
		try {
			// getOrderItems now fetches both food and bar items
			List<OpenOrderItem> items = billingService.getOrderItems(order.getId());
			double total = 0;
			int totalQty = 0; 

			orderDetailArea.append(String.format("%-25s %s %s%n", "PRODUCT", "QTY", "STATUS"));
			orderDetailArea.append("--------------------------------------------------\n");

			for (OpenOrderItem item : items) {
				orderDetailArea.append(item.toText() + "\n");
				total += item.qty * item.price;
				totalQty += item.qty; 
			}

			orderDetailArea.append("--------------------------------------------------\n");
			String totalText = String.format("QTY: %d  |  TOTAL: ₹%.2f", totalQty, round(total));
			if (lblFinalTableTotal != null) {
				lblFinalTableTotal.setText(totalText); 
			}
		} catch (Exception ex) {
			orderDetailArea.setText("Error loading items: " + ex.getMessage());
			Logger.error(APP_SOURCE, "Failed to refresh order items: " + ex.getMessage());
		}
	}

	private void removeItemFromOrder(OpenOrder order) {
		try {
			List<OpenOrderItem> items = billingService.getOpenOrderItems(order.getId());
			if (items.isEmpty()) {
				JOptionPane.showMessageDialog(this, "The order is empty.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			OpenOrderItem[] itemArray = items.toArray(new OpenOrderItem[0]);
			OpenOrderItem selectedItem = (OpenOrderItem) JOptionPane.showInputDialog(
					this,
					"Select the item to correct/remove from Table " + order.getTableNo() + ":",
					"Remove Order Item",
					JOptionPane.PLAIN_MESSAGE,
					null, itemArray, itemArray[0]);

			if (selectedItem == null) return; 

			String qtyInput = JOptionPane.showInputDialog(
					this,
					String.format("Current Qty: %d. Enter quantity to REMOVE (1 to %d):",
							selectedItem.qty, selectedItem.qty),
					"Input Quantity to Remove",
					JOptionPane.QUESTION_MESSAGE
					);

			if (qtyInput == null) return; 

			int quantityToRemove;
			try {
				quantityToRemove = Integer.parseInt(qtyInput.trim());
				if (quantityToRemove <= 0 || quantityToRemove > selectedItem.qty) {
					JOptionPane.showMessageDialog(this, "Invalid quantity.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(this, "Invalid number.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			int confirm = JOptionPane.showConfirmDialog(
					this,
					String.format("Confirm removing %d units of %s?\nStock will be returned (if applicable).",
							quantityToRemove, selectedItem.productName),
					"Confirm Quantity Correction",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
					);

			if (confirm == JOptionPane.YES_OPTION) {
				int remainingQty = selectedItem.qty - quantityToRemove;

				billingService.reduceOpenOrderItemQuantity(
						selectedItem,
						quantityToRemove,
						remainingQty
						);

				JOptionPane.showMessageDialog(this, "Quantity corrected successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

				loadOpenTables(); 
				loadOrderDetails(billingService.getOpenOrder(order.getTableNo())); 
			}
		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Error during item removal: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void closeOrder(OpenOrder order) {
		String[] payments = {"CASH","CARD","ONLINE"};
		String payment = (String) JOptionPane.showInputDialog(this, 
				"Select Payment Mode for Table " + order.getTableNo() + ":", 
				"Final Checkout", JOptionPane.QUESTION_MESSAGE, null, payments, payments[0]);
		if (payment == null) return;

		try {
			List<OpenOrderItem> items = billingService.getOrderItems(order.getId());
			double total = items.stream().mapToDouble(i -> i.qty * i.price).sum();
			double cgst = round(total * 0.00); 
			double sgst = round(total * 0.00);
			double finalTotal = round(total + cgst + sgst);

			String summary = String.format("Table %d Final Bill:\nTotal: ₹%.2f\nFinal: ₹%.2f\nPayment: %s\n\nConfirm?",
					order.getTableNo(), total, finalTotal, payment);

			int ok = JOptionPane.showConfirmDialog(this, summary, "Final Checkout Confirmation", JOptionPane.YES_NO_OPTION);
			if (ok != JOptionPane.YES_OPTION) return;

			int billId = -1;
			try (Connection c = DBUtil.getConnection()) {
				c.setAutoCommit(false);

				// --- MODIFIED: Use datetime('now', 'localtime') for SQLite ---
				try (PreparedStatement psBill = c.prepareStatement(
						"INSERT INTO bills (manager_name, table_no, payment_mode, total, cgst, sgst, bill_time) VALUES (?,?,?,?,?,?,datetime('now', 'localtime'))",
						Statement.RETURN_GENERATED_KEYS)) {
					psBill.setString(1, order.getManagerName());
					psBill.setInt(2, order.getTableNo());
					psBill.setString(3, payment);
					psBill.setDouble(4, total);
					psBill.setDouble(5, cgst);
					psBill.setDouble(6, sgst);
					psBill.executeUpdate();
					ResultSet keys = psBill.getGeneratedKeys();
					keys.next();
					billId = keys.getInt(1);
				}

				try (PreparedStatement psItem = c.prepareStatement(
						"INSERT INTO bill_items (bill_id, variant_id, food_item_id, qty, price_per_unit, amount) VALUES (?,?,?,?,?,?)")) {
					for (OpenOrderItem it : items) {
						psItem.setInt(1, billId);
						if (it.itemType.equals("BAR")) {
							psItem.setInt(2, it.getVariantId());
							psItem.setNull(3, java.sql.Types.INTEGER);
						} else {
							psItem.setNull(2, java.sql.Types.INTEGER);
							psItem.setInt(3, it.getFoodItemId());
						}
						psItem.setInt(4, it.qty);
						psItem.setDouble(5, it.price);
						psItem.setDouble(6, it.qty * it.price);
						psItem.executeUpdate();
					}
				}

				try (PreparedStatement psDelItems = c.prepareStatement("DELETE FROM open_order_items WHERE order_id = ?");
						PreparedStatement psDelOrder = c.prepareStatement("DELETE FROM open_orders WHERE id = ?")) {
					psDelItems.setInt(1, order.getId());
					psDelItems.executeUpdate();
					psDelOrder.setInt(1, order.getId());
					psDelOrder.executeUpdate();
				}

				c.commit();

				printFinalBill(items, billId, finalTotal, payment, order.getTableNo());
				JOptionPane.showMessageDialog(this, "Order closed successfully. Bill ID: " + billId);
				Logger.info(APP_SOURCE, "Table " + order.getTableNo() + " closed. Bill ID: " + billId);

			} catch (Exception ex) {
				try (Connection c = DBUtil.getConnection()) { c.rollback(); } catch (Exception rbEx) {}
				throw ex;
			}

			loadOpenTables();
			pnlTablesCenter.removeAll();
			pnlTablesCenter.add(new JLabel("Select a table from the left to manage the order."), BorderLayout.CENTER);
			pnlTablesCenter.revalidate();
			pnlTablesCenter.repaint();
			this.currentlySelectedOrder = null; 

		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Checkout Error: " + ex.getMessage());
			Logger.error(APP_SOURCE, "Failed to close order: " + ex.getMessage());
		}
	}

	private void printFinalBill(List<OpenOrderItem> items, int billId, double finalTotal, String payment, int tableNo) {
		StringBuilder sb = new StringBuilder();

		// --- 58mm Header (Centered for 32 chars) ---
		sb.append("SWAGAT GARDEN BAR & RESTO\n");
		sb.append("-------------------------\n"); // 32 dashes
		sb.append("Bill ID: ").append(billId).append(" | Table: ").append(tableNo).append("\n");
		sb.append("Date: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"))).append("\n");
		sb.append("-------------------------\n");

		// Columns: Item(12) Qty(3) Rate(6) Total(7) = 31 chars + spaces
		sb.append(String.format("%-10s %2s %4s %4s%n", "ITEM", "QTY", "RATE", "TOTAL"));
		sb.append("-------------------------\n");

		double total = 0;
		int totalQty = 0; 

		for (OpenOrderItem item : items) {
			double itemRate = item.price;
			double itemTotal = item.qty * item.price;

			String itemName = item.productName;
			if(item.itemType.equals("BAR")) {
				itemName += " " + item.sizeMl + "ml";
			}

			// Truncate name to 12 chars so it fits on one line
			if (itemName.length() > 12) {
				itemName = itemName.substring(0, 12);
			}

			// Format Line
			sb.append(String.format("%-10s %2d %4.0f %4.0f%n", itemName, item.qty, itemRate, itemTotal));

			total += itemTotal;
			totalQty += item.qty;
		}

		sb.append("-------------------------\n");
		sb.append(String.format("%-15s %6d%n", "TOTAL QTY:", totalQty));
		sb.append(String.format("%-15s %6.0f%n", "SUBTOTAL:", total));
		sb.append("-------------------------\n");
		sb.append(String.format("%-15s %4.0f%n", "GRAND TOTAL:", finalTotal));
		sb.append("-------------------------\n");
		sb.append("Payment Mode: ").append(payment).append("\n");
		sb.append("THANK YOU, VISIT AGAIN\n");
		sb.append("-------------------------\n"); // Extra feed
		sb.append("-------------------------\n\n\n");
		printTextToPrinter("Final Bill " + billId, sb.toString());
	}

	public void refreshCurrentDetails() {
		if (currentlySelectedOrder == null) {
			return;
		}
		Logger.info(APP_SOURCE, "Refreshing details for currently selected table: " + currentlySelectedOrder.getTableNo());

		try {
			OpenOrder updatedOrder = billingService.getOpenOrder(currentlySelectedOrder.getTableNo());

			if (updatedOrder != null) {
				loadOrderDetails(updatedOrder);
			} else {
				pnlTablesCenter.removeAll();
				pnlTablesCenter.add(new JLabel("Order was closed or is no longer available."), BorderLayout.CENTER);
				pnlTablesCenter.revalidate();
				pnlTablesCenter.repaint();
				currentlySelectedOrder = null;
			}
		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Failed to auto-refresh order details: " + ex.getMessage());
			pnlTablesCenter.add(new JLabel("Error refreshing details: " + ex.getMessage()), BorderLayout.SOUTH);
		}
	}

	private void printTextToPrinter(String jobName, String content) {
		if (content == null || content.isEmpty()) { 
			JOptionPane.showMessageDialog(this, "No content to print"); 
			return; 
		}

		JTextArea ta = new JTextArea(content);

		ta.setFont(new Font("Monospaced", Font.PLAIN, 8)); 

		boolean ok = JOptionPane.showConfirmDialog(this, new JScrollPane(ta), 
				"Print preview - " + jobName + " - Confirm to send to printer", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
		if (!ok) return;

		PrinterJob pj = PrinterJob.getPrinterJob();
		pj.setJobName(jobName);


		PageFormat pf = pj.defaultPage();
		Paper paper = pf.getPaper();

		double width = 164; 
		double height = 3000; 
		double margin = 5; 

		paper.setSize(width, height);
		paper.setImageableArea(margin, margin, width - (margin * 2), height - (margin * 2));

		pf.setOrientation(PageFormat.PORTRAIT);
		pf.setPaper(paper);

		pj.setPrintable(ta.getPrintable(null, null), pf);

		if (pj.printDialog()) {
			try { 
				pj.print(); 
				Logger.info(APP_SOURCE, jobName + " printed successfully."); 
			} catch (PrinterException pe) { 
				pe.printStackTrace(); 
				JOptionPane.showMessageDialog(this, "Print failed: " + pe.getMessage()); 
				Logger.error(APP_SOURCE, "Print failed: " + pe.getMessage()); 
			}
		}
	}

	private double round(double v) { return Math.round(v*100.0)/100.0; }
}