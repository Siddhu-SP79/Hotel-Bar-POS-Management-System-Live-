Bar & Resto POS - Final scaffold

What's included:
- src/... Java source files for a single-window POS panel (Billing, Owner, Reports, History).
- barDb_pos.sql - SQL schema to create required tables.
- resources/images/ (created at runtime) - Owner's Add Brand copies images here.

How to run:
1. Ensure Java 11+ and MySQL are installed.
2. Create the database and tables: run barDb_pos.sql (use mysql CLI or a GUI tool).
3. Update DB credentials in src/com/barpay/pos/util/DBUtil.java if needed.
4. Import the 'src' folder as a Java project in your IDE (Eclipse/IntelliJ).
5. Add MySQL Connector/J to project's classpath.
6. Run com.barpay.pos.MainApp (or run POSPanel).
7. Owner password: swagat

Notes:
- Owner tab allows add/edit/remove brand and variants, including image file copy to resources/images/.
- Manager billing supports search by name/rack/id, add to cart, checkout (saves bill), print preview & send to printer.
- Reports export CSV. PDF export/printing receipts improvements can be added later.
- If images do not show via getResource, the code falls back to loading from resources/images/ directory in project root.

If you want, I can now:
- Package this as a runnable JAR.
- Add prettier printable receipt (PDF) using a library (iText/PDFBox).
- Add unit tests or migrations.
