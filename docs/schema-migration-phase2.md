# Schema Migration — Phase 2 Revamp

**Tanggal:** 2026-03-30
**Commit:** `85ba01e`
**Tipe:** ALTER TABLE (nullable) + CREATE TABLE
**Risiko ke data production:** ✅ Nihil — semua perubahan backward-compatible

---

## Kenapa Dilakukan Sekarang

Semua perubahan schema untuk fitur Phase 2 dilakukan **sebelum fitur diimplementasi**. Alasannya:

1. **Database production belum padat data** — ALTER TABLE di tabel kosong atau sedikit data jauh lebih cepat dan aman.
2. **Semua kolom baru bersifat nullable** — baris lama yang sudah ada tidak terpengaruh, nilainya otomatis `NULL`.
3. **Tidak ada rename, tidak ada type change, tidak ada DROP** — tiga operasi paling berbahaya ini tidak dilakukan.
4. **ALTER di kemudian hari lebih mahal** — tabel `transaction` dengan jutaan baris + `ALTER TABLE ADD COLUMN` bisa menyebabkan locking dan downtime.
5. **Hibernate `ddl-auto=update` menangani eksekusi** — saat aplikasi restart berikutnya, semua perubahan diapply otomatis.

---

## Cara Kerja `ddl-auto=update`

Saat Spring Boot startup:
1. Hibernate membandingkan entity class di kode dengan struktur tabel di database.
2. Jika ada kolom baru di entity → `ALTER TABLE ... ADD COLUMN ...`
3. Jika ada tabel baru (entity baru) → `CREATE TABLE ...`
4. Hibernate **tidak pernah** DROP kolom atau tabel secara otomatis.
5. Hibernate **tidak mengubah** tipe kolom yang sudah ada.

```
Startup aplikasi
      │
      ▼
Hibernate scan semua @Entity
      │
      ▼
Bandingkan dengan schema DB
      │
      ├─ Kolom baru? → ALTER TABLE ADD COLUMN (nullable)
      ├─ Tabel baru? → CREATE TABLE
      └─ Kolom hilang? → DIBIARKAN (tidak di-drop)
```

---

## Perubahan pada Tabel yang Sudah Ada

> Semua kolom baru adalah **nullable**. Baris lama akan memiliki nilai `NULL` pada kolom-kolom ini, yang merupakan behavior yang diharapkan.

---

### `transaction`

Tabel ini menyimpan setiap transaksi penjualan.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `customer_id` | BIGINT NULL | CRM POS | ID pelanggan jika transaksi dikaitkan ke member |
| `order_type_id` | BIGINT NULL | Order Management | ID tipe pesanan (Dine In, Take Away, dll) |
| `shift_id` | BIGINT NULL | User Management | ID shift kasir yang membuat transaksi |
| `discount_id` | BIGINT NULL | Marketing Tools | ID diskon yang diterapkan |
| `discount_amount` | DECIMAL NULL | Marketing Tools | Nilai diskon dalam rupiah |
| `promo_id` | BIGINT NULL | Marketing Tools | ID promosi yang diterapkan |
| `promo_amount` | DECIMAL NULL | Marketing Tools | Nilai promosi dalam rupiah |
| `voucher_amount` | DECIMAL NULL | Voucher/Split Payment | Nilai voucher yang dipakai |
| `gross_amount` | DECIMAL NULL | Financial Reporting | Total sebelum diskon/promo |
| `net_amount` | DECIMAL NULL | Financial Reporting | Total setelah diskon, sebelum pajak & SC |
| `refund_amount` | DECIMAL NULL | Refund | Jumlah yang sudah direfund |
| `refund_reason` | VARCHAR NULL | Refund | Alasan refund |
| `refund_by` | VARCHAR NULL | Refund | Username yang melakukan refund |
| `refund_date` | TIMESTAMP NULL | Refund | Waktu refund dilakukan |

**SQL yang dieksekusi Hibernate:**
```sql
ALTER TABLE transaction ADD COLUMN customer_id BIGINT;
ALTER TABLE transaction ADD COLUMN order_type_id BIGINT;
ALTER TABLE transaction ADD COLUMN shift_id BIGINT;
ALTER TABLE transaction ADD COLUMN discount_id BIGINT;
ALTER TABLE transaction ADD COLUMN discount_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN promo_id BIGINT;
ALTER TABLE transaction ADD COLUMN promo_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN voucher_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN gross_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN net_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN refund_amount NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN refund_reason VARCHAR(255);
ALTER TABLE transaction ADD COLUMN refund_by VARCHAR(255);
ALTER TABLE transaction ADD COLUMN refund_date TIMESTAMP;
```

---

### `transaction_items`

Snapshot item produk dalam transaksi.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `variant_id` | BIGINT NULL | Product Varian | ID varian produk yang dipilih |
| `original_price` | DECIMAL NULL | Discount/Price Adj. | Harga sebelum diskon item |
| `discount_amount` | DECIMAL NULL | Marketing Tools | Diskon yang diterapkan per item |
| `price_book_item_id` | BIGINT NULL | Price Book | ID price book item yang digunakan |

```sql
ALTER TABLE transaction_items ADD COLUMN variant_id BIGINT;
ALTER TABLE transaction_items ADD COLUMN original_price NUMERIC(38,2);
ALTER TABLE transaction_items ADD COLUMN discount_amount NUMERIC(38,2);
ALTER TABLE transaction_items ADD COLUMN price_book_item_id BIGINT;
```

---

### `product`

Master data produk.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `product_type` | VARCHAR NULL | Varian & Modifier | `SIMPLE` \| `VARIANT` \| `MODIFIER` |
| `display_order` | INT NULL | Product Management | Urutan tampil di POS |
| `is_active` | BOOLEAN NULL | Product Management | Status aktif (null = ikut `deleted_date`) |

```sql
ALTER TABLE product ADD COLUMN product_type VARCHAR(255);
ALTER TABLE product ADD COLUMN display_order INT;
ALTER TABLE product ADD COLUMN is_active BOOLEAN;
```

> **Catatan `product_type`:** Produk lama yang belum diset dianggap `SIMPLE`. Tidak perlu data migration — logic di service cukup fallback ke `SIMPLE` jika null.

---

### `payment_setting`

Konfigurasi pajak dan biaya per merchant.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `outlet_id` | BIGINT NULL | Tax per Outlet | Jika NULL = berlaku untuk semua outlet merchant |
| `service_charge_source` | VARCHAR NULL | SC Diferensiasi | `BEFORE_TAX` \| `AFTER_TAX` \| `DPP` \| `AFTER_DISCOUNT` |

```sql
ALTER TABLE payment_setting ADD COLUMN outlet_id BIGINT;
ALTER TABLE payment_setting ADD COLUMN service_charge_source VARCHAR(255);
```

> **Strategi per-outlet:** Record `payment_setting` yang memiliki `outlet_id = NULL` berlaku sebagai default untuk semua outlet. Jika ada record dengan `outlet_id` terisi, maka setting itu yang dipakai untuk outlet tersebut (override). Ini backward-compatible — semua data lama tetap valid karena `outlet_id`-nya `NULL`.

---

### `outlet`

Data gerai/cabang merchant.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `default_order_type_id` | BIGINT NULL | Order Management | Tipe pesanan default outlet ini |
| `language_code` | VARCHAR NULL | POS Language | Kode bahasa (mis: `id`, `en`) |

```sql
ALTER TABLE outlet ADD COLUMN default_order_type_id BIGINT;
ALTER TABLE outlet ADD COLUMN language_code VARCHAR(10);
```

---

### `merchant`

Data merchant utama.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `language_code` | VARCHAR NULL | POS Language | Bahasa default merchant (bisa di-override per outlet) |

```sql
ALTER TABLE merchant ADD COLUMN language_code VARCHAR(10);
```

---

### `user_detail`

Data detail user/kasir.

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|-----------|------|-------------|-----------|
| `pin` | VARCHAR NULL | Refund / Auth | PIN ter-hash untuk otorisasi refund atau operasi sensitif |
| `outlet_id` | BIGINT NULL | User Management | Outlet tempat kasir ini bertugas |

```sql
ALTER TABLE user_detail ADD COLUMN pin VARCHAR(255);
ALTER TABLE user_detail ADD COLUMN outlet_id BIGINT;
```

> **Keamanan `pin`:** Wajib di-hash (BCrypt) sebelum disimpan, sama seperti password. Jangan pernah simpan plain text.

---

## Tabel Baru yang Dibuat

Semua tabel ini dibuat dari nol — tidak ada risiko ke data existing.

---

### `order_type` — Tipe Pesanan

```sql
CREATE TABLE order_type (
    id          BIGSERIAL PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(255),
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  VARCHAR(255),
    created_date TIMESTAMP,
    modified_by  VARCHAR(255),
    modified_date TIMESTAMP
);
```

**Contoh data:**
```
id=1, merchant_id=1, name="Dine In",  code="DINE_IN",  is_default=true
id=2, merchant_id=1, name="Take Away", code="TAKE_AWAY", is_default=false
id=3, merchant_id=1, name="Delivery", code="DELIVERY",  is_default=false
```

---

### `customer` — Data Pelanggan (CRM)

```sql
CREATE TABLE customer (
    id                BIGSERIAL PRIMARY KEY,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    phone             VARCHAR(255),
    email             VARCHAR(255),
    address           TEXT,
    gender            VARCHAR(20),
    loyalty_points    NUMERIC(38,2) NOT NULL DEFAULT 0,
    total_transaction INT NOT NULL DEFAULT 0,
    total_spend       NUMERIC(38,2) NOT NULL DEFAULT 0,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255),
    created_date      TIMESTAMP,
    modified_by       VARCHAR(255),
    modified_date     TIMESTAMP
);
```

---

### `cashier_shift` — Shift Kasir

```sql
CREATE TABLE cashier_shift (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    outlet_id    BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    username     VARCHAR(255),
    opening_cash NUMERIC(38,2) NOT NULL DEFAULT 0,
    closing_cash NUMERIC(38,2),
    open_date    TIMESTAMP NOT NULL,
    close_date   TIMESTAMP,
    status       VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | CLOSED
    note         TEXT,
    opened_by    VARCHAR(255),
    closed_by    VARCHAR(255)
);
```

**Flow:**
```
Buka shift → status=OPEN, open_date=now(), opening_cash=X
Transaksi  → shift_id diisi di tabel transaction
Tutup shift → status=CLOSED, close_date=now(), closing_cash=Y
```

---

### `price_book` + `price_book_item` — Multiple Harga per Produk

```sql
CREATE TABLE price_book (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    order_type_id BIGINT,        -- harga khusus untuk tipe pesanan ini
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE price_book_item (
    id            BIGSERIAL PRIMARY KEY,
    price_book_id BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL,
    price         NUMERIC(38,2) NOT NULL DEFAULT 0,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);
```

**Contoh skenario:**
```
price_book: id=1, name="Harga Dine In",  order_type_id=1 (Dine In)
price_book: id=2, name="Harga Take Away", order_type_id=2 (Take Away)

price_book_item: price_book_id=1, product_id=10, price=25000  ← harga dine in
price_book_item: price_book_id=2, product_id=10, price=22000  ← harga take away
```

---

### `product_variant_group` + `product_variant` — Varian Produk

```
Produk: Kopi Susu
  └── Variant Group: "Ukuran" (required)
        ├── Variant: "Regular"   additional_price=0
        ├── Variant: "Large"     additional_price=5000
        └── Variant: "Extra Large" additional_price=10000
```

```sql
CREATE TABLE product_variant_group (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    is_required  BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE product_variant (
    id                BIGSERIAL PRIMARY KEY,
    variant_group_id  BIGINT NOT NULL,
    product_id        BIGINT NOT NULL,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    additional_price  NUMERIC(38,2) NOT NULL DEFAULT 0,
    sku               VARCHAR(255),
    display_order     INT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);
```

**Kalkulasi harga dengan varian:**
```
Harga produk utama  = 20.000
Additional price    = +5.000 (pilih "Large")
Harga item          = 25.000
```

---

### `product_modifier_group` + `product_modifier` — Modifier Produk

Berbeda dari varian: modifier bisa dipilih **lebih dari satu** dan bersifat optional.

```
Produk: Nasi Goreng
  └── Modifier Group: "Tambahan" (min=0, max=3, not required)
        ├── Modifier: "Tambah Telur"   additional_price=3000
        ├── Modifier: "Tambah Kerupuk" additional_price=1000
        └── Modifier: "Extra Pedas"    additional_price=0
```

```sql
CREATE TABLE product_modifier_group (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    min_select    INT NOT NULL DEFAULT 0,
    max_select    INT NOT NULL DEFAULT 1,
    is_required   BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE product_modifier (
    id                BIGSERIAL PRIMARY KEY,
    modifier_group_id BIGINT NOT NULL,
    product_id        BIGINT NOT NULL,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    additional_price  NUMERIC(38,2) NOT NULL DEFAULT 0,
    display_order     INT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);
```

### `transaction_modifier` — Snapshot Modifier dalam Transaksi

```sql
CREATE TABLE transaction_modifier (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    modifier_id         BIGINT,            -- nullable: snapshot jika modifier dihapus
    modifier_name       VARCHAR(255),      -- snapshot nama saat transaksi
    additional_price    NUMERIC(38,2) NOT NULL DEFAULT 0,  -- snapshot harga
    qty                 INT NOT NULL DEFAULT 1,
    created_by          VARCHAR(255),
    created_date        TIMESTAMP
);
```

> Sama dengan snapshot produk di `transaction_items` — harga dan nama modifier disimpan saat transaksi agar tidak terpengaruh perubahan modifier di kemudian hari.

---

### `discount` + `discount_tier` + `discount_outlet` + `discount_payment_method`

#### Tipe Diskon

| `type` | Keterangan |
|--------|-----------|
| `TRANSACTION` | Diskon langsung ke total transaksi |
| `ITEM_QTY` | Diskon bertingkat berdasarkan qty item tertentu |

#### Tipe Nilai (`value_type`)

| `value_type` | Keterangan |
|-------------|-----------|
| `PERCENTAGE` | Nilai dalam persen (mis: `10` = 10%) |
| `AMOUNT` | Nilai dalam rupiah (mis: `5000` = Rp5.000) |

```sql
CREATE TABLE discount (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(20) NOT NULL DEFAULT 'TRANSACTION',  -- TRANSACTION | ITEM_QTY
    value_type   VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',   -- PERCENTAGE | AMOUNT
    value        NUMERIC(38,2) NOT NULL DEFAULT 0,            -- dipakai jika type=TRANSACTION
    product_id   BIGINT,           -- dipakai jika type=ITEM_QTY (produk yang dihitung qty-nya)
    min_purchase NUMERIC(38,2),
    visibility   VARCHAR(20) NOT NULL DEFAULT 'ALL_OUTLET',
    start_date   TIMESTAMP,
    end_date     TIMESTAMP,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

-- Tier bertingkat untuk type=ITEM_QTY
CREATE TABLE discount_tier (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL,
    min_qty     INT NOT NULL,
    max_qty     INT,               -- null = tidak ada batas atas
    value_type  VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    value       NUMERIC(38,2) NOT NULL DEFAULT 0,
    display_order INT
);

-- Outlet yang berlaku (jika visibility=SPECIFIC_OUTLET)
CREATE TABLE discount_outlet (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    outlet_id   BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL
);

-- Metode pembayaran yang mendapat diskon ini
CREATE TABLE discount_payment_method (
    id                  BIGSERIAL PRIMARY KEY,
    discount_id         BIGINT NOT NULL,
    payment_method_code VARCHAR(255) NOT NULL,
    merchant_id         BIGINT NOT NULL
);
```

#### Contoh Skenario `TRANSACTION`

```
Diskon "Promo QRIS 10%":
  discount: type=TRANSACTION, value_type=PERCENTAGE, value=10
  discount_payment_method: payment_method_code=QRIS

Diskon "Diskon Outlet A Rp5.000":
  discount: type=TRANSACTION, value_type=AMOUNT, value=5000, visibility=SPECIFIC_OUTLET
  discount_outlet: outlet_id=3
```

**Kalkulasi:**
```
subTotal = 100.000
discount (10%) → discount_amount = 10.000
totalAmount = 100.000 - 10.000 = 90.000
```

#### Contoh Skenario `ITEM_QTY`

```
Diskon "Beli Kopi Susu Makin Banyak Makin Hemat":
  discount: type=ITEM_QTY, product_id=5 (Kopi Susu)
  discount_tier: min_qty=1, max_qty=2,    value_type=PERCENTAGE, value=0   → 0%
  discount_tier: min_qty=3, max_qty=5,    value_type=PERCENTAGE, value=10  → 10%
  discount_tier: min_qty=6, max_qty=null, value_type=PERCENTAGE, value=20  → 20%
```

**Kalkulasi saat transaksi (beli 4 Kopi Susu @ Rp15.000):**
```
qty Kopi Susu = 4  → cocok dengan tier min_qty=3, max_qty=5 → diskon 10%
itemTotal     = 4 × 15.000 = 60.000
discount_amount = 60.000 × 10% = 6.000
totalPrice item setelah diskon = 54.000
```

**Logika pencarian tier:**
```
Cari tier yang memenuhi: min_qty <= qty_item <= max_qty
Jika max_qty = null, berarti min_qty <= qty_item
Jika tidak ada tier yang cocok → tidak ada diskon
```

---

### `promotion` + `promotion_outlet`

Mirip diskon, tapi diterapkan dari sisi merchant sebagai reward (bukan potongan harga).

```sql
CREATE TABLE promotion (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',
    value               NUMERIC(38,2) NOT NULL DEFAULT 0,
    min_purchase        NUMERIC(38,2),
    visibility          VARCHAR(20) NOT NULL DEFAULT 'ALL_OUTLET',
    payment_method_code VARCHAR(255),   -- null = berlaku semua metode
    start_date          TIMESTAMP,
    end_date            TIMESTAMP,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE promotion_outlet (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    outlet_id    BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL
);
```

---

### `loyalty_program` + `loyalty_transaction`

```sql
CREATE TABLE loyalty_program (
    id                BIGSERIAL PRIMARY KEY,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    points_per_amount NUMERIC(38,2) NOT NULL DEFAULT 0,
    redeem_rate       NUMERIC(38,2) NOT NULL DEFAULT 0,
    min_redeem_points NUMERIC(38,2),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE loyalty_transaction (
    id             BIGSERIAL PRIMARY KEY,
    merchant_id    BIGINT NOT NULL,
    customer_id    BIGINT NOT NULL,
    transaction_id BIGINT,
    points         NUMERIC(38,2) NOT NULL DEFAULT 0,
    type           VARCHAR(20) NOT NULL DEFAULT 'GET',  -- GET | REDEEM | ADJUSTMENT
    note           TEXT,
    created_by     VARCHAR(255),
    created_date   TIMESTAMP
);
```

**Contoh kalkulasi poin:**
```
Loyalty program: points_per_amount=10.000, redeem_rate=100
→ Setiap Rp10.000 transaksi = 1 poin
→ 1 poin = Rp100 saat redeem

Transaksi Rp150.000 → dapat 15 poin
Redeem 50 poin → diskon Rp5.000
```

---

### `voucher` + `voucher_usage`

```sql
CREATE TABLE voucher (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    code         VARCHAR(255) NOT NULL UNIQUE,
    type         VARCHAR(20) NOT NULL DEFAULT 'AMOUNT',  -- PERCENTAGE | AMOUNT
    value        NUMERIC(38,2) NOT NULL DEFAULT 0,
    min_purchase NUMERIC(38,2),
    max_use      INT,         -- null = unlimited
    used_count   INT NOT NULL DEFAULT 0,
    expired_date TIMESTAMP,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);

CREATE TABLE voucher_usage (
    id             BIGSERIAL PRIMARY KEY,
    voucher_id     BIGINT NOT NULL,
    transaction_id BIGINT NOT NULL,
    merchant_id    BIGINT NOT NULL,
    customer_id    BIGINT,
    amount         NUMERIC(38,2) NOT NULL DEFAULT 0,
    used_date      TIMESTAMP NOT NULL
);
```

---

### `refund`

```sql
CREATE TABLE refund (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    merchant_id    BIGINT NOT NULL,
    outlet_id      BIGINT,
    amount         NUMERIC(38,2) NOT NULL DEFAULT 0,
    reason         TEXT,
    type           VARCHAR(20) NOT NULL DEFAULT 'FULL',     -- FULL | PARTIAL
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED
    refund_by      VARCHAR(255),
    approved_by    VARCHAR(255),
    refund_date    TIMESTAMP NOT NULL,
    approved_date  TIMESTAMP,
    note           TEXT
);
```

**Flow refund:**
```
Kasir minta refund → status=PENDING
Manager approve (PIN) → status=APPROVED, approved_by=manager
Sistem update transaction.refund_amount
```

---

### `receipt_template`

```sql
CREATE TABLE receipt_template (
    id                   BIGSERIAL PRIMARY KEY,
    merchant_id          BIGINT NOT NULL,
    outlet_id            BIGINT,        -- null = berlaku semua outlet
    header               TEXT,
    footer               TEXT,
    show_tax             BOOLEAN NOT NULL DEFAULT TRUE,
    show_service_charge  BOOLEAN NOT NULL DEFAULT TRUE,
    show_rounding        BOOLEAN NOT NULL DEFAULT TRUE,
    show_logo            BOOLEAN NOT NULL DEFAULT FALSE,
    logo_url             TEXT,
    show_queue_number    BOOLEAN NOT NULL DEFAULT TRUE,
    paper_size           VARCHAR(20),
    ...audit fields...
);
```

---

### `disbursement_rule` + `disbursement_log` — Revenue Sharing

```sql
CREATE TABLE disbursement_rule (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    name                VARCHAR(255) NOT NULL,
    layer               VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    recipient_id        BIGINT,
    recipient_name      VARCHAR(255),
    percentage          NUMERIC(38,2) NOT NULL DEFAULT 0,
    source              VARCHAR(30) NOT NULL DEFAULT 'NET',
    product_type_filter VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    display_order       INT,
    ...audit fields...
);

CREATE TABLE disbursement_log (
    id             BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    rule_id        BIGINT NOT NULL,
    merchant_id    BIGINT NOT NULL,
    recipient_id   BIGINT,
    recipient_name VARCHAR(255),
    layer          VARCHAR(20) NOT NULL,
    base_amount    NUMERIC(38,2) NOT NULL DEFAULT 0,
    percentage     NUMERIC(38,2) NOT NULL DEFAULT 0,
    amount         NUMERIC(38,2) NOT NULL DEFAULT 0,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_date   TIMESTAMP NOT NULL,
    settled_date   TIMESTAMP
);
```

**Field `source` pada `disbursement_rule`:**

| Nilai | Basis Kalkulasi |
|-------|----------------|
| `GROSS` | subTotal (sebelum diskon, pajak, SC) |
| `NET` | subTotal setelah diskon |
| `NET_AFTER_TAX` | NET + pajak |
| `NET_AFTER_TAX_SC` | NET + pajak + service charge |

**Contoh skenario multi-layer:**
```
Transaksi: total_amount = Rp100.000
  Rule 1: PLATFORM, 2%, source=GROSS   → Rp2.000
  Rule 2: DEALER,   5%, source=NET     → Rp5.000
  Rule 3: MERCHANT, 93%, source=NET    → Rp93.000

disbursement_log: 3 baris per transaksi
```

---

### `mdr_setting` — Pengaturan MDR

```sql
CREATE TABLE mdr_setting (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    payment_method_code VARCHAR(255) NOT NULL,
    percentage          NUMERIC(38,2) NOT NULL DEFAULT 0,
    flat_fee            NUMERIC(38,2) NOT NULL DEFAULT 0,
    charged_to          VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);
```

---

### `printer_setting`

```sql
CREATE TABLE printer_setting (
    id              BIGSERIAL PRIMARY KEY,
    merchant_id     BIGINT NOT NULL,
    outlet_id       BIGINT,
    type            VARCHAR(20) NOT NULL DEFAULT 'RECEIPT',  -- RECEIPT | KITCHEN | ORDER
    name            VARCHAR(255) NOT NULL,
    connection_type VARCHAR(20),   -- NETWORK | USB | BLUETOOTH
    ip_address      VARCHAR(255),
    port            INT,
    paper_size      VARCHAR(20),
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    ...audit fields...
);
```

---

### `notification_setting`

```sql
CREATE TABLE notification_setting (
    id           BIGSERIAL PRIMARY KEY,
    merchant_id  BIGINT NOT NULL,
    email_address VARCHAR(255),
    is_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    notify_types VARCHAR(255),  -- comma-separated: DAILY_SUMMARY,SETTLEMENT,SHIFT_CLOSE
    send_time    VARCHAR(5),    -- format HH:mm, mis: "23:00"
    ...audit fields...
);
```

---

## Ringkasan Total Perubahan

| Kategori | Jumlah |
|----------|--------|
| Tabel existing yang dimodifikasi | 7 |
| Kolom nullable baru di tabel existing | 28 (+ `value_type`, `product_id` di `discount`) |
| Tabel baru dibuat | 27 (+ `discount_tier`) |
| **Total tabel setelah migrasi** | **60** |

---

## Checklist Saat Deploy ke Production

Sebelum restart aplikasi di server production:

- [ ] Backup database terlebih dahulu
- [ ] Pastikan aplikasi bisa di-rollback jika ada masalah
- [ ] Monitor log Hibernate saat startup — pastikan tidak ada error DDL
- [ ] Verifikasi kolom baru dengan query: `SELECT column_name FROM information_schema.columns WHERE table_name = 'transaction' ORDER BY ordinal_position;`
- [ ] Verifikasi tabel baru dengan query: `SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;`
- [ ] Pastikan `spring.jpa.hibernate.ddl-auto=update` di `application.properties`
- [ ] Test endpoint existing tidak terganggu (login, create transaction, dll)

---

## Yang Tidak Berubah

- Semua endpoint existing tetap berfungsi normal
- Semua data lama tetap valid
- Tidak ada service/controller yang diubah dalam commit ini
- Field lama tidak ada yang dihapus atau diubah tipenya
