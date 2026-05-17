package com.barpay.pos.model;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class Product {
	private int id;
	private String name;
	private String rackId;
	private String imageFilename;
	// 1. New field for Barcode ID
	private String barcodeId; 

	// 2. Updated constructor to include barcodeId
	public Product(int id, String name, String rackId, String imageFilename, String barcodeId) {
		this.id = id;
		this.name = name;
		this.rackId = rackId;
		this.imageFilename = imageFilename;
		this.barcodeId = barcodeId; // Initialize the new field
	}

	public int getId() { return id; }
	public String getName() { return name; }
	public String getRackId() { return rackId; }
	public String getImageFilename() { return imageFilename; }

	// 3. New getter for Barcode ID
	public String getBarcodeId() { return barcodeId; } 

	@Override public String toString(){ return name; }
}