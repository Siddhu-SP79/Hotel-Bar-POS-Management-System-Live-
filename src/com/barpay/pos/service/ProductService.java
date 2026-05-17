package com.barpay.pos.service;

import com.barpay.pos.model.Product;
import com.barpay.pos.model.Variant;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductService {
	private static final String SOURCE = "ProductService";

	// --- 1. CORE PRODUCT LOADING ---
	public List<Product> loadProducts(String keyword) throws Exception {
		List<Product> products = new ArrayList<>();
		String sql = "SELECT id,name,rack_id,image_filename,barcode_id FROM products";

		boolean useWhere = keyword != null && !keyword.isEmpty();
		if (useWhere) sql += " WHERE name LIKE ? OR rack_id = ? OR barcode_id = ?";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			if (useWhere) {
				ps.setString(1, "%" + keyword + "%");
				ps.setString(2, keyword);
				ps.setString(3, keyword);
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					products.add(new Product(
							rs.getInt("id"),
							rs.getString("name"),
							rs.getString("rack_id"),
							rs.getString("image_filename"),
							rs.getString("barcode_id")
							));
				}
			}
			return products;
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to load products: " + ex.getMessage());
			throw ex;
		}
	}

	// --- 2. VARIANT LOADING ---
	public List<Variant> getVariantsByProductBarcode(String barcodeId) throws Exception {
		List<Variant> variants = new ArrayList<>();
		String sql = "SELECT v.id, v.product_id, v.size_ml, v.price, v.counter_qty, v.store_qty, v.barcode_id " +
				"FROM variants v JOIN products p ON v.product_id = p.id " +
				"WHERE p.barcode_id = ?";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, barcodeId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					variants.add(new Variant(
							rs.getInt("id"),
							rs.getInt("product_id"),
							rs.getInt("size_ml"),
							rs.getDouble("price"),
							rs.getInt("counter_qty"),
							rs.getInt("store_qty"),
							rs.getString("barcode_id")
							));
				}
			}
			return variants;
		} catch (Exception ex) { throw ex; }
	}

	public Product findProductByBarcode(String barcodeId) throws Exception {
		String sql = "SELECT id, name, rack_id, image_filename, barcode_id FROM products WHERE barcode_id = ?";
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, barcodeId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new Product(
							rs.getInt("id"),
							rs.getString("name"),
							rs.getString("rack_id"),
							rs.getString("image_filename"),
							rs.getString("barcode_id")
							);
				}
			}
			return null;
		} catch (Exception ex) { throw ex; }
	}

	// --- 3. STOCK TRANSFER LOGIC ---
	public boolean transferStock(String variantBarcodeId, int quantity, String direction) throws Exception {
		if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero.");

		String sourceQtyCol, targetQtyCol, sourceLocation, targetLocation;

		if (direction.equalsIgnoreCase("STORE_TO_COUNTER")) {
			sourceQtyCol = "store_qty"; targetQtyCol = "counter_qty";
			sourceLocation = "Store"; targetLocation = "Counter";
		} else if (direction.equalsIgnoreCase("COUNTER_TO_STORE")) {
			sourceQtyCol = "counter_qty"; targetQtyCol = "store_qty";
			sourceLocation = "Counter"; targetLocation = "Store";
		} else {
			throw new IllegalArgumentException("Invalid direction");
		}

		int prevQtyStore = 0, prevQtyCounter = 0;
		try (Connection c = DBUtil.getConnection();
				PreparedStatement psSelect = c.prepareStatement("SELECT store_qty, counter_qty FROM variants WHERE barcode_id = ?")) {
			psSelect.setString(1, variantBarcodeId);
			try (ResultSet rs = psSelect.executeQuery()) {
				if (rs.next()) {
					prevQtyStore = rs.getInt("store_qty");
					prevQtyCounter = rs.getInt("counter_qty");
				} else return false;
			}
		}

		String updateSql = "UPDATE variants SET " + targetQtyCol + " = " + targetQtyCol + " + ?, " +
				sourceQtyCol + " = " + sourceQtyCol + " - ? WHERE barcode_id = ? AND " + sourceQtyCol + " >= ?";

		try (Connection c = DBUtil.getConnection()) {
			c.setAutoCommit(false);
			try (PreparedStatement psUpdate = c.prepareStatement(updateSql)) {
				psUpdate.setInt(1, quantity);
				psUpdate.setInt(2, quantity);
				psUpdate.setString(3, variantBarcodeId);
				psUpdate.setInt(4, quantity);

				if (psUpdate.executeUpdate() == 0) {
					c.rollback();
					throw new Exception("Insufficient stock in " + sourceLocation);
				}
			}

			int newQtyStore = (sourceLocation.equals("Store")) ? (prevQtyStore - quantity) : (prevQtyStore + quantity);
			int newQtyCounter = (sourceLocation.equals("Counter")) ? (prevQtyCounter - quantity) : (prevQtyCounter + quantity);

			// SQLite Change: Use datetime('now', 'localtime')
			String logSql = "INSERT INTO stock_transfer_history (transfer_date, variant_barcode_id, transfer_qty, source_location, target_location, prev_counter_qty, prev_store_qty, new_counter_qty, new_store_qty) VALUES (datetime('now', 'localtime'), ?, ?, ?, ?, ?, ?, ?, ?)";

			try (PreparedStatement psLog = c.prepareStatement(logSql)) {
				psLog.setString(1, variantBarcodeId);
				psLog.setInt(2, quantity);
				psLog.setString(3, sourceLocation);
				psLog.setString(4, targetLocation);
				psLog.setInt(5, prevQtyCounter);
				psLog.setInt(6, prevQtyStore);
				psLog.setInt(7, newQtyCounter);
				psLog.setInt(8, newQtyStore);
				psLog.executeUpdate();
			}
			c.commit();
			return true;
		} catch (Exception e) { throw e; }
	}

	// --- 4. STOCK HISTORY REPORTING ---
	public List<Object[]> loadStockTransferHistory(String nameKeyword, String barcodeId, String dateString) throws Exception {
		List<Object[]> historyData = new ArrayList<>();

		StringBuilder sqlBuilder = new StringBuilder();
		sqlBuilder.append("SELECT h.transfer_date, p.name, h.variant_barcode_id, v.size_ml, h.transfer_qty, ");
		sqlBuilder.append("h.source_location, h.target_location, h.prev_counter_qty, h.prev_store_qty, ");
		sqlBuilder.append("h.new_counter_qty, h.new_store_qty, v.mrp_price, ");
		sqlBuilder.append("(SELECT COALESCE(SUM(qty), 0) FROM bill_items WHERE variant_id = v.id) AS total_sold_qty ");
		sqlBuilder.append("FROM stock_transfer_history h ");
		sqlBuilder.append("LEFT JOIN variants v ON h.variant_barcode_id = v.barcode_id ");
		sqlBuilder.append("LEFT JOIN products p ON v.product_id = p.id ");
		sqlBuilder.append("WHERE 1=1");

		List<Object> params = new ArrayList<>();

		if (nameKeyword != null && !nameKeyword.trim().isEmpty()) {
			sqlBuilder.append(" AND p.name LIKE ?");
			params.add("%" + nameKeyword.trim() + "%");
		}
		if (barcodeId != null && !barcodeId.trim().isEmpty()) {
			sqlBuilder.append(" AND h.variant_barcode_id LIKE ?");
			params.add("%" + barcodeId.trim() + "%");
		}
		if (dateString != null && !dateString.trim().isEmpty()) {
			sqlBuilder.append(" AND h.transfer_date LIKE ?");
			params.add(dateString.trim() + "%");
		}
		sqlBuilder.append(" ORDER BY h.transfer_date DESC LIMIT 500");

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sqlBuilder.toString())) {
			for (int i = 0; i < params.size(); i++) ps.setString(i + 1, (String) params.get(i));

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int soldQty = rs.getInt("total_sold_qty");
					double mrp = rs.getDouble("mrp_price");

					historyData.add(new Object[]{
							rs.getString("transfer_date"),
							rs.getString("name") + " (" + rs.getString("size_ml") + "ml)",
							rs.getString("variant_barcode_id"),
							rs.getInt("transfer_qty"),
							rs.getString("source_location") + " -> " + rs.getString("target_location"),
							rs.getInt("prev_counter_qty"),
							rs.getInt("new_counter_qty"),
							rs.getInt("prev_store_qty"),
							rs.getInt("new_store_qty"),
							soldQty,
							String.format("%.2f", (soldQty * mrp))
					});
				}
			}
		}
		return historyData;
	}

	// --- 5. STOCK IN (SUPPLIER) ---
	public boolean insertStockIntoStore(String variantBarcodeId, int quantity) throws Exception {
		if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive.");

		int prevQtyStore = 0, prevQtyCounter = 0;
		try (Connection c = DBUtil.getConnection();
				PreparedStatement psSelect = c.prepareStatement("SELECT store_qty, counter_qty FROM variants WHERE barcode_id = ?")) {
			psSelect.setString(1, variantBarcodeId);
			try (ResultSet rs = psSelect.executeQuery()) {
				if (rs.next()) {
					prevQtyStore = rs.getInt("store_qty");
					prevQtyCounter = rs.getInt("counter_qty");
				} else return false;
			}
		}

		int newQtyStore = prevQtyStore + quantity;

		try (Connection c = DBUtil.getConnection()) {
			c.setAutoCommit(false);

			try (PreparedStatement psUpdate = c.prepareStatement("UPDATE variants SET store_qty = ? WHERE barcode_id = ?")) {
				psUpdate.setInt(1, newQtyStore);
				psUpdate.setString(2, variantBarcodeId);
				if (psUpdate.executeUpdate() == 0) {
					c.rollback(); return false;
				}
			}

			// SQLite Change: Use datetime('now', 'localtime')
			String log = "INSERT INTO stock_transfer_history (transfer_date, variant_barcode_id, transfer_qty, source_location, target_location, prev_counter_qty, prev_store_qty, new_counter_qty, new_store_qty) VALUES (datetime('now', 'localtime'), ?, ?, 'Supplier/In', 'Store', ?, ?, ?, ?)";
			try (PreparedStatement psHistory = c.prepareStatement(log)) {
				psHistory.setString(1, variantBarcodeId);
				psHistory.setInt(2, quantity);
				psHistory.setInt(3, prevQtyCounter);
				psHistory.setInt(4, prevQtyStore);
				psHistory.setInt(5, prevQtyCounter);
				psHistory.setInt(6, newQtyStore);
				psHistory.executeUpdate();
			}
			c.commit();
			return true;
		} catch (Exception e) { throw e; }
	}

	// --- 6. COMPLETED IMPLEMENTATION OF BARCODE UPDATES ---

	public boolean updateProductBarcode(String oldBarcode, String newBarcode) throws Exception {
		String sql = "UPDATE products SET barcode_id = ? WHERE barcode_id = ?";
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, newBarcode);
			ps.setString(2, oldBarcode);
			int rows = ps.executeUpdate();
			if(rows > 0) {
				Logger.info(SOURCE, "Product Barcode updated: " + oldBarcode + " -> " + newBarcode);
				return true;
			}
			return false;
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to update product barcode: " + ex.getMessage());
			throw ex;
		}
	}

	public boolean updateVariantBarcode(String oldBarcode, String newBarcode) throws Exception {
		// We must update the Variants table AND the History table to keep links alive
		Connection c = null;
		try {
			c = DBUtil.getConnection();
			c.setAutoCommit(false);

			// 1. Update Variant
			String sqlVar = "UPDATE variants SET barcode_id = ? WHERE barcode_id = ?";
			try(PreparedStatement ps = c.prepareStatement(sqlVar)) {
				ps.setString(1, newBarcode);
				ps.setString(2, oldBarcode);
				int rows = ps.executeUpdate();
				if(rows == 0) {
					c.rollback(); return false;
				}
			}

			// 2. Update History Logs (so reports don't lose data)
			String sqlHist = "UPDATE stock_transfer_history SET variant_barcode_id = ? WHERE variant_barcode_id = ?";
			try(PreparedStatement ps = c.prepareStatement(sqlHist)) {
				ps.setString(1, newBarcode);
				ps.setString(2, oldBarcode);
				ps.executeUpdate();
			}

			c.commit();
			Logger.info(SOURCE, "Variant Barcode updated: " + oldBarcode + " -> " + newBarcode);
			return true;

		} catch (Exception ex) {
			if(c != null) c.rollback();
			Logger.error(SOURCE, "Failed to update variant barcode: " + ex.getMessage());
			throw ex;
		} finally {
			if(c != null) c.close();
		}
	}
}