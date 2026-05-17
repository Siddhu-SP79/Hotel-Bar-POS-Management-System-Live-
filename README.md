# Hotel & Bar POS Management System (Live)

A full-scale, production-ready Point-of-Sale (POS) application designed specifically to streamline hospitality operations, food and beverage (F&B) billing, and specialized bar stock tracking. The platform bridges desktop UI interactions with ACID-compliant relational database management to handle high-concurrency order distributions, real-time spatial floor tables, and precise dynamic menu variations.

---

### 🚀 Core Architectural Features

- 🍽️ **Real-Time Spatial Table Management:** Provides a visual, color-coded floor grid mapping live table statuses (Available, Seated, Bill Generated) to avoid transactional conflicts.
- 🥃 **Volume Variant-Based Selling:** Engineered automated structural support for specialized bar menu breakdowns, allowing granular pricing and billing across distinct fluid variants (`30ml`, `60ml`, `90ml`, `180ml`, Full Bottle).
- 📦 **Automated Inventory Lifecycle & Auto-Deduction:** 
  - Real-time stock entry validation modules.
  - Live availability trackers synchronized across active billing counters.
  - Automated relational database triggers that instantly deduct raw or bottled stock upon successful order generation.
- 🔐 **Role-Based Access Control (RBAC):** Implemented secure authentication layers separating administrative back-office privileges (inventory management, sales reports, master menu pricing) from front-of-house service roles (billing, KOT printing).

---

### 💻 Technical Stack

- **Frontend User Interface:** Java (Swing UI / AWT Graphics for responsive layouts)
- **Database Architecture:** MySQL / Oracle SQL (Relational Engine)
- **Data Connectivity Layer:** JDBC (Java Database Connectivity with optimized connection pooling parameters)

---

### 🗄️ Relational Schema Design Layout (Core Baseline)

The billing and inventory engines rely on structured relational constraints to maintain high data integrity. Below is an architectural overview of how menu variants and inventory are mapped inside the system:

```sql
-- 1. Master Inventory Stock Tracking
CREATE TABLE inventory_stock (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(150) NOT NULL,
    current_stock_volume DECIMAL(10,2) NOT NULL, -- Tracked in ml or unit counts
    minimum_threshold_limit INT DEFAULT 10,
    last_updated_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. Liquid/Menu Variant Mapping Layout
CREATE TABLE item_variants (
    variant_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT,
    variant_name VARCHAR(50) NOT NULL, -- e.g., '30ml', '60ml', 'Bottle'
    volume_value_ml INT DEFAULT 0,     -- Numeric breakdown used for auto-deduction logic
    selling_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES inventory_stock(item_id) ON DELETE CASCADE
);

-- 3. Sales Order Processing Workflow
CREATE TABLE bill_transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    table_number INT NOT NULL,
    sub_total DECIMAL(10,2) NOT NULL,
    tax_percentage DECIMAL(5,2) DEFAULT 18.00,
    grand_total DECIMAL(10,2) NOT NULL,
    payment_mode ENUM('Cash', 'UPI', 'Card') NOT NULL,
    settled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
