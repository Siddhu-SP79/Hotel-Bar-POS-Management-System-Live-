package com.barpay.pos.model;

public class CartItem {
	// Item identifiers
	public int variantId;
	public int foodItemId;
	public String itemType; // "BAR" or "FOOD"

	// Common details
	public String productName;
	public int sizeMl; // 0 for food items
	public double price;
	public int qty;

	// Order context
	public String manager;
	public int tableNo;
	public String payment;

	// Constructor for BAR items (Variants)
	public CartItem(int variantId, String productName, int sizeMl, double price, int qty, String manager, int tableNo, String payment) {
		this.itemType = "BAR";
		this.variantId = variantId;
		this.foodItemId = 0; // 0 indicates not a food item
		this.productName = productName;
		this.sizeMl = sizeMl;
		this.price = price;
		this.qty = qty;
		this.manager = manager;
		this.tableNo = tableNo;
		this.payment = payment;
	}

	// Constructor for FOOD items
	public CartItem(int foodItemId, String productName, double price, int qty, String manager, int tableNo, String payment) {
		this.itemType = "FOOD";
		this.variantId = 0; // 0 indicates not a variant
		this.foodItemId = foodItemId;
		this.productName = productName;
		this.sizeMl = 0; // Food items don't have ml size
		this.price = price;
		this.qty = qty;
		this.manager = manager;
		this.tableNo = tableNo;
		this.payment = payment;
	}

	// Dynamically formats the text based on item type
	public String toText() {
		String nameDisplay = productName;
		if (itemType.equals("BAR")) {
			nameDisplay += " " + sizeMl + "ml";
		}
		
		return String.format("%-25s x%d @ %.2f (Table %d)", nameDisplay, qty, price, tableNo);
	}
}