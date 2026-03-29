# API Documentation — POS Service Revamp

**Base URL:** `http://<host>/`
**Auth:** Semua endpoint kecuali `/pos/auth/login` dan `/images/upload` wajib menyertakan header:
```
Authorization: Bearer <token>
```

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
| 14 | GET    | `/pos/payment-setting` | Get payment setting |
| 15 | POST   | `/pos/payment-setting/create` | Buat payment setting |
| 16 | PUT    | `/pos/payment-setting/update` | Update payment setting |
| 17 | GET    | `/pos/payment-method/merchant/list` | List metode pembayaran |
| 18 | GET    | `/pos/transaction/list` | List transaksi |
| 19 | GET    | `/pos/transaction/detail/{transactionId}` | Detail transaksi |
| 20 | POST   | `/pos/transaction/create` | Buat transaksi |
| 21 | PUT    | `/pos/transaction/update/{merchantTrxId}` | Update transaksi / catat pembayaran |
| 22 | GET    | `/pos/summary-report/list` | Laporan ringkasan |
| 23 | POST   | `/images/upload` | Upload gambar |

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

### GET `/pos/payment-setting`

**Response 200:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "paymentSettingId": 1,
    "isPriceIncludeTax": false,
    "isRounding": true,
    "roundingTarget": 100,
    "roundingType": "NEAREST",
    "isServiceCharge": true,
    "serviceChargePercentage": 5.00,
    "serviceChargeAmount": null,
    "isTax": true,
    "taxPercentage": 11.00,
    "taxName": "PPN"
  }
}
```

---

### POST `/pos/payment-setting/create`

Hanya bisa dibuat sekali per merchant. Jika sudah ada, gunakan endpoint update.

**Request Body:**
```json
{
  "isPriceIncludeTax": false,
  "isRounding": true,
  "roundingTarget": 100,
  "roundingType": "NEAREST",
  "isServiceCharge": true,
  "serviceChargePercentage": 5.00,
  "serviceChargeAmount": null,
  "isTax": true,
  "taxPercentage": 11.00,
  "taxName": "PPN"
}
```

| Field | Tipe | Default | Keterangan |
|-------|------|---------|-----------|
| `isPriceIncludeTax` | Boolean | false | Harga produk sudah termasuk pajak |
| `isRounding` | Boolean | false | Aktifkan pembulatan |
| `roundingTarget` | Int | null | Target pembulatan (mis: `100`, `500`, `1000`) |
| `roundingType` | String | null | `UP` / `DOWN` / `NEAREST` |
| `isServiceCharge` | Boolean | false | Aktifkan service charge |
| `serviceChargePercentage` | BigDecimal | null | Persentase SC (%). Jika diisi, `serviceChargeAmount` diabaikan. |
| `serviceChargeAmount` | BigDecimal | null | SC nominal tetap. Dipakai jika `serviceChargePercentage` null. |
| `isTax` | Boolean | false | Aktifkan pajak global |
| `taxPercentage` | BigDecimal | null | Persentase pajak (%) |
| `taxName` | String | null | Nama pajak (mis: "PPN") |

**Response 200:** Data payment setting yang baru dibuat.
**Error 400:** Payment setting sudah ada untuk merchant ini.

---

### PUT `/pos/payment-setting/update`

**Request Body:** Sama seperti create, tambah field `paymentSettingId`:
```json
{
  "paymentSettingId": 1,
  "isPriceIncludeTax": false,
  ...
}
```

**Response 200:** Data payment setting yang telah diupdate.

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

`productList` diurutkan berdasarkan jumlah item terjual terbanyak.
`paymentListInternal` = metode yang mengandung kata CASH / CARD / DEBIT / CREDIT.
`paymentListExternal` = metode lainnya (QRIS, e-wallet, dll).

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
