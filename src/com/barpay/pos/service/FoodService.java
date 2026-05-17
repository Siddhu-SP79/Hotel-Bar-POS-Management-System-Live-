package com.barpay.pos.service;

import com.barpay.pos.model.FoodItem;
import com.barpay.pos.util.DBUtil;
import com.barpay.pos.util.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FoodService {
	private static final String SOURCE = "FoodService";

	/**
	 * Loads all available food items, optionally filtered by a search keyword.
	 * The keyword will search against the item name AND category.
	 */
	public List<FoodItem> loadFoodItems(String keyword) throws Exception {
		List<FoodItem> items = new ArrayList<>();

		// Base SQL: Only load active items
		StringBuilder sql = new StringBuilder("SELECT id, name, category, price, image_filename FROM food_items WHERE is_available = 1");

		// Check if a keyword was provided (Logic matches POSPanel search)
		boolean useFilter = (keyword != null && !keyword.isEmpty() && !keyword.equals("All"));

		if (useFilter) {
			// Search by name OR category
			sql.append(" AND (name LIKE ? OR category LIKE ?)");
		}
		sql.append(" ORDER BY category, name");

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql.toString())) {

			if (useFilter) {
				String searchPattern = "%" + keyword + "%";
				ps.setString(1, searchPattern); // For name
				ps.setString(2, searchPattern); // For category
			}

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					items.add(new FoodItem(
							rs.getInt("id"),
							rs.getString("name"),
							rs.getString("category"),
							rs.getDouble("price"),
							rs.getString("image_filename")
							));
				}
			}
			return items;
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to load food items: " + ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Gets a distinct list of all food categories.
	 * Useful for the Owner Panel or Filter Dropdowns.
	 */
	public List<String> getAllFoodCategories() throws Exception {
		List<String> categories = new ArrayList<>();
		categories.add("All"); // Add default
		String sql = "SELECT DISTINCT category FROM food_items WHERE is_available = 1 ORDER BY category";

		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql);
				ResultSet rs = ps.executeQuery()) {

			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
			return categories;
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to load food categories: " + ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Adds a new food item to the database.
	 */
	public void addFoodItem(String name, String category, double price, String imageFilename) throws Exception {
		String sql = "INSERT INTO food_items (name, category, price, image_filename, is_available) VALUES (?, ?, ?, ?, 1)";
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, category);
			ps.setDouble(3, price);
			ps.setString(4, imageFilename);
			ps.executeUpdate();
			Logger.info(SOURCE, "Added new food item: " + name);
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to add food item: " + ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Updates an existing food item.
	 */
	public void updateFoodItem(int id, String name, String category, double price, String imageFilename) throws Exception {
		String sql = "UPDATE food_items SET name = ?, category = ?, price = ?, image_filename = ? WHERE id = ?";
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setString(2, category);
			ps.setDouble(3, price);
			ps.setString(4, imageFilename);
			ps.setInt(5, id);
			ps.executeUpdate();
			Logger.info(SOURCE, "Updated food item ID: " + id);
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to update food item: " + ex.getMessage());
			throw ex;
		}
	}

	/**
	 * Soft Deletes a food item (Industry Standard).
	 * Sets is_available = 0 so old bills don't break.
	 */
	public void deleteFoodItem(int id) throws Exception {
		String sql = "UPDATE food_items SET is_available = 0 WHERE id = ?";
		try (Connection c = DBUtil.getConnection();
				PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setInt(1, id);
			ps.executeUpdate();
			Logger.warn(SOURCE, "Deactivated food item ID: " + id);
		} catch (Exception ex) {
			Logger.error(SOURCE, "Failed to deactivate food item: " + ex.getMessage());
			throw ex;
		}
	}
}