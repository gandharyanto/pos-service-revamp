# Penjelasan Lengkap: Modul Product & Transaction

## Daftar Isi
1. [Modul Product](#1-modul-product)
   - [Entity & Tabel](#11-entity--tabel)
   - [Relasi Antar Tabel](#12-relasi-antar-tabel)
   - [Alur Bisnis](#13-alur-bisnis)
   - [API Endpoints](#14-api-endpoints)
   - [Aturan Bisnis](#15-aturan-bisnis)
2. [Modul Transaction](#2-modul-transaction)
   - [Entity & Tabel](#21-entity--tabel)
   - [Relasi Antar Tabel](#22-relasi-antar-tabel)
   - [Alur Bisnis](#23-alur-bisnis)
   - [Kalkulasi Harga](#24-kalkulasi-harga)
   - [API Endpoints](#25-api-endpoints)
   - [Status Transaksi](#26-status-transaksi)
3. [Hubungan Product ↔ Transaction](#3-hubungan-product--transaction)

---

## 1. Modul Product

### 1.1 Entity & Tabel

#### Tabel `product`
Tabel utama yang menyimpan data produk milik merchant.

| Kolom | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `id` | bigint | ✓ | Primary key, auto increment |
| `merchant_id` | bigint | ✓ | FK → merchant. Produk terikat ke satu merchant |
| `merchant_unique_code` | varchar | - | Kode unik merchant (denormalisasi) |
| `name` | varchar | ✓ | Nama produk |
| `price` | decimal | ✓ | Harga jual utama |
| `base_price` | decimal | - | Harga dasar / modal |
| `sku` | varchar | - | Stock Keeping Unit — kode internal toko |
| `upc` | varchar | - | Universal Product Code — barcode |
| `image_url` | text | - | URL gambar ukuran penuh |
| `image_thumb_url` | text | - | URL gambar thumbnail |
| `description` | varchar | - | Deskripsi produk |
| `product_hash` | varchar | - | Hash untuk deteksi perubahan data |
| `stock_mode` | enum | - | Mode pengelolaan stok produk |
| `is_taxable` | boolean | - | Apakah produk dikenakan pajak |
| `tax_id` | bigint | - | FK → tax. Jenis pajak yang berlaku |
| `deleted_by` | varchar | - | Username yang menghapus (soft delete) |
| `deleted_date` | timestamp | - | Waktu soft delete. NULL = produk aktif |
| `created_by` | varchar | - | Audit: dibuat oleh |
| `created_date` | timestamp | - | Audit: waktu dibuat |
| `modified_by` | varchar | - | Audit: diubah oleh |
| `modified_date` | timestamp | - | Audit: waktu diubah |

> **Soft Delete:** Produk tidak pernah benar-benar dihapus dari database. Saat dihapus, field `deleted_date` diisi dengan timestamp sekarang. Semua query produk aktif menyertakan kondisi `deleted_date IS NULL`.

#### Tabel `stock`
Menyimpan jumlah stok terkini per produk. Relasi **1:1** dengan `product`.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `product_id` | bigint | FK → product, UNIQUE |
| `qty` | int | Jumlah stok saat ini |

> Satu produk hanya memiliki **satu** baris di tabel `stock`. Perubahan stok dicatat di `stock_movement`.

#### Tabel `stock_movement`
Riwayat setiap perubahan stok — bersifat append-only (tidak pernah diubah).

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `product_id` | bigint | FK → product |
| `merchant_id` | bigint | FK → merchant |
| `outlet_id` | bigint | FK → outlet (opsional) |
| `reference_id` | bigint | ID referensi (mis: transaction_id jika stok berkurang karena transaksi) |
| `qty` | int | Jumlah perubahan |
| `movement_type` | varchar | `ADD` / `SUBTRACT` / `SET` |
| `movement_reason` | varchar | Alasan perubahan stok |
| `note` | text | Catatan tambahan |

#### Tabel `category`
Kategori produk, per merchant.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `merchant_id` | bigint | FK → merchant |
| `name` | varchar | Nama kategori |
| `image` | text | URL gambar kategori |
| `description` | varchar | Deskripsi |

#### Tabel `product_categories`
Join table many-to-many antara `product` dan `category`.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `product_id` | bigint | FK → product |
| `category_id` | bigint | FK → category |

#### Tabel `product_outlet`
Harga dan ketersediaan produk **per outlet**. Mendukung skenario harga berbeda di outlet yang berbeda.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `product_id` | bigint | FK → product |
| `outlet_id` | bigint | FK → outlet |
| `outlet_price` | decimal | Harga khusus di outlet ini (override `product.price`) |
| `stock_qty` | int | Stok di outlet ini |
| `is_visible` | boolean | Produk tampil di outlet ini |
| `can_standalone` | boolean | Bisa dijual satuan |

#### Tabel `product_images`
Gambar tambahan per produk (selain `image_url` di tabel utama).

#### Tabel `product_histories`
Audit trail perubahan data produk — menyimpan snapshot sebelum dan sesudah perubahan.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `product_id` | bigint | FK → product |
| `change_type` | varchar | Tipe perubahan |
| `action` | enum | `CREATE` / `UPDATE` / `DELETE` |
| `before_snapshot` | json | Data produk sebelum diubah |
| `after_snapshot` | json | Data produk setelah diubah |

#### Tabel `product_archived`
Salinan data produk yang sudah dihapus permanen — untuk keperluan historis.

---

### 1.2 Relasi Antar Tabel

```
product
  │
  ├──[1:1]──► stock                  (stok saat ini)
  │
  ├──[1:N]──► stock_movement         (riwayat perubahan stok)
  │
  ├──[M:N]──► category               (via product_categories)
  │
  ├──[1:N]──► product_outlet         (harga & stok per outlet)
  │
  ├──[1:N]──► product_images         (gambar tambahan)
  │
  └──[1:N]──► product_histories      (audit trail perubahan)
```

---

### 1.3 Alur Bisnis

#### Menambah Produk Baru
```
POST /pos/product/add
  │
  ├─ Validasi: name wajib, price wajib
  │
  ├─ Ambil categories berdasarkan categoryIds (boleh kosong)
  │
  ├─ Simpan ke tabel product
  │   └─ merchantId diambil dari JWT (bukan dari request body)
  │
  └─ Buat entri awal di tabel stock
      └─ qty = nilai dari request.qty (default 0 jika tidak diisi)
```

#### Mengubah Produk
```
PUT /pos/product/update
  │
  ├─ Cari produk: WHERE id = productId AND merchant_id = {jwt.merchantId} AND deleted_date IS NULL
  │
  ├─ Update field: name, price, sku, upc, imageUrl, imageThumbUrl, description, categories
  │
  └─ Stok TIDAK diubah lewat endpoint ini — gunakan /pos/stock/update
```

#### Menghapus Produk (Soft Delete)
```
DELETE /pos/product/delete/{productId}
  │
  ├─ Cari produk (milik merchant, belum dihapus)
  │
  └─ Set: deleted_by = username, deleted_date = NOW()
      Data tetap ada di DB, tidak muncul di list/detail
```

#### Mengubah Stok
```
PUT /pos/stock/update
  │
  ├─ Cek produk milik merchant
  │
  ├─ Ambil/buat record stock
  │
  ├─ Hitung stok baru:
  │   ADD      → stock.qty + request.qty
  │   SUBTRACT → stock.qty - request.qty (error jika stok tidak cukup)
  │   SET      → request.qty (langsung set ke nilai tertentu)
  │
  ├─ Simpan stock baru
  │
  └─ Catat ke stock_movement (audit trail)
```

---

### 1.4 API Endpoints

#### `GET /pos/product/list`

**Query Parameters:**

| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|-----------|
| `page` | int | `0` | Halaman (0-based) |
| `size` | int | `20` | Jumlah item per halaman |
| `keyword` | string | - | Cari berdasarkan nama produk (case-insensitive, contains) |
| `categoryId` | long | - | Filter produk yang memiliki kategori ini |
| `sku` | string | - | Filter exact match SKU |
| `upc` | string | - | Filter exact match UPC |
| `startDate` | string | - | Filter `created_date >=`, format `yyyy-MM-dd` |
| `endDate` | string | - | Filter `created_date <=`, format `yyyy-MM-dd` |
| `sortBy` | string | `createdDate` | Field yang dijadikan urutan |
| `sortDir` | string | `DESC` | `ASC` atau `DESC` |

**Contoh Request:**
```
GET /pos/product/list?page=0&size=10&keyword=kopi&categoryId=3&sortBy=name&sortDir=ASC
Authorization: Bearer eyJ...
```

**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Kopi Susu",
        "price": 25000,
        "sku": "KPI-001",
        "upc": null,
        "imageUrl": "uploads/abc.jpg",
        "imageThumbUrl": "uploads/abc_thumb.jpg",
        "description": "Kopi susu segar",
        "stockQty": 50,
        "isTaxable": true,
        "taxId": 1,
        "categories": [
          { "id": 3, "name": "Minuman", "image": null, "description": null }
        ],
        "createdDate": "2026-03-29T10:00:00",
        "modifiedDate": "2026-03-29T10:00:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
}
```

> `stockQty` selalu disertakan di setiap item list — diambil dari tabel `stock` secara terpisah (N+1 query per produk di list). Untuk list dengan banyak produk, perlu dioptimasi ke batch query di masa depan.

---

#### `GET /pos/product/detail/{productId}`

**Contoh Request:**
```
GET /pos/product/detail/1
Authorization: Bearer eyJ...
```

**Response:** sama dengan satu objek di list di atas.

> Endpoint ini memverifikasi bahwa produk milik merchant yang terautentikasi dan belum dihapus.

---

#### `POST /pos/product/add`

**Request Body:**
```json
{
  "name": "Kopi Susu",
  "price": 25000,
  "sku": "KPI-001",
  "upc": null,
  "imageUrl": "uploads/abc.jpg",
  "imageThumbUrl": "uploads/abc_thumb.jpg",
  "description": "Kopi susu segar",
  "qty": 50,
  "categoryIds": [3, 5]
}
```

| Field | Wajib | Keterangan |
|-------|-------|-----------|
| `name` | ✓ | Minimal 1 karakter |
| `price` | ✓ | Tidak boleh null |
| `sku` | - | |
| `upc` | - | |
| `imageUrl` | - | URL dari hasil `/images/upload` |
| `imageThumbUrl` | - | URL thumbnail dari hasil `/images/upload` |
| `description` | - | |
| `qty` | - | Stok awal. Default `0` |
| `categoryIds` | - | List ID kategori. Boleh kosong `[]` |

**Response:** objek `ProductResponse` lengkap.

---

#### `PUT /pos/product/update`

**Request Body:**
```json
{
  "productId": 1,
  "name": "Kopi Susu Spesial",
  "price": 28000,
  "sku": "KPI-001",
  "upc": null,
  "imageUrl": "uploads/baru.jpg",
  "imageThumbUrl": "uploads/baru_thumb.jpg",
  "description": "Versi premium",
  "categoryIds": [3]
}
```

> `categoryIds` yang dikirim akan **menggantikan** seluruh kategori produk sebelumnya (full replace, bukan append). Kirim `[]` untuk menghapus semua kategori.

---

#### `DELETE /pos/product/delete/{productId}`

Tidak ada request body. Produk ditandai soft delete.

**Response:**
```json
{ "success": true, "message": "Product deleted", "data": null }
```

---

#### `PUT /pos/stock/update`

**Request Body:**
```json
{
  "productId": 1,
  "qty": 10,
  "updateType": "ADD"
}
```

| `updateType` | Keterangan | Contoh (stok awal 20) |
|---|---|---|
| `ADD` | Tambah stok | `qty=10` → stok jadi 30 |
| `SUBTRACT` | Kurangi stok | `qty=5` → stok jadi 15 |
| `SET` | Set stok ke nilai tertentu | `qty=100` → stok jadi 100 |

> `SUBTRACT` akan gagal dengan `400 Bad Request` jika stok yang tersedia kurang dari `qty` yang diminta.

---

#### `GET /pos/stock-movement/product/list`

**Query Parameters:**

| Parameter | Wajib | Keterangan |
|-----------|-------|-----------|
| `productId` | ✓ | ID produk |
| `startDate` | ✓ | Format `yyyy-MM-dd` |
| `endDate` | ✓ | Format `yyyy-MM-dd` |

**Response:**
```json
{
  "data": [
    {
      "id": 10,
      "productId": 1,
      "qty": 10,
      "movementType": "ADD",
      "movementReason": null,
      "note": null,
      "createdBy": "admin",
      "createdDate": "2026-03-29T09:00:00"
    }
  ]
}
```

---

### 1.5 Aturan Bisnis

| Aturan | Implementasi |
|--------|-------------|
| Produk terikat ke merchant | `merchantId` dari JWT, tidak bisa diubah client |
| Produk yang dihapus tidak tampil | Semua query wajib `deleted_date IS NULL` |
| Stok tidak boleh negatif | `SUBTRACT` dicek sebelum eksekusi |
| Setiap perubahan stok tercatat | `stock_movement` selalu diinsert |
| Kategori bersifat full replace | Update kategori mengganti semua, bukan menambah |

---

## 2. Modul Transaction

### 2.1 Entity & Tabel

#### Tabel `transaction`
Tabel utama transaksi POS.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `trx_id` | varchar | Kode unik format `TRX-XXXXXXXX` (8 karakter UUID) |
| `merchant_id` | bigint | FK → merchant |
| `outlet_id` | bigint | FK → outlet tempat transaksi |
| `customer_id` | bigint | FK → customer (nullable) |
| `order_type_id` | bigint | FK → order_type (nullable) |
| `cashier_shift_id` | bigint | FK → cashier_shift (nullable) |
| `username` | varchar | Username kasir yang melakukan transaksi |
| `status` | varchar | Status transaksi: `PAID` / `PENDING` / `CANCELLED` |
| `payment_method` | varchar | Metode pembayaran (CASH, QRIS, dll) |
| `price_include_tax` | boolean | Apakah harga di item sudah termasuk pajak |
| `sub_total` | decimal | Net subtotal setelah semua diskon/promo |
| `gross_sub_total` | decimal | Subtotal sebelum diskon/promo (Σ effectivePrice × qty) |
| `promo_amount` | decimal | Total diskon dari promosi (Layer 2) |
| `discount_amount` | decimal | Total diskon dari kode diskon (Layer 3) |
| `discount_code` | varchar | Kode diskon yang digunakan |
| `discount_id` | bigint | FK → discount yang digunakan |
| `voucher_id` | bigint | FK → voucher (tabel voucher) yang digunakan |
| `voucher_amount` | decimal | Nilai voucher yang dikreditkan |
| `loyalty_points_earned` | decimal | Poin loyalty yang diperoleh dari transaksi ini |
| `loyalty_points_redeemed` | decimal | Poin loyalty yang ditukar |
| `loyalty_redeem_amount` | decimal | Nilai rupiah dari poin yang ditukar |
| `total_service_charge` | decimal | Total service charge |
| `total_tax` | decimal | Total pajak |
| `total_rounding` | decimal | Total pembulatan |
| `total_amount` | decimal | Jumlah yang harus dibayar (final) |
| `cash_tendered` | decimal | Uang yang diberikan pelanggan (tunai) |
| `cash_change` | decimal | Kembalian |
| `tax_percentage` | decimal | % pajak yang berlaku saat transaksi |
| `tax_name` | varchar | Nama pajak (mis: "PPN") |
| `service_charge_percentage` | decimal | % service charge |
| `service_charge_amount` | decimal | Nominal service charge tetap |
| `rounding_type` | varchar | Tipe pembulatan |
| `rounding_target` | varchar | Target pembulatan |
| `queue_id` | bigint | FK → transaction_queue |
| `transaction_origin` | varchar | Asal transaksi (POS, ONLINE, dll) |
| `merchant_unique_code` | varchar | Denormalisasi dari merchant |

#### Tabel `transaction_items`
Item-item produk yang ada dalam satu transaksi. Relasi **N:1** ke `transaction`.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `transaction_id` | bigint | FK → transaction |
| `product_id` | bigint | FK → product (nullable — produk mungkin sudah dihapus) |
| `product_name` | varchar | Nama produk **snapshot** saat transaksi terjadi |
| `price` | decimal | Harga per unit **snapshot** |
| `qty` | int | Jumlah yang dibeli |
| `total_price` | decimal | `price × qty` |
| `product_snapshot` | text | JSON snapshot data produk lengkap |
| `tax_id` | bigint | FK → tax |
| `tax_name` | varchar | Nama pajak snapshot |
| `tax_percentage` | decimal | % pajak snapshot |
| `tax_amount` | decimal | Nominal pajak untuk item ini |

> **Kenapa snapshot?** Data produk (nama, harga) disimpan di item transaksi sehingga laporan historis tetap akurat meskipun produk sudah diubah atau dihapus setelah transaksi.

#### Tabel `payment`
Record pembayaran yang terhubung ke transaksi. Satu transaksi bisa memiliki **beberapa** record pembayaran (split payment atau retry).

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `transaction_id` | bigint | FK → transaction |
| `payment_trx_id` | varchar | ID transaksi dari payment gateway eksternal |
| `payment_method` | varchar | Metode pembayaran |
| `payment_source` | varchar | Sumber pembayaran |
| `amount_paid` | decimal | Jumlah yang dibayarkan |
| `status` | varchar | Status pembayaran |
| `is_effective` | boolean | Apakah pembayaran ini efektif/valid |
| `payment_reference` | varchar | Nomor referensi dari payment gateway |
| `payment_date` | timestamp | Waktu pembayaran diproses |
| `payment_snapshot` | text | JSON snapshot response payment gateway |

#### Tabel `transaction_queue`
Nomor antrian untuk setiap transaksi. Nomor antrian di-reset setiap hari per outlet.

| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| `id` | bigint | PK |
| `merchant_id` | bigint | FK → merchant |
| `outlet_id` | bigint | FK → outlet |
| `queue_number` | varchar | Format 3 digit: "001", "002", ... |
| `queue_date` | date | Tanggal antrian (reset harian) |
| `status` | varchar | `OPEN` |

---

### 2.2 Relasi Antar Tabel

```
transaction
  │
  ├──[1:N]──► transaction_items      (item produk yang dibeli)
  │
  ├──[1:N]──► payment                (record pembayaran)
  │
  └──[N:1]──► transaction_queue      (nomor antrian)

transaction_items
  └──[N:1]──► product                (boleh null jika produk dihapus)
```

---

### 2.3 Alur Bisnis

#### Membuat Transaksi Baru (Create)

```
POST /pos/transaction/create
  │
  ├─ [1] Tentukan outlet
  │   └─ Cari outlet default merchant (is_default = true)
  │       Jika tidak ada, ambil outlet pertama milik merchant
  │       Jika tidak ada outlet sama sekali → error 404
  │
  ├─ [2] Buat nomor antrian
  │   └─ Hitung jumlah antrian hari ini di outlet ini
  │       queueNumber = format "%03d" (jumlah + 1)
  │       Contoh: antrian ke-5 hari ini → "005"
  │       (Bisa di-override via request.queueNumber)
  │
  ├─ [3] Simpan transaction_queue
  │   └─ status = "OPEN"
  │
  ├─ [4] Generate trxId
  │   └─ Format: "TRX-" + 8 karakter UUID uppercase
  │       Contoh: "TRX-A1B2C3D4"
  │
  ├─ [5] Simpan transaction
  │   ├─ Semua nilai kalkulasi (subTotal, totalTax, dll) berasal dari request
  │   │   → Kalkulasi dilakukan di client/frontend, bukan di server
  │   └─ status = "PAID" (langsung)
  │
  └─ [6] Simpan transaction_items
      └─ Setiap item disimpan dengan snapshot nama & harga produk
```

> **Penting:** Status transaksi baru langsung `PAID`. Jika menggunakan pembayaran non-tunai (QRIS, kartu), perlu memanggil endpoint update setelah konfirmasi dari payment gateway.

#### Mengupdate Status Transaksi (Update Payment)

```
PUT /pos/transaction/update/{trxId}
  │
  ├─ Cari transaksi berdasarkan trxId milik merchant
  │
  ├─ Update transaction:
  │   └─ status = request.status
  │       payment_method = request.paymentMethod
  │
  └─ Insert record baru ke tabel payment:
      ├─ paymentTrxId (ID dari payment gateway)
      ├─ amountPaid
      ├─ status
      ├─ paymentReference
      ├─ paymentDate
      └─ isEffective = true
```

> Endpoint ini tidak mengubah item transaksi — hanya menambah payment record dan update status.

#### Melihat Detail Transaksi

```
GET /pos/transaction/detail/{transactionId}
  │
  ├─ Cari transaksi: WHERE id = transactionId AND merchant_id = {jwt.merchantId}
  │
  ├─ Query transaction_queue untuk nomor antrian
  │
  ├─ Query transaction_items untuk semua item
  │
  └─ Query payment untuk semua record pembayaran
```

---

### 2.4 Kalkulasi Harga

Kalkulasi dilakukan di **client** (frontend/mobile), kemudian nilai yang dihitung dikirim ke server. Server **memvalidasi** ulang semua nilai yang dikirim — jika ada ketidakcocokan, server mengembalikan HTTP **422** dengan detail field yang tidak cocok.

Kalkulasi mengikuti **5 layer** berurutan:

```
LAYER 1 — Price Book       → effectivePrice per item (override harga)
LAYER 2 — Promotion        → promoAmount (auto-apply, tidak perlu kode)
LAYER 3 — Discount Code    → discountAmount (input kode manual)
─────────────────────────────────────────────────────────────────
grossSubTotal = Σ(effectivePrice × qty)
netSubTotal   = grossSubTotal - promoAmount - discountAmount
─────────────────────────────────────────────────────────────────
LAYER 4 — Voucher          → voucherAmount (kurangi totalAmount, bukan netSubTotal)
LAYER 5 — Loyalty Redeem   → loyaltyRedeemAmount (kurangi totalAmount)
─────────────────────────────────────────────────────────────────
totalServiceCharge = netSubTotal × SC% (atau flat amount)
totalTax           = (netSubTotal + SC) × tax%
amountBeforeRound  = netSubTotal + SC + tax
totalRounding      = pembulatan ke target terdekat
totalAmount        = amountBeforeRound + rounding - voucherAmount - loyaltyRedeemAmount
cashChange         = cashTendered - totalAmount
```

> Lihat `docs/discount-simulation.md` untuk simulasi lengkap semua layer.

**Contoh Kalkulasi (tanpa diskon/loyalty):**

| Komponen | Nilai |
|----------|-------|
| Item: Kopi Susu × 2 @ Rp25.000 | Rp50.000 |
| Item: Roti Bakar × 1 @ Rp15.000 | Rp15.000 |
| **Gross Sub Total** | **Rp65.000** |
| Promo Amount | Rp0 |
| Discount Amount | Rp0 |
| **Net Sub Total** | **Rp65.000** |
| Service Charge 5% | Rp3.250 |
| Pajak PPN 11% | Rp7.150 |
| Rounding (ke 100 terdekat) | Rp0 |
| **Total Amount** | **Rp75.400** |
| Cash Tendered | Rp80.000 |
| **Cash Change** | **Rp4.600** |

---

### 2.5 API Endpoints

#### `GET /pos/transaction/list`

**Query Parameters:**

| Parameter | Wajib | Default | Keterangan |
|-----------|-------|---------|-----------|
| `startDate` | ✓ | - | Format `yyyy-MM-dd` |
| `endDate` | ✓ | - | Format `yyyy-MM-dd` |
| `page` | - | `0` | |
| `size` | - | `20` | |
| `sortBy` | - | `createdDate` | |
| `sortType` | - | `DESC` | `ASC` / `DESC` |

**Response (tiap item):**
```json
{
  "id": 1,
  "trxId": "TRX-A1B2C3D4",
  "status": "PAID",
  "paymentMethod": "CASH",
  "totalAmount": 75400,
  "createdDate": "2026-03-29T10:30:00"
}
```

---

#### `GET /pos/transaction/detail/{transactionId}`

**Response Lengkap:**
```json
{
  "data": {
    "transactionId": 1,
    "code": "TRX-A1B2C3D4",
    "paymentMethod": "CASH",
    "status": "PAID",
    "subTotal": 65000,
    "totalTax": 7150,
    "totalServiceCharge": 3250,
    "totalRounding": 0,
    "totalAmount": 75400,
    "cashTendered": 80000,
    "cashChange": 4600,
    "taxName": "PPN",
    "taxPercentage": 11.00,
    "serviceChargeAmount": null,
    "serviceChargePercentage": 5.00,
    "roundingTarget": null,
    "roundingType": null,
    "transactionDate": "2026-03-29T10:30:00",
    "queueNumber": "001",
    "transactionItems": [
      {
        "id": 1,
        "productId": 1,
        "productName": "Kopi Susu",
        "price": 25000,
        "qty": 2,
        "totalPrice": 50000,
        "taxName": "PPN",
        "taxPercentage": 11.00,
        "taxAmount": 5500
      },
      {
        "id": 2,
        "productId": 5,
        "productName": "Roti Bakar",
        "price": 15000,
        "qty": 1,
        "totalPrice": 15000,
        "taxName": "PPN",
        "taxPercentage": 11.00,
        "taxAmount": 1650
      }
    ],
    "payments": [
      {
        "id": 1,
        "paymentMethod": "CASH",
        "amountPaid": 75400,
        "status": "PAID",
        "paymentReference": null,
        "paymentDate": "2026-03-29T10:30:00"
      }
    ]
  }
}
```

---

#### `POST /pos/transaction/create`

**Request Body Lengkap:**
```json
{
  "subTotal": 65000,
  "grossSubTotal": 65000,
  "promoAmount": 0,
  "discountAmount": 0,
  "discountCode": null,
  "voucherCode": null,
  "voucherAmount": 0,
  "loyaltyRedeemPoints": null,
  "loyaltyRedeemMode": null,
  "loyaltyRedeemAmount": 0,
  "totalServiceCharge": 3250,
  "totalTax": 7150,
  "totalRounding": 0,
  "totalAmount": 75400,
  "paymentMethod": "CASH",
  "cashTendered": 80000,
  "cashChange": 4600,
  "priceIncludeTax": false,
  "customerId": null,
  "orderTypeId": null,
  "queueNumber": null,
  "transactionItems": [
    {
      "productId": 1,
      "productName": "Kopi Susu",
      "price": 25000,
      "qty": 2,
      "totalPrice": 50000,
      "discountAmount": 0,
      "taxId": 1,
      "taxAmount": 5500
    },
    {
      "productId": 5,
      "productName": "Roti Bakar",
      "price": 15000,
      "qty": 1,
      "totalPrice": 15000,
      "discountAmount": 0,
      "taxId": 1,
      "taxAmount": 1650
    }
  ]
}
```

| Field | Wajib | Keterangan |
|-------|-------|-----------|
| `subTotal` | ✓ | Net subtotal = grossSubTotal - promoAmount - discountAmount |
| `grossSubTotal` | ✓ | Σ(effectivePrice × qty) sebelum diskon/promo |
| `totalAmount` | ✓ | Jumlah final yang dibayar |
| `paymentMethod` | ✓ | Kode metode pembayaran |
| `promoAmount` | - | Total diskon dari promosi. Default `0` |
| `discountAmount` | - | Total diskon dari kode diskon. Default `0` |
| `discountCode` | - | Kode diskon yang digunakan |
| `voucherCode` | - | Kode voucher yang digunakan |
| `voucherAmount` | - | Nilai voucher (dikurangi dari totalAmount). Default `0` |
| `loyaltyRedeemPoints` | - | Jumlah poin yang ingin ditukar |
| `loyaltyRedeemMode` | - | `PAYMENT` (kurangi totalAmount) atau `DISCOUNT` (kurangi netSubTotal) |
| `loyaltyRedeemAmount` | - | Nilai rupiah dari poin yang ditukar. Default `0` |
| `totalServiceCharge` | - | Default `0` |
| `totalTax` | - | Default `0` |
| `totalRounding` | - | Default `0` |
| `cashTendered` | - | Wajib diisi jika pembayaran tunai |
| `cashChange` | - | Wajib diisi jika pembayaran tunai |
| `customerId` | - | ID customer (untuk loyalty earn/redeem) |
| `orderTypeId` | - | ID order type (untuk Price Book ORDER_TYPE) |
| `priceIncludeTax` | - | Default `false` |
| `queueNumber` | - | Jika `null`, sistem generate otomatis |
| `transactionItems` | ✓ | Minimal 1 item |

**Response:**
```json
{
  "data": {
    "id": 1,
    "trxId": "TRX-A1B2C3D4",
    "queueNumber": "001"
  }
}
```

---

#### `PUT /pos/transaction/update/{merchantTrxId}`

Path `{merchantTrxId}` = nilai `trxId` dari response create (contoh: `TRX-A1B2C3D4`).

**Use case:** digunakan setelah payment gateway (QRIS, kartu) mengembalikan konfirmasi pembayaran.

```json
{
  "paymentTrxId": "GW-TRX-98765",
  "paymentMethod": "QRIS",
  "amountPaid": 75400,
  "status": "PAID",
  "paymentReference": "REF-QRIS-12345",
  "paymentDate": "2026-03-29T10:31:00"
}
```

| Field | Wajib | Keterangan |
|-------|-------|-----------|
| `paymentMethod` | ✓ | |
| `amountPaid` | ✓ | |
| `status` | ✓ | `PAID` / `FAILED` / `CANCELLED` |
| `paymentTrxId` | - | ID dari payment gateway |
| `paymentReference` | - | Nomor referensi |
| `paymentDate` | - | Format ISO 8601: `yyyy-MM-ddTHH:mm:ss` |

**Response:**
```json
{ "success": true, "message": "Transaction updated", "data": null }
```

---

### 2.6 Status Transaksi

| Status | Keterangan | Kapan Terjadi |
|--------|-----------|---------------|
| `PAID` | Transaksi berhasil dibayar | Default saat create; atau setelah update konfirmasi |
| `PENDING` | Menunggu konfirmasi pembayaran | Bisa diset via update untuk non-tunai |
| `CANCELLED` | Transaksi dibatalkan | Diset via update |

> Saat ini transisi status tidak divalidasi di server — client bertanggung jawab mengirim status yang benar.

---

## 3. Hubungan Product ↔ Transaction

```
product.id ──────────────────► transaction_items.product_id
product.name ─── SNAPSHOT ──► transaction_items.product_name
product.price ── SNAPSHOT ──► transaction_items.price
tax.id ──────────────────────► transaction_items.tax_id
tax.name ──── SNAPSHOT ──────► transaction_items.tax_name
tax.percentage ─ SNAPSHOT ──► transaction_items.tax_percentage
```

**Kenapa snapshot dan bukan foreign key langsung?**

Karena di POS, data historis harus selalu akurat. Jika:
- Nama produk diubah setelah transaksi → laporan lama harus tetap menampilkan nama lama
- Harga produk naik → transaksi yang sudah terjadi tidak boleh berubah nilainya
- Produk dihapus (soft delete) → transaksi lama harus tetap bisa dilihat detail itemnya

Dengan menyimpan snapshot, integritas data historis terjaga tanpa bergantung pada data produk yang mungkin berubah.

---

## Ringkasan Dependency Antar Modul

```
[Product Module]                    [Transaction Module]
     │                                       │
     ├── product                    ┌─── transaction
     ├── category                   │       │
     ├── product_categories         ├─── transaction_items (snapshot dari product)
     ├── stock ◄─────────────────── │       └── FK product_id (nullable)
     ├── stock_movement             ├─── payment
     ├── product_outlet             └─── transaction_queue
     ├── product_images
     └── product_histories
```

**Catatan penting:** Modul Transaction membaca data produk **hanya saat transaksi dibuat** (untuk mengisi snapshot). Setelah itu, `transaction_items` tidak memiliki ketergantungan runtime ke modul Product.
