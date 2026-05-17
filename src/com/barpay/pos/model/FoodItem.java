package com.barpay.pos.model;

public class FoodItem {
    private int id;
    private String name;
    private String category;
    private double price;
    private String imageFilename; 

    public FoodItem(int id, String name, String category, double price, String imageFilename) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageFilename = imageFilename;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getImageFilename() { return imageFilename; } 

    // Used for potential JList/ComboBox displays
    @Override
    public String toString() { return name + " (" + category + ") - ₹" + price; }
}