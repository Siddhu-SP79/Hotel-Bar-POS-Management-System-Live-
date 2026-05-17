package com.barpay.pos.model;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class Variant {
	private int id;
	private int productId;
	private int sizeMl;
	private double price;
	private int counterQty;
	private int storeQty;
	// New field for Variant Barcode ID
	private String barcodeId; 

	// Full constructor including the new barcodeId
	public Variant(int id, int productId, int sizeMl, double price, int counterQty, int storeQty, String barcodeId) {
		this.id = id;
		this.productId = productId;
		this.sizeMl = sizeMl;
		this.price = price;
		this.counterQty = counterQty;
		this.storeQty = storeQty;
		this.barcodeId = barcodeId;
	}

	// Original constructor, now delegates to the full constructor with a null barcodeId
	public Variant(int id, int productId, int sizeMl, double price, int counterQty, int storeQty) {
		this(id, productId, sizeMl, price, counterQty, storeQty, null);
	}

	public int getId(){ return id; }
	public int getProductId(){ return productId; }
	public int getSizeMl(){ return sizeMl; }
	public double getPrice(){ return price; }
	public int getCounterQty(){ return counterQty; }
	public int getStoreQty(){ return storeQty; }

	// New getter for Barcode ID
	public String getBarcodeId() { return barcodeId; }

	public void setCounterQty(int q){ this.counterQty = q; }
}