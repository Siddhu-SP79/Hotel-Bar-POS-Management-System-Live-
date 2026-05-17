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
