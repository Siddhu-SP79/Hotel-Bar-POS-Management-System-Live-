package com.barpay.pos.model;

public class OpenOrderItem {
	public int itemId; // ID in open_order_items table
	public int orderId; // FK to open_orders table
	public boolean isKotPrinted;

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

	// Constructor for combined data from database
	public OpenOrderItem(int itemId, int orderId, int variantId, int foodItemId, 
						 String productName, int sizeMl, double price, int qty, 
						 String manager, int tableNo, boolean isKotPrinted) 
	{
		this.itemId = itemId;
		this.orderId = orderId;
		this.variantId = variantId;
		this.foodItemId = foodItemId;
		this.productName = productName;
		this.sizeMl = sizeMl;
		this.price = price;
		this.qty = qty;
		this.manager = manager;
		this.tableNo = tableNo;
		this.isKotPrinted = isKotPrinted;
		
		// Determine type based on which ID is present
		this.itemType = (variantId > 0) ? "BAR" : "FOOD";
	}

	public int getId() {
		return itemId;
	}

	public String getProductName() {
		return productName;
	}

	public int getSizeMl() {
		return sizeMl;
	}

	public int getQty() {
		return qty;
	}

	public double getPrice() {
		return price;
	}

	public int getVariantId() {
		return variantId;
	}

	public int getFoodItemId() {
		return foodItemId;
	}

	public boolean isKotPrinted() {
		return isKotPrinted;
	}

	// Formats text for display in the "Tables" panel list
	public String toText() {
		String status = isKotPrinted ? "" : " (NEW/Pending KOT)";
		String nameDisplay = productName;
		
		if (itemType.equals("BAR")) {
			nameDisplay += " " + sizeMl + "ml";
		}
		
		return String.format("₹%.0f - %-25s x%d @ %.2f%s", (qty * price), nameDisplay, qty, price, status);
	}

	// Used for JDialog selection
	@Override
	public String toString() {
		return toText();
	}
}