package com.barpay.pos.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import javax.swing.JOptionPane;

public class DBUtil {

    // --- 1. INDUSTRY STANDARD: Use AppData for User Data ---
    // Returns: C:\Users\YourName\AppData\Roaming\BillPAY
    public static String getAppDataFolder() {
        String appData = System.getenv("APPDATA");
        File folder = new File(appData, "BillPAY");
        if (!folder.exists()) {
            folder.mkdirs(); // Automatically create folder if missing
        }
        return folder.getAbsolutePath();
    }

    // --- 2. CONNECTION: Connects to 'pos_db.sqlite' in AppData ---
    public static Connection getConnection() throws Exception {
        // Build path: C:\Users\Name\AppData\Roaming\BillPAY\pos_db.sqlite
        String dbPath = getAppDataFolder() + File.separator + "pos_db.sqlite";
        String url = "jdbc:sqlite:" + dbPath;
        
        // Load the SQLite Driver
        Class.forName("org.sqlite.JDBC"); 
        
        Connection c = DriverManager.getConnection(url);
        
        // SQLite specific: Enforce Foreign Key constraints
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON;");
        }
        return c;
    }

 // --- 3. AUTO-SETUP: Cleaned version without inventory tracking ---
    public static void initializeDatabase() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            
            // 1. Products Table (Added Category for Sub-Menus)
            s.execute("CREATE TABLE IF NOT EXISTS products (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "name TEXT, " +
                      "category TEXT, " + // Used for Whisky, Beer, etc.
                      "rack_id TEXT, " + 
                      "image_filename TEXT, " + 
                      "barcode_id TEXT UNIQUE)");

            // 2. Variants Table (CLEANED: Removed counter_qty, store_qty, and min_stock)
            s.execute("CREATE TABLE IF NOT EXISTS variants (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "product_id INTEGER, " +
                      "size_ml INTEGER, " +
                      "price REAL, " +
                      "mrp_price REAL DEFAULT 0, " + 
                      "barcode_id TEXT UNIQUE, " + 
                      "FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE)");

            // 3. Food Items Table
            s.execute("CREATE TABLE IF NOT EXISTS food_items (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "name TEXT, " +
                      "category TEXT, " + 
                      "price REAL, " + 
                      "image_filename TEXT, " + 
                      "is_available INTEGER DEFAULT 1)");

            // 4. Bills (Order History)
            s.execute("CREATE TABLE IF NOT EXISTS bills (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "manager_name TEXT, " +
                      "table_no INTEGER, " +
                      "payment_mode TEXT, " +
                      "total REAL, " +
                      "cgst REAL, " +
                      "sgst REAL, " +
                      "bill_time TIMESTAMP DEFAULT (datetime('now','localtime')))");

            // 5. Bill Items
            s.execute("CREATE TABLE IF NOT EXISTS bill_items (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "bill_id INTEGER, " +
                      "variant_id INTEGER, " +
                      "food_item_id INTEGER, " +
                      "qty INTEGER, " +
                      "price_per_unit REAL, " +
                      "amount REAL, " +
                      "FOREIGN KEY(bill_id) REFERENCES bills(id) ON DELETE CASCADE)");
            
            // 6. Open Orders (Active Tables)
            s.execute("CREATE TABLE IF NOT EXISTS open_orders (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "table_no INTEGER, " +
                      "manager_name TEXT, " +
                      "status TEXT DEFAULT 'OPEN', " +
                      "total_amount REAL DEFAULT 0, " +
                      "start_time TIMESTAMP DEFAULT (datetime('now','localtime')))");

            // 7. Open Order Items
            s.execute("CREATE TABLE IF NOT EXISTS open_order_items (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                      "order_id INTEGER, " +
                      "variant_id INTEGER, " +
                      "food_item_id INTEGER, " +
                      "qty INTEGER, " +
                      "price_per_unit REAL, " +
                      "amount REAL, " +
                      "is_kot_printed INTEGER DEFAULT 0, " +
                      "FOREIGN KEY(order_id) REFERENCES open_orders(id) ON DELETE CASCADE)");

            // NOTE: We removed the Stock History table entirely since you don't use it.

            Logger.info("DBUtil", "Clean Database initialized successfully.");
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database Init Error: " + e.getMessage());
        }
    }
}