# Discount & Promotion System — Dokumentasi & Simulasi

Dokumen ini menjelaskan mekanisme diskon, promosi, dan price book secara lengkap untuk membantu implementasi aplikasi (frontend/mobile/POS client).

---

## Arsitektur 3 Layer

Kalkulasi diskon dieksekusi dalam urutan ini setiap kali transaksi dibuat:

```
Cart items (productId + qty)
       │
       ▼
┌──────────────────────────────┐
│  LAYER 1 — PRICE BOOK        │  Ubah harga satuan per item
│  (sebelum diskon/promosi)    │
└──────────────────────────────┘
       │ effectivePrice per item
       ▼
┌──────────────────────────────┐
│  LAYER 2 — PROMOTION         │  Auto-apply berdasarkan kondisi cart
│  (tidak perlu kode)          │
└──────────────────────────────┘
       │ promoAmount (total diskon promosi)
       ▼
┌──────────────────────────────┐
│  LAYER 3 — DISCOUNT CODE     │  Validasi & apply kode diskon manual
│  (kasir/pelanggan input kode)│
└──────────────────────────────┘
       │ discountAmount
       ▼
netSubTotal = grossSubTotal - promoAmount - discountAmount
       │
       ▼
Tax + Service Charge + Rounding → totalAmount
```

---

## Istilah Penting

| Field | Keterangan |
|---|---|
| `grossSubTotal` | Σ(effectivePrice × qty) — setelah price book, sebelum promo/diskon |
| `promoAmount` | Total diskon dari semua promosi yang berlaku |
| `discountAmount` | Total diskon dari kode diskon |
| `subTotal` (net) | `grossSubTotal - promoAmount - discountAmount` |
| `totalServiceCharge` | Dihitung dari `netSubTotal` |
| `totalTax` | Dihitung dari `netSubTotal + totalServiceCharge` |
| `totalAmount` | `netSubTotal + SC + tax + rounding` |

---

## LAYER 1 — Price Book

### Tipe Price Book

| type | Keterangan |
|---|---|
| `WHOLESALE` | Harga satuan berbeda per tier qty |
| `ORDER_TYPE` | Harga berbeda per tipe pesanan (Dine In, Take Away, dll) |
| `PRODUCT` | Override harga per produk |
| `CATEGORY` | Adjustment harga seluruh produk dalam kategori |

### Prioritas (jika beberapa berlaku)
`WHOLESALE` > `ORDER_TYPE` > `PRODUCT` > `CATEGORY` > harga default produk

### Category Adjustment Type

| adjustmentType | Rumus |
|---|---|
| `PERCENTAGE_OFF` | `price - (price × value / 100)` |
| `AMOUNT_OFF` | `price - value` |
| `SPECIAL_PRICE` | `value` (harga tetap) |

### Simulasi WHOLESALE

**Config:**
```
Price Book: type=WHOLESALE, product=Kopi Susu
  Tier 1: min_qty=1,  max_qty=2,  price=20.000
  Tier 2: min_qty=3,  max_qty=5,  price=18.000
  Tier 3: min_qty=6,  max_qty=null, price=15.000
```

| qty dibeli | Tier match | effectivePrice |
|---|---|---|
| 1 | Tier 1 | 20.000 |
| 2 | Tier 1 | 20.000 |
| 3 | Tier 2 | 18.000 |
| 5 | Tier 2 | 18.000 |
| 6 | Tier 3 | 15.000 |
| 10 | Tier 3 | 15.000 |

### Simulasi CATEGORY

**Config:**
```
Price Book: type=CATEGORY, category=Minuman
  adjustmentType=PERCENTAGE_OFF, adjustmentValue=10
```

Produk dalam kategori Minuman yang harga aslinya 25.000:
```
effectivePrice = 25.000 - (25.000 × 10 / 100) = 22.500
```

---

## LAYER 2 — Promotion

### Tipe Promosi

| promoType | Trigger | Diskon ke |
|---|---|---|
| `DISCOUNT_BY_ORDER` | total cart ≥ minPurchase | total transaksi |
| `DISCOUNT_BY_ITEM_SUBTOTAL` | subtotal item tertentu ≥ minPurchase | item-item tersebut |
| `BUY_X_GET_Y` | jumlah item tertentu ≥ buyQty | item reward |

### Stacking Rules

```
Dari semua promosi yang match:
  exclusive (canCombine=false) → ambil SATU dengan priority terkecil
  combinable (canCombine=true) → apply SEMUA

Contoh:
  Promo A: canCombine=false, priority=1 → MENANG dari exclusive
  Promo B: canCombine=false, priority=2 → GUGUR
  Promo C: canCombine=true,  priority=3 → TETAP APPLY
  Promo D: canCombine=true,  priority=5 → TETAP APPLY

Hasil: apply Promo A + Promo C + Promo D
```

### Filter Promosi yang Berlaku

Sebuah promosi dievaluasi HANYA jika semua kondisi ini terpenuhi:
1. `is_active = true`
2. `now >= start_date` (jika start_date tidak null)
3. `now <= end_date` (jika end_date tidak null)
4. Hari ini ada di `valid_days` (jika valid_days tidak null)
5. `channel = BOTH` atau `channel = POS`
6. `visibility = ALL_OUTLET` atau outlet ada di `promotion_outlet`
7. Customer ada di `promotion_customer` ATAU `promotion_customer` kosong

---

### Simulasi DISCOUNT_BY_ORDER

**Config:**
```
Promo: type=DISCOUNT_BY_ORDER, minPurchase=50.000
  valueType=PERCENTAGE, value=10
  canCombine=false, priority=1
```

| grossSubTotal | Syarat terpenuhi? | promoAmount |
|---|---|---|
| 45.000 | ✗ (< 50.000) | 0 |
| 50.000 | ✓ | 5.000 (10%) |
| 100.000 | ✓ | 10.000 (10%) |

---

### Simulasi DISCOUNT_BY_ITEM_SUBTOTAL

**Config:**
```
Promo: type=DISCOUNT_BY_ITEM_SUBTOTAL
  buyScope=PRODUCT, products=[Kopi Susu]
  minPurchase=50.000
  valueType=PERCENTAGE, value=15
  canCombine=true, priority=2
```

**Cart:**
- Kopi Susu × 3 @ 20.000 = 60.000
- Matcha Latte × 1 @ 25.000 = 25.000

```
Subtotal item qualify (Kopi Susu) = 60.000
60.000 >= 50.000 → ✓

Diskon = 60.000 × 15% = 9.000
Distribusi ke item: 9.000 (100% ke Kopi Susu)
```

**Cart dengan 2 produk qualify:**
- Kopi Susu × 2 @ 20.000 = 40.000
- Americano × 1 @ 25.000 = 25.000

```
Subtotal qualify (keduanya) = 65.000
Diskon total = 65.000 × 15% = 9.750
Distribusi proporsional:
  Kopi Susu: 9.750 × (40.000/65.000) = 6.000
  Americano: 9.750 × (25.000/65.000) = 3.750
```

---

### Simulasi BUY_X_GET_Y

#### Kasus A: Buy 2 Get 1 Free (item reward di cart)

**Config:**
```
Promo: type=BUY_X_GET_Y
  buyScope=PRODUCT, products=[Kopi Susu]
  buyQty=2, getQty=1
  rewardScope=PRODUCT, rewardProducts=[Kopi Susu]
  rewardType=FREE
  allowMultiple=false
  canCombine=true
```

**Cart: Kopi Susu × 3 @ 18.000**
```
qualifying qty = 3 ≥ buyQty=2 → ✓
multiples = 1 (allowMultiple=false)
totalRewardQty = 1 × 1 = 1

Reward: 1 unit Kopi Susu gratis = 18.000
itemDiscount[Kopi Susu] = 18.000
promoAmount = 18.000
```

#### Kasus B: Buy 2 Get 1 Free — dengan allowMultiple

**Config sama, allowMultiple=true**

**Cart: Kopi Susu × 6 @ 18.000**
```
qualifying qty = 6
multiples = floor(6 / 2) = 3
totalRewardQty = 3 × 1 = 3

Reward: 3 unit Kopi Susu gratis = 3 × 18.000 = 54.000
promoAmount = 54.000
```

#### Kasus C: Buy 3 Kopi, Get 1 Matcha 50% Off

**Config:**
```
buyScope=PRODUCT, products=[Kopi Susu], buyQty=3
rewardScope=PRODUCT, rewardProducts=[Matcha Latte]
rewardType=PERCENTAGE, rewardValue=50
```

**Cart: Kopi Susu × 3 @ 18.000, Matcha Latte × 1 @ 25.000**
```
qualifying qty Kopi Susu = 3 ≥ 3 → ✓
Reward: 1 Matcha Latte diskon 50% = 25.000 × 50% = 12.500
itemDiscount[Matcha Latte] = 12.500
```

#### Kasus D: Buy 3, Get 1 Fixed Price 5.000

```
rewardType=FIXED_PRICE, rewardValue=5.000
Item reward: Kopi Susu effectivePrice=18.000
discountPerUnit = 18.000 - 5.000 = 13.000
```

---

## LAYER 3 — Discount Code

### Validasi (urutan)

1. Kode ada & `is_active=true`
2. Dalam date range (`start_date` s/d `end_date`)
3. Channel cocok (`POS`/`ONLINE`/`BOTH`)
4. Outlet cocok (jika `visibility=SPECIFIC_OUTLET`)
5. Customer eligible (jika ada binding di `discount_customer`)
6. `grossSubTotal >= minPurchase` (cek terhadap subTotal **sebelum promosi**)
7. Scope check: item yang qualify sesuai `scope`

### Scope

| scope | Item yang didiskon |
|---|---|
| `ALL` | Seluruh transaksi |
| `PRODUCT` | Hanya produk yang ada di `discount_product` |
| `CATEGORY` | Hanya produk dari kategori yang ada di `discount_category` |

### Cap untuk PERCENTAGE

Jika `maxDiscountAmount` diisi, nilai diskon tidak bisa melebihi batas ini.

```
discount.valueType=PERCENTAGE, value=50, maxDiscountAmount=20.000
base=60.000 → kalkulasi = 30.000 → di-cap menjadi 20.000
```

### Simulasi Discount Code

#### Kasus A: Kode "SAVE10" — 10% semua item, min 100k

**Config:**
```
code=SAVE10, valueType=PERCENTAGE, value=10
minPurchase=100.000, scope=ALL
maxDiscountAmount=null
```

| grossSubTotal | Valid? | discountAmount |
|---|---|---|
| 80.000 | ✗ (< 100.000) | error |
| 100.000 | ✓ | 10.000 |
| 200.000 | ✓ | 20.000 |

#### Kasus B: Kode "KOPI5K" — 5.000 hanya untuk produk Kopi

**Config:**
```
code=KOPI5K, valueType=AMOUNT, value=5.000
scope=PRODUCT, discount_product=[Kopi Susu, Americano]
minPurchase=null
```

**Cart: Kopi Susu × 2 @ 18.000 = 36.000, Matcha × 1 @ 25.000 = 25.000**
```
Item qualify: Kopi Susu (36.000)
discountBase = 36.000
discountAmount = min(5.000, 36.000) = 5.000
Distribusi: 5.000 ke Kopi Susu
Matcha tidak terdampak
```

#### Kasus C: Kode "MINUMAN20" — 20% kategori Minuman, max 15.000

**Config:**
```
code=MINUMAN20, valueType=PERCENTAGE, value=20
scope=CATEGORY, discount_category=[Minuman]
maxDiscountAmount=15.000
```

**Cart: Minuman × total subtotal 90.000**
```
discountBase = 90.000
kalkulasi = 90.000 × 20% = 18.000
di-cap menjadi 15.000 (maxDiscountAmount)
discountAmount = 15.000
```

---

## Simulasi Gabungan (Full Flow)

### Skenario: Semua Layer Aktif

**Konfigurasi:**
- Price Book WHOLESALE: Kopi Susu qty ≥ 3 → 18.000 (normal 20.000)
- Promo 1 (DISCOUNT_BY_ORDER, canCombine=false, priority=1): total ≥ 50.000 → 10% off
- Promo 2 (BUY_X_GET_Y, canCombine=true, priority=2): beli 3 Kopi → 1 Kopi gratis
- Promo 3 (DISCOUNT_BY_ORDER, canCombine=false, priority=3): total ≥ 80.000 → 15% off
- Kode "SAVE5K": AMOUNT 5.000, min 60.000, scope=ALL
- PaymentSetting: SC 5%, Tax exclusive 11%, Rounding NEAREST 1.000

**Cart:** Kopi Susu × 3, Matcha Latte × 1 @ 25.000

```
LAYER 1 — Price Book
  Kopi Susu qty=3 → WHOLESALE tier match → 18.000
  Matcha Latte → no price book → 25.000
  grossSubTotal = (18.000×3) + 25.000 = 79.000

LAYER 2 — Promotions
  Promo 1: 79.000 ≥ 50.000 → match, canCombine=false, priority=1
  Promo 2: Kopi Susu qty=3 ≥ 3 → match, canCombine=true
  Promo 3: 79.000 ≥ 80.000? → ✗ tidak match

  Exclusive winner: Promo 1 (priority=1)
  Combinable: [Promo 2]

  Promo 1 (DISCOUNT_BY_ORDER 10%):
    diskon = 79.000 × 10% = 7.900 (ke total)

  Promo 2 (BUY_X_GET_Y, FREE):
    reward = 1 Kopi Susu gratis = 18.000 (ke item Kopi Susu)

  promoAmount = 7.900 + 18.000 = 25.900

LAYER 3 — Discount Code "SAVE5K"
  Validasi: grossSubTotal=79.000 ≥ minPurchase=60.000 → ✓
  discountBase = 79.000 (scope=ALL)
  discountAmount = min(5.000, 79.000) = 5.000

KALKULASI
  netSubTotal = 79.000 - 25.900 - 5.000 = 48.100

  SC 5%: 48.100 × 5% = 2.405
  taxBase: 48.100 + 2.405 = 50.505
  Tax 11% exclusive: 50.505 × 11% = 5.555,55 → 5.556
  amountBeforeRounding: 48.100 + 2.405 + 5.556 = 56.061
  Rounding NEAREST 1.000: remainder=61 < 500 → DOWN, delta=-61
  totalAmount = 56.061 - 61 = 56.000

HASIL:
  grossSubTotal    =  79.000
  promoAmount      = -25.900
  discountAmount   =  -5.000
  netSubTotal      =  48.100
  SC               =   2.405
  tax              =   5.556
  rounding         =     -61
  totalAmount      =  56.000
```

---

## Request Body ke Server

Client HARUS mengirim nilai yang sudah dikalkulasi agar server bisa memvalidasi:

```json
{
  "subTotal": 48100,
  "promoAmount": 25900,
  "discountAmount": 5000,
  "totalServiceCharge": 2405,
  "totalTax": 5556,
  "totalRounding": -61,
  "totalAmount": 56000,
  "paymentMethod": "CASH",
  "cashTendered": 60000,
  "cashChange": 4000,
  "orderTypeId": null,
  "customerId": null,
  "discountCode": "SAVE5K",
  "transactionItems": [
    {
      "productId": 42,
      "qty": 3,
      "price": 18000,
      "totalPrice": 54000,
      "discountAmount": 18000
    },
    {
      "productId": 17,
      "qty": 1,
      "price": 25000,
      "totalPrice": 25000,
      "discountAmount": null
    }
  ]
}
```

Jika nilai dari client tidak cocok dengan kalkulasi server → HTTP **422** dengan detail mismatch.

---

## Edge Cases

### 1. netSubTotal Negatif
Jika total diskon melebihi grossSubTotal, `netSubTotal` di-floor ke 0:
```
grossSubTotal = 20.000
promoAmount = 15.000
discountAmount = 10.000
netSubTotal = max(20.000 - 25.000, 0) = 0
totalAmount = 0 + SC + tax + rounding
```

### 2. Discount Code + Promosi Aktif Bersamaan
- Keduanya berlaku secara independen
- `min_purchase` discount code dicek terhadap `grossSubTotal` (sebelum promosi)
- Ini konsisten dengan iSeller behavior

### 3. BUY_X_GET_Y — Item Reward Tidak Ada di Cart
Jika item reward tidak ada di cart, diskon tidak bisa diberikan (reward tidak dimasukkan otomatis).
Server tidak menambahkan item baru ke cart secara otomatis.

### 4. Multiple Exclusive Promotions Tie (priority sama)
Jika dua promosi exclusive punya priority yang sama, yang diambil adalah yang lebih dulu ditemukan (berdasarkan `id` ascending). Sebaiknya merchant selalu memberi priority unik.

### 5. Price Book WHOLESALE + Discount Code
Price Book mengubah harga dasar, lalu diskon dihitung dari `effectivePrice` (bukan harga asli).
```
harga asli: 20.000
WHOLESALE tier: effectivePrice = 18.000
discount 10%: dihitung dari 18.000, bukan 20.000 → diskon = 1.800
```

---

## Tabel Relasi Singkat

```
discount
  ├── discount_product    (scope=PRODUCT: produk yang didiskon)
  ├── discount_category   (scope=CATEGORY: kategori yang didiskon)
  ├── discount_outlet     (visibility=SPECIFIC_OUTLET)
  └── discount_customer   (eligibilitas customer)

promotion
  ├── promotion_product         (buyScope: syarat beli)
  ├── promotion_reward_product  (rewardScope: item reward)
  ├── promotion_outlet          (visibility=SPECIFIC_OUTLET)
  └── promotion_customer        (eligibilitas customer)

price_book
  ├── price_book_item              (type=PRODUCT/ORDER_TYPE: harga per produk)
  ├── price_book_wholesale_tier    (type=WHOLESALE: tier qty)
  └── price_book_outlet            (visibility=SPECIFIC_OUTLET)
```
