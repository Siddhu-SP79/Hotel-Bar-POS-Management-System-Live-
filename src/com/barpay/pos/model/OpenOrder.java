package com.barpay.pos.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class OpenOrder {
	private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("HH:mm:ss");
	private int id;
	private int tableNo;
	private String managerName;
	private LocalDateTime startTime;
	private String status;
	private double totalAmount; // Not stored in DB, calculated from items

	public OpenOrder(int id, int tableNo, String managerName, String startTimeStr, String status, double totalAmount) {
		this.id = id;
		this.tableNo = tableNo;
		this.managerName = managerName;
		this.startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));
		this.status = status;
		this.totalAmount = totalAmount;
	}

	public int getId() { return id; }
	public int getTableNo() { return tableNo; }
	public String getManagerName() { return managerName; }
	public LocalDateTime getStartTime() { return startTime; }
	public String getStatus() { return status; }
	public double getTotalAmount() { return totalAmount; }

	@Override
	public String toString() {
		return String.format("Table %d (M: %s) - Status: %s - Total: ₹%.2f - Start: %s", 
				tableNo, managerName, status, totalAmount, startTime.format(DTF));
	}
}