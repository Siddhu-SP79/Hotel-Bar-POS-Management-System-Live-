package com.barpay.pos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.barpay.pos.model.CartItem;
import com.barpay.pos.model.FoodItem;
import com.barpay.pos.model.OpenOrder;
import com.barpay.pos.model.Product;
import com.barpay.pos.model.Variant;
import com.barpay.pos.service.BillingService;
import com.barpay.pos.service.FoodService;
import com.barpay.pos.service.ProductService;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;
import com.barpay.pos.util.UITheme;


@SuppressWarnings({ "serial" })
public class POSPanel extends JFrame {
	private JTabbedPane mainTabs;

	// Services
	private ProductService productService;
	private BillingService billingService;
	private FoodService foodService;

	// Billing/Order Management State
	private JPanel productPanel; 
	private JPanel foodPanel; 

	private JTextArea txtBill;
	private java.util.List<CartItem> currentCart = new ArrayList<>(); 
	private OpenOrder activeOrder = null; 
	private JTextField tfSearchBilling;
	private JLabel lblTotalNewCost;

	private static final String APP_SOURCE = "POSPanel";

	public POSPanel() {
		// --- Init Services & DB ---
		DBUtil.initializeDatabase(); 
		productService = new ProductService();
		billingService = new BillingService();
		foodService = new FoodService();


		// --- Modernize L&F ---
		UITheme.applyTheme(this);

		setTitle("BillPay : Sukoon Se Bhugtan Aapka Bharosa.");
		// INSTEAD OF MAXIMIZED_BOTH, use this to fit "Perfectly"
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		this.setMaximizedBounds(env.getMaximumWindowBounds());
		this.setExtendedState(JFrame.MAXIMIZED_BOTH);

		// Set a minimum size so the layout doesn't crash if they resize it
		setMinimumSize(new Dimension(1024, 720));

		// --- SET APPLICATION ICON ---
		try {
			// 1. Priority: Try to load from inside the JAR/EXE (Standard Production)
			java.net.URL iconUrl = getClass().getResource("/images/logo.png");
			if (iconUrl != null) {
				ImageIcon icon = new ImageIcon(iconUrl);
				this.setIconImage(icon.getImage());
			} 
			// 2. Fallback: Check AppData (If you uploaded a custom logo via Owner Panel)
			else {
				File appDataLogo = getAppDataImageFile("logo.png"); // Uses the helper method we added
				if (appDataLogo.exists()) {
					this.setIconImage(new ImageIcon(appDataLogo.getAbsolutePath()).getImage());
				}
			}
		} catch (Exception ex) {
			// Fail silently or print to console - do not crash the app for an icon
			System.err.println("Warning: Application logo could not be loaded: " + ex.getMessage());
		}

		setLayout(new BorderLayout());

		mainTabs = new JTabbedPane();
		mainTabs.setFont(new Font("Segoe UI", Font.BOLD, 12)); 

		// Tab 1: Billing
		mainTabs.add(" Billing & Ordering ", buildBillingTab());

		// Tab 2: Tables
		// --- FIX: Pass 'this' to TablesPanel ---
		TablesPanel tablesPanel = new TablesPanel(billingService, mainTabs, this);
		mainTabs.add(" Tables & Orders ", tablesPanel); 

		// Tab 3: Owner
		OwnerPanel ownerPanel = new OwnerPanel(productService, billingService, foodService, mainTabs);
		mainTabs.add(" Owner Console ", ownerPanel);

		// --- TAB LISTENER ---
		mainTabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int selectedIndex = mainTabs.getSelectedIndex();

				if (selectedIndex == 0) { // Billing Tab
					Logger.info(APP_SOURCE, "Billing tab selected. Refreshing menus.");
					loadProducts(""); 
					loadFoodItems(null); 
				}
				if (selectedIndex == 1) { // Tables Tab
					Logger.info(APP_SOURCE, "Tables tab selected. Refreshing open tables.");
					tablesPanel.loadOpenTables(); 
					tablesPanel.refreshCurrentDetails(); 
				}
			}
		});

		add(mainTabs, BorderLayout.CENTER);
		Logger.info(APP_SOURCE, "Application started and services initialized.");
	}

	// --- NEW METHOD: Set Active Order from TablesPanel ---
	public void setActiveOrderForBilling(OpenOrder order) {
		this.activeOrder = order;
		this.currentCart.clear(); // Start fresh for new items
		updateCartDisplay(); // Show "Items already on table..."
	}

	// ---------------- Billing Tab Logic ----------------

	private JPanel buildBillingTab() {
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		JPanel pnl = new JPanel(new BorderLayout());


		Color addColor = new Color(2, 202, 58);       
		Color deleteColor = new Color(233, 70, 70);   
		Color clearColor = new Color(255, 165, 0);    
		Color searchOrange = new Color(255, 111, 0);  
		Color primaryBlue = new Color(2, 92, 202);    

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

		JLabel lblSearch = new JLabel("Search Item (name / code):");
		lblSearch.setFont(new Font("Arial", Font.BOLD, 14));
		top.add(lblSearch);

		tfSearchBilling = new JTextField(20);
		tfSearchBilling.setFont(new Font("Arial", Font.BOLD, 24)); 
		tfSearchBilling.setForeground(Color.BLACK); 
		tfSearchBilling.setCaretColor(Color.BLACK); 
		tfSearchBilling.setBackground(Color.WHITE); 
		tfSearchBilling.setMargin(new java.awt.Insets(5, 5, 5, 5)); 
		top.add(tfSearchBilling);

		JButton btnScanMode = new JButton("SCAN MODE");
		btnScanMode.setBackground(addColor); 
		btnScanMode.setForeground(Color.WHITE);
		btnScanMode.setFont(new Font("Arial", Font.BOLD, 14));

		btnScanMode.addActionListener(e -> {
			tfSearchBilling.setText(""); 
			tfSearchBilling.requestFocusInWindow(); 
			tfSearchBilling.setBackground(new Color(220, 255, 220)); 
		});

		tfSearchBilling.addFocusListener(new java.awt.event.FocusAdapter() {
			public void focusLost(java.awt.event.FocusEvent e) {
				tfSearchBilling.setBackground(Color.WHITE); 
			}
		});
		top.add(btnScanMode);


		int gridCols = (screenSize.width < 1300) ? 3 : 5;
		final JTabbedPane itemSelectionTabs = new JTabbedPane();
		itemSelectionTabs.setFont(new Font("Arial", Font.BOLD, 12)); 

		productPanel = new JPanel(new GridLayout(0, gridCols, 12, 12));
		JScrollPane scrBar = new JScrollPane(productPanel);
		itemSelectionTabs.addTab("Bar Menu", scrBar);

		foodPanel = new JPanel(new GridLayout(0, gridCols, 12, 12));
		JScrollPane scrFood = new JScrollPane(foodPanel);
		itemSelectionTabs.addTab("Food Menu", scrFood);

		tfSearchBilling.addActionListener(e -> {
			String query = tfSearchBilling.getText().trim();
			final int selectedTab = itemSelectionTabs.getSelectedIndex();

			if (query.isEmpty()) {
				if (selectedTab == 0) loadProducts(""); 
				else loadFoodItems(null);
				return;
			}

			if (selectedTab == 0) { // Bar Menu
				try {
					Product pByCode = productService.findProductByBarcode(query);
					if (pByCode != null) {
						openSizeSelector(pByCode);
						tfSearchBilling.setText(""); 
						tfSearchBilling.requestFocusInWindow();
						return; 
					}
					loadProducts(query);
					tfSearchBilling.selectAll(); 

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			} 
			else if (selectedTab == 1) { // Food Menu
				loadFoodItems(query); 
				tfSearchBilling.selectAll();
			}
		});

		JButton btnSearch = new JButton("Search");
		btnSearch.setForeground(Color.WHITE);
		btnSearch.setFont(new Font("Arial", Font.BOLD, 15));
		btnSearch.setBackground(searchOrange); 
		btnSearch.addActionListener(e -> {
			String query = tfSearchBilling.getText().trim();
			int selectedTab = itemSelectionTabs.getSelectedIndex();
			if (selectedTab == 0) loadProducts(query);
			else loadFoodItems(query);
		});

		JButton btnLoadAll = new JButton("Load All");
		btnLoadAll.setForeground(Color.WHITE);
		btnLoadAll.setFont(new Font("Arial", Font.BOLD, 15));
		btnLoadAll.setBackground(primaryBlue); 
		btnLoadAll.addActionListener(e -> { 
			tfSearchBilling.setText(""); 
			int selectedTab = itemSelectionTabs.getSelectedIndex();
			if (selectedTab == 0) loadProducts(""); 
			else loadFoodItems(null);
		});

		top.add(btnSearch); 
		top.add(btnLoadAll);
		pnl.add(top, BorderLayout.NORTH);
		pnl.add(itemSelectionTabs, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout());
		txtBill = new JTextArea();
		txtBill.setFont(new Font("Monospaced", Font.PLAIN, 14));
		txtBill.setEditable(false);
		JScrollPane billScr = new JScrollPane(txtBill);
		Dimension screenSize1 = Toolkit.getDefaultToolkit().getScreenSize();
		int sidebarWidth = (screenSize1.width < 1400) ? 320 : 400; // Shrink for smaller screens
		billScr.setPreferredSize(new Dimension(sidebarWidth, 0));
		right.add(billScr, BorderLayout.CENTER); 

		lblTotalNewCost = new JLabel("Total Items: 0  |  Total Cost: ₹0.00", SwingConstants.CENTER);
		lblTotalNewCost.setFont(new Font("Arial", Font.BOLD, 22)); 
		lblTotalNewCost.setForeground(searchOrange); 
		lblTotalNewCost.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		lblTotalNewCost.setOpaque(true);
		lblTotalNewCost.setBackground(new Color(40, 40, 40)); 

		JPanel pnlSouthWrapper = new JPanel(new BorderLayout()); 
		pnlSouthWrapper.add(lblTotalNewCost, BorderLayout.NORTH); 

		JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		Font actionButtonFont = new Font("Arial", Font.BOLD, 14);

		JButton btnSaveOrder = new JButton("Save/Update Order"); 
		btnSaveOrder.setFont(actionButtonFont); 
		btnSaveOrder.setBackground(addColor); 
		btnSaveOrder.setForeground(Color.WHITE);
		btnSaveOrder.addActionListener(e -> saveCurrentOrder());

		JButton btnDeleteQty = new JButton("Remove Qty (Cart)");
		btnDeleteQty.setFont(actionButtonFont); 
		btnDeleteQty.setBackground(deleteColor); 
		btnDeleteQty.setForeground(Color.WHITE);
		btnDeleteQty.addActionListener(e -> deleteQtyDialog());

		JButton btnClear = new JButton("Clear Cart");
		btnClear.setFont(actionButtonFont); 
		btnClear.setBackground(clearColor); 
		btnClear.setForeground(Color.WHITE);
		btnClear.addActionListener(e -> { 
			currentCart.clear();
			activeOrder = null; 
			updateCartDisplay(); 
		});

		rightBottom.add(btnSaveOrder);
		rightBottom.add(btnDeleteQty); 
		rightBottom.add(btnClear);

		pnlSouthWrapper.add(rightBottom, BorderLayout.CENTER); 
		right.add(pnlSouthWrapper, BorderLayout.SOUTH); 
		pnl.add(right, BorderLayout.EAST);

		loadProducts("");
		loadFoodItems(null); 

		return pnl;
	}

	// ---------------- HELPER METHODS ----------------

	private void loadProducts(String keyword) {
		productPanel.removeAll();
		try {
			List<Product> products = productService.loadProducts(keyword);
			for (Product p : products) {
				productPanel.add(createProductButton(p));
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Product Service Error: " + ex.getMessage());
		}
		productPanel.revalidate();
		productPanel.repaint();
	}

	private JButton createProductButton(Product p) {
		JButton btn = new JButton();
		btn.setLayout(new BorderLayout());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int btnW = (screenSize.width < 1300) ? 160 : 220;
		int btnH = (screenSize.width < 1300) ? 220 : 280;

		btn.setPreferredSize(new Dimension(btnW, btnH));

		String imgfn = p.getImageFilename();
		if (imgfn != null && !imgfn.isEmpty()) {
			try {
				// 1. Try AppData (User Uploaded Brand Image)
				File f = getAppDataImageFile(imgfn);
				if (f.exists()) {
					ImageIcon icon = new ImageIcon(f.getAbsolutePath());
					Image img = icon.getImage().getScaledInstance(113, 210, Image.SCALE_SMOOTH);
					btn.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
				} 
				// 2. Fallback to JAR (Built-in)
				else {
					URL u = getClass().getResource("/images/" + imgfn);
					if (u != null) {
						ImageIcon icon = new ImageIcon(u);
						Image img = icon.getImage().getScaledInstance(113, 210, Image.SCALE_SMOOTH);
						btn.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
					}
				}
			} catch (Exception ignore) {}
		}

		JLabel lblName = new JLabel("<html><center><b>" + p.getName() + "</b><br/>Rack: " + p.getRackId() + "</center></html>", SwingConstants.CENTER);
		btn.add(lblName, BorderLayout.SOUTH);
		btn.addActionListener(e -> openSizeSelector(p));
		return btn;
	}

	private File getAppDataImageFile(String filename) {
		String appData = System.getenv("APPDATA");
		// Must match the folder used in OwnerPanel
		File folder = new File(appData, "BillPAY" + File.separator + "images");
		return new File(folder, filename);
	}

	// Method 1: The Size Selector (Buttons for 30ml, 60ml, etc.)
	private void openSizeSelector(Product p) {
		JDialog dlg = new JDialog(this, "Sizes - " + p.getName(), true);
		dlg.setSize(520, 240);
		dlg.setLayout(new BorderLayout());
		JPanel sizesPanel = new JPanel(new GridLayout(1, 4, 10, 10));

		try {
			List<Variant> variants = productService.getVariantsByProductBarcode(p.getBarcodeId());
			for (Variant v : variants) {
				// UPDATED: Stock text removed from button
				JButton b = new JButton("<html><center><font size='5'>" + v.getSizeMl() + " ml</font><br>₹" + v.getPrice() + "</center></html>");
				b.addActionListener(ev -> {
					addVariantToCart(v, p);
					dlg.dispose();
				});
				sizesPanel.add(b);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
		dlg.add(sizesPanel, BorderLayout.CENTER);
		dlg.setLocationRelativeTo(this);
		dlg.setVisible(true);
	}

	// Method 2: Adding to Cart (The Quantity Popup)
	private void addVariantToCart(Variant v, Product p) {
		JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
		panel.add(new JLabel("Product:")); 
		panel.add(new JLabel(p.getName() + " - " + v.getSizeMl() + "ml"));
		panel.add(new JLabel("Price:")); 
		panel.add(new JLabel("₹" + v.getPrice()));

		// UPDATED: Available Stock Row Removed completely

		panel.add(new JLabel("Quantity:")); 
		JTextField tfQty = new JTextField("1"); 
		panel.add(tfQty);

		int result = JOptionPane.showConfirmDialog(this, panel, "Add to Cart", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			int qty;
			try { qty = Integer.parseInt(tfQty.getText()); } 
			catch (Exception ex) { return; }

			// UPDATED: Logic now directly adds to cart without checking if stock is > 0
			addCartItemLogic(v.getId(), "BAR", p.getName(), v.getSizeMl(), v.getPrice(), qty, 0);
		}
	}

	private void loadFoodItems(String keyword) {
		foodPanel.removeAll();
		try {
			List<FoodItem> items = foodService.loadFoodItems(keyword);
			boolean any = false;
			for (FoodItem f : items) {
				any = true;
				foodPanel.add(createFoodButton(f));
			}
			if (!any) {
				JLabel lbl = new JLabel("No food found for: " + (keyword == null ? "All" : keyword));
				lbl.setFont(new Font("Arial", Font.BOLD, 14));
				lbl.setForeground(Color.RED);
				foodPanel.add(lbl);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Food Service Error: " + ex.getMessage());
		}
		foodPanel.revalidate();
		foodPanel.repaint();
	}

	private JButton createFoodButton(FoodItem f) {
		JButton btn = new JButton();
		btn.setLayout(new BorderLayout());
		btn.setPreferredSize(new Dimension(220, 280));

		String imgfn = f.getImageFilename();
		if (imgfn != null && !imgfn.isEmpty()) {
			try {
				// 1. Try AppData (User Uploaded Images)
				File file = getAppDataImageFile(imgfn);
				if (file.exists()) {
					ImageIcon icon = new ImageIcon(file.getAbsolutePath());
					Image img = icon.getImage().getScaledInstance(113, 210, Image.SCALE_SMOOTH);
					btn.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
				} 
				// 2. Fallback to JAR (Built-in Icons)
				else {
					URL u = getClass().getResource("/images/" + imgfn);
					if (u != null) {
						ImageIcon icon = new ImageIcon(u);
						Image img = icon.getImage().getScaledInstance(113, 210, Image.SCALE_SMOOTH);
						btn.add(new JLabel(new ImageIcon(img)), BorderLayout.CENTER);
					}
				}
			} catch (Exception ignore) {}
		}

		JLabel lblName = new JLabel("<html><center><b>" + f.getName() + "</b><br/>" + f.getCategory() + "<br/>₹" + f.getPrice() + "</center></html>", SwingConstants.CENTER);
		btn.add(lblName, BorderLayout.SOUTH);
		btn.addActionListener(e -> addFoodItemToCart(f));
		return btn;
	}

	private void addFoodItemToCart(FoodItem f) {
		JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
		panel.add(new JLabel("Food Item:")); panel.add(new JLabel(f.getName()));
		panel.add(new JLabel("Price:")); panel.add(new JLabel("₹" + f.getPrice()));
		panel.add(new JLabel("Quantity:")); JTextField tfQty = new JTextField("1"); panel.add(tfQty);

		int result = JOptionPane.showConfirmDialog(this, panel, "Add to Cart", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			int qty;
			try { qty = Integer.parseInt(tfQty.getText()); } 
			catch (Exception ex) { JOptionPane.showMessageDialog(this, "Invalid quantity."); return; }

			if (qty <= 0) { JOptionPane.showMessageDialog(this, "Quantity must be > 0."); return; }

			addCartItemLogic(0, "FOOD", f.getName(), 0, f.getPrice(), qty, f.getId());
		}
	}

	// --- UNIFIED CART LOGIC ---
	private void addCartItemLogic(int variantId, String type, String name, int size, double price, int qty, int foodId) {
		int tableNo;
		String manager;

		// --- BUG FIX LOGIC: Check activeOrder first ---
		if (activeOrder != null) {
			tableNo = activeOrder.getTableNo();
			manager = activeOrder.getManagerName();
		} else if (!currentCart.isEmpty()) {
			tableNo = currentCart.get(0).tableNo;
			manager = currentCart.get(0).manager;
		} else {
			tableNo = getTableNoFromUser();
			if (tableNo == -1) return; 
			manager = getManagerFromUser();
			if (manager == null) return; 
		}

		boolean updatedExisting = false;
		for (CartItem item : currentCart) {
			if (type.equals("BAR")) {
				if (item.itemType.equals("BAR") && item.variantId == variantId) {
					item.qty += qty;
					updatedExisting = true;
					break;
				}
			} else { // FOOD
				if (item.itemType.equals("FOOD") && item.foodItemId == foodId) {
					item.qty += qty;
					updatedExisting = true;
					break;
				}
			}
		}

		if (!updatedExisting) {
			CartItem item = new CartItem(variantId, name, size, price, qty, manager, tableNo, null);
			item.itemType = type;
			if (type.equals("FOOD")) item.foodItemId = foodId;
			currentCart.add(item);
		}
		updateCartDisplay();
	}

	private int getTableNoFromUser() {
		String tableStr = JOptionPane.showInputDialog(this, "Table No (number):", "1");
		if (tableStr == null) return -1;
		try { return Integer.parseInt(tableStr); } 
		catch (Exception ex) { return -1; }
	}

	private String getManagerFromUser() {
		String manager = JOptionPane.showInputDialog(this, "Manager Name:", "Manager1");
		if (manager == null || manager.trim().isEmpty()) return null;
		return manager;
	}

	private void updateCartDisplay() {
		txtBill.setText("");
		txtBill.setForeground(Color.WHITE);

		// --- VISUAL INDICATOR ---
		if (activeOrder != null) {
			txtBill.append("             >>> ITEMS ALREADY ON TABLE " + activeOrder.getTableNo() + " <<<\n");
		}

		double total = 0;
		int itemsCount = 0;

		if (currentCart.isEmpty()) {
			txtBill.append("----------------------------------------------------------");
			txtBill.append("\n       >>  CART IS EMPTY (Ready to select items)  <<\n");
			txtBill.append("----------------------------------------------------------\n");
			lblTotalNewCost.setText("Total Items: 0  |  Total Cost: ₹0.00");
			return;
		}
		txtBill.append("\n----------------- NEW ITEMS TO BE ADDED -----------------\n");

		for (CartItem it : currentCart) { 
			txtBill.append(it.toText() + "\n");
			total += it.qty * it.price;
			itemsCount += it.qty;
		}

		txtBill.append("\n---------------------------------------------------------\n");
		String totalText = String.format("Total Items: %d |  Total Cost: ₹%.2f", itemsCount, round(total));
		lblTotalNewCost.setText(totalText);
	}

	private void saveCurrentOrder() {
		if (currentCart.isEmpty()) { 
			JOptionPane.showMessageDialog(this, "Cart is empty. Select items first."); 
			return; 
		}

		int tableNo = currentCart.get(0).tableNo;
		String manager = currentCart.get(0).manager;

		try {
			// 1. SILENT SAVE: No "Confirm" popup, no "Success" message
			int orderId = billingService.saveOrder(currentCart, tableNo, manager);

			if (orderId > 0) {
				// 2. KOT REMOVED: We no longer call printKOT() here

				// 3. FAST RESET: Clear cart and UI immediately
				currentCart.clear();
				activeOrder = null; 
				updateCartDisplay();

				// OPTIONAL: Switch back to Tables tab automatically for speed
				mainTabs.setSelectedIndex(1); 
			}
		} catch (Exception ex) {
			// We only show a message if there is an actual Database Error
			JOptionPane.showMessageDialog(this, "Order Save Error: " + ex.getMessage());
			Logger.error(APP_SOURCE, "Failed to save order: " + ex.getMessage());
		}
	}

	private void deleteQtyDialog() {
		if (currentCart.isEmpty()) { JOptionPane.showMessageDialog(this, "Cart is empty"); return; }

		String[] items = currentCart.stream().map(it -> {
			String name = it.productName;
			if(it.itemType.equals("BAR")) name += " - ".concat(String.valueOf(it.sizeMl)).concat("ml");
			return name + " x" + it.qty;
		}).toArray(String[]::new);

		String selected = (String) JOptionPane.showInputDialog(this, "Select item to delete quantity:", "Delete Quantity", JOptionPane.QUESTION_MESSAGE, null, items, items[0]);
		if (selected == null) return;

		int index = -1;
		for (int i = 0; i < items.length; i++) {
			if (items[i].equals(selected)) { index = i; break; }
		}
		if (index == -1) return;

		CartItem item = currentCart.get(index);
		String qtyStr = JOptionPane.showInputDialog(this, "Enter quantity to remove (max " + item.qty + "):", "1");
		if (qtyStr == null) return;

		try {
			int removeQty = Integer.parseInt(qtyStr);
			if (removeQty <= 0 || removeQty > item.qty) { JOptionPane.showMessageDialog(this, "Invalid quantity entered."); return; }

			item.qty -= removeQty;
			if (item.qty == 0) { currentCart.remove(index); }

			Logger.warn(APP_SOURCE, "Removed " + removeQty + " qty of " + item.productName + " from current cart.");
			updateCartDisplay();
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Invalid number.");
		}
	}
	private double round(double v) { return Math.round(v*100.0)/100.0; }
}