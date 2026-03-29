# Schema Migration — Revamp

**Tanggal:** 2026-03-30
**Tipe:** ALTER TABLE (nullable) + CREATE TABLE
**Risiko ke data production:** ✅ Nihil — semua perubahan backward-compatible

---

## Kenapa Dilakukan Sekarang

1. **Database production belum padat data** — ALTER TABLE di tabel kosong atau sedikit data jauh lebih cepat dan aman.
2. **Semua kolom baru bersifat nullable** — baris lama tidak terpengaruh, nilainya otomatis `NULL`.
3. **Tidak ada rename, tidak ada type change, tidak ada DROP** — tiga operasi paling berbahaya ini tidak dilakukan.
4. **ALTER di kemudian hari lebih mahal** — tabel `transaction` dengan jutaan baris + `ALTER TABLE ADD COLUMN` bisa menyebabkan locking dan downtime.
5. **Hibernate `ddl-auto=update` menangani eksekusi** — saat aplikasi restart berikutnya, semua perubahan diapply otomatis.

---

## Cara Kerja `ddl-auto=update`

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

## A. Perubahan pada Tabel yang Sudah Ada

> Semua kolom baru **nullable**. Baris lama otomatis bernilai `NULL`.

---

### `transaction`

| Kolom Baru | Tipe | Untuk Fitur | Keterangan |
|---|---|---|---|
| `customer_id` | BIGINT NULL | CRM | ID pelanggan jika transaksi dikaitkan ke member |
| `order_type_id` | BIGINT NULL | Order Management | ID tipe pesanan (Dine In, Take Away, dll) |
| `shift_id` | BIGINT NULL | Cashier Shift | ID shift kasir yang membuat transaksi |
| `discount_id` | BIGINT NULL | Discount | ID kode diskon yang diterapkan |
| `discount_amount` | DECIMAL NULL | Discount | Nilai diskon kode dalam rupiah |
| `promo_id` | BIGINT NULL | Promotion | ID promosi otomatis yang diterapkan |
| `promo_amount` | DECIMAL NULL | Promotion | Nilai promosi dalam rupiah |
| `voucher_amount` | DECIMAL NULL | Voucher | Nilai voucher yang dipakai sebagai alat bayar |
| `gross_amount` | DECIMAL NULL | Reporting | grossSubTotal setelah price book, sebelum promo/diskon |
| `net_amount` | DECIMAL NULL | Reporting | netSubTotal setelah promo + diskon, base pajak & SC |
| `loyalty_points_earned` | DECIMAL NULL | Loyalty | Poin yang diperoleh dari transaksi ini |
| `loyalty_points_redeemed` | DECIMAL NULL | Loyalty | Poin yang ditukarkan dalam transaksi ini |
| `loyalty_redeem_amount` | DECIMAL NULL | Loyalty | Nilai rupiah dari redeem poin (mode PAYMENT) |
| `refund_amount` | DECIMAL NULL | Refund | Jumlah yang sudah direfund |
| `refund_reason` | VARCHAR NULL | Refund | Alasan refund |
| `refund_by` | VARCHAR NULL | Refund | Username yang melakukan refund |
| `refund_date` | TIMESTAMP NULL | Refund | Waktu refund dilakukan |

```sql
ALTER TABLE transaction ADD COLUMN customer_id             BIGINT;
ALTER TABLE transaction ADD COLUMN order_type_id           BIGINT;
ALTER TABLE transaction ADD COLUMN shift_id                BIGINT;
ALTER TABLE transaction ADD COLUMN discount_id             BIGINT;
ALTER TABLE transaction ADD COLUMN discount_amount         NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN promo_id                BIGINT;
ALTER TABLE transaction ADD COLUMN promo_amount            NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN voucher_amount          NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN gross_amount            NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN net_amount              NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN loyalty_points_earned   NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN loyalty_points_redeemed NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN loyalty_redeem_amount   NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN refund_amount           NUMERIC(38,2);
ALTER TABLE transaction ADD COLUMN refund_reason           VARCHAR(255);
ALTER TABLE transaction ADD COLUMN refund_by               VARCHAR(255);
ALTER TABLE transaction ADD COLUMN refund_date             TIMESTAMP;
```

---

### `transaction_items`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `variant_id` | BIGINT NULL | ID varian produk yang dipilih |
| `original_price` | DECIMAL NULL | Harga asal sebelum price book |
| `discount_amount` | DECIMAL NULL | Total diskon per item (promo + kode) |
| `price_book_item_id` | BIGINT NULL | ID price book item yang menentukan harga efektif |

```sql
ALTER TABLE transaction_items ADD COLUMN variant_id        BIGINT;
ALTER TABLE transaction_items ADD COLUMN original_price    NUMERIC(38,2);
ALTER TABLE transaction_items ADD COLUMN discount_amount   NUMERIC(38,2);
ALTER TABLE transaction_items ADD COLUMN price_book_item_id BIGINT;
```

---

### `product`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `product_type` | VARCHAR NULL | `SIMPLE` \| `VARIANT` \| `MODIFIER`. Null = dianggap `SIMPLE` |
| `display_order` | INT NULL | Urutan tampil di POS |
| `is_active` | BOOLEAN NULL | Status aktif. Null = ikut `deleted_date` |

```sql
ALTER TABLE product ADD COLUMN product_type  VARCHAR(255);
ALTER TABLE product ADD COLUMN display_order INT;
ALTER TABLE product ADD COLUMN is_active     BOOLEAN;
```

---

### `payment_setting`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `outlet_id` | BIGINT NULL | Null = berlaku untuk semua outlet merchant |
| `service_charge_source` | VARCHAR NULL | `BEFORE_TAX` \| `AFTER_TAX` \| `DPP` \| `AFTER_DISCOUNT` |

```sql
ALTER TABLE payment_setting ADD COLUMN outlet_id              BIGINT;
ALTER TABLE payment_setting ADD COLUMN service_charge_source  VARCHAR(255);
```

> Record dengan `outlet_id = NULL` adalah default. Jika ada record dengan `outlet_id` terisi, record itu yang dipakai untuk outlet tersebut (override).

---

### `outlet`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `default_order_type_id` | BIGINT NULL | Tipe pesanan default outlet ini |
| `language_code` | VARCHAR NULL | Kode bahasa (`id`, `en`) |

```sql
ALTER TABLE outlet ADD COLUMN default_order_type_id BIGINT;
ALTER TABLE outlet ADD COLUMN language_code         VARCHAR(10);
```

---

### `merchant`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `language_code` | VARCHAR NULL | Bahasa default merchant, bisa di-override per outlet |

```sql
ALTER TABLE merchant ADD COLUMN language_code VARCHAR(10);
```

---

### `user_detail`

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `pin` | VARCHAR NULL | PIN ter-hash (BCrypt) untuk otorisasi refund / operasi sensitif |
| `outlet_id` | BIGINT NULL | Outlet tempat kasir ini bertugas |

```sql
ALTER TABLE user_detail ADD COLUMN pin       VARCHAR(255);
ALTER TABLE user_detail ADD COLUMN outlet_id BIGINT;
```

> `pin` wajib di-hash (BCrypt) sebelum disimpan — jangan pernah simpan plain text.

---

### `voucher` — Repurpose sebagai VoucherCode

Tabel `voucher` direpurpose menjadi kode individual. Kolom lama dipertahankan untuk backward compatibility.

| Kolom Baru | Tipe | Keterangan |
|---|---|---|
| `group_id` | BIGINT NULL | FK ke `voucher_group`. Null = data lama |
| `status` | VARCHAR NULL | `AVAILABLE` \| `USED` \| `EXPIRED` \| `CANCELLED` |
| `used_date` | TIMESTAMP NULL | Tanggal kode ditukarkan |
| `transaction_id` | BIGINT NULL | ID transaksi yang menggunakan kode ini |

```sql
ALTER TABLE voucher ADD COLUMN group_id       BIGINT;
ALTER TABLE voucher ADD COLUMN status         VARCHAR(20);
ALTER TABLE voucher ADD COLUMN used_date      TIMESTAMP;
ALTER TABLE voucher ADD COLUMN transaction_id BIGINT;
```

**Kolom lama dipertahankan (deprecated untuk data baru):** `type`, `value`, `min_purchase`, `max_use`, `used_count`, `expired_date`

---

## B. Tabel Baru

Semua tabel dibuat dari nol — tidak ada risiko ke data existing.

---

### `order_type` — Tipe Pesanan

Digunakan untuk Price Book `ORDER_TYPE` dan field `order_type_id` di transaksi.

```sql
CREATE TABLE order_type (
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    code          VARCHAR(255),
    is_default    BOOLEAN NOT NULL DEFAULT FALSE,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(255),
    created_date  TIMESTAMP,
    modified_by   VARCHAR(255),
    modified_date TIMESTAMP
);
```

**Contoh data:**
```
id=1, name="Dine In",   code="DINE_IN",   is_default=true
id=2, name="Take Away", code="TAKE_AWAY"
id=3, name="Delivery",  code="DELIVERY"
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
    total_transaction INT          NOT NULL DEFAULT 0,
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
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL,
    outlet_id     BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    username      VARCHAR(255),
    opening_cash  NUMERIC(38,2) NOT NULL DEFAULT 0,
    closing_cash  NUMERIC(38,2),
    open_date     TIMESTAMP NOT NULL,
    close_date    TIMESTAMP,
    status        VARCHAR(20) NOT NULL DEFAULT 'OPEN',  -- OPEN | CLOSED
    note          TEXT,
    opened_by     VARCHAR(255),
    closed_by     VARCHAR(255)
);
```

**Flow:**
```
Kasir buka shift   → status=OPEN,   open_date=now(),  opening_cash=X
Kasir bertransaksi → transaction.shift_id diisi
Kasir tutup shift  → status=CLOSED, close_date=now(), closing_cash=Y
```

---

### `price_book` — Daftar Harga

Tipe price book:
- `PRODUCT` — override harga per produk (lihat `price_book_item`)
- `ORDER_TYPE` — harga berbeda per tipe pesanan (lihat `price_book_item`)
- `CATEGORY` — adjust harga semua produk dalam kategori
- `WHOLESALE` — harga bertingkat berdasarkan qty (lihat `price_book_wholesale_tier`)

```sql
CREATE TABLE price_book (
    id               BIGSERIAL PRIMARY KEY,
    merchant_id      BIGINT NOT NULL,
    name             VARCHAR(255) NOT NULL,
    type             VARCHAR(20) NOT NULL DEFAULT 'PRODUCT',  -- PRODUCT|CATEGORY|WHOLESALE|ORDER_TYPE
    order_type_id    BIGINT,         -- untuk type=ORDER_TYPE
    category_id      BIGINT,         -- untuk type=CATEGORY
    adjustment_type  VARCHAR(20),    -- PERCENTAGE_OFF | AMOUNT_OFF | SPECIAL_PRICE (untuk CATEGORY)
    adjustment_value NUMERIC(38,2),  -- nilai penyesuaian (untuk CATEGORY)
    visibility       VARCHAR(20) NOT NULL DEFAULT 'ALL_OUTLET',
    is_default       BOOLEAN NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(255),
    created_date     TIMESTAMP,
    modified_by      VARCHAR(255),
    modified_date    TIMESTAMP
);
```

---

### `price_book_item` — Override Harga per Produk

Untuk `price_book.type = PRODUCT` dan `ORDER_TYPE`.

```sql
CREATE TABLE price_book_item (
    id            BIGSERIAL PRIMARY KEY,
    price_book_id BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL,
    price         NUMERIC(38,2) NOT NULL DEFAULT 0,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(255),
    created_date  TIMESTAMP,
    modified_by   VARCHAR(255),
    modified_date TIMESTAMP
);
```

**Contoh (Harga Dine In vs Take Away):**
```
price_book: id=1, name="Harga Dine In",  type=ORDER_TYPE, order_type_id=1
price_book: id=2, name="Harga Take Away", type=ORDER_TYPE, order_type_id=2

price_book_item: price_book_id=1, product_id=10, price=25000  ← dine in
price_book_item: price_book_id=2, product_id=10, price=22000  ← take away
```

---

### `price_book_wholesale_tier` — Tier Harga Grosir

Untuk `price_book.type = WHOLESALE`. Harga berbeda berdasarkan jumlah yang dibeli.

```sql
CREATE TABLE price_book_wholesale_tier (
    id            BIGSERIAL PRIMARY KEY,
    price_book_id BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL,
    min_qty       INT NOT NULL DEFAULT 1,
    max_qty       INT,              -- null = tidak ada batas atas
    price         NUMERIC(38,2) NOT NULL DEFAULT 0,
    display_order INT
);
```

**Contoh (Kopi Susu):**
```
min_qty=1, max_qty=2,    price=15000
min_qty=3, max_qty=5,    price=13000
min_qty=6, max_qty=null, price=11000
```

**Lookup saat transaksi:**
```sql
SELECT price FROM price_book_wholesale_tier
WHERE price_book_id = ? AND product_id = ?
  AND min_qty <= :qty AND (max_qty IS NULL OR max_qty >= :qty)
ORDER BY min_qty DESC LIMIT 1;
```

---

### `price_book_outlet` — Binding Price Book ↔ Outlet

Untuk `price_book.visibility = SPECIFIC_OUTLET`.

```sql
CREATE TABLE price_book_outlet (
    id            BIGSERIAL PRIMARY KEY,
    price_book_id BIGINT NOT NULL,
    outlet_id     BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL
);
```

---

### `product_variant_group` + `product_variant` — Varian Produk

```
Produk: Kopi Susu
  └── Variant Group: "Ukuran" (required)
        ├── Variant: "Regular"     additional_price=0
        ├── Variant: "Large"       additional_price=5000
        └── Variant: "Extra Large" additional_price=10000
```

```sql
CREATE TABLE product_variant_group (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    merchant_id   BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    is_required   BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(255), created_date TIMESTAMP,
    modified_by   VARCHAR(255), modified_date TIMESTAMP
);

CREATE TABLE product_variant (
    id               BIGSERIAL PRIMARY KEY,
    variant_group_id BIGINT NOT NULL,
    product_id       BIGINT NOT NULL,
    merchant_id      BIGINT NOT NULL,
    name             VARCHAR(255) NOT NULL,
    additional_price NUMERIC(38,2) NOT NULL DEFAULT 0,
    sku              VARCHAR(255),
    display_order    INT,
    is_active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by       VARCHAR(255), created_date TIMESTAMP,
    modified_by      VARCHAR(255), modified_date TIMESTAMP
);
```

**Kalkulasi:** `harga item = product.price + variant.additional_price`

---

### `product_modifier_group` + `product_modifier` — Modifier Produk

Berbeda dari varian: modifier bisa dipilih lebih dari satu dan bersifat optional.

```
Produk: Nasi Goreng
  └── Modifier Group: "Tambahan" (min=0, max=3)
        ├── Modifier: "Tambah Telur"   +3000
        ├── Modifier: "Tambah Kerupuk" +1000
        └── Modifier: "Extra Pedas"    +0
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
    created_by    VARCHAR(255), created_date TIMESTAMP,
    modified_by   VARCHAR(255), modified_date TIMESTAMP
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
    created_by        VARCHAR(255), created_date TIMESTAMP,
    modified_by       VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `transaction_modifier` — Snapshot Modifier dalam Transaksi

```sql
CREATE TABLE transaction_modifier (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_item_id BIGINT NOT NULL,
    modifier_id         BIGINT,          -- nullable: snapshot jika modifier dihapus
    modifier_name       VARCHAR(255),    -- snapshot nama saat transaksi
    additional_price    NUMERIC(38,2) NOT NULL DEFAULT 0,
    qty                 INT NOT NULL DEFAULT 1,
    created_by          VARCHAR(255),
    created_date        TIMESTAMP
);
```

---

### `discount` — Diskon Berbasis Kode (iSeller model)

Diskon diaktifkan dengan kode yang diinput kasir/pelanggan saat checkout.

```sql
CREATE TABLE discount (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    name                VARCHAR(255) NOT NULL,
    code                VARCHAR(255),     -- null = predefined tanpa kode
    value_type          VARCHAR(20) NOT NULL DEFAULT 'PERCENTAGE',  -- PERCENTAGE | AMOUNT
    value               NUMERIC(38,2) NOT NULL DEFAULT 0,
    max_discount_amount NUMERIC(38,2),    -- cap untuk value_type=PERCENTAGE
    min_purchase        NUMERIC(38,2),
    scope               VARCHAR(20) NOT NULL DEFAULT 'ALL',  -- ALL | PRODUCT | CATEGORY
    channel             VARCHAR(20) NOT NULL DEFAULT 'BOTH', -- POS | ONLINE | BOTH
    visibility          VARCHAR(20) NOT NULL DEFAULT 'ALL_OUTLET',
    usage_limit         INT,              -- null = tidak terbatas
    usage_per_customer  INT,
    start_date          TIMESTAMP,
    end_date            TIMESTAMP,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(255), created_date TIMESTAMP,
    modified_by         VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `discount_product` — Binding Diskon ↔ Produk

Untuk `discount.scope = PRODUCT`.

```sql
CREATE TABLE discount_product (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL
);
```

---

### `discount_category` — Binding Diskon ↔ Kategori

Untuk `discount.scope = CATEGORY`.

```sql
CREATE TABLE discount_category (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL
);
```

---

### `discount_outlet` — Binding Diskon ↔ Outlet

Untuk `discount.visibility = SPECIFIC_OUTLET`.

```sql
CREATE TABLE discount_outlet (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    outlet_id   BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL
);
```

---

### `discount_customer` — Eligibilitas Pelanggan untuk Diskon

Jika diisi, hanya pelanggan dalam daftar ini yang bisa menggunakan kode diskon.

```sql
CREATE TABLE discount_customer (
    id          BIGSERIAL PRIMARY KEY,
    discount_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    merchant_id BIGINT NOT NULL
);
```

---

### `discount_payment_method` — Diskon untuk Metode Bayar Tertentu

```sql
CREATE TABLE discount_payment_method (
    id                  BIGSERIAL PRIMARY KEY,
    discount_id         BIGINT NOT NULL,
    payment_method_code VARCHAR(255) NOT NULL,
    merchant_id         BIGINT NOT NULL
);
```

---

### `promotion` — Promosi Otomatis (iSeller model)

Diterapkan otomatis tanpa input kode saat kondisi cart terpenuhi.

Tipe promosi (`promo_type`):
- `DISCOUNT_BY_ORDER` — diskon ke total transaksi jika memenuhi `min_purchase`
- `BUY_X_GET_Y` — beli X item, dapatkan reward Y
- `DISCOUNT_BY_ITEM_SUBTOTAL` — diskon ke item tertentu jika subtotal item memenuhi threshold

```sql
CREATE TABLE promotion (
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    promo_type    VARCHAR(50) NOT NULL DEFAULT 'DISCOUNT_BY_ORDER',
    priority      INT NOT NULL DEFAULT 0,      -- angka lebih kecil = lebih prioritas
    can_combine   BOOLEAN NOT NULL DEFAULT FALSE,
    -- DISCOUNT_BY_ORDER / DISCOUNT_BY_ITEM_SUBTOTAL
    value_type    VARCHAR(20),                 -- PERCENTAGE | AMOUNT
    value         NUMERIC(38,2),
    min_purchase  NUMERIC(38,2),
    -- BUY_X_GET_Y
    buy_qty       INT,
    get_qty       INT,
    allow_multiple BOOLEAN DEFAULT FALSE,      -- reward berlipat per kelipatan buy_qty
    reward_type   VARCHAR(20),                 -- FREE | PERCENTAGE | AMOUNT | FIXED_PRICE
    reward_value  NUMERIC(38,2),
    buy_scope     VARCHAR(20),                 -- ALL | PRODUCT | CATEGORY
    reward_scope  VARCHAR(20),                 -- PRODUCT | CATEGORY
    -- Validity
    valid_days    VARCHAR(50),                 -- MON,TUE,WED,THU,FRI,SAT,SUN
    channel       VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    visibility    VARCHAR(20) NOT NULL DEFAULT 'ALL_OUTLET',
    start_date    TIMESTAMP,
    end_date      TIMESTAMP,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(255), created_date TIMESTAMP,
    modified_by   VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `promotion_product` — Produk Syarat Beli (BUY_X_GET_Y)

```sql
CREATE TABLE promotion_product (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL
);
```

---

### `promotion_reward_product` — Produk Reward (BUY_X_GET_Y)

```sql
CREATE TABLE promotion_reward_product (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL
);
```

---

### `promotion_outlet` — Binding Promosi ↔ Outlet

Untuk `promotion.visibility = SPECIFIC_OUTLET`.

```sql
CREATE TABLE promotion_outlet (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    outlet_id    BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL
);
```

---

### `promotion_customer` — Eligibilitas Pelanggan untuk Promosi

```sql
CREATE TABLE promotion_customer (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT NOT NULL,
    customer_id  BIGINT NOT NULL,
    merchant_id  BIGINT NOT NULL
);
```

---

### `voucher_brand` — Penerbit Voucher

```sql
CREATE TABLE voucher_brand (
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    logo_url      TEXT,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(255), created_date TIMESTAMP,
    modified_by   VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `voucher_group` — Grup Voucher dalam Brand

```sql
CREATE TABLE voucher_group (
    id                   BIGSERIAL PRIMARY KEY,
    brand_id             BIGINT NOT NULL,
    merchant_id          BIGINT NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    purchase_price       NUMERIC(38,2) NOT NULL DEFAULT 0,
    selling_price        NUMERIC(38,2) NOT NULL DEFAULT 0,
    expired_date         TIMESTAMP,
    valid_days           VARCHAR(50),   -- MON,TUE,WED,THU,FRI,SAT,SUN
    is_required_customer BOOLEAN NOT NULL DEFAULT FALSE,
    channel              VARCHAR(20) NOT NULL DEFAULT 'BOTH',
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_by           VARCHAR(255), created_date TIMESTAMP,
    modified_by          VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `voucher_usage` — Audit Trail Penggunaan Voucher

```sql
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

### `loyalty_program` — Konfigurasi Program Poin

```sql
CREATE TABLE loyalty_program (
    id                BIGSERIAL PRIMARY KEY,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,
    earn_mode         VARCHAR(20) NOT NULL DEFAULT 'RATIO',  -- RATIO | MULTIPLY
    points_per_amount NUMERIC(38,2) NOT NULL DEFAULT 0,      -- RATIO: setiap Rp X = 1 poin
    earn_multiplier   NUMERIC(38,10),                        -- MULTIPLY: total × multiplier = poin
    expiry_mode       VARCHAR(20) NOT NULL DEFAULT 'NONE',   -- NONE | ROLLING_DAYS | FIXED_DATE
    expiry_days       INT,
    expiry_date       TIMESTAMP,
    -- Legacy (deprecated, untuk backward compat)
    redeem_rate       NUMERIC(38,2) NOT NULL DEFAULT 0,
    min_redeem_points NUMERIC(38,2),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255), created_date TIMESTAMP,
    modified_by       VARCHAR(255), modified_date TIMESTAMP
);
```

**Contoh earn RATIO:** `pointsPerAmount=10000` → transaksi Rp75.000 = `floor(75000/10000)` = 7 poin

**Contoh earn MULTIPLY:** `earnMultiplier=0.001` → transaksi Rp75.000 = `floor(75000×0.001)` = 75 poin

---

### `loyalty_redemption_rule` — Aturan Tukar Poin

Satu program bisa punya beberapa rule (PAYMENT, DISCOUNT, FREE_PRODUCT).

```sql
CREATE TABLE loyalty_redemption_rule (
    id                  BIGSERIAL PRIMARY KEY,
    loyalty_program_id  BIGINT NOT NULL,
    merchant_id         BIGINT NOT NULL,
    type                VARCHAR(20) NOT NULL,  -- PAYMENT | DISCOUNT | FREE_PRODUCT
    -- PAYMENT: poin sebagai alat bayar
    redeem_rate         NUMERIC(38,2),         -- 1 poin = Rp X
    min_redeem_points   NUMERIC(38,2),
    max_redeem_points   NUMERIC(38,2),
    -- DISCOUNT: poin ditukar diskon
    required_points     NUMERIC(38,2),
    discount_type       VARCHAR(20),           -- PERCENTAGE | AMOUNT
    discount_value      NUMERIC(38,2),
    max_discount_amount NUMERIC(38,2),
    min_purchase        NUMERIC(38,2),
    -- FREE_PRODUCT: poin ditukar produk gratis
    reward_product_id   BIGINT,
    reward_qty          INT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_date        TIMESTAMP
);
```

---

### `loyalty_transaction` — Riwayat Mutasi Poin

```sql
CREATE TABLE loyalty_transaction (
    id             BIGSERIAL PRIMARY KEY,
    merchant_id    BIGINT NOT NULL,
    customer_id    BIGINT NOT NULL,
    transaction_id BIGINT,
    points         NUMERIC(38,2) NOT NULL DEFAULT 0,
    type           VARCHAR(30) NOT NULL DEFAULT 'EARN',
    -- EARN | REDEEM_PAYMENT | REDEEM_DISCOUNT | REDEEM_PRODUCT | EXPIRE | ADJUST
    note           TEXT,
    expiry_date    TIMESTAMP,    -- untuk rolling expiry: kapan poin ini kadaluarsa
    created_by     VARCHAR(255),
    created_date   TIMESTAMP
);
```

---

### `product_loyalty_setting` — Override Loyalty per Produk

```sql
CREATE TABLE product_loyalty_setting (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT NOT NULL UNIQUE,
    merchant_id        BIGINT NOT NULL,
    is_loyalty_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    fixed_points       NUMERIC(38,2),   -- null = ikuti global earn rate
    created_date       TIMESTAMP,
    modified_date      TIMESTAMP
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

**Flow:**
```
Kasir minta refund   → status=PENDING
Manager approve PIN  → status=APPROVED, transaction.refund_amount diupdate
```

---

### `receipt_template`

```sql
CREATE TABLE receipt_template (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    outlet_id           BIGINT,
    header              TEXT,
    footer              TEXT,
    show_tax            BOOLEAN NOT NULL DEFAULT TRUE,
    show_service_charge BOOLEAN NOT NULL DEFAULT TRUE,
    show_rounding       BOOLEAN NOT NULL DEFAULT TRUE,
    show_logo           BOOLEAN NOT NULL DEFAULT FALSE,
    logo_url            TEXT,
    show_queue_number   BOOLEAN NOT NULL DEFAULT TRUE,
    paper_size          VARCHAR(20),
    created_by          VARCHAR(255), created_date TIMESTAMP,
    modified_by         VARCHAR(255), modified_date TIMESTAMP
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
    -- GROSS | NET | NET_AFTER_TAX | NET_AFTER_TAX_SC
    product_type_filter VARCHAR(255),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    display_order       INT,
    created_by          VARCHAR(255), created_date TIMESTAMP,
    modified_by         VARCHAR(255), modified_date TIMESTAMP
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

---

### `mdr_setting` — MDR per Metode Pembayaran

```sql
CREATE TABLE mdr_setting (
    id                  BIGSERIAL PRIMARY KEY,
    merchant_id         BIGINT NOT NULL,
    payment_method_code VARCHAR(255) NOT NULL,
    percentage          NUMERIC(38,2) NOT NULL DEFAULT 0,
    flat_fee            NUMERIC(38,2) NOT NULL DEFAULT 0,
    charged_to          VARCHAR(20) NOT NULL DEFAULT 'MERCHANT',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(255), created_date TIMESTAMP,
    modified_by         VARCHAR(255), modified_date TIMESTAMP
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
    created_by      VARCHAR(255), created_date TIMESTAMP,
    modified_by     VARCHAR(255), modified_date TIMESTAMP
);
```

---

### `notification_setting`

```sql
CREATE TABLE notification_setting (
    id            BIGSERIAL PRIMARY KEY,
    merchant_id   BIGINT NOT NULL,
    email_address VARCHAR(255),
    is_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    notify_types  VARCHAR(255),  -- comma-separated: DAILY_SUMMARY,SETTLEMENT,SHIFT_CLOSE
    send_time     VARCHAR(5),    -- format HH:mm
    created_by    VARCHAR(255), created_date TIMESTAMP,
    modified_by   VARCHAR(255), modified_date TIMESTAMP
);
```

---

## C. Ringkasan Total Perubahan

| Kategori | Jumlah |
|---|---|
| Tabel existing yang dimodifikasi | 8 (`transaction`, `transaction_items`, `product`, `payment_setting`, `outlet`, `merchant`, `user_detail`, `voucher`) |
| Kolom nullable baru di tabel existing | 34 |
| Tabel baru dibuat | 38 |
| **Total tabel setelah migrasi** | **~75** |

---

## D. Checklist Deploy

- [ ] **Backup database** sebelum restart aplikasi
- [ ] Pastikan `spring.jpa.hibernate.ddl-auto=update` di `application.properties`
- [ ] Monitor log Hibernate saat startup — pastikan tidak ada error DDL
- [ ] Verifikasi kolom baru di tabel yang dimodifikasi:
  ```sql
  SELECT table_name, column_name, data_type
  FROM information_schema.columns
  WHERE table_name IN ('transaction','transaction_items','product','payment_setting',
                       'outlet','merchant','user_detail','voucher')
    AND column_name IN ('customer_id','order_type_id','shift_id','loyalty_points_earned',
                        'group_id','status','product_type','outlet_id','language_code','pin')
  ORDER BY table_name, ordinal_position;
  ```
- [ ] Verifikasi tabel baru terbentuk:
  ```sql
  SELECT table_name FROM information_schema.tables
  WHERE table_schema = 'public'
    AND table_name IN (
      'order_type','customer','cashier_shift',
      'price_book','price_book_item','price_book_wholesale_tier','price_book_outlet',
      'product_variant_group','product_variant',
      'product_modifier_group','product_modifier','transaction_modifier',
      'discount','discount_product','discount_category','discount_outlet',
      'discount_customer','discount_payment_method',
      'promotion','promotion_product','promotion_reward_product',
      'promotion_outlet','promotion_customer',
      'voucher_brand','voucher_group','voucher_usage',
      'loyalty_program','loyalty_redemption_rule','loyalty_transaction',
      'product_loyalty_setting',
      'refund','receipt_template','disbursement_rule','disbursement_log',
      'mdr_setting','printer_setting','notification_setting'
    )
  ORDER BY table_name;
  ```
- [ ] Test endpoint existing tidak terganggu (login, list product, create transaction)
- [ ] Test endpoint baru: `/pos/voucher`, `/pos/discount`, `/pos/promotion`, `/pos/price-book`, `/pos/customer`, `/pos/order-type`, `/pos/shift`, `/pos/loyalty`

---

## E. Data Migration (Jika Ada Data Lama)

### Voucher: set status pada kode lama

```sql
UPDATE voucher SET status = 'AVAILABLE' WHERE status IS NULL AND is_active = TRUE;
UPDATE voucher SET status = 'CANCELLED'  WHERE status IS NULL AND is_active = FALSE;
```

### Discount: set default kolom baru

```sql
UPDATE discount SET scope = 'ALL', channel = 'BOTH' WHERE scope IS NULL;
```
