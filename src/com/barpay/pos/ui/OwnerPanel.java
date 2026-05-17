package com.barpay.pos.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import com.barpay.pos.model.FoodItem;
import com.barpay.pos.service.BillingService;
import com.barpay.pos.service.FoodService;
import com.barpay.pos.service.ProductService;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;

@SuppressWarnings({ "serial" })
public class OwnerPanel extends JPanel {

	private static final String APP_SOURCE = "OwnerPanel";
	private static String OWNER_PASS = null;
	
	

	// Services
	private ProductService productService;
	private BillingService billingService;
	private FoodService foodService;
	@SuppressWarnings("unused")
	private JTabbedPane mainTabs; // Reference to the main window

	// NEW
	public OwnerPanel(ProductService productService, BillingService billingService, FoodService foodService, JTabbedPane mainTabs) {
		this.productService = productService;
		this.billingService = billingService;
		this.foodService = foodService;
		this.mainTabs = mainTabs;
		loadOwnerPassword();

		this.setLayout(new BorderLayout());

		// --- Top Button Bar ---
		// 1. Get client screen width
		Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

		// 2. Decide columns: 5 for big screens, 3 for small laptops
		int buttonCols = (screenSize.width < 1400) ? 3 : 5;

		// 3. Set layout to a Grid so buttons don't disappear
		JPanel top = new JPanel(new GridLayout(0, buttonCols, 5, 5)); 
		top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// 4. Shrink font so labels fit on small screens
		int fontSize = (screenSize.width < 1300) ? 9 : 11;
		Font responsiveFont = new Font("Arial", Font.BOLD, fontSize);

		Color addColor = new Color(2, 202, 58);      // #02CA3A (Green for 'Add' buttons)
		Color editColor = new Color(2, 92, 202);     // #025CCA (Blue for 'Edit/Remove' buttons)
		Color viewColor = new Color(171, 171, 171);  // #ABABAB (Grey for 'View Variants')
		Color securityColor = new Color(255, 111, 0); // #FF6F00 (Rich Burnt Orange/Gold for Security/Change Pass)
		Color textColor = Color.WHITE; // Keep text White for contrast               

		
		JButton btnView = new JButton("View Variants");
		btnView.setBackground(viewColor);
		btnView.setForeground(textColor);
		btnView.setFont(responsiveFont);
		btnView.addActionListener(e -> viewVariantsTable());
		top.add(btnView);

		JButton btnAddBrand = new JButton("Add New Brand");
		btnAddBrand.setBackground(addColor);
		btnAddBrand.setForeground(textColor);
		btnAddBrand.setFont(responsiveFont);
		btnAddBrand.addActionListener(e -> addBrandOwner());
		top.add(btnAddBrand); 

		JButton btnEditBrand = new JButton("Edit/Remove Brand");
		btnEditBrand.setBackground(editColor);
		btnEditBrand.setForeground(textColor);
		btnEditBrand.setFont(responsiveFont);
		btnEditBrand.addActionListener(e -> editRemoveBrand());
		top.add(btnEditBrand); 

		JButton btnAddVariant = new JButton("Add Variant");
		btnAddVariant.setBackground(addColor);
		btnAddVariant.setForeground(textColor);
		btnAddVariant.setFont(responsiveFont);
		btnAddVariant.addActionListener(e -> addVariantOwner());
		top.add(btnAddVariant); 

		JButton btnEditVariant = new JButton("Edit/Remove Variant");
		btnEditVariant.setBackground(editColor);
		btnEditVariant.setForeground(textColor);
		btnEditVariant.setFont(responsiveFont);
		btnEditVariant.addActionListener(e -> editRemoveVariant());
		top.add(btnEditVariant); 

		JButton btnAddFood = new JButton("Add Food Item");
		btnAddFood.setBackground(addColor);
		btnAddFood.setForeground(textColor);
		btnAddFood.setFont(responsiveFont);
		btnAddFood.addActionListener(e -> addFoodItemOwner());
		top.add(btnAddFood);

		JButton btnEditFood = new JButton("Edit/Remove Food");
		btnEditFood.setBackground(editColor);
		btnEditFood.setFont(responsiveFont);
		btnEditFood.addActionListener(e -> editRemoveFoodItemOwner());
		top.add(btnEditFood);
		// --- End Food Buttons ---

		JButton btnEditProductBarcode = new JButton("Edit/Product Barcode");
		btnEditProductBarcode.setBackground(editColor);
		btnEditProductBarcode.setForeground(textColor);
		btnEditProductBarcode.setFont(responsiveFont);
		btnEditProductBarcode.addActionListener(e -> showEditProductBarcodeDialog());
		top.add(btnEditProductBarcode);

		JButton btnEditVariantBarcode = new JButton("Edit/Variant Barcode");
		btnEditVariantBarcode.setBackground(editColor);
		btnEditVariantBarcode.setForeground(textColor);
		btnEditVariantBarcode.setFont(responsiveFont);
		btnEditVariantBarcode.addActionListener(e -> showEditVariantBarcodeDialog());
		top.add(btnEditVariantBarcode);

		JButton btnChangePass = new JButton("Change Password");
		btnChangePass.setBackground(securityColor);
		btnChangePass.setForeground(textColor);
		btnChangePass.setFont(responsiveFont);
		btnChangePass.addActionListener(e -> handleChangePassword());
		top.add(btnChangePass);

		JButton btnBackup = new JButton("Backup Data");
		btnBackup.setBackground(new Color(100, 0, 200)); // Purple color to stand out
		btnBackup.setForeground(Color.WHITE);
		btnBackup.setFont(responsiveFont);
		btnBackup.addActionListener(e -> performBackup());
		top.add(btnBackup);

		// --- Owner Tabs Setup ---
		JTabbedPane ownerTabs = new JTabbedPane();

		// --- NEW: SET SUB-TAB FONT SIZE 14 BOLD ---
		ownerTabs.setFont(new Font("Segoe UI", Font.BOLD, 14));
		// ------------------------------------------

		ownerTabs.add(" Reports ", buildReportsTab() ); // This method is now upgraded
		ownerTabs.add(" Bill History ", buildHistoryTab() );

		// Wrap the buttons in a scroll pane so they never get cut off
		JScrollPane topScroll = new JScrollPane(top);
		topScroll.setPreferredSize(new Dimension(0, (screenSize.width < 1300) ? 150 : 80)); 
		topScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		this.add(topScroll, BorderLayout.NORTH);
		this.add(ownerTabs, BorderLayout.CENTER); 

		// --- Password Check ---
		// ADD THIS NEW BLOCK
		// --- Password Check ---
		mainTabs.addChangeListener(e -> {
			// Check if the tab being selected is THIS panel
			if (mainTabs.getSelectedComponent() == OwnerPanel.this) {
				// If it is, ask for the password
				if (!askOwnerPassword()) {
					// If the password is wrong, go back to the first tab (Billing)
					mainTabs.setSelectedIndex(0); 
				}
			}
		});
	}

	// ----------------
	// Password & Security
	// ----------------

	// In OwnerPanel.java...

	private boolean askOwnerPassword() {
		// --- Step 1: show blank background (Glass Pane) ---
		JPanel blankPane = new JPanel();
		blankPane.setBackground(Color.WHITE); // You can change this to Color.BLACK
		blankPane.setOpaque(true);

		// Get the top-level window (the JFrame) and set its glass pane
		getRootPane().setGlassPane(blankPane);
		getRootPane().getGlassPane().setVisible(true);

		// --- Step 2: show password dialog ---
		JPanel p = new JPanel(new FlowLayout());
		JPasswordField pf = new JPasswordField(12);
		p.add(new JLabel("Owner Password:"));
		p.add(pf);

		// Use 'this' (the OwnerPanel) as the parent
		int r = JOptionPane.showConfirmDialog(
				this, 
				p, "Owner Access",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
				);

		// --- Step 3: always hide blank background when dialog closes ---
		getRootPane().getGlassPane().setVisible(false);

		if (r != JOptionPane.OK_OPTION) return false;
		String pass = new String(pf.getPassword());
		return OWNER_PASS != null && OWNER_PASS.equals(pass);
	}

	// --- NEW HELPER METHOD: Gets a safe writable path ---
	private Path getConfigFile() {
		// 1. Get the user's AppData folder (e.g., C:\Users\Name\AppData\Roaming)
		String appData = System.getenv("APPDATA");

		// 2. Create a specific folder for your app (e.g., BarPayPOS)
		Path folder = java.nio.file.Paths.get(appData, "BillPAY");

		// 3. Make sure the folder exists
		try {
			if (!Files.exists(folder)) {
				Files.createDirectories(folder);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 4. Return the full path to the file
		return folder.resolve("owner.properties");
	}

	private void loadOwnerPassword() {
		try {
			// CHANGE: Use the safe path instead of just "owner.properties"
			Path pf = getConfigFile(); 

			if (Files.exists(pf)) {
				Properties props = new Properties();
				try (java.io.InputStream in = Files.newInputStream(pf)) {
					props.load(in);
				}
				OWNER_PASS = props.getProperty("owner.pass", "admin1234");
			} else {
				OWNER_PASS = "admin123";
			}
		} catch (Exception ex) {
			ex.printStackTrace(); // Good to see errors in console if testing
			OWNER_PASS = "admin123";
		}
	}

	private void saveOwnerPassword(String newPass) throws Exception {
		Properties props = new Properties();
		props.setProperty("owner.pass", newPass);

		// CHANGE: Use the safe path
		Path pf = getConfigFile(); 

		try (java.io.OutputStream out = Files.newOutputStream(pf)) {
			props.store(out, "Owner password for POS - do not share");
		}
		OWNER_PASS = newPass;
	}

	private void handleChangePassword() {
		JPanel p = new JPanel(new GridLayout(0,2,6,6));
		JPasswordField pfCurr = new JPasswordField(12);
		JPasswordField pfNew = new JPasswordField(12);
		JPasswordField pfNew2 = new JPasswordField(12);
		p.add(new JLabel("Current Password:")); p.add(pfCurr);
		p.add(new JLabel("New Password:")); p.add(pfNew);
		p.add(new JLabel("Confirm New:")); p.add(pfNew2);
		int r = JOptionPane.showConfirmDialog(this, p, "Change Owner Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (r != JOptionPane.OK_OPTION) return;
		String curr = new String(pfCurr.getPassword());
		if (!OWNER_PASS.equals(curr)) {
			JOptionPane.showMessageDialog(this, "Current password incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		String n1 = new String(pfNew.getPassword());
		String n2 = new String(pfNew2.getPassword());
		if (n1.length() < 4) {
			JOptionPane.showMessageDialog(this, "New password must be at least 4 characters.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if (!n1.equals(n2)) {
			JOptionPane.showMessageDialog(this, "New passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			saveOwnerPassword(n1);
			JOptionPane.showMessageDialog(this, "Owner password changed successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Failed to save new password: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// ----------------
	// NEW: Food Management
	// ----------------

	private void addFoodItemOwner() {
		try {
			JTextField tfName = new JTextField();
			JTextField tfCategory = new JTextField();
			JTextField tfPrice = new JTextField();
			JLabel lblChosen = new JLabel("No file chosen");
			JButton btnChoose = new JButton("Choose Image");
			final File[] chosen = new File[1];

			btnChoose.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser();
				int rv = chooser.showOpenDialog(this);
				if (rv == JFileChooser.APPROVE_OPTION) {
					chosen[0] = chooser.getSelectedFile();
					lblChosen.setText(chosen[0].getName());
				}
			});

			JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
			panel.add(new JLabel("Food Name*:")); panel.add(tfName);
			panel.add(new JLabel("Category*:"));  panel.add(tfCategory);
			panel.add(new JLabel("Price*:"));     panel.add(tfPrice);
			panel.add(new JLabel("Image:"));      panel.add(btnChoose);
			panel.add(new JLabel("Chosen:"));     panel.add(lblChosen);

			int res = JOptionPane.showConfirmDialog(this, panel, "Add Food Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;

			String name = tfName.getText().trim();
			String category = tfCategory.getText().trim();
			if (name.isEmpty() || category.isEmpty() || tfPrice.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(this, "Name, Category, and Price are required.");
				return;
			}
			double price = Double.parseDouble(tfPrice.getText().trim());

			String imageFilename = "";
			if (chosen[0] != null) {
				try {
					// --- FIX: Save to AppData instead of Program Files ---
					java.nio.file.Path imagesDir = getImagesDirectory(); 
					String fname = System.currentTimeMillis() + "_" + chosen[0].getName();
					java.nio.file.Path dest = imagesDir.resolve(fname);
					java.nio.file.Files.copy(chosen[0].toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					imageFilename = fname;
				} catch (Exception e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, "Failed to save image: " + e.getMessage());
				}
			}

			foodService.addFoodItem(name, category, price, imageFilename);
			JOptionPane.showMessageDialog(this, "Food item added successfully!");

		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(this, "Invalid price entered.", "Error", JOptionPane.ERROR_MESSAGE);
		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Error adding food item: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}

	private void editRemoveFoodItemOwner() {
		try {
			List<FoodItem> items = foodService.loadFoodItems(null); 
			if (items.isEmpty()) {
				JOptionPane.showMessageDialog(this, "No food items to edit.");
				return;
			}

			// 1. Create Table Model
			DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Category", "Price"}, 0) {
				@Override public boolean isCellEditable(int row, int col) { return false; }
			};

			for (FoodItem f : items) {
				model.addRow(new Object[] { f.getId(), f.getName(), f.getCategory(), f.getPrice() });
			}

			JTable table = new JTable(model);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			// 2. Search Logic
			TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
			table.setRowSorter(sorter);
			JTextField tfSearch = new JTextField(20);
			tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
				void filter() {
					String text = tfSearch.getText().trim();
					if (text.length() == 0) sorter.setRowFilter(null);
					else try { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text)); } catch (Exception e) {}
				}
				public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
			});

			JPanel pnl = new JPanel(new BorderLayout(5, 5));
			JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
			top.add(new JLabel("Search (Name / Price):")); top.add(tfSearch);
			pnl.add(top, BorderLayout.NORTH);
			pnl.add(new JScrollPane(table), BorderLayout.CENTER);
			pnl.setPreferredSize(new Dimension(600, 400));

			// 3. Show Dialog
			int res = JOptionPane.showConfirmDialog(this, pnl, "Select Food Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;

			int selectedRow = table.getSelectedRow();
			if (selectedRow == -1) return;

			int modelRow = table.convertRowIndexToModel(selectedRow);
			int foodId = (int) model.getValueAt(modelRow, 0);

			FoodItem selectedItem = null;
			for(FoodItem f : items) { if(f.getId() == foodId) { selectedItem = f; break; } }
			if (selectedItem == null) return;

			String[] opts = {"Edit","Remove (Hide)","Cancel"};
			int act = JOptionPane.showOptionDialog(this, "Edit or Remove " + selectedItem.getName() + "?", "Choose", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

			if (act == 0) { // --- EDIT ---
				JTextField tfName = new JTextField(selectedItem.getName());
				JTextField tfCategory = new JTextField(selectedItem.getCategory());
				JTextField tfPrice = new JTextField(String.valueOf(selectedItem.getPrice()));
				JLabel lblImg = new JLabel(selectedItem.getImageFilename());
				JButton btnChoose = new JButton("Choose Image");
				final File[] chosen = new File[1];
				btnChoose.addActionListener(ev -> {
					JFileChooser chooser = new JFileChooser();
					if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
						chosen[0] = chooser.getSelectedFile();
						lblImg.setText(chosen[0].getName());
					}
				});

				JPanel panel = new JPanel(new GridLayout(0,2,6,6));
				panel.add(new JLabel("Name:")); panel.add(tfName);
				panel.add(new JLabel("Category:")); panel.add(tfCategory);
				panel.add(new JLabel("Price (₹):")); panel.add(tfPrice);
				panel.add(new JLabel("Image:")); panel.add(btnChoose);
				panel.add(new JLabel("Current:")); panel.add(lblImg);

				if (JOptionPane.showConfirmDialog(this, panel, "Edit Food Item", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

				String newName = tfName.getText().trim();
				String newCategory = tfCategory.getText().trim();
				double newPrice = Double.parseDouble(tfPrice.getText().trim());
				String newImg = selectedItem.getImageFilename();

				if (chosen[0] != null) {
					try {
						// --- THE FIX: USE APPDATA FOLDER ---
						Path imagesDir = getImagesDirectory(); 
						String fname = System.currentTimeMillis() + "_" + chosen[0].getName();
						Path dest = imagesDir.resolve(fname);
						Files.copy(chosen[0].toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
						newImg = fname;
					} catch (Exception e) {
						JOptionPane.showMessageDialog(this, "Error saving image: " + e.getMessage());
					}
				}

				foodService.updateFoodItem(selectedItem.getId(), newName, newCategory, newPrice, newImg);
				JOptionPane.showMessageDialog(this, "Food item updated.");

			} else if (act == 1) { // --- REMOVE ---
				if (JOptionPane.showConfirmDialog(this, "Hide this item from menu?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					foodService.deleteFoodItem(selectedItem.getId());
					JOptionPane.showMessageDialog(this, "Food item hidden.");
				}
			}

		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Error editing food: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}

	private void addBrandOwner() {
		try {
			JTextField tfBarcodeId = new JTextField();
			JTextField tfName = new JTextField();
			JTextField tfRack = new JTextField();
			JLabel lblChosen = new JLabel("No file chosen");
			JButton btnChoose = new JButton("Choose Image");
			final File[] chosen = new File[1];

			btnChoose.addActionListener(e -> {
				JFileChooser chooser = new JFileChooser();
				int rv = chooser.showOpenDialog(this);
				if (rv == JFileChooser.APPROVE_OPTION) {
					chosen[0] = chooser.getSelectedFile();
					lblChosen.setText(chosen[0].getName());
				}
			});

			JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
			panel.add(new JLabel("Product Barcode ID*:")); panel.add(tfBarcodeId);
			panel.add(new JLabel("Brand Name*:")); panel.add(tfName);
			panel.add(new JLabel("Rack ID*:")); panel.add(tfRack);
			panel.add(new JLabel("Image:")); panel.add(btnChoose);
			panel.add(new JLabel("Chosen:")); panel.add(lblChosen);

			int res = JOptionPane.showConfirmDialog(this, panel, "Add Brand", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;

			String barcodeId = tfBarcodeId.getText().trim();
			String name = tfName.getText().trim();
			String rack = tfRack.getText().trim();

			if (name.isEmpty() || rack.isEmpty() || barcodeId.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Barcode ID, Name, and Rack ID are required."); return;
			}

			String imageFilename = "";
			if (chosen[0] != null) {
				try {
					// --- FIX: Save to AppData ---
					java.nio.file.Path imagesDir = getImagesDirectory();
					String fname = System.currentTimeMillis() + "_" + chosen[0].getName();
					java.nio.file.Path dest = imagesDir.resolve(fname);
					java.nio.file.Files.copy(chosen[0].toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					imageFilename = fname;
				} catch(Exception e) {
					e.printStackTrace();
				}
			}

			// Insert into DB (Make sure connection code matches new DBUtil)
			try (Connection c = DBUtil.getConnection();
					PreparedStatement ps = c.prepareStatement("INSERT INTO products (name, rack_id, image_filename, barcode_id) VALUES (?,?,?,?)")) {
				ps.setString(1, name);
				ps.setString(2, rack);
				ps.setString(3, imageFilename);
				ps.setString(4, barcodeId); 
				ps.executeUpdate();
				JOptionPane.showMessageDialog(this, "Brand added successfully!");
			} catch (Exception dbEx) {
				if (dbEx.getMessage().contains("Duplicate entry")) {
					JOptionPane.showMessageDialog(this, "DB Error: The Barcode ID '" + barcodeId + "' already exists.", "Error", JOptionPane.ERROR_MESSAGE);
				} else {
					throw dbEx;
				}
			}
		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Error adding brand: " + ex.getMessage());
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
		}
	}

	private java.nio.file.Path getImagesDirectory() {
		// 1. Get User AppData (e.g. C:\Users\Name\AppData\Roaming)
		String appData = System.getenv("APPDATA");
		// 2. Target folder: ...\AppData\Roaming\BillPAY\images
		java.nio.file.Path folder = java.nio.file.Paths.get(appData, "BillPAY", "images");

		// 3. Create if not exists
		try {
			if (!java.nio.file.Files.exists(folder)) {
				java.nio.file.Files.createDirectories(folder);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return folder;
	}

	private void editRemoveBrand() {
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement("SELECT id, barcode_id, rack_id, name FROM products ORDER BY barcode_id, name");
				ResultSet rs = ps.executeQuery()) {

			// 1. Prepare Table Data
			String[] columnNames = {"Barcode ID", "Brand Name", "Rack ID"};
			DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
				@Override public boolean isCellEditable(int row, int column) { return false; }
			};

			while (rs.next()) {
				model.addRow(new Object[]{
						rs.getString("barcode_id"),
						rs.getString("name"),
						rs.getString("rack_id")
				});
			}

			JTable table = new JTable(model);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			// Set column widths for better look
			table.getColumnModel().getColumn(0).setPreferredWidth(100);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setPreferredWidth(80);

			// 2. Add Search Field
			TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
			table.setRowSorter(sorter);

			JTextField tfSearch = new JTextField(20);
			tfSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
				void filter() {
					String text = tfSearch.getText().trim();
					if (text.length() == 0) {
						sorter.setRowFilter(null);
					} else {
						try {
							// Search across all columns
							sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
						} catch (Exception e) {}
					}
				}
				public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
			});

			JPanel pnl = new JPanel(new BorderLayout(5, 5));
			JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
			top.add(new JLabel("Search Brand:"));
			top.add(tfSearch);

			pnl.add(top, BorderLayout.NORTH);
			pnl.add(new JScrollPane(table), BorderLayout.CENTER);
			// EXACT PATH FIX: Get screen height and make the window 50% of it
			Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			int dynamicHeight = (int)(screen.height * 0.5); 
			int dynamicWidth = (int)(screen.width * 0.4); // 40% of width
			pnl.setPreferredSize(new Dimension(dynamicWidth, dynamicHeight));

			// 3. Show Dialog
			int res = JOptionPane.showConfirmDialog(this, pnl, "Select Brand to Edit/Remove", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;

			int selectedRow = table.getSelectedRow();
			if (selectedRow == -1) return;

			// Convert view row to model row (in case of sorting/filtering)
			int modelRow = table.convertRowIndexToModel(selectedRow);
			String chosenBarcodeId = (String) model.getValueAt(modelRow, 0); // Column 0 is Barcode ID

			String[] opts = {"Edit", "Remove", "Cancel"};
			int act = JOptionPane.showOptionDialog(this, "Action for Brand?", "Choose", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

			if (act == 0) { 
				// --- EDIT LOGIC ---
				try (PreparedStatement p2 = c.prepareStatement("SELECT name,rack_id,image_filename,barcode_id FROM products WHERE barcode_id=?")) {
					p2.setString(1, chosenBarcodeId);
					try (ResultSet r2 = p2.executeQuery()) {
						if (r2.next()) {
							JTextField tfName = new JTextField(r2.getString("name"));
							JTextField tfRack = new JTextField(r2.getString("rack_id"));
							JLabel lblImg = new JLabel(r2.getString("image_filename"));
							JButton btnChoose = new JButton("Choose Image");
							final File[] chosen = new File[1];
							btnChoose.addActionListener(ev -> {
								JFileChooser chooser = new JFileChooser();
								if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
									chosen[0] = chooser.getSelectedFile();
									lblImg.setText(chosen[0].getName());
								}
							});

							JPanel panel = new JPanel(new GridLayout(0, 2, 10, 10));
							panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
							panel.add(new JLabel("Brand Name:")); panel.add(tfName);
							panel.add(new JLabel("Rack ID:")); panel.add(tfRack);
							panel.add(new JLabel("Image:")); panel.add(btnChoose);
							panel.add(new JLabel("Current:")); panel.add(lblImg);

							if (JOptionPane.showConfirmDialog(this, panel, "Edit Brand", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

							String newName = tfName.getText().trim();
							String newRack = tfRack.getText().trim();
							String newImg = r2.getString("image_filename");

							if (chosen[0] != null) {
								try {
									Path imagesDir = getImagesDirectory();
									String fname = System.currentTimeMillis() + "_" + chosen[0].getName();
									Path dest = imagesDir.resolve(fname);
									Files.copy(chosen[0].toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
									newImg = fname;
								} catch (Exception e) {
									JOptionPane.showMessageDialog(this, "Image Save Error: " + e.getMessage());
								}
							}

							try (PreparedStatement up = c.prepareStatement("UPDATE products SET name=?, rack_id=?, image_filename=? WHERE barcode_id=?")) {
								up.setString(1, newName); up.setString(2, newRack); up.setString(3, newImg); 
								up.setString(4, chosenBarcodeId);
								up.executeUpdate();
								JOptionPane.showMessageDialog(this, "Brand updated.");
							}
						}
					}
				}
			} else if (act == 1) { 
				// --- REMOVE LOGIC ---
				if (JOptionPane.showConfirmDialog(this, "DELETE Brand & All Variants?", "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					// 1. Get Internal ID
					int pid = -1;
					try (PreparedStatement psId = c.prepareStatement("SELECT id FROM products WHERE barcode_id=?")) {
						psId.setString(1, chosenBarcodeId);
						ResultSet rsId = psId.executeQuery();
						if (rsId.next()) pid = rsId.getInt("id");
					}

					if (pid != -1) {
						// 2. Delete Child Data
						try (PreparedStatement d1 = c.prepareStatement("DELETE FROM bill_items WHERE variant_id IN (SELECT id FROM variants WHERE product_id=?)")) {
							d1.setInt(1, pid); d1.executeUpdate();
						}
						try (PreparedStatement d2 = c.prepareStatement("DELETE FROM stock_transfer_history WHERE variant_barcode_id IN (SELECT barcode_id FROM variants WHERE product_id=?)")) {
							d2.setInt(1, pid); d2.executeUpdate();
						}
						try (PreparedStatement d3 = c.prepareStatement("DELETE FROM variants WHERE product_id=?")) {
							d3.setInt(1, pid); d3.executeUpdate();
						}
						// 3. Delete Parent
						try (PreparedStatement d4 = c.prepareStatement("DELETE FROM products WHERE barcode_id=?")) {
							d4.setString(1, chosenBarcodeId); d4.executeUpdate();
						}
						JOptionPane.showMessageDialog(this, "Brand removed.");
					}
				}
			}
		} catch (Exception ex) { 
			ex.printStackTrace(); 
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); 
		}
	}

	private void addVariantOwner() {
	    try {
	        // Only keep fields for Identification, Size, and Price
	        JTextField tfProdBarcode = new JTextField(); 
	        JTextField tfVariantBarcode = new JTextField(); 
	        JTextField tfSize = new JTextField();
	        JTextField tfMRP = new JTextField(); 
	        JTextField tfPrice = new JTextField();

	        // Create the UI panel without any Stock labels or fields
	        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
	        panel.add(new JLabel("Product (Brand) Barcode:")); panel.add(tfProdBarcode);
	        panel.add(new JLabel("Variant (Size) Barcode:"));  panel.add(tfVariantBarcode); 
	        panel.add(new JLabel("Size (ml):"));               panel.add(tfSize);
	        panel.add(new JLabel("MRP Price (₹):"));           panel.add(tfMRP);
	        panel.add(new JLabel("Selling Price (₹):"));       panel.add(tfPrice);

	        int res = JOptionPane.showConfirmDialog(this, panel, "Add New Bar Item", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
	        if (res != JOptionPane.OK_OPTION) return;

	        // Collect the data
	        String prodBarcode = tfProdBarcode.getText().trim();
	        String variantBarcode = tfVariantBarcode.getText().trim();
	        int size = Integer.parseInt(tfSize.getText().trim());
	        double mrp = Double.parseDouble(tfMRP.getText().trim());
	        double price = Double.parseDouble(tfPrice.getText().trim());

	        try (Connection c = DBUtil.getConnection()) {
	            // Find the Brand ID using the Barcode
	            int pid = -1;
	            try (PreparedStatement psLookup = c.prepareStatement("SELECT id FROM products WHERE barcode_id = ?")) {
	                psLookup.setString(1, prodBarcode);
	                try (ResultSet rsLookup = psLookup.executeQuery()) {
	                    if (rsLookup.next()) {
	                        pid = rsLookup.getInt("id");
	                    } else {
	                        JOptionPane.showMessageDialog(this, "Error: Brand Barcode not found."); 
	                        return;
	                    }
	                }
	            }

	            // Insert into database with stock set to 0 automatically
	            // This allows the item to exist so you can bill it immediately
	            String insertSql = "INSERT INTO variants (product_id, size_ml, price,barcode_id, mrp_price) VALUES (?,?,?,?,?)";
	            try (PreparedStatement ps = c.prepareStatement(insertSql)) {
	                ps.setInt(1, pid); 
	                ps.setInt(2, size); 
	                ps.setDouble(3, price); 
	                ps.setString(4, variantBarcode); 
	                ps.setDouble(5, mrp); 

	                ps.executeUpdate();
	                JOptionPane.showMessageDialog(this, "Item added! You can now bill this item immediately.");
	            }
	        }
	    } catch (NumberFormatException nf) { 
	        JOptionPane.showMessageDialog(this, "Please enter valid numbers for Size and Price."); 
	    } catch (Exception ex) { 
	        ex.printStackTrace(); 
	        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); 
	    }
	}

	private void editRemoveVariant() {
		try (Connection c = DBUtil.getConnection();
				// MODIFICATION: Added v.mrp_price to the SELECT query
				PreparedStatement ps = c.prepareStatement("SELECT v.id, v.barcode_id, p.name, p.rack_id, v.size_ml, v.price, v.mrp_price FROM variants v JOIN products p ON v.product_id=p.id ORDER BY p.name, v.size_ml");
				ResultSet rs = ps.executeQuery()) {

			// MODIFICATION: Added "MRP" to the column list
			DefaultTableModel model = new DefaultTableModel(new String[]{"Variant ID","Barcode","Product","Rack","Size","Sell Price","MRP"}, 0);
			java.util.List<String> barcodes = new ArrayList<>(); 

			while (rs.next()) {
				String currentBarcode = rs.getString("barcode_id");
				barcodes.add(currentBarcode);

				// MODIFICATION: Added mrp_price to the row
				model.addRow(new Object[]{
						rs.getInt("id"), 
						currentBarcode, 
						rs.getString("name"), 
						rs.getString("rack_id"), 
						rs.getInt("size_ml"), 
						rs.getDouble("price"), 
						rs.getDouble("mrp_price") // Show MRP in table
				});
			}
			JTable table = new JTable(model);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
			table.setRowSorter(sorter);
			JTextField search = new JTextField(20);
			search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
				void filter() {
					String q = search.getText().trim();
					if (q.isEmpty()) { sorter.setRowFilter(null); return; }
					try { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q))); } catch (Exception ex) { sorter.setRowFilter(null); }
				}
				public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
				public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
			});

			JPanel pnl = new JPanel(new BorderLayout());
			JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
			top.add(new JLabel("Search Variants:"));
			top.add(search);
			pnl.add(top, BorderLayout.NORTH);
			JScrollPane sp = new JScrollPane(table);
			sp.setPreferredSize(new Dimension(850,350)); // Widen table slightly for new column
			pnl.add(sp, BorderLayout.CENTER);

			int res = JOptionPane.showConfirmDialog(this, pnl, "Select Variant to Edit/Remove", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (res != JOptionPane.OK_OPTION) return;
			int r = table.getSelectedRow();
			if (r < 0) return;

			int modelRow = table.getRowSorter().convertRowIndexToModel(r);
			String chosenVariantBarcodeId = barcodes.get(modelRow);

			String[] opts = {"Edit","Remove","Cancel"};
			int act = JOptionPane.showOptionDialog(this, "Edit or Remove variant?", "Action", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

			if (act == 0) {
				// --- EDIT LOGIC ---
				try (PreparedStatement p2 = c.prepareStatement("SELECT size_ml, price,barcode_id, mrp_price FROM variants WHERE barcode_id=?")) {
					p2.setString(1, chosenVariantBarcodeId);
					try (ResultSet r2 = p2.executeQuery()) {
						if (r2.next()) {
							JTextField tfBarcode = new JTextField(r2.getString("barcode_id"));
							tfBarcode.setEditable(false);
							JTextField tfSize = new JTextField(String.valueOf(r2.getInt("size_ml")));

							// MRP Field
							JTextField tfMRP = new JTextField(String.valueOf(r2.getDouble("mrp_price")));

							JTextField tfPrice = new JTextField(String.valueOf(r2.getDouble("price")));
							JPanel panel = new JPanel(new GridLayout(0,2,6,6));
							panel.add(new JLabel("Barcode ID:")); panel.add(tfBarcode);
							panel.add(new JLabel("Size (ml):")); panel.add(tfSize);
							panel.add(new JLabel("MRP Price (₹):")); panel.add(tfMRP); // Added MRP field
							panel.add(new JLabel("Selling Price (₹):")); panel.add(tfPrice);
							int rr = JOptionPane.showConfirmDialog(this, panel, "Edit Variant", JOptionPane.OK_CANCEL_OPTION);
							if (rr != JOptionPane.OK_OPTION) return;

							try (PreparedStatement up = c.prepareStatement("UPDATE variants SET size_ml=?, price=?, mrp_price=? WHERE barcode_id=?")) {
								up.setInt(1, Integer.parseInt(tfSize.getText().trim()));
								up.setDouble(2, Double.parseDouble(tfPrice.getText().trim()));
								up.setDouble(3, Double.parseDouble(tfMRP.getText().trim())); // Update MRP
								up.setString(4, chosenVariantBarcodeId);
								up.executeUpdate();
								JOptionPane.showMessageDialog(this, "Variant updated successfully!");
							}
						}
					}
				}
			} else if (act == 1) {
				// --- REMOVE LOGIC ---
				int conf = JOptionPane.showConfirmDialog(this, 
						"Are you sure? This will DELETE all history logs for this item too.", 
						"Confirm Delete", JOptionPane.YES_NO_OPTION);
				if (conf != JOptionPane.YES_OPTION) return;

				int vid = -1;
				try(PreparedStatement psLookup = c.prepareStatement("SELECT id FROM variants WHERE barcode_id = ?")) {
					psLookup.setString(1, chosenVariantBarcodeId);
					try(ResultSet rsLookup = psLookup.executeQuery()) {
						if (rsLookup.next()) { vid = rsLookup.getInt("id"); }
					}
				}
				if (vid == -1) { JOptionPane.showMessageDialog(this, "Internal ID not found for barcode."); return; }

				try (PreparedStatement del = c.prepareStatement("DELETE FROM bill_items WHERE variant_id = ?")) {
					del.setInt(1, vid); del.executeUpdate();
				}

				try (PreparedStatement delHist = c.prepareStatement("DELETE FROM stock_transfer_history WHERE variant_barcode_id = ?")) {
					delHist.setString(1, chosenVariantBarcodeId); delHist.executeUpdate();
				}

				try (PreparedStatement del2 = c.prepareStatement("DELETE FROM variants WHERE barcode_id = ?")) {
					del2.setString(1, chosenVariantBarcodeId); del2.executeUpdate();
				}
				JOptionPane.showMessageDialog(this, "Variant and its history removed.");
			}
		} catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
	}

	private void viewVariantsTable() {
	    try (Connection c = DBUtil.getConnection();
	            PreparedStatement ps = c.prepareStatement(
	                    "SELECT v.id AS vid, p.name AS name, v.size_ml, v.price, v.mrp_price, " +
	                    "(SELECT COALESCE(SUM(qty), 0) FROM bill_items WHERE variant_id = v.id) AS total_sold_qty " +
	                    "FROM variants v " +
	                    "JOIN products p ON v.product_id = p.id " +
	                    "ORDER BY p.name, v.size_ml");
	            ResultSet rs = ps.executeQuery()) {

	        // CLEAN COLUMNS: Only information relevant to billing and simple sales tracking
	        String[] columnHeaders = {
	                "Brand Name",      
	                "Size",            
	                "Selling Price",   
	                "MRP",             
	                "Total Sold Qty"
	        };

	        DefaultTableModel model = new DefaultTableModel(columnHeaders, 0) {
	            @Override
	            public boolean isCellEditable(int row, int column) { return false; }
	        };

	        while (rs.next()) {
	            model.addRow(new Object[]{
	                    rs.getString("name"),
	                    rs.getInt("size_ml") + " ml",
	                    "₹" + String.format("%.2f", rs.getDouble("price")),
	                    "₹" + String.format("%.2f", rs.getDouble("mrp_price")),
	                    rs.getInt("total_sold_qty")
	            });
	        }

	        JTable table = new JTable(model);
	        table.setAutoCreateRowSorter(true);
	        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
	        table.setRowSorter(sorter);

	        // UI Panel Setup
	        JPanel pnl = new JPanel(new BorderLayout());
	        
	        // Top: Search Field (Useful for finding specific brands)
	        JTextField search = new JTextField(20);
	        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
	            void filter() {
	                String q = search.getText().trim();
	                if (q.isEmpty()) { sorter.setRowFilter(null); return; }
	                try { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(q))); } catch (Exception ex) {}
	            }
	            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
	            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
	            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
	        });

	        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
	        top.add(new JLabel("Search Brand:"));
	        top.add(search);
	        pnl.add(top, BorderLayout.NORTH);

	        // Center: Scrollable Table
	        JScrollPane sp = new JScrollPane(table);
	        sp.setPreferredSize(new Dimension(800, 400));
	        pnl.add(sp, BorderLayout.CENTER);

	        JOptionPane.showMessageDialog(this, pnl, "Bar Menu List", JOptionPane.PLAIN_MESSAGE);
	        
	    } catch (Exception ex) { 
	        ex.printStackTrace(); 
	        JOptionPane.showMessageDialog(this, "Error loading table: " + ex.getMessage()); 
	    }
	}

	// --- LOGIC ADDED BACK: From old POSPanel.java ---
	private void showEditProductBarcodeDialog() {
		String productBarcode = JOptionPane.showInputDialog(
				this, 
				"Enter the **current** Barcode of the MAIN Product to modify:", 
				"Edit Product Barcode", 
				JOptionPane.QUESTION_MESSAGE
				);
		if (productBarcode == null || productBarcode.trim().isEmpty()) return; 
		String newBarcode = JOptionPane.showInputDialog(
				this, 
				"Enter the **NEW** Barcode value:", 
				"Set New Product Barcode", 
				JOptionPane.QUESTION_MESSAGE
				);
		if (newBarcode == null || newBarcode.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "New Barcode cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			boolean success = productService.updateProductBarcode(productBarcode.trim(), newBarcode.trim());
			if (success) {
				JOptionPane.showMessageDialog(this, 
						"Product Barcode updated successfully!\nOld Barcode: " + productBarcode + "\nNew Barcode: " + newBarcode, 
						"Success", 
						JOptionPane.INFORMATION_MESSAGE
						);
			} else {
				JOptionPane.showMessageDialog(this, 
						"Update failed. Barcode '" + productBarcode + "' might not exist.", 
						"Error", 
						JOptionPane.ERROR_MESSAGE
						);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void showEditVariantBarcodeDialog() {
		String variantBarcode = JOptionPane.showInputDialog(
				this, 
				"Enter the **current** Barcode of the Variant to modify:", 
				"Edit Variant Barcode", 
				JOptionPane.QUESTION_MESSAGE
				);
		if (variantBarcode == null || variantBarcode.trim().isEmpty()) return; 
		String newBarcode = JOptionPane.showInputDialog(
				this, 
				"Enter the **NEW** Barcode value:", 
				"Set New Barcode", 
				JOptionPane.QUESTION_MESSAGE
				);
		if (newBarcode == null || newBarcode.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, "New Barcode cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			boolean success = productService.updateVariantBarcode(variantBarcode.trim(), newBarcode.trim());
			if (success) {
				JOptionPane.showMessageDialog(this, "Variant Barcode updated successfully!\nOld Barcode: " + variantBarcode + "\nNew Barcode: " + newBarcode, "Success", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(this, "Update failed. Barcode '" + variantBarcode + "' might not exist.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	// --- END LOGIC ADDED BACK ---

	// ----------------
	// Reports Tab (UPGRADED: Merged old and new logic)
	// ----------------


	private JPanel buildReportsTab() {
		// Main panel uses BorderLayout
		JPanel pnl = new JPanel(new BorderLayout(15, 15));
		pnl.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// --- Panel 1: Date Range Sales Report ---
		JPanel pnlDateReport = new JPanel(new BorderLayout(15, 15));
		pnlDateReport.setBorder(BorderFactory.createTitledBorder("Date Range Sales Report"));

		// Date filters for Panel 1
		// --- FIX: Changed from FlowLayout to GridLayout to stack components ---
		JPanel topFilters = new JPanel(new GridLayout(0, 2, 8, 8)); // 2 columns, auto rows
		topFilters.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Add padding

		topFilters.add(new JLabel("From (YYYY-MM-DD):"));
		JTextField rptFrom = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
		topFilters.add(rptFrom);

		topFilters.add(new JLabel("To (YYYY-MM-DD):"));
		JTextField rptTo = new JTextField(LocalDate.now().toString(), 10);
		topFilters.add(rptTo);

		JButton btnRun = new JButton(" Run Report ");
		btnRun.setBackground(new Color(2, 92, 202)); // Blue
		btnRun.setForeground(Color.WHITE);

		JButton btnExport = new JButton(" Export CSV ");
		btnExport.setBackground(new Color(2, 202, 58)); // Green
		btnExport.setForeground(Color.WHITE);

		topFilters.add(btnRun);
		topFilters.add(btnExport);
		// --- END OF FIX ---

		pnlDateReport.add(topFilters, BorderLayout.NORTH);

		// Text area for Panel 1
		JTextArea ta = new JTextArea("Run a report to see results.");
		ta.setEditable(false);
		ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
		pnlDateReport.add(new JScrollPane(ta), BorderLayout.CENTER);


		// --- Panel 2: Today's FOOD Sales (Cleaned) ---
		JPanel foodSummaryPnl = new JPanel(new BorderLayout());
		foodSummaryPnl.setBorder(BorderFactory.createTitledBorder("Today's Food Sales"));
		JTextArea foodArea = new JTextArea();
		foodArea.setEditable(false);
		foodArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JButton btnRefreshFood = new JButton("Refresh Food");
		btnRefreshFood.addActionListener(e -> generateFoodSummary(foodArea)); // Updated call
		foodSummaryPnl.add(new JScrollPane(foodArea), BorderLayout.CENTER);
		foodSummaryPnl.add(btnRefreshFood, BorderLayout.SOUTH);
		
		// --- Panel 3: Today's DRINK Sales (Replaces Low Stock Alert) ---
		JPanel drinkSummaryPnl = new JPanel(new BorderLayout());
		drinkSummaryPnl.setBorder(BorderFactory.createTitledBorder("Today's Drink Sales (Bar)"));
		JTextArea drinkArea = new JTextArea();
		drinkArea.setEditable(false);
		drinkArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		JButton btnRefreshDrink = new JButton("Refresh Drinks");
		btnRefreshDrink.addActionListener(e -> generateDrinkSummary(drinkArea)); // New call
		drinkSummaryPnl.add(new JScrollPane(drinkArea), BorderLayout.CENTER);
		drinkSummaryPnl.add(btnRefreshDrink, BorderLayout.SOUTH);


		// --- Main Content Panel (1x3 Grid) ---
		JPanel contentPanel = new JPanel(new GridLayout(1, 3, 15, 15));
		contentPanel.add(pnlDateReport);   // Column 1
		contentPanel.add(foodSummaryPnl);  // Column 2
		contentPanel.add(drinkSummaryPnl); // Column 3 (Stock Alert is now REMOVED)

		// Add the 3-column grid to the center of the main panel
		pnl.add(contentPanel, BorderLayout.CENTER);

		// --- Action Listeners for Date Range Report ---
		btnRun.addActionListener(e -> {
			String from = rptFrom.getText().trim();
			String to = rptTo.getText().trim();
			StringBuilder sb = new StringBuilder();

			sb.append("===================================================================\n");
			sb.append(String.format("             SALES REPORT (%s → %s)\n", from, to));
			sb.append("===================================================================\n\n");

			try (Connection c = DBUtil.getConnection()) {
				// Manager-wise totals
				sb.append(">>> MANAGER-WISE TOTALS\n\n");
				sb.append(String.format("%-20s %-10s %-12s\n", "Manager", "Bills", "Sales(₹)"));
				sb.append("-------------------------------------------------------------------\n");

				PreparedStatement ps = c.prepareStatement(
						"SELECT manager_name, SUM(total) AS total_sales, COUNT(*) AS bills_count " +
						"FROM bills WHERE DATE(bill_time) BETWEEN ? AND ? GROUP BY manager_name");
				ps.setString(1, from);
				ps.setString(2, to);
				double totalAllManagers = 0;
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						sb.append(String.format("%-20s %-10d ₹%-12.2f\n",
								rs.getString("manager_name"),
								rs.getInt("bills_count"),
								rs.getDouble("total_sales")));
						totalAllManagers += rs.getDouble("total_sales");
					}
				}
				sb.append("-------------------------------------------------------------------\n");
				sb.append(String.format("%-20s %-10s ₹%-12.2f\n\n", "TOTAL", "", totalAllManagers));

				// Item Sales (Food and Bar)
				sb.append("\n>>> ITEM SALES SUMMARY (Food & Bar)\n\n");
				sb.append(String.format("%-30s %-8s %-12s\n", "Item", "Qty", "Amount(₹)"));
				sb.append("-------------------------------------------------------------------\n\n");

				String itemSQL = """
							SELECT 
								COALESCE(p.name, f.name) AS item_name,
								CASE WHEN v.size_ml IS NOT NULL THEN v.size_ml ELSE 0 END AS size_ml,
								SUM(bi.qty) AS sold_qty, 
								SUM(bi.amount) AS sold_amount
							FROM bill_items bi
							JOIN bills b ON bi.bill_id = b.id
							LEFT JOIN variants v ON bi.variant_id = v.id
							LEFT JOIN products p ON v.product_id = p.id
							LEFT JOIN food_items f ON bi.food_item_id = f.id
							WHERE DATE(b.bill_time) BETWEEN ? AND ?
							GROUP BY item_name, size_ml ORDER BY item_name, size_ml
						""";

				PreparedStatement ps2 = c.prepareStatement(itemSQL);
				ps2.setString(1, from);
				ps2.setString(2, to);

				double totalAmt = 0;
				int totalQty = 0;
				try (ResultSet rs2 = ps2.executeQuery()) {
					while (rs2.next()) {
						String name = rs2.getString("item_name");
						int size = rs2.getInt("size_ml");
						if (size > 0) name += " " + size + "ml";

						sb.append(String.format("%-30s %-8d ₹%-12.2f\n",
								name,
								rs2.getInt("sold_qty"),
								rs2.getDouble("sold_amount")));
						totalQty += rs2.getInt("sold_qty");
						totalAmt += rs2.getDouble("sold_amount");
					}
				}
				sb.append("-------------------------------------------------------------------\n");
				sb.append(String.format("%-30s %-8d ₹%-12.2f\n\n", "TOTAL", totalQty, totalAmt));

				// Gross Sales
				PreparedStatement ps3 = c.prepareStatement(
						"SELECT SUM(total) AS total_sales FROM bills WHERE DATE(bill_time) BETWEEN ? AND ?");
				ps3.setString(1, from);
				ps3.setString(2, to);
				try (ResultSet rs3 = ps3.executeQuery()) {
					if (rs3.next()) {
						sb.append(">>> GROSS SALES\n");
						sb.append("-------------------------------------------------------------------\n");
						sb.append(String.format("TOTAL SALES VALUE : ₹%.2f\n", rs3.getDouble("total_sales")));
						sb.append("===================================================================\n");
					}
				}
				ta.setText(sb.toString());
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
			}
		});

		btnExport.addActionListener(e -> {
			String from = rptFrom.getText().trim();
			String to = rptTo.getText().trim();
			JFileChooser chooser = new JFileChooser();
			chooser.setSelectedFile(new File("sales_report_" + from + "_to_" + to + ".csv"));
			int rv = chooser.showSaveDialog(this);
			if (rv != JFileChooser.APPROVE_OPTION) return;
			File out = chooser.getSelectedFile();

			String itemSQL = """
						SELECT 
							COALESCE(p.name, f.name) AS item_name,
							CASE WHEN v.size_ml IS NOT NULL THEN v.size_ml ELSE 0 END AS size_ml,
							SUM(bi.qty) AS sold_qty, 
							SUM(bi.amount) AS sold_amount
						FROM bill_items bi
						JOIN bills b ON bi.bill_id = b.id
						LEFT JOIN variants v ON bi.variant_id = v.id
						LEFT JOIN products p ON v.product_id = p.id
						LEFT JOIN food_items f ON bi.food_item_id = f.id
						WHERE DATE(b.bill_time) BETWEEN ? AND ?
						GROUP BY item_name, size_ml ORDER BY item_name, size_ml
					""";

			try (Connection c = DBUtil.getConnection(); FileWriter fw = new FileWriter(out)) {
				fw.write("Item,Size,SoldQty,Amount\n");
				PreparedStatement ps2 = c.prepareStatement(itemSQL);
				ps2.setString(1, from); ps2.setString(2, to);
				try (ResultSet rs2 = ps2.executeQuery()) {
					while (rs2.next()) {
						String name = rs2.getString("item_name").replace("\"", "'");
						int size = rs2.getInt("size_ml");
						String sizeStr = (size > 0) ? size + "ml" : "Food";

						fw.write(String.format("\"%s\",%s,%d,%.2f\n",
								name,
								sizeStr,
								rs2.getInt("sold_qty"),
								rs2.getDouble("sold_amount")));
					}
				}
				JOptionPane.showMessageDialog(this, " Exported to " + out.getAbsolutePath());
			} catch (Exception ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
		});

		// Initial load
		generateFoodSummary(foodArea);
		generateDrinkSummary(drinkArea);

		return pnl;
	}


	private void generateFoodSummary(JTextArea area) {
	    area.setText("Loading Today's Food Sales...\n");
	    try (Connection c = DBUtil.getConnection()) {
	        // Only picks items from the food_items table
	        String sql = "SELECT f.name, SUM(bi.qty) as q, SUM(bi.amount) as a " +
	                     "FROM bill_items bi JOIN food_items f ON bi.food_item_id = f.id " +
	                     "JOIN bills b ON bi.bill_id = b.id " +
	                     "WHERE date(b.bill_time) = date('now', 'localtime') " +
	                     "GROUP BY f.name";
	        
	        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	            area.setText(String.format("%-20s %3s %10s\n", "Food Item", "Qty", "Amount"));
	            area.append("------------------------------------------\n");
	            while(rs.next()) {
	                area.append(String.format("%-20s %3d  ₹%.2f\n", rs.getString("name"), rs.getInt("q"), rs.getDouble("a")));
	            }
	        }
	    } catch (Exception ex) { area.setText("Error: " + ex.getMessage()); }
	}

	private void generateDrinkSummary(JTextArea area) {
	    area.setText("Loading Today's Drink Sales...\n");
	    try (Connection c = DBUtil.getConnection()) {
	        // Only picks items from the variants/products table (Drinks)
	        String sql = "SELECT p.name, v.size_ml, SUM(bi.qty) as q, SUM(bi.amount) as a " +
	                     "FROM bill_items bi JOIN variants v ON bi.variant_id = v.id " +
	                     "JOIN products p ON v.product_id = p.id " +
	                     "JOIN bills b ON bi.bill_id = b.id " +
	                     "WHERE date(b.bill_time) = date('now', 'localtime') " +
	                     "GROUP BY p.name, v.size_ml";
	        
	        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
	            area.setText(String.format("%-18s %4s %9s\n", "Drink (Size)", "Qty", "Amount"));
	            area.append("------------------------------------------\n");
	            while(rs.next()) {
	                String item = rs.getString("name") + " " + rs.getInt("size_ml") + "ml";
	                area.append(String.format("%-18s %4d  ₹%.2f\n", item, rs.getInt("q"), rs.getDouble("a")));
	            }
	        }
	    } catch (Exception ex) { area.setText("Error: " + ex.getMessage()); }
	}

	// ----------------
	// Bill History Tab
	// ----------------

	private JPanel buildHistoryTab() {
		JPanel pnl = new JPanel(new BorderLayout());

		// 1. Define Table Model
		DefaultTableModel model = new DefaultTableModel(
				new String[]{"Bill ID","Table","Manager","Total","Time","Payment","Items Sold"}, 0) {
			@Override public boolean isCellEditable(int row, int column) { return false; }
		};

		// 2. Create Table (Must be final for the listener)
		final JTable table = new JTable(model); 

		// 3. Custom Renderer for "Items Sold" column (Wrap text)
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {

				// Special handling for Column 6 (Items Sold)
				if (column == 6) { 
					JTextArea textArea = new JTextArea(value != null ? value.toString() : "");
					textArea.setLineWrap(true);
					textArea.setWrapStyleWord(true);
					textArea.setOpaque(true);
					textArea.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
					textArea.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
					textArea.setFont(table.getFont());

					// Adjust row height to fit content (optional, keeps rows neat)
					int preferredHeight = (int) textArea.getPreferredSize().getHeight();
					// Limit max height so one huge bill doesn't break the table
					preferredHeight = Math.min(preferredHeight, 100); 
					if (table.getRowHeight(row) != preferredHeight) {
						table.setRowHeight(row, preferredHeight);
					}
					return textArea;
				}
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			}
		});

		// --- 4. THE FIX: MOUSE HOVER TOOLTIP LOGIC ---
		// This code detects when your mouse is over Column 6 and shows the full list
		table.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());

				// The "Items Sold" column is Index 6
				int itemsColumnIndex = 6;

				if (row >= 0 && col >= 0 && table.convertColumnIndexToModel(col) == itemsColumnIndex) {

					Object value = table.getValueAt(row, col); 
					String itemString = value != null ? value.toString() : "";

					if (!itemString.isEmpty()) {
						// Format using HTML to make it look like a list
						// Replaces the separator " | " with a line break "<br>"
						String tooltip = "<html><p style='width: 300px;'>" + 
								"<b>Bill Items:</b><br>" + 
								itemString.replace(" | ", "<br>") + 
								"</p></html>";
						table.setToolTipText(tooltip);
						return; 
					}
				}
				// If not on that column, turn off the tooltip
				table.setToolTipText(null);
			}
		});
		// ---------------------------------------------

		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
		table.setRowSorter(sorter);

		final JTextField tfSearch = new JTextField(20);
		JButton btnSearch = new JButton("Search Bill");
		btnSearch.addActionListener(e -> filterBillHistory(sorter, tfSearch.getText().trim()));
		JButton btnRefresh = new JButton("Load All Bills");
		btnRefresh.addActionListener(e -> loadBillHistory(model));

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
		top.add(new JLabel("Search (ID/Table/Manager/Items):"));
		top.add(tfSearch);
		top.add(btnSearch);
		top.add(btnRefresh);

		pnl.add(top, BorderLayout.NORTH);
		pnl.add(new JScrollPane(table), BorderLayout.CENTER);
		loadBillHistory(model);
		return pnl;
	}

	private void filterBillHistory(TableRowSorter<DefaultTableModel> sorter, String query) {
		if (query.isEmpty()) {
			sorter.setRowFilter(null); return;
		}
		try {
			sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(query)));
		} catch (Exception ex) {
			sorter.setRowFilter(null);
		}
	}

	private void loadBillHistory(DefaultTableModel model) {
		model.setRowCount(0);
		try {
			List<Object[]> historyData = billingService.loadBillHistoryData(); 
			for (Object[] row : historyData) {
				model.addRow(row);
			}
		} catch (Exception ex) {
			Logger.error(APP_SOURCE, "Failed to load bill history: " + ex.getMessage());
			JOptionPane.showMessageDialog(null, "Error loading bill history: " + ex.getMessage());
		}
	}

	// --- BACKUP FEATURE: Copies AppData -> Desktop ---
	private void performBackup() {
		try {
			// 1. Source: The AppData Folder
			String appData = System.getenv("APPDATA");
			java.nio.file.Path sourceDir = java.nio.file.Paths.get(appData, "BillPAY");

			if (!java.nio.file.Files.exists(sourceDir)) {
				JOptionPane.showMessageDialog(this, "No data found to backup yet!", "Warning", JOptionPane.WARNING_MESSAGE);
				return;
			}

			// 2. Destination: Desktop + Timestamp Folder
			String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
			String desktop = System.getProperty("user.home") + File.separator + "Desktop";
			java.nio.file.Path backupDir = java.nio.file.Paths.get(desktop, "BillPAY_Backup_" + timestamp);

			// 3. Perform Copy
			copyDirectory(sourceDir, backupDir);

			// 4. Success Message
			JOptionPane.showMessageDialog(this, 
					"Backup Successful!\nSaved to Desktop: " + backupDir.getFileName(), 
					"Backup Complete", 
					JOptionPane.INFORMATION_MESSAGE);

			// Optional: Open the folder for them
			java.awt.Desktop.getDesktop().open(backupDir.toFile());

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Backup Failed: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	// --- HELPER: Recursively copy folder ---
	private void copyDirectory(java.nio.file.Path source, java.nio.file.Path target) throws java.io.IOException {
		java.nio.file.Files.walkFileTree(source, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
			@Override
			public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
				java.nio.file.Path targetDir = target.resolve(source.relativize(dir));
				if (!java.nio.file.Files.exists(targetDir)) {
					java.nio.file.Files.createDirectories(targetDir);
				}
				return java.nio.file.FileVisitResult.CONTINUE;
			}

			@Override
			public java.nio.file.FileVisitResult visitFile(java.nio.file.Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws java.io.IOException {
				java.nio.file.Files.copy(file, target.resolve(source.relativize(file)), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				return java.nio.file.FileVisitResult.CONTINUE;
			}
		});
	}
}