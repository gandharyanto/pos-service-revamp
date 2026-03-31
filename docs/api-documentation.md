# API Documentation — POS Service Revamp

**Base URL:** `http://<host>/`
**Auth:** Semua endpoint kecuali `/pos/auth/login` dan `/images/upload` wajib menyertakan header:
```
Authorization: Bearer <token>
```

## Konvensi Naming

Dokumen API ini memakai **nama field request/response yang human-readable**. Persistence database mengikuti schema migration Phase 2. Mapping penting:

| Nama API / Bisnis | Nama Database |
|---|---|
| `grossSubTotal` / `grossRevenue` | `transaction.gross_amount` |
| `netSubTotal` / `netRevenue` | `transaction.net_amount` |
| `refundAmount` | `transaction.refund_amount` |
| `refundReason` | `transaction.refund_reason` |
| `refundBy` | `transaction.refund_by` |
| `refundDate` | `transaction.refund_date` |
| `voucher code` | tabel `voucher` |
| `loyalty history` | tabel `loyalty_transaction` |
| `printer` | tabel `printer_setting` |

Jika ada perbedaan istilah antara FSD, API doc, dan schema, kontrak HTTP mengikuti dokumen ini, sedangkan entity/repository mengikuti schema migration.

---

## Daftar Endpoint

| No | Method | URL | Deskripsi |
|----|--------|-----|-----------|
| 1  | POST   | `/pos/auth/login` | Login |
| 2  | GET    | `/pos/category/list` | List kategori |
| 3  | GET    | `/pos/category/detail/{categoryId}` | Detail kategori |
| 4  | POST   | `/pos/category/add` | Tambah kategori |
| 5  | PUT    | `/pos/category/update` | Update kategori |
| 6  | DELETE | `/pos/category/delete/{categoryId}` | Hapus kategori |
| 7  | GET    | `/pos/product/list` | List produk |
| 8  | GET    | `/pos/product/detail/{productId}` | Detail produk |
| 9  | POST   | `/pos/product/add` | Tambah produk |
| 10 | PUT    | `/pos/product/update` | Update produk |
| 11 | DELETE | `/pos/product/delete/{productId}` | Hapus produk (soft delete) |
| 12 | PUT    | `/pos/stock/update` | Update stok |
| 13 | GET    | `/pos/stock/movement` | Riwayat pergerakan stok |
| 14 | GET    | `/pos/payment-setting` | Get payment setting default (tanpa outlet) |
| 14a | GET   | `/pos/payment-setting/list` | Semua setting: default + per-outlet overrides |
| 14b | GET   | `/pos/payment-setting/outlet/{outletId}` | Setting efektif untuk outlet (fallback ke default) |
| 15 | POST   | `/pos/payment-setting/create` | Buat payment setting (default atau per-outlet) |
| 16 | PUT    | `/pos/payment-setting/update` | Update payment setting |
| 17 | GET    | `/pos/payment-method/merchant/list` | List metode pembayaran |
| **Tax** | | | |
| 75 | GET    | `/pos/tax/list` | List semua tarif pajak |
| 76 | GET    | `/pos/tax/detail/{taxId}` | Detail tarif pajak |
| 77 | POST   | `/pos/tax/add` | Tambah tarif pajak |
| 78 | PUT    | `/pos/tax/update` | Update tarif pajak |
| 79 | DELETE | `/pos/tax/delete/{taxId}` | Nonaktifkan tarif pajak |
| 18 | GET    | `/pos/transaction/list` | List transaksi |
| 19 | GET    | `/pos/transaction/detail/{transactionId}` | Detail transaksi |
| 20 | POST   | `/pos/transaction/create` | Buat transaksi |
| 21 | PUT    | `/pos/transaction/update/{merchantTrxId}` | Update transaksi / catat pembayaran |
| 22 | GET    | `/pos/summary-report/list` | Laporan ringkasan (legacy) |
| **Financial Report** | | | |
| 104 | GET   | `/pos/report/summary` | Ringkasan keuangan: gross, net, tax, SC, diskon, rounding, refund |
| 105 | GET   | `/pos/report/payment-method` | Breakdown per metode pembayaran |
| 106 | GET   | `/pos/report/top-products` | Top produk terlaris (qty + revenue) |
| 107 | GET   | `/pos/report/outlet` | Breakdown per outlet |
| 108 | GET   | `/pos/report/disbursement` | Rekapitulasi disbursement per layer/penerima |
| 23 | POST   | `/images/upload` | Upload gambar |
| **Customer** | | | |
| 24 | GET    | `/pos/customer/list` | List customer. `?phone=` / `?email=` untuk cari spesifik |
| 25 | GET    | `/pos/customer/detail/{customerId}` | Detail customer |
| 26 | POST   | `/pos/customer/add` | Tambah customer |
| 27 | PUT    | `/pos/customer/update` | Update customer |
| 28 | DELETE | `/pos/customer/delete/{customerId}` | Hapus customer (soft delete) |
| 29 | PUT    | `/pos/customer/loyalty/adjust` | Adjust poin loyalty manual |
| 30 | GET    | `/pos/customer/{customerId}/loyalty-history` | Riwayat transaksi poin |
| **Order Type** | | | |
| 31 | GET    | `/pos/order-type/list` | List order type |
| 32 | POST   | `/pos/order-type/add` | Tambah order type |
| 33 | PUT    | `/pos/order-type/update` | Update order type |
| 34 | DELETE | `/pos/order-type/delete/{id}` | Hapus order type |
| **Cashier Shift** | | | |
| 35 | POST   | `/pos/shift/open` | Buka shift kasir |
| 36 | PUT    | `/pos/shift/close` | Tutup shift kasir |
| 37 | GET    | `/pos/shift/list/{outletId}` | Riwayat shift. `?status=OPEN` untuk shift aktif |
| **Discount** | | | |
| 40 | GET    | `/pos/discount/list` | List diskon |
| 41 | GET    | `/pos/discount/detail/{id}` | Detail diskon |
| 42 | POST   | `/pos/discount/add` | Tambah diskon |
| 43 | PUT    | `/pos/discount/update` | Update diskon |
| 44 | DELETE | `/pos/discount/delete/{id}` | Hapus diskon |
| 45 | POST   | `/pos/discount/validate` | Validasi kode diskon |
| **Promotion** | | | |
| 46 | GET    | `/pos/promotion/list` | List promosi |
| 47 | GET    | `/pos/promotion/detail/{id}` | Detail promosi |
| 48 | POST   | `/pos/promotion/add` | Tambah promosi |
| 49 | PUT    | `/pos/promotion/update` | Update promosi |
| 50 | DELETE | `/pos/promotion/delete/{id}` | Hapus promosi |
| **Price Book** | | | |
| 51 | GET    | `/pos/price-book/list` | List price book |
| 52 | GET    | `/pos/price-book/detail/{id}` | Detail price book |
| 53 | POST   | `/pos/price-book/add` | Tambah price book |
| 54 | PUT    | `/pos/price-book/update` | Update price book |
| 55 | DELETE | `/pos/price-book/delete/{id}` | Hapus price book |
| **Voucher** | | | |
| 56 | GET    | `/pos/voucher/brand/list` | List brand beserta groups-nya (embed) |
| 57 | GET    | `/pos/voucher/brand/detail/{id}` | Detail brand beserta groups-nya |
| 58 | POST   | `/pos/voucher/brand/add` | Tambah voucher brand |
| 59 | PUT    | `/pos/voucher/brand/update` | Update voucher brand |
| 60 | DELETE | `/pos/voucher/brand/delete/{id}` | Hapus voucher brand |
| 61 | POST   | `/pos/voucher/group/add` | Tambah group ke brand |
| 62 | PUT    | `/pos/voucher/group/update` | Update voucher group |
| 63 | DELETE | `/pos/voucher/group/delete/{id}` | Hapus voucher group |
| 64 | GET    | `/pos/voucher/code/list?groupId=` | List kode voucher per group |
| 65 | POST   | `/pos/voucher/code/add` | Tambah kode voucher |
| 66 | POST   | `/pos/voucher/code/bulk-import` | Import kode voucher massal |
| 67 | PUT    | `/pos/voucher/code/cancel/{voucherId}` | Batalkan voucher |
| **Loyalty** | | | |
| 68 | GET    | `/pos/loyalty/list` | List program beserta rules-nya. `?isActive=true` untuk aktif |
| 69 | GET    | `/pos/loyalty/detail/{id}` | Detail program beserta rules-nya |
| 70 | POST   | `/pos/loyalty/add` | Tambah program (beserta rules dalam body) |
| 71 | PUT    | `/pos/loyalty/update` | Update program + full-replace rules |
| 72 | DELETE | `/pos/loyalty/delete/{id}` | Hapus program |
| 73 | GET    | `/pos/loyalty/product-setting/{productId}` | Get loyalty setting per produk |
| 74 | PUT    | `/pos/loyalty/product-setting` | Set loyalty setting per produk |
| **Cashier Management** | | | |
| 80 | GET    | `/pos/cashier/list` | List kasir merchant |
| 81 | GET    | `/pos/cashier/detail/{cashierId}` | Detail kasir |
| 82 | POST   | `/pos/cashier/add` | Tambah kasir baru |
| 83 | PUT    | `/pos/cashier/update` | Update data kasir |
| 84 | DELETE | `/pos/cashier/delete/{cashierId}` | Nonaktifkan kasir |
| 85 | PUT    | `/pos/cashier/set-pin` | Set PIN kasir untuk otorisasi refund |
| 86 | PUT    | `/pos/cashier/reset-password` | Reset password kasir |
| **Receipt Template** | | | |
| 87 | GET    | `/pos/receipt-template/list` | List template struk |
| 88 | GET    | `/pos/receipt-template/detail/{receiptId}` | Detail template |
| 89 | GET    | `/pos/receipt-template/outlet/{outletId}` | Template efektif untuk outlet (fallback ke default) |
| 90 | POST   | `/pos/receipt-template/add` | Buat template struk |
| 91 | PUT    | `/pos/receipt-template/update` | Update template |
| 92 | DELETE | `/pos/receipt-template/delete/{receiptId}` | Hapus template |
| **Printer Settings** | | | |
| 93 | GET    | `/pos/printer/list` | List printer. `?outletId=` untuk filter per outlet |
| 94 | GET    | `/pos/printer/detail/{printerId}` | Detail printer |
| 95 | POST   | `/pos/printer/add` | Tambah printer |
| 96 | PUT    | `/pos/printer/update` | Update printer |
| 97 | DELETE | `/pos/printer/delete/{printerId}` | Hapus printer |

Catatan sinkronisasi resource:

- Resource `voucher/code` pada API dipersist ke tabel `voucher` pada schema migration.
- Resource `customer/{id}/loyalty-history` pada API dipersist ke tabel `loyalty_transaction`.
- Resource `printer` pada API dipersist ke tabel `printer_setting`.

| **Disbursement (Revenue Sharing)** | | | |
| 98  | GET    | `/pos/disbursement/rule/list` | List aturan disbursement. `?activeOnly=true` |
| 99  | GET    | `/pos/disbursement/rule/detail/{ruleId}` | Detail aturan |
| 100 | POST   | `/pos/disbursement/rule/add` | Tambah aturan disbursement |
| 101 | PUT    | `/pos/disbursement/rule/update` | Update aturan |
| 102 | DELETE | `/pos/disbursement/rule/delete/{ruleId}` | Nonaktifkan aturan |
| 103 | GET    | `/pos/disbursement/log/list` | Log disbursement per transaksi. `?startDate=&endDate=` |

---

## Response Wrapper

Semua response dibungkus dengan format:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... }
}
```

**Error:**
```json
{
  "success": false,
  "message": "Pesan error",
  "data": null
}
```

**HTTP Status yang digunakan:**

| Status | Keterangan |
|--------|-----------|
| 200 | Berhasil |
| 400 | Bad request / validasi gagal / business rule violation |
| 401 | Token tidak valid / tidak ada |
| 404 | Resource tidak ditemukan |
| 422 | Amount mismatch (khusus create transaction) |
| 500 | Internal server error |

---

## 1. Auth

### POST `/pos/auth/login`

Tidak memerlukan Authorization header.

**Request Body:**
```json
{
  "username": "kasir01",
  "password": "password123"
}
```

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "posToken": "eyJhbGciOiJIUzI1NiJ9...",
    "posKey": "MCH-ABC123"
  }
}
```

| Field | Keterangan |
|-------|-----------|
| `token` | JWT utama — berisi `username` + `merchantId`. Dipakai di header `Authorization`. |
| `posToken` | JWT khusus POS — berisi `merchantId` saja. |
| `posKey` | `merchantUniqueCode` dari tabel `merchant`. |

**Error 400:** Username tidak terdaftar sebagai user merchant.
**Error 401:** Password salah.

---

## 2. Category

### GET `/pos/category/list`

**Query Params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `page` | int | 0 | Halaman (0-based) |
| `size` | int | 20 | Jumlah per halaman |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Minuman",
        "image": "https://...",
        "description": "Kategori minuman"
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "number": 0
  }
}
```

---

### GET `/pos/category/detail/{categoryId}`

**Path Param:** `categoryId` (Long)

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Minuman",
    "image": "https://...",
    "description": "Kategori minuman"
  }
}
```

---

### POST `/pos/category/add`

**Request Body:**
```json
{
  "name": "Makanan",
  "image": "https://cdn.example.com/food.jpg",
  "description": "Kategori makanan"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `name` | String | Ya | Nama kategori |
| `image` | String | Tidak | URL gambar |
| `description` | String | Tidak | Deskripsi |

**Response 200:** Data kategori yang baru dibuat.

---

### PUT `/pos/category/update`

**Request Body:**
```json
{
  "categoryId": 1,
  "name": "Makanan Berat",
  "image": "https://...",
  "description": "Update deskripsi"
}
```

**Response 200:** Data kategori yang telah diupdate.

---

### DELETE `/pos/category/delete/{categoryId}`

**Path Param:** `categoryId` (Long)

**Response 200:**
```json
{
  "success": true,
  "message": "Category deleted",
  "data": null
}
```

---

## 3. Product

### GET `/pos/product/list`

**Query Params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `page` | int | 0 | Halaman |
| `size` | int | 20 | Jumlah per halaman |
| `keyword` | String | null | Pencarian nama produk (case-insensitive) |
| `categoryId` | Long | null | Filter berdasarkan kategori |
| `sku` | String | null | Filter berdasarkan SKU |
| `upc` | String | null | Filter berdasarkan UPC |
| `startDate` | String | null | Filter tanggal dibuat dari (format: `YYYY-MM-DD`) |
| `endDate` | String | null | Filter tanggal dibuat sampai (format: `YYYY-MM-DD`) |
| `sortBy` | String | `createdDate` | Field untuk pengurutan |
| `sortDir` | String | `DESC` | Arah pengurutan (`ASC` / `DESC`) |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 10,
        "name": "Es Teh Manis",
        "price": 5000.00,
        "sku": "ETM-001",
        "upc": null,
        "imageUrl": "https://...",
        "imageThumbUrl": "https://...",
        "description": null,
        "stockQty": 100,
        "isTaxable": true,
        "taxId": 2,
        "categories": [
          { "id": 1, "name": "Minuman", "image": null, "description": null }
        ],
        "createdDate": "2025-01-01T10:00:00",
        "modifiedDate": "2025-01-10T08:30:00"
      }
    ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0
  }
}
```

---

### GET `/pos/product/detail/{productId}`

**Path Param:** `productId` (Long)

**Response 200:** Objek produk sama seperti item di list. Produk yang sudah dihapus (soft-delete) akan mengembalikan 404.

---

### POST `/pos/product/add`

**Request Body:**
```json
{
  "name": "Nasi Goreng",
  "price": 25000,
  "sku": "NG-001",
  "upc": null,
  "imageUrl": "https://...",
  "imageThumbUrl": "https://...",
  "description": "Nasi goreng spesial",
  "qty": 50,
  "categoryIds": [1, 3]
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `name` | String | Ya | Nama produk |
| `price` | BigDecimal | Ya | Harga jual |
| `sku` | String | Tidak | Stock Keeping Unit |
| `upc` | String | Tidak | Barcode |
| `imageUrl` | String | Tidak | URL gambar full |
| `imageThumbUrl` | String | Tidak | URL gambar thumbnail |
| `description` | String | Tidak | Deskripsi |
| `qty` | Int | Tidak (default: 0) | Stok awal |
| `categoryIds` | List\<Long\> | Tidak | ID kategori yang ditautkan |

> Saat produk dibuat, record stok awal otomatis dibuat di tabel `stock` dengan nilai `qty` dari request.

**Response 200:** Data produk yang baru dibuat termasuk `stockQty`.

---

### PUT `/pos/product/update`

**Request Body:**
```json
{
  "productId": 10,
  "name": "Nasi Goreng Special",
  "price": 28000,
  "sku": "NG-001",
  "upc": null,
  "imageUrl": "https://...",
  "imageThumbUrl": "https://...",
  "description": "Update deskripsi",
  "categoryIds": [1]
}
```

> Update produk tidak mengubah stok. Gunakan endpoint `/pos/stock/update` untuk mengubah stok.

**Response 200:** Data produk yang telah diupdate.

---

### DELETE `/pos/product/delete/{productId}`

**Path Param:** `productId` (Long)

Produk **tidak dihapus secara fisik** dari database. Field `deleted_by` dan `deleted_date` diisi (soft-delete). Produk yang sudah dihapus tidak akan muncul di list maupun detail, dan tidak bisa ditambahkan ke transaksi baru.

**Response 200:**
```json
{
  "success": true,
  "message": "Product deleted",
  "data": null
}
```

---

## 4. Stock

### PUT `/pos/stock/update`

**Request Body:**
```json
{
  "productId": 10,
  "qty": 20,
  "updateType": "ADD"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `productId` | Long | Ya | ID produk |
| `qty` | Int | Ya | Jumlah perubahan stok |
| `updateType` | Enum | Ya | `ADD` / `SUBTRACT` / `SET` |

**Logika `updateType`:**

| Tipe | Efek | Contoh (stok awal = 50) |
|------|------|------------------------|
| `ADD` | `stok + qty` | qty=20 → stok jadi 70 |
| `SUBTRACT` | `stok - qty` | qty=10 → stok jadi 40. Error jika stok < qty. |
| `SET` | `stok = qty` | qty=100 → stok jadi 100 |

Setiap perubahan stok dicatat di tabel `stock_movement`.

**Response 200:**
```json
{
  "success": true,
  "message": "Stock updated",
  "data": null
}
```

**Error 400:** Stok tidak mencukupi untuk tipe `SUBTRACT`.

---

### GET `/pos/stock/movement`

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `productId` | Long | Ya | ID produk |
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 5,
      "productId": 10,
      "qty": 20,
      "movementType": "ADD",
      "movementReason": null,
      "note": null,
      "createdBy": "kasir01",
      "createdDate": "2025-03-01T09:00:00"
    }
  ]
}
```

---

## 5. Payment Setting

Satu merchant bisa memiliki lebih dari satu record payment setting:
- **Default** (`outletId = null`) — berlaku untuk semua outlet yang tidak punya override.
- **Per-outlet override** (`outletId` terisi) — berlaku khusus untuk outlet tersebut.

### GET `/pos/payment-setting`

Mengembalikan setting **default** (tanpa outlet).

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "paymentSettingId": 1,
    "outletId": null,
    "isPriceIncludeTax": false,
    "isRounding": true,
    "roundingTarget": 100,
    "roundingType": "NEAREST",
    "isServiceCharge": true,
    "serviceChargePercentage": 5.00,
    "serviceChargeAmount": null,
    "serviceChargeSource": "AFTER_DISCOUNT"
  }
}
```

---

### GET `/pos/payment-setting/list`

Mengembalikan **semua** setting merchant: default + seluruh per-outlet overrides.

**Response 200:** Array of payment setting objects.

---

### GET `/pos/payment-setting/outlet/{outletId}`

Mengembalikan setting **efektif** untuk outlet tertentu. Jika override tidak ada, fallback ke default.

**Path Param:** `outletId` (Long)

**Response 200:** Satu payment setting object.

---

### POST `/pos/payment-setting/create`

Satu record per scope (`outletId`). Jika sudah ada untuk scope yang sama, gunakan update.

**Request Body:**
```json
{
  "outletId": null,
  "isPriceIncludeTax": false,
  "isRounding": true,
  "roundingTarget": 100,
  "roundingType": "NEAREST",
  "isServiceCharge": true,
  "serviceChargePercentage": 5.00,
  "serviceChargeAmount": null,
  "serviceChargeSource": "AFTER_DISCOUNT"
}
```

| Field | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `outletId` | Long | null | Null = berlaku semua outlet (default). Isi untuk per-outlet override. |
| `isPriceIncludeTax` | Boolean | false | Harga produk sudah termasuk pajak (include tax) |
| `isRounding` | Boolean | false | Aktifkan pembulatan |
| `roundingTarget` | Int | null | Target pembulatan (mis: `100`, `500`, `1000`) |
| `roundingType` | String | null | `UP` / `DOWN` / `NEAREST` |
| `isServiceCharge` | Boolean | false | Aktifkan service charge |
| `serviceChargePercentage` | BigDecimal | null | Persentase SC (%). Jika diisi, `serviceChargeAmount` diabaikan. |
| `serviceChargeAmount` | BigDecimal | null | SC nominal tetap. Dipakai jika `serviceChargePercentage` null. |
| `serviceChargeSource` | String | null | Basis kalkulasi SC: `BEFORE_TAX` \| `AFTER_TAX` \| `DPP` \| `AFTER_DISCOUNT` |

> **Catatan:** Konfigurasi pajak (tarif, nama) dikelola terpisah melalui endpoint `/pos/tax`. `isPriceIncludeTax` di sini hanya menentukan apakah harga produk sudah include pajak atau belum (exclude).

**Response 200:** Data payment setting yang baru dibuat.
**Error 400:** Payment setting sudah ada untuk scope ini.

---

### PUT `/pos/payment-setting/update`

**Request Body:** Sama seperti create, tambah field `paymentSettingId`:
```json
{
  "paymentSettingId": 1,
  "isPriceIncludeTax": false,
  "serviceChargeSource": "AFTER_DISCOUNT",
  ...
}
```

**Response 200:** Data payment setting yang telah diupdate.

---

## 5a. Tax

Tarif pajak yang dapat ditetapkan ke produk via `product.taxId`. Berbeda dari `payment_setting` yang mengatur pajak global — tabel ini untuk pajak bernama yang di-assign per produk.

### GET `/pos/tax/list`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "name": "PPN",
      "percentage": 11.00,
      "isActive": true,
      "isDefault": true
    },
    {
      "id": 2,
      "name": "PPN Bebas",
      "percentage": 0.00,
      "isActive": true,
      "isDefault": false
    }
  ]
}
```

---

### GET `/pos/tax/detail/{taxId}`

**Path Param:** `taxId` (Long)

**Response 200:** Satu objek tax.

---

### POST `/pos/tax/add`

**Request Body:**
```json
{
  "name": "PPN",
  "percentage": 11.00,
  "isDefault": true
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `name` | String | Ya | Nama tarif pajak |
| `percentage` | BigDecimal | Ya | Persentase pajak |
| `isDefault` | Boolean | Tidak (false) | Jadikan tarif default. Otomatis unset tarif default sebelumnya. |

**Response 200:** Data tax yang baru dibuat.

---

### PUT `/pos/tax/update`

**Request Body:**
```json
{
  "taxId": 1,
  "name": "PPN",
  "percentage": 12.00,
  "isDefault": true,
  "isActive": true
}
```

**Response 200:** Data tax yang telah diupdate.

---

### DELETE `/pos/tax/delete/{taxId}`

Nonaktifkan tarif pajak (soft delete via `isActive = false`). Tax yang sudah di-assign ke produk tetap tersimpan di `product.taxId`.

**Path Param:** `taxId` (Long)

**Response 200:**
```json
{ "success": true, "message": "Tax deleted", "data": null }
```

---

### GET `/pos/payment-method/merchant/list`

Mengembalikan daftar metode pembayaran yang tersedia untuk merchant, dibagi menjadi internal dan external.

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "internalPayments": [
      {
        "code": "CASH",
        "name": "Tunai",
        "category": "INTERNAL",
        "paymentType": "CASH",
        "provider": null
      },
      {
        "code": "CARD",
        "name": "Kartu Debit/Kredit",
        "category": "INTERNAL",
        "paymentType": "CARD",
        "provider": null
      }
    ],
    "externalPayments": [
      {
        "code": "QRIS",
        "name": "QRIS",
        "category": "EXTERNAL",
        "paymentType": "QR",
        "provider": "MIDTRANS"
      }
    ]
  }
}
```

---

## 6. Transaction

### GET `/pos/transaction/list`

**Query Params:**

| Param | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `page` | int | 0 | Halaman |
| `size` | int | 20 | Jumlah per halaman |
| `startDate` | String | — | **Wajib.** Format: `YYYY-MM-DD` |
| `endDate` | String | — | **Wajib.** Format: `YYYY-MM-DD` |
| `sortBy` | String | `createdDate` | Field pengurutan |
| `sortType` | String | `DESC` | `ASC` / `DESC` |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 101,
        "trxId": "TRX-AB12CD34",
        "status": "PAID",
        "paymentMethod": "CASH",
        "totalAmount": 57750.00,
        "createdDate": "2025-03-01T10:30:00"
      }
    ],
    "totalElements": 120,
    "totalPages": 6,
    "number": 0
  }
}
```

---

### GET `/pos/transaction/detail/{transactionId}`

**Path Param:** `transactionId` (Long)

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "transactionId": 101,
    "code": "TRX-AB12CD34",
    "paymentMethod": "CASH",
    "status": "PAID",
    "subTotal": 50000.00,
    "totalTax": 5500.00,
    "totalServiceCharge": 2500.00,
    "totalRounding": -250.00,
    "totalAmount": 57750.00,
    "cashTendered": 60000.00,
    "cashChange": 2250.00,
    "taxName": "PPN",
    "taxPercentage": 11.00,
    "serviceChargeAmount": null,
    "serviceChargePercentage": 5.00,
    "roundingTarget": "100",
    "roundingType": "NEAREST",
    "transactionDate": "2025-03-01T10:30:00",
    "queueNumber": "003",
    "transactionItems": [
      {
        "id": 201,
        "productId": 10,
        "productName": "Es Teh Manis",
        "price": 5000.00,
        "qty": 2,
        "totalPrice": 10000.00,
        "taxName": null,
        "taxPercentage": null,
        "taxAmount": null
      },
      {
        "id": 202,
        "productId": 11,
        "productName": "Nasi Goreng",
        "price": 20000.00,
        "qty": 2,
        "totalPrice": 40000.00,
        "taxName": "PPN",
        "taxPercentage": 10.00,
        "taxAmount": 4000.00
      }
    ],
    "payments": [
      {
        "id": 301,
        "paymentMethod": "CASH",
        "amountPaid": 60000.00,
        "status": "PAID",
        "paymentReference": null,
        "paymentDate": "2025-03-01T10:30:00"
      }
    ]
  }
}
```

---

### POST `/pos/transaction/create`

**Request Body:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 60000,
  "subTotal": 50000,
  "totalServiceCharge": 2500,
  "totalTax": 5500,
  "totalRounding": -250,
  "totalAmount": 57750,
  "cashChange": 2250,
  "priceIncludeTax": false,
  "queueNumber": null,
  "transactionItems": [
    {
      "productId": 10,
      "qty": 2,
      "price": 5000,
      "totalPrice": 10000
    },
    {
      "productId": 11,
      "qty": 2,
      "price": 20000,
      "totalPrice": 40000,
      "taxAmount": 4000
    }
  ]
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `paymentMethod` | String | Ya | Kode metode pembayaran (mis: `CASH`, `QRIS`) |
| `cashTendered` | BigDecimal | Jika CASH | Uang yang diterima dari pembeli |
| `subTotal` | BigDecimal | Ya | Total harga item sebelum tambahan. **Divalidasi server.** |
| `totalServiceCharge` | BigDecimal | Tidak (default 0) | Service charge. **Divalidasi server.** |
| `totalTax` | BigDecimal | Tidak (default 0) | Total pajak. **Divalidasi server.** |
| `totalRounding` | BigDecimal | Tidak (default 0) | Delta pembulatan (bisa negatif). **Divalidasi server.** |
| `totalAmount` | BigDecimal | Ya | Total akhir yang dibayar. **Divalidasi server.** |
| `cashChange` | BigDecimal | Tidak | Kembalian. **Divalidasi server jika `cashTendered` ada.** |
| `priceIncludeTax` | Boolean | Tidak (default false) | Diabaikan jika `PaymentSetting.isPriceIncludeTax` sudah diset. |
| `queueNumber` | Int | Tidak | Nomor antrean manual. Jika null, digenerate otomatis. |
| `transactionItems` | Array | Ya | Daftar item |
| `transactionItems[].productId` | Long | Ya | ID produk dari DB |
| `transactionItems[].qty` | Int | Ya | Jumlah |
| `transactionItems[].price` | BigDecimal | Tidak | Harga dari client — divalidasi vs `product.price` di DB |
| `transactionItems[].totalPrice` | BigDecimal | Tidak | Total dari client — divalidasi vs kalkulasi server |
| `transactionItems[].taxAmount` | BigDecimal | Tidak | Tax item dari client — divalidasi vs kalkulasi server |

> **Penting:** Server mengambil harga (`price`) dari tabel `product` di DB, bukan dari request. Field `price`, `totalPrice`, `taxAmount` pada item bersifat opsional dan hanya dipakai untuk validasi silang.

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 101,
    "trxId": "TRX-AB12CD34",
    "queueNumber": "003"
  }
}
```

**Response 422 (Amount Mismatch):**
```json
{
  "success": false,
  "message": "Transaction amount mismatch: 2 field(s) do not match",
  "data": {
    "mismatches": [
      {
        "field": "totalAmount",
        "fromRequest": 55000.00,
        "calculated": 57750.00
      },
      {
        "field": "items[1].price",
        "fromRequest": 18000.00,
        "calculated": 20000.00
      }
    ]
  }
}
```

---

### PUT `/pos/transaction/update/{merchantTrxId}`

Digunakan untuk mencatat pembayaran setelah transaksi dibuat, atau memperbarui status transaksi.

**Path Param:** `merchantTrxId` (String) — nilai field `trxId` (bukan `id`)

**Request Body:**
```json
{
  "paymentTrxId": "PAY-XYZ789",
  "paymentMethod": "QRIS",
  "amountPaid": 57750,
  "status": "PAID",
  "paymentReference": "REF-MIDTRANS-001",
  "paymentDate": "2025-03-01T10:30:00"
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `paymentTrxId` | String | Tidak | ID transaksi dari payment gateway |
| `paymentMethod` | String | Ya | Metode pembayaran |
| `amountPaid` | BigDecimal | Ya | Jumlah yang dibayarkan |
| `status` | String | Ya | Status transaksi baru (mis: `PAID`, `REFUNDED`) |
| `paymentReference` | String | Tidak | Referensi dari payment provider |
| `paymentDate` | String | Tidak | Waktu pembayaran (ISO 8601: `YYYY-MM-DDTHH:mm:ss`) |

**Response 200:**
```json
{
  "success": true,
  "message": "Transaction updated",
  "data": null
}
```

---

## 7. Report

### GET `/pos/summary-report/list`

Endpoint legacy. Mengembalikan top produk + breakdown pembayaran internal/eksternal.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "productList": [
      { "productName": "Nasi Goreng", "totalSaleItems": 150 },
      { "productName": "Es Teh Manis", "totalSaleItems": 320 }
    ],
    "paymentListInternal": [
      { "paymentMethod": "CASH", "totalAmount": 1500000.00 },
      { "paymentMethod": "CARD", "totalAmount": 800000.00 }
    ],
    "paymentListExternal": [
      { "paymentMethod": "QRIS", "totalAmount": 2200000.00 }
    ]
  }
}
```

`paymentListInternal` = metode yang mengandung kata CASH / CARD / DEBIT / CREDIT.
`paymentListExternal` = metode lainnya (QRIS, e-wallet, dll).

---

### GET `/pos/report/summary`

Ringkasan keuangan lengkap untuk rentang tanggal tertentu. Opsional filter per outlet.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |
| `outletId` | Long | Tidak | Filter ke satu outlet saja |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "period": { "startDate": "2026-03-01", "endDate": "2026-03-31" },
    "outletId": null,
    "totalTransactions": 520,
    "paidTransactions": 510,
    "refundedTransactions": 5,
    "grossRevenue": 25000000.00,
    "totalDiscount": 800000.00,
    "totalPromo": 500000.00,
    "totalVoucher": 200000.00,
    "totalLoyaltyRedeem": 150000.00,
    "netRevenue": 23350000.00,
    "totalTax": 2335000.00,
    "totalServiceCharge": 1167500.00,
    "totalRounding": 12500.00,
    "totalAmount": 26865000.00,
    "totalRefund": 350000.00
  }
}
```

**Kalkulasi:**
- `grossRevenue` = subtotal setelah price book, sebelum promo/diskon (`gross_amount`)
- `netRevenue` = subtotal setelah diskon + promo (`net_amount`)
- `totalAmount` = net + tax + service charge + rounding

---

### GET `/pos/report/payment-method`

Breakdown jumlah dan nilai transaksi per metode pembayaran.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |
| `outletId` | Long | Tidak | Filter ke satu outlet saja |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    { "paymentMethod": "CASH", "transactionCount": 250, "totalAmount": 12500000.00 },
    { "paymentMethod": "QRIS", "transactionCount": 180, "totalAmount": 9200000.00 },
    { "paymentMethod": "CARD", "transactionCount": 80, "totalAmount": 5165000.00 }
  ]
}
```

Diurutkan dari total terbesar ke terkecil.

---

### GET `/pos/report/top-products`

Produk terlaris berdasarkan jumlah unit terjual, beserta total revenue.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |
| `limit` | Int | Tidak | Jumlah produk (default: 10) |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    { "rank": 1, "productName": "Es Teh Manis", "qtySold": 320, "totalRevenue": 4800000.00 },
    { "rank": 2, "productName": "Nasi Goreng", "qtySold": 150, "totalRevenue": 7500000.00 }
  ]
}
```

Diurutkan berdasarkan `qtySold` terbanyak. Hanya transaksi berstatus `PAID` yang dihitung.

---

### GET `/pos/report/outlet`

Perbandingan performa antar outlet dalam rentang tanggal tertentu.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "outletId": 1,
      "totalTransactions": 310,
      "grossRevenue": 15500000.00,
      "netRevenue": 14200000.00,
      "totalTax": 1420000.00,
      "totalServiceCharge": 710000.00,
      "totalAmount": 16330000.00
    },
    {
      "outletId": 2,
      "totalTransactions": 200,
      "grossRevenue": 9500000.00,
      "netRevenue": 9150000.00,
      "totalTax": 915000.00,
      "totalServiceCharge": 457500.00,
      "totalAmount": 10535000.00
    }
  ]
}
```

Diurutkan dari total penjualan terbesar ke terkecil.

---

### GET `/pos/report/disbursement`

Rekapitulasi disbursement (revenue sharing) per rule/penerima untuk rentang tanggal.

**Query Params:**

| Param | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `startDate` | String | Ya | Format: `YYYY-MM-DD` |
| `endDate` | String | Ya | Format: `YYYY-MM-DD` |

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": [
    {
      "layer": "PLATFORM",
      "recipientName": "iSeller Platform",
      "percentage": 2.00,
      "totalBaseAmount": 23350000.00,
      "totalAmount": 467000.00,
      "transactionCount": 510,
      "settledCount": 500,
      "pendingCount": 10
    },
    {
      "layer": "MERCHANT",
      "recipientName": "Merchant A",
      "percentage": 95.00,
      "totalBaseAmount": 23350000.00,
      "totalAmount": 22182500.00,
      "transactionCount": 510,
      "settledCount": 500,
      "pendingCount": 10
    }
  ]
}
```

Diurutkan berdasarkan `layer` secara alphabetical.

---

## 8. Image Upload

### POST `/images/upload`

Tidak memerlukan Authorization header.

**Request:** `multipart/form-data`

| Field | Tipe | Keterangan |
|-------|------|-----------|
| `file` | File | Gambar yang diupload (max 10 MB) |

**Response 200:**
```json
{
  "success": true,
  "message": "Upload successful",
  "data": {
    "url": "http://<host>/images/abc123.jpg"
  }
}
```

---

---

# Simulasi Kalkulasi Transaksi

Server menghitung ulang semua nilai amount secara otomatis berdasarkan `PaymentSetting` merchant dan `Product.price` dari DB. Nilai yang dikirim client divalidasi — jika berbeda, transaksi **ditolak** dengan HTTP 422 dan detail mismatch.

## Urutan Kalkulasi

```
subTotal           = Σ (product.price × qty)
totalServiceCharge = subTotal × serviceChargePercentage / 100
                     (atau serviceChargeAmount jika persentase null)

taxBase            = subTotal + totalServiceCharge

totalTax           = jika priceIncludeTax:
                       taxBase × taxPercentage / (100 + taxPercentage)   ← ekstrak dari harga
                     jika NOT priceIncludeTax:
                       taxBase × taxPercentage / 100                     ← tambah di atas

amountBeforeRound  = jika priceIncludeTax:
                       subTotal + totalServiceCharge                     ← tax sudah ada dalam harga
                     jika NOT priceIncludeTax:
                       subTotal + totalServiceCharge + totalTax

totalRounding      = delta ke roundingTarget sesuai roundingType
totalAmount        = amountBeforeRound + totalRounding
cashChange         = cashTendered - totalAmount
```

---

## Skenario 1 — Tanpa tax, service charge, rounding

**PaymentSetting:**
```
isTax = false, isServiceCharge = false, isRounding = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Es Teh | 5.000 | 3 | 15.000 |
| Nasi Goreng | 25.000 | 1 | 25.000 |

**Kalkulasi:**
```
subTotal           = 15.000 + 25.000 = 40.000
totalServiceCharge = 0
totalTax           = 0
totalRounding      = 0
totalAmount        = 40.000
```

**Request yang benar:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 50000,
  "subTotal": 40000,
  "totalServiceCharge": 0,
  "totalTax": 0,
  "totalRounding": 0,
  "totalAmount": 40000,
  "cashChange": 10000,
  "transactionItems": [
    { "productId": 1, "qty": 3 },
    { "productId": 2, "qty": 1 }
  ]
}
```

---

## Skenario 2 — Tax eksklusif 11% (tax ditambahkan di atas harga)

**PaymentSetting:**
```
isTax = true, taxPercentage = 11, isPriceIncludeTax = false
isServiceCharge = false, isRounding = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Nasi Goreng | 25.000 | 2 | 50.000 |

**Kalkulasi:**
```
subTotal           = 50.000
totalServiceCharge = 0
taxBase            = 50.000 + 0 = 50.000
totalTax           = 50.000 × 11 / 100 = 5.500
amountBeforeRound  = 50.000 + 0 + 5.500 = 55.500
totalRounding      = 0
totalAmount        = 55.500
```

**Request yang benar:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 60000,
  "subTotal": 50000,
  "totalServiceCharge": 0,
  "totalTax": 5500,
  "totalRounding": 0,
  "totalAmount": 55500,
  "cashChange": 4500,
  "transactionItems": [
    { "productId": 2, "qty": 2 }
  ]
}
```

---

## Skenario 3 — Tax inklusif 11% (tax sudah termasuk dalam harga)

**PaymentSetting:**
```
isTax = true, taxPercentage = 11, isPriceIncludeTax = true
isServiceCharge = false, isRounding = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Nasi Goreng | 25.000 | 2 | 50.000 |

**Kalkulasi:**
```
subTotal           = 50.000
totalServiceCharge = 0
taxBase            = 50.000
                          ↑ tax sudah embedded dalam harga
totalTax (ekstrak) = 50.000 × 11 / (100 + 11)
                   = 550.000 / 111
                   = 4.954,05 ≈ 4.954,05
amountBeforeRound  = subTotal + serviceCharge (tax tidak ditambahkan lagi)
                   = 50.000 + 0 = 50.000
totalRounding      = 0
totalAmount        = 50.000
```

> `totalTax` di sini adalah nilai informatif (berapa pajak yang sudah terkandung), bukan ditambahkan ke total.

**Request yang benar:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 50000,
  "subTotal": 50000,
  "totalServiceCharge": 0,
  "totalTax": 4954.05,
  "totalRounding": 0,
  "totalAmount": 50000,
  "cashChange": 0,
  "transactionItems": [
    { "productId": 2, "qty": 2 }
  ]
}
```

---

## Skenario 4 — Service charge 5% + Tax eksklusif 10%

**PaymentSetting:**
```
isTax = true, taxPercentage = 10, isPriceIncludeTax = false
isServiceCharge = true, serviceChargePercentage = 5
isRounding = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Nasi Goreng | 25.000 | 2 | 50.000 |
| Es Teh | 5.000 | 4 | 20.000 |

**Kalkulasi:**
```
subTotal           = 50.000 + 20.000 = 70.000
totalServiceCharge = 70.000 × 5 / 100 = 3.500
taxBase            = 70.000 + 3.500 = 73.500
totalTax           = 73.500 × 10 / 100 = 7.350
amountBeforeRound  = 70.000 + 3.500 + 7.350 = 80.850
totalRounding      = 0
totalAmount        = 80.850
```

**Request yang benar:**
```json
{
  "paymentMethod": "QRIS",
  "subTotal": 70000,
  "totalServiceCharge": 3500,
  "totalTax": 7350,
  "totalRounding": 0,
  "totalAmount": 80850,
  "transactionItems": [
    { "productId": 2, "qty": 2 },
    { "productId": 1, "qty": 4 }
  ]
}
```

---

## Skenario 5 — Rounding ke 100 terdekat (NEAREST)

**PaymentSetting:**
```
isTax = true, taxPercentage = 10, isPriceIncludeTax = false
isServiceCharge = true, serviceChargePercentage = 5
isRounding = true, roundingTarget = 100, roundingType = NEAREST
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Nasi Goreng | 25.000 | 2 | 50.000 |
| Es Teh | 5.000 | 4 | 20.000 |

**Kalkulasi:**
```
subTotal           = 70.000
totalServiceCharge = 70.000 × 5 / 100 = 3.500
taxBase            = 73.500
totalTax           = 73.500 × 10 / 100 = 7.350
amountBeforeRound  = 80.850

Rounding ke 100 terdekat (NEAREST):
  remainder = 80.850 mod 100 = 50
  half      = 100 / 2 = 50
  remainder >= half → bulatkan ke atas
  delta     = 100 - 50 = +50

totalRounding      = +50
totalAmount        = 80.850 + 50 = 80.900
```

**Request yang benar:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 100000,
  "subTotal": 70000,
  "totalServiceCharge": 3500,
  "totalTax": 7350,
  "totalRounding": 50,
  "totalAmount": 80900,
  "cashChange": 19100,
  "transactionItems": [
    { "productId": 2, "qty": 2 },
    { "productId": 1, "qty": 4 }
  ]
}
```

---

## Skenario 6 — Rounding ke 1000 ke bawah (DOWN)

**PaymentSetting:**
```
isRounding = true, roundingTarget = 1000, roundingType = DOWN
isServiceCharge = false, isTax = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Item A | 18.500 | 3 | 55.500 |

**Kalkulasi:**
```
subTotal           = 55.500
totalServiceCharge = 0
totalTax           = 0
amountBeforeRound  = 55.500

Rounding DOWN ke 1000:
  remainder     = 55.500 mod 1000 = 500
  delta (DOWN)  = -(500) = -500

totalRounding      = -500
totalAmount        = 55.500 + (-500) = 55.000
```

**Request yang benar:**
```json
{
  "paymentMethod": "CASH",
  "cashTendered": 55000,
  "subTotal": 55500,
  "totalServiceCharge": 0,
  "totalTax": 0,
  "totalRounding": -500,
  "totalAmount": 55000,
  "cashChange": 0,
  "transactionItems": [
    { "productId": 5, "qty": 3 }
  ]
}
```

---

## Skenario 7 — Service charge nominal tetap (bukan persentase)

**PaymentSetting:**
```
isServiceCharge = true, serviceChargePercentage = null, serviceChargeAmount = 5000
isTax = false, isRounding = false
```

**Items:**
| Produk | Harga (DB) | Qty | Total |
|--------|-----------|-----|-------|
| Paket Makan | 45.000 | 1 | 45.000 |

**Kalkulasi:**
```
subTotal           = 45.000
totalServiceCharge = 5.000  ← fixed amount, bukan persentase
taxBase            = 50.000
totalTax           = 0
amountBeforeRound  = 45.000 + 5.000 = 50.000
totalRounding      = 0
totalAmount        = 50.000
```

---

## Ringkasan Aturan Validasi Request

| Kondisi | Aturan |
|---------|--------|
| `paymentMethod = CASH` | `cashTendered` **wajib** ada |
| `cashTendered` ada | Harus `>= totalAmount`, error jika kurang |
| `price` di item dikirim | Harus sama dengan `product.price` di DB |
| `totalPrice` di item dikirim | Harus sama dengan `product.price × qty` |
| `taxAmount` di item dikirim | Harus sama dengan `totalPrice × tax.percentage / 100` |
| `subTotal` dikirim | Harus sama dengan Σ(`product.price × qty`) |
| `totalServiceCharge` dikirim | Harus sama dengan hasil kalkulasi dari `PaymentSetting` |
| `totalTax` dikirim | Harus sama dengan hasil kalkulasi dari `PaymentSetting` |
| `totalRounding` dikirim | Harus sama dengan hasil kalkulasi rounding |
| `totalAmount` dikirim | Harus sama dengan total akhir server |
| `cashChange` dikirim | Harus sama dengan `cashTendered - totalAmount` |

Semua mismatch dikumpulkan dan dikembalikan sekaligus dalam satu response 422.

---

## Catatan untuk Revamp

1. **`isTaxable` dan `taxId` pada produk** belum bisa diset melalui endpoint product add/update saat ini — field tidak ada di `AddProductRequest`. Perlu ditambahkan jika revamp ingin mendukung pajak per-produk.

2. **`priceIncludeTax` pada request** diabaikan jika `PaymentSetting.isPriceIncludeTax` sudah diset. Client tidak perlu mengirim field ini.

3. **Nomor antrean** di-reset otomatis setiap hari berdasarkan hitungan transaksi pada outlet yang sama. Format: `001` – `999`.

4. **Soft-delete produk** — `deleted_date` diisi tetapi data fisik tetap ada di DB, sehingga data transaksi historis tetap valid.

5. **Snapshot transaksi** — `transaction_items` menyimpan `productName`, `price`, `taxName`, `taxPercentage` pada saat transaksi dibuat. Perubahan harga produk setelah transaksi tidak mempengaruhi data historis.

6. **Kalkulasi transaksi** — Server melakukan validasi server-side terhadap semua nilai yang dikirim client. Lihat `docs/discount-simulation.md` untuk detail flow 5-layer (Price Book → Promotion → Discount Code → Voucher → Loyalty Redeem).

---

## Cashier Management

Kelola user kasir per merchant. Setiap kasir punya akun login (`users`) dan detail merchant (`user_detail`). PIN dipakai untuk otorisasi operasi sensitif (refund).

### GET `/pos/cashier/list`

**Response 200:** Array of cashier objects.
```json
[
  {
    "id": 3,
    "username": "kasir01",
    "fullName": "Budi Santoso",
    "email": "budi@merchant.com",
    "employeeCode": "EMP-001",
    "outletId": 1,
    "isActive": true,
    "hasPin": true,
    "createdDate": "2026-01-01T09:00:00"
  }
]
```

---

### POST `/pos/cashier/add`

**Request Body:**
```json
{
  "username": "kasir02",
  "password": "password123",
  "fullName": "Ani Rahayu",
  "email": "ani@merchant.com",
  "employeeCode": "EMP-002",
  "outletId": 1
}
```

| Field | Tipe | Wajib | Keterangan |
|-------|------|-------|-----------|
| `username` | String | Ya | Harus unik di seluruh sistem |
| `password` | String | Ya | Di-hash BCrypt sebelum disimpan |
| `fullName` | String | Tidak | Nama lengkap |
| `email` | String | Tidak | Email kasir |
| `employeeCode` | String | Tidak | Kode karyawan internal |
| `outletId` | Long | Tidak | Outlet tempat kasir bertugas |

**Error 400:** Username sudah digunakan.

---

### PUT `/pos/cashier/update`

**Request Body:**
```json
{ "cashierId": 3, "fullName": "Ani Rahayu Updated", "outletId": 2, "isActive": true }
```

---

### PUT `/pos/cashier/set-pin`

Set atau ganti PIN kasir. PIN di-hash BCrypt sebelum disimpan.

```json
{ "cashierId": 3, "pin": "1234" }
```

---

### PUT `/pos/cashier/reset-password`

Reset password kasir oleh manager/admin.

```json
{ "cashierId": 3, "newPassword": "newpassword123" }
```

---

## Receipt Template

Template struk per merchant, bisa di-override per outlet. Satu record dengan `outletId = null` adalah default.

### POST `/pos/receipt-template/add`

**Request Body:**
```json
{
  "outletId": null,
  "header": "Selamat Datang di Toko Kami",
  "footer": "Terima kasih telah berbelanja!",
  "showTax": true,
  "showServiceCharge": true,
  "showRounding": true,
  "showLogo": true,
  "logoUrl": "https://cdn.example.com/logo.png",
  "showQueueNumber": true,
  "paperSize": "80mm"
}
```

| Field | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `outletId` | Long | null | Null = default semua outlet |
| `header` | String | null | Teks header struk |
| `footer` | String | null | Teks footer struk |
| `showTax` | Boolean | true | Tampilkan baris pajak |
| `showServiceCharge` | Boolean | true | Tampilkan service charge |
| `showRounding` | Boolean | true | Tampilkan rounding |
| `showLogo` | Boolean | false | Tampilkan logo merchant |
| `logoUrl` | String | null | URL logo |
| `showQueueNumber` | Boolean | true | Tampilkan nomor antrean |
| `paperSize` | String | null | `58mm` \| `80mm` |

**Error 400:** Template untuk scope ini sudah ada.

---

### GET `/pos/receipt-template/outlet/{outletId}`

Mengembalikan template efektif untuk outlet. Jika override belum dibuat, fallback ke template default.

---

## Printer Settings

Konfigurasi printer per merchant/outlet. Satu outlet bisa punya lebih dari satu printer dengan tipe berbeda (RECEIPT, KITCHEN, ORDER).

### POST `/pos/printer/add`

**Request Body:**
```json
{
  "outletId": 1,
  "type": "KITCHEN",
  "name": "Printer Dapur",
  "connectionType": "NETWORK",
  "ipAddress": "192.168.1.100",
  "port": 9100,
  "paperSize": "80mm",
  "isDefault": true
}
```

| Field | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `outletId` | Long | null | Outlet tujuan. Null = berlaku semua outlet. |
| `type` | String | RECEIPT | `RECEIPT` \| `KITCHEN` \| `ORDER` |
| `name` | String | Ya | Nama printer |
| `connectionType` | String | null | `NETWORK` \| `USB` \| `BLUETOOTH` |
| `ipAddress` | String | null | IP untuk NETWORK |
| `port` | Int | null | Port untuk NETWORK (biasanya 9100) |
| `paperSize` | String | null | `58mm` \| `80mm` |
| `isDefault` | Boolean | false | Printer default untuk type ini di outlet ini |

> Jika `isDefault = true`, printer default sebelumnya untuk type+outlet yang sama otomatis di-unset.

---

### GET `/pos/printer/list`

`?outletId=1` untuk filter per outlet. Tanpa param = semua printer merchant.

---

## Disbursement (Revenue Sharing)

Atur pembagian pendapatan dari setiap transaksi ke berbagai pihak (Platform, Dealer, Merchant).

### POST `/pos/disbursement/rule/add`

**Request Body:**
```json
{
  "name": "Dealer Fee",
  "layer": "DEALER",
  "recipientId": 5,
  "recipientName": "PT Distributor ABC",
  "percentage": 2.50,
  "source": "NET",
  "productTypeFilter": null,
  "displayOrder": 1
}
```

| Field | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `name` | String | Ya | Nama aturan |
| `layer` | String | MERCHANT | `PLATFORM` \| `DEALER` \| `MERCHANT` \| `CUSTOM` |
| `recipientId` | Long | null | ID penerima |
| `recipientName` | String | null | Nama penerima |
| `percentage` | BigDecimal | Ya | Persentase dari base amount |
| `source` | String | NET | `GROSS` \| `NET` \| `NET_AFTER_TAX` \| `NET_AFTER_TAX_SC` |
| `productTypeFilter` | String | null | Filter jenis produk. Null = semua produk. |
| `displayOrder` | Int | null | Urutan eksekusi |

---

### GET `/pos/disbursement/log/list`

Riwayat disbursement per transaksi. Dibuat otomatis oleh sistem saat transaksi selesai.

**Query Params:** `startDate` (YYYY-MM-DD), `endDate` (YYYY-MM-DD) — opsional.

**Response 200:** Array of log objects dengan field: `transactionId`, `ruleId`, `recipientName`, `layer`, `baseAmount`, `percentage`, `amount`, `status` (`PENDING`\|`SETTLED`\|`FAILED`), `createdDate`, `settledDate`.

7. **Endpoint Customer, OrderType, CashierShift, Voucher, Loyalty, Discount, Promotion, PriceBook** (no. 24–80) belum memiliki dokumentasi request/response detail di dokumen ini. Lihat docs masing-masing modul: `docs/voucher.md`, `docs/loyalty.md`, `docs/discount-simulation.md`.
