# Loyalty Program Documentation

## 1. Konsep (Referensi: iSeller)

Program loyalty memberikan poin kepada pelanggan setiap kali bertransaksi. Poin yang terkumpul bisa ditukarkan sebagai **diskon** atau sebagai **metode pembayaran**.

Berdasarkan analisis iSeller, loyalty bekerja pada level **merchant** (omnichannel) — poin dari POS dan online store masuk ke pool yang sama.

---

## 2. Earn (Perolehan Poin)

### 2.1 Mode Kalkulasi

iSeller menyediakan dua mode, kita implementasi keduanya:

| Mode | Field | Cara Hitung |
|---|---|---|
| `RATIO` | `pointsPerAmount` | `floor(totalAmount / pointsPerAmount)` poin |
| `MULTIPLY` | `earnMultiplier` | `floor(totalAmount × earnMultiplier)` poin |

**Default: `RATIO`** — setiap Rp X mendapat 1 poin.

**Contoh RATIO:**
```
pointsPerAmount = 10.000
Transaksi = Rp 75.000
Poin = floor(75.000 / 10.000) = 7 poin
```

**Contoh MULTIPLY:**
```
earnMultiplier = 0.001
Transaksi = Rp 75.000
Poin = floor(75.000 × 0.001) = 75 poin
```

### 2.2 Product-Level Override

Setiap produk bisa dikonfigurasi secara individual (tabel `product_loyalty_setting`):

| Field | Keterangan |
|---|---|
| `isLoyaltyEnabled` | False = produk ini tidak menyumbang poin sama sekali |
| `fixedPoints` | Jika diset, produk ini selalu memberi nilai poin ini (abaikan rate global) |

**Prioritas resolusi:**
```
1. product_loyalty_setting.isLoyaltyEnabled = false → 0 poin untuk item ini
2. product_loyalty_setting.fixedPoints != null      → gunakan fixed points
3. Fallback ke global earn rate
```

### 2.3 Basis Kalkulasi Earn

Poin dihitung dari **netSubTotal** (setelah promo/diskon, sebelum tax & SC).

```
netSubTotal = grossSubTotal - promoAmount - discountAmount
earnedPoints = floor(netSubTotal / pointsPerAmount)   // mode RATIO
             = floor(netSubTotal * earnMultiplier)     // mode MULTIPLY
```

### 2.4 Poin Dikreditkan Kapan?

Poin dikreditkan **setelah transaksi selesai** (status `COMPLETED` / `PAID`). Bukan saat transaksi dibuat. Ini mencegah fraud redeem poin dari transaksi yang kemudian dibatalkan.

---

## 3. Redeem (Penukaran Poin)

iSeller menyediakan dua mode redeem yang dikonfigurasi terpisah:

### Mode A: Sebagai Diskon (Promosi)

Poin ditukar untuk mendapat potongan harga. Dikonfigurasi di `loyalty_redemption_rule` dengan `type = DISCOUNT`.

| Field Config | Keterangan |
|---|---|
| `requiredPoints` | Jumlah poin minimum yang harus ditukarkan |
| `discountType` | `PERCENTAGE` atau `AMOUNT` |
| `discountValue` | Nilai diskon |
| `minPurchase` | Minimum total transaksi agar bisa redeem |
| `maxDiscountAmount` | Cap maksimum diskon (untuk discountType=PERCENTAGE) |

**Contoh:**
```
Config: requiredPoints=50, discountType=PERCENTAGE, discountValue=10, maxDiscountAmount=20.000
→ 50 poin ditukar → diskon 10%, maks Rp 20.000
```

**Posisi dalam kalkulasi:** Mode ini mengurangi `netSubTotal` (mirip discount code) → mempengaruhi basis pajak.

### Mode B: Sebagai Payment Method

Poin dikonversi ke rupiah dan digunakan sebagai alat bayar. Dikonfigurasi di `loyalty_redemption_rule` dengan `type = PAYMENT`.

| Field Config | Keterangan |
|---|---|
| `redeemRate` | 1 poin = Rp X |
| `minRedeemPoints` | Minimum poin yang bisa diredeem |
| `maxRedeemPoints` | Maksimum poin yang bisa diredeem per transaksi (opsional) |

**Contoh:**
```
Config: redeemRate=100, minRedeemPoints=10
Customer punya 150 poin, mau redeem 100 poin
→ redeemValue = 100 × 100 = Rp 10.000 → dikurangi dari totalAmount
```

**Posisi dalam kalkulasi:** Sama seperti voucher — dikurangi dari `totalAmount`, **tidak mempengaruhi** basis pajak & SC.

### Mode C: Reward Produk Gratis

Poin ditukar dengan produk tertentu secara gratis. `type = FREE_PRODUCT`.

| Field Config | Keterangan |
|---|---|
| `requiredPoints` | Jumlah poin yang ditukar |
| `rewardProductId` | Produk yang diberikan gratis |
| `rewardQty` | Jumlah unit produk gratis |

---

## 4. Point Expiry

Poin bisa dikonfigurasi untuk kadaluarsa:

| Mode | Keterangan |
|---|---|
| `NONE` | Poin tidak pernah kadaluarsa |
| `ROLLING_DAYS` | Poin kadaluarsa N hari setelah diperoleh |
| `FIXED_DATE` | Poin kadaluarsa pada tanggal tertentu |

Sistem perlu scheduled job untuk mengecek dan mengekspirasi poin yang sudah lewat tanggalnya.

---

## 5. Schema

### `loyalty_program` (sudah ada, perlu update)

```sql
CREATE TABLE loyalty_program (
    id                BIGSERIAL PRIMARY KEY,
    merchant_id       BIGINT NOT NULL,
    name              VARCHAR(255) NOT NULL,

    -- Earn configuration
    earn_mode         VARCHAR(20) NOT NULL DEFAULT 'RATIO',   -- RATIO | MULTIPLY
    points_per_amount NUMERIC(38,2) NOT NULL DEFAULT 0,       -- untuk RATIO: setiap X rupiah = 1 poin
    earn_multiplier   NUMERIC(38,10),                         -- untuk MULTIPLY: total × multiplier = poin

    -- Expiry configuration
    expiry_mode       VARCHAR(20) NOT NULL DEFAULT 'NONE',    -- NONE | ROLLING_DAYS | FIXED_DATE
    expiry_days       INT,                                    -- untuk ROLLING_DAYS
    expiry_date       TIMESTAMP,                              -- untuk FIXED_DATE

    -- Legacy field (tetap untuk backward compat)
    redeem_rate       NUMERIC(38,2) NOT NULL DEFAULT 0,
    min_redeem_points NUMERIC(38,2),

    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_by        VARCHAR(255),
    created_date      TIMESTAMP,
    modified_by       VARCHAR(255),
    modified_date     TIMESTAMP
);
```

### `loyalty_redemption_rule` (baru)

```sql
CREATE TABLE loyalty_redemption_rule (
    id                  BIGSERIAL PRIMARY KEY,
    loyalty_program_id  BIGINT NOT NULL,
    merchant_id         BIGINT NOT NULL,

    -- DISCOUNT | PAYMENT | FREE_PRODUCT
    type                VARCHAR(20) NOT NULL,

    -- Untuk DISCOUNT
    required_points     NUMERIC(38,2),
    discount_type       VARCHAR(20),          -- PERCENTAGE | AMOUNT
    discount_value      NUMERIC(38,2),
    max_discount_amount NUMERIC(38,2),
    min_purchase        NUMERIC(38,2),

    -- Untuk PAYMENT
    redeem_rate         NUMERIC(38,2),        -- 1 poin = Rp X
    min_redeem_points   NUMERIC(38,2),
    max_redeem_points   NUMERIC(38,2),

    -- Untuk FREE_PRODUCT
    reward_product_id   BIGINT,
    reward_qty          INT,

    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_by          VARCHAR(255),
    created_date        TIMESTAMP
);
```

### `product_loyalty_setting` (baru)

```sql
CREATE TABLE product_loyalty_setting (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL UNIQUE,
    merchant_id         BIGINT NOT NULL,
    is_loyalty_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    fixed_points        NUMERIC(38,2),   -- null = gunakan global rate
    created_date        TIMESTAMP,
    modified_date       TIMESTAMP
);
```

### `loyalty_transaction` (sudah ada, cukup)

```sql
CREATE TABLE loyalty_transaction (
    id             BIGSERIAL PRIMARY KEY,
    merchant_id    BIGINT NOT NULL,
    customer_id    BIGINT NOT NULL,
    transaction_id BIGINT,
    points         NUMERIC(38,2) NOT NULL DEFAULT 0,
    type           VARCHAR(20) NOT NULL DEFAULT 'EARN',  -- EARN | REDEEM_PAYMENT | REDEEM_DISCOUNT | REDEEM_PRODUCT | EXPIRE | ADJUST
    note           TEXT,
    expiry_date    TIMESTAMP,   -- untuk rolling expiry: kapan poin ini kadaluarsa
    created_by     VARCHAR(255),
    created_date   TIMESTAMP
);
```

### `transaction` — kolom tambahan

```sql
ALTER TABLE transaction ADD COLUMN loyalty_points_earned   NUMERIC(38,2);  -- poin yang diperoleh
ALTER TABLE transaction ADD COLUMN loyalty_points_redeemed NUMERIC(38,2);  -- poin yang ditukarkan
ALTER TABLE transaction ADD COLUMN loyalty_redeem_amount   NUMERIC(38,2);  -- nilai rupiah dari redeem poin (mode PAYMENT)
```

---

## 6. Alur Transaksi dengan Loyalty

```
[1] Kasir pilih customer
       ↓
[2] Kasir tambah item ke cart
       ↓
[3] Hitung grossSubTotal (Layer 1: Price Book)
       ↓
[4] Hitung promoAmount (Layer 2: Promotion otomatis)
       ↓
[5] Kasir input kode diskon (opsional)
    Hitung discountAmount (Layer 3: Discount Code)
       ↓
[6] netSubTotal = grossSubTotal - promoAmount - discountAmount
       ↓
[7] Kasir aktifkan redeem poin (opsional)
    → Mode DISCOUNT: kurangi netSubTotal lebih lanjut
    → Mode PAYMENT:  simpan sebagai loyaltyRedeemAmount
       ↓
[8] Hitung tax + SC dari netSubTotal (atau netSubTotal setelah redeem diskon)
       ↓
[9] totalAmount = netSubTotal + tax + SC + rounding
       ↓
[10] Kurangi voucherAmount (payment instrument)
     Kurangi loyaltyRedeemAmount (mode PAYMENT)
       ↓
[11] amountDue = totalAmount - voucherAmount - loyaltyRedeemAmount
       ↓
[12] Transaksi selesai (PAID)
     → Kredit poin: earnedPoints ke customer.loyalty_points
     → Buat record loyalty_transaction (type=EARN)
     → Jika redeem: kurangi poin, buat record (type=REDEEM_*)
```

---

## 7. Kalkulasi Lengkap — Contoh

**Setup:**
- Loyalty: RATIO, pointsPerAmount = 10.000
- Redeem rule PAYMENT: redeemRate = 100 (1 poin = Rp 100)
- Customer punya 200 poin

**Cart:**
- Kopi Susu × 2 = Rp 60.000
- Croissant × 1 = Rp 25.000
- grossSubTotal = Rp 85.000

**Layer 1-3 (asumsi tidak ada promo/diskon):**
```
netSubTotal = 85.000
```

**Customer redeem 100 poin (mode PAYMENT):**
```
loyaltyRedeemAmount = 100 × 100 = 10.000
```

**Tax 11%, SC 5%:**
```
tax = 85.000 × 11% = 9.350     (dihitung dari netSubTotal, SEBELUM loyalty payment)
SC  = 85.000 × 5%  = 4.250
totalAmount = 85.000 + 9.350 + 4.250 = 98.600
```

**Setelah redeem:**
```
amountDue = 98.600 - 10.000 = 88.600
```

**Earn poin dari transaksi ini:**
```
earnedPoints = floor(85.000 / 10.000) = 8 poin
```

**Ringkasan mutasi poin customer:**
```
Sebelum: 200 poin
Redeem:  -100 poin
Earn:    +8 poin
Setelah: 108 poin
```

---

## 8. API Endpoints

### Program Konfigurasi
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/loyalty/list` | Semua program |
| GET | `/pos/loyalty/active` | Program yang aktif |
| GET | `/pos/loyalty/detail/{id}` | Detail program |
| POST | `/pos/loyalty/add` | Buat program |
| PUT | `/pos/loyalty/update` | Update program |
| DELETE | `/pos/loyalty/delete/{id}` | Hapus program |

### Redemption Rules
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/loyalty/{programId}/rules` | List aturan redeem |
| POST | `/pos/loyalty/{programId}/rules/add` | Tambah aturan redeem |
| PUT | `/pos/loyalty/{programId}/rules/update` | Update aturan |
| DELETE | `/pos/loyalty/{programId}/rules/{ruleId}` | Hapus aturan |

### Product Settings
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/loyalty/product-setting/{productId}` | Cek setting per produk |
| PUT | `/pos/loyalty/product-setting` | Set override per produk |

### Customer Points
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/customer/{customerId}/loyalty-history` | Riwayat poin |
| POST | `/pos/customer/loyalty/adjust` | Adjust manual |

### Di Transaksi
Field tambahan di `CreateTransactionRequest`:
```json
{
  "loyaltyRedeemPoints": 100,
  "loyaltyRedeemMode": "PAYMENT"
}
```

---

## 9. Gap Implementasi Saat Ini

| Fitur | Status | Prioritas |
|---|---|---|
| Program CRUD basic | ✅ Ada | — |
| Earn rate RATIO | ✅ Ada (pointsPerAmount) | — |
| Earn rate MULTIPLY | ❌ Belum | Sedang |
| Product-level override | ❌ Belum | Sedang |
| Redeem sebagai Payment | ❌ Belum | Tinggi |
| Redeem sebagai Diskon | ❌ Belum | Tinggi |
| Redeem produk gratis | ❌ Belum | Rendah |
| Point expiry | ❌ Belum | Sedang |
| Earn dikreditkan otomatis di TransactionService | ❌ Belum | Tinggi |
| Validasi redeem di TransactionService | ❌ Belum | Tinggi |

---

## 10. Roadmap

### Phase 1 (Sekarang — Basic)
- ✅ LoyaltyProgram CRUD
- ✅ CustomerService dengan poin manual
- ✅ LoyaltyTransaction log (EARN/REDEEM/ADJUST)

### Phase 2 (Align dengan iSeller)
- Tambah `earn_mode`, `earn_multiplier` ke `loyalty_program`
- Buat tabel `loyalty_redemption_rule`
- Buat tabel `product_loyalty_setting`
- Tambah `loyalty_points_earned`, `loyalty_points_redeemed`, `loyalty_redeem_amount` ke `transaction`
- Integrasikan earn poin di `TransactionService` (setelah transaksi PAID)
- Integrasikan redeem poin di `TransactionService` (saat checkout)

### Phase 3 (Advanced)
- Point expiry dengan scheduled job
- Tier/level membership (Bronze/Silver/Gold) — masing-masing punya earn multiplier berbeda
- Birthday bonus points
- Referral points
- Push notification saat poin mau kadaluarsa
