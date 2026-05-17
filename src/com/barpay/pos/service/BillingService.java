package com.barpay.pos.service;

import com.barpay.pos.model.CartItem;
import com.barpay.pos.model.OpenOrder;
import com.barpay.pos.model.OpenOrderItem;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;

import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BillingService implements Printable {
	private static final String SOURCE = "BillingService";
	@Override
	public int print(java.awt.Graphics graphics, java.awt.print.PageFormat pageFormat, int pageIndex) throws PrinterException {
	    if (pageIndex > 0) {
	        return NO_SUCH_PAGE;
	    }

	    java.awt.Graphics2D g2d = (java.awt.Graphics2D) graphics;
	    g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

	    // This is where you write what the printer should draw
	    g2d.drawString("FINAL BILL", 100, 100); 

	    return PAGE_EXISTS;
	}

	/**
	 * Finds an open order for a given table number.
	 */
	
	public OpenOrder getOpenOrder(int tableNo) throws Exception {
		String sql = "SELECT o.id, o.table_no, o.manager_name, o.start_time, o.status, SUM(i.qty * i.price_per_unit) AS total " +
				"FROM open_orders o " +
				"LEFT JOIN open_order_items i ON o.id = i.order_id " +
				"WHERE o.table_no = ? " +
				"GROUP BY o.id, o.table_no, o.manager_name, o.start_time, o.status";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, tableNo);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					double total = rs.getDouble("total");
					return new OpenOrder(
							rs.getInt("id"),
							rs.getInt("table_no"),
							rs.getString("manager_name"),
							rs.getString("start_time"),
							rs.getString("status"),
							rs.wasNull() ? 0.0 : total
							);
				}
			}
		} catch (Exception ex) {
			Logger.error(SOURCE, "Error getting open order for table " + tableNo + ": " + ex.getMessage());
			throw ex;
		}
		return null;
	}

	/**
	 * Gets all items for a given open order ID.
	 * Handles both Food and Bar items.
	 */
	public List<OpenOrderItem> getOrderItems(int orderId) throws Exception {
		List<OpenOrderItem> items = new ArrayList<>();
		// COALESCE combines data from products (Bar) and food_items (Food)
		String sql = "" +
				"SELECT oi.id, oi.order_id, oi.variant_id, oi.food_item_id, oi.qty, oi.price_per_unit, " +
				"       o.manager_name, o.table_no, oi.is_kot_printed, " +
				"       COALESCE(p.name, f.name) AS item_name, " +
				"       COALESCE(v.size_ml, 0) AS size_ml " +
				"FROM open_order_items oi " +
				"JOIN open_orders o ON oi.order_id = o.id " +
				"LEFT JOIN variants v ON oi.variant_id = v.id " +
				"LEFT JOIN products p ON v.product_id = p.id " +
				"LEFT JOIN food_items f ON oi.food_item_id = f.id " +
				"WHERE oi.order_id = ?";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setInt(1, orderId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					items.add(new OpenOrderItem(
							rs.getInt("id"),
							rs.getInt("order_id"),
							rs.getInt("variant_id"),
							rs.getInt("food_item_id"),
							rs.getString("item_name"),
							rs.getInt("size_ml"),
							rs.getDouble("price_per_unit"),
							rs.getInt("qty"),
							rs.getString("manager_name"),
							rs.getInt("table_no"),
							rs.getBoolean("is_kot_printed")
							));
				}
			}
			return items;
		} catch (Exception ex) {
			Logger.error(SOURCE, "Error loading order items for order ID " + orderId + ": " + ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Creates a new open order or updates an existing one.
	 * Updates Inventory ONLY for "BAR" items.
	 */
	public int saveOrder(List<CartItem> cart, int tableNo, String manager) throws Exception {
		if (cart.isEmpty()) return -1;

		int orderId = -1;

		try (Connection c = DBUtil.getConnection()) {
			c.setAutoCommit(false);

			// 1. Check/Create Order Header
			OpenOrder existingOrder = getOpenOrder(tableNo);

			if (existingOrder == null) {
				String sql = "INSERT INTO open_orders (table_no, manager_name) VALUES (?, ?)";
				try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
					ps.setInt(1, tableNo);
					ps.setString(2, manager);
					ps.executeUpdate();
					ResultSet keys = ps.getGeneratedKeys();
					if (keys.next()) orderId = keys.getInt(1);
				}
			} else {
				orderId = existingOrder.getId();
			}

			// 2. Process Items
			String updateExistingItemSql = "UPDATE open_order_items SET qty = qty + ?, amount = amount + ? WHERE order_id = ? AND ";
			String insertNewItemSql = "INSERT INTO open_order_items (order_id, variant_id, food_item_id, qty, price_per_unit, amount, is_kot_printed) VALUES (?, ?, ?, ?, ?, ?, 0)";
			String updateQtySql = "UPDATE variants SET counter_qty = counter_qty - ? WHERE id = ?";

			try (PreparedStatement psUpdateQty = c.prepareStatement(updateQtySql)) {

				for (CartItem item : cart) {
					int updatedRows = 0;
					double itemAmount = item.qty * item.price;

					String specificUpdateSql;
					if (item.itemType.equals("BAR")) {
						specificUpdateSql = updateExistingItemSql + "variant_id = ?";
					} else {
						specificUpdateSql = updateExistingItemSql + "food_item_id = ?";
					}

					// 2a. Update existing line item
					try (PreparedStatement psUpdateExisting = c.prepareStatement(specificUpdateSql)) {
						psUpdateExisting.setInt(1, item.qty);
						psUpdateExisting.setDouble(2, itemAmount);
						psUpdateExisting.setInt(3, orderId);
						if (item.itemType.equals("BAR")) {
							psUpdateExisting.setInt(4, item.variantId);
						} else {
							psUpdateExisting.setInt(4, item.foodItemId);
						}
						updatedRows = psUpdateExisting.executeUpdate();
					}

					// 2b. Insert new line item if not updated
					if (updatedRows == 0) {
						try (PreparedStatement psInsertNew = c.prepareStatement(insertNewItemSql)) {
							psInsertNew.setInt(1, orderId);
							if (item.itemType.equals("BAR")) {
								psInsertNew.setInt(2, item.variantId);
								psInsertNew.setNull(3, java.sql.Types.INTEGER);
							} else {
								psInsertNew.setNull(2, java.sql.Types.INTEGER);
								psInsertNew.setInt(3, item.foodItemId);
							}
							psInsertNew.setInt(4, item.qty);
							psInsertNew.setDouble(5, item.price);
							psInsertNew.setDouble(6, itemAmount);
							psInsertNew.executeUpdate();
						}
					}

					// 2c. Deduct Stock (BAR ONLY)
					if (item.itemType.equals("BAR")) {
						psUpdateQty.setInt(1, item.qty);
						psUpdateQty.setInt(2, item.variantId);
						psUpdateQty.executeUpdate();
					}
				}
			}

			c.commit();
			return orderId;

		} catch (Exception ex) {
			try (Connection c = DBUtil.getConnection()) { c.rollback(); } catch (Exception e) {}
			throw ex;
		}
	}

	public List<OpenOrder> getAllOpenOrders() throws Exception {
		List<OpenOrder> orders = new ArrayList<>();
		String sql = "SELECT o.id, o.table_no, o.manager_name, o.start_time, o.status, SUM(i.qty * i.price_per_unit) AS total " +
				"FROM open_orders o " +
				"LEFT JOIN open_order_items i ON o.id = i.order_id " +
				"GROUP BY o.id, o.table_no, o.manager_name, o.start_time, o.status " +
				"ORDER BY o.table_no";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				double total = rs.getDouble("total");
				orders.add(new OpenOrder(
						rs.getInt("id"),
						rs.getInt("table_no"),
						rs.getString("manager_name"),
						rs.getString("start_time"),
						rs.getString("status"),
						rs.wasNull() ? 0.0 : total
						));
			}
			return orders;
		} catch (Exception ex) { throw ex; }
	}

	public List<OpenOrderItem> getOpenOrderItems(int orderId) throws Exception {
		return getOrderItems(orderId);
	}

	public void reduceOpenOrderItemQuantity(OpenOrderItem itemToRemove, int quantityToRemove, int remainingQty) throws Exception {
		String updateItemSql;
		int itemId = itemToRemove.getId();

		if (remainingQty > 0) updateItemSql = "UPDATE open_order_items SET qty = ? WHERE id = ?";
		else updateItemSql = "DELETE FROM open_order_items WHERE id = ?";

		String updateStockSql = "UPDATE variants SET counter_qty = counter_qty + ? WHERE id = ?";

		try (Connection c = DBUtil.getConnection()) {
			c.setAutoCommit(false); 

			try (PreparedStatement psItem = c.prepareStatement(updateItemSql)) {
				if (remainingQty > 0) {
					psItem.setInt(1, remainingQty);
					psItem.setInt(2, itemId);
				} else {
					psItem.setInt(1, itemId);
				}
				psItem.executeUpdate();
			}

			// Return Stock (BAR ONLY)
			if (itemToRemove.itemType.equals("BAR")) {
				try (PreparedStatement psStock = c.prepareStatement(updateStockSql)) {
					psStock.setInt(1, quantityToRemove);
					psStock.setInt(2, itemToRemove.getVariantId());
					psStock.executeUpdate();
				}
			}
			c.commit(); 

		} catch (Exception ex) {
			try (Connection c = DBUtil.getConnection()) { c.rollback(); } catch (Exception e) {}
			throw ex;
		}
	}

	public void markOrderItemsAsPrinted(List<OpenOrderItem> items) throws Exception {
		if (items == null || items.isEmpty()) return;
		String sql = "UPDATE open_order_items SET is_kot_printed = 1 WHERE id = ?";

		try (Connection c = DBUtil.getConnection()) {
			c.setAutoCommit(false); 
			try (PreparedStatement ps = c.prepareStatement(sql)) {
				for (OpenOrderItem item : items) {
					ps.setInt(1, item.getId()); 
					ps.addBatch();
				}
				ps.executeBatch();
				c.commit(); 
			}
		} catch (Exception ex) {
			try (Connection c = DBUtil.getConnection()) { c.rollback(); } catch (Exception e) {}
			throw ex;
		}
	}

	// --- COMPLETED IMPLEMENTATION FOR OWNER PANEL ---
	public List<Object[]> loadBillHistoryData() throws Exception {
		List<Object[]> dataList = new ArrayList<>();

		// SQLITE UPDATE:
		// 1. GROUP_CONCAT(str, ' | ') instead of GROUP_CONCAT(str SEPARATOR ' | ')
		// 2. '||' for concatenation instead of CONCAT()
		// 3. CASE WHEN... for IF logic
		String sql = "SELECT b.id, b.table_no, b.manager_name, b.total, b.bill_time, b.payment_mode, " +
				"GROUP_CONCAT( " +
				"   COALESCE(p.name, f.name) || " +
				"   CASE WHEN v.size_ml IS NOT NULL THEN ' ' || v.size_ml || 'ml' ELSE '' END || " +
				"   ' x' || bi.qty, " +
				"   ' | ' " +
				") AS items_sold " +
				"FROM bills b " +
				"LEFT JOIN bill_items bi ON b.id = bi.bill_id " +
				"LEFT JOIN variants v ON bi.variant_id = v.id " +
				"LEFT JOIN products p ON v.product_id = p.id " +
				"LEFT JOIN food_items f ON bi.food_item_id = f.id " +
				"GROUP BY b.id " +
				"ORDER BY b.bill_time DESC " +
				"LIMIT 500";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				// Format the timestamp nicely (Remove the 'T')
				String timeStr = rs.getString("bill_time");
				if(timeStr != null && timeStr.length() >= 16) {
					timeStr = timeStr.substring(0, 16).replace("T", " ");
				}

				dataList.add(new Object[]{
						rs.getInt("id"),
						rs.getInt("table_no"),
						rs.getString("manager_name"),
						rs.getDouble("total"),
						timeStr,
						rs.getString("payment_mode"),
						rs.getString("items_sold") // This feeds the Tooltip in OwnerPanel
				});
			}
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to load bill history: " + ex.getMessage());
			throw ex;
		}
		return dataList;
	}
	
	/**
	 * Fetches only Drink Sales for today. 
	 * Replaces the old 'Low Stock' data logic in the Owner Panel.
	 */
	public List<Object[]> getTodayDrinkSales() throws Exception {
	    List<Object[]> data = new ArrayList<>();
	    String sql = """
	            SELECT p.name, v.size_ml, SUM(bi.qty) as total_qty, SUM(bi.amount) as total_amount
	            FROM bill_items bi
	            JOIN bills b ON bi.bill_id = b.id
	            JOIN variants v ON bi.variant_id = v.id
	            JOIN products p ON v.product_id = p.id
	            WHERE date(b.bill_time) = date('now', 'localtime')
	            GROUP BY p.name, v.size_ml
	            ORDER BY p.name ASC, v.size_ml ASC
	            """;

	    try (Connection c = DBUtil.getConnection();
	         PreparedStatement ps = c.prepareStatement(sql);
	         ResultSet rs = ps.executeQuery()) {

	        while (rs.next()) {
	            data.add(new Object[]{
	                rs.getString("name"),
	                rs.getInt("size_ml") + "ml",
	                rs.getInt("total_qty"),
	                rs.getDouble("total_amount")
	            });
	        }
	    } catch (Exception ex) {
	        Logger.error(SOURCE, "Error fetching drink sales: " + ex.getMessage());
	        throw ex;
	    }
	    return data;
	}
	
	public void processFinalBill() {
		PrinterJob job = PrinterJob.getPrinterJob();
	    job.setPrintable(this); 
	    try {
	        job.print(); // NO POPUP - Instant print
	    } catch (PrinterException e) {
	        e.printStackTrace();
	    }
	}
}