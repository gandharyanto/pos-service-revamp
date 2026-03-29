# Voucher System Documentation

## 1. Konsep Dasar

Voucher di sistem ini berfungsi sebagai **payment instrument**, bukan diskon.

- Voucher **mengurangi jumlah yang harus dibayar** (payment side), bukan mengurangi subtotal
- Tax dan Service Charge **tetap dihitung dari total penuh** (sebelum voucher)
- Voucher tidak mempengaruhi promoAmount maupun discountAmount

### Perbedaan Voucher vs Diskon

| Aspek | Diskon / Promosi | Voucher |
|---|---|---|
| Berlaku pada | Subtotal / item | Total akhir (payment) |
| Pengaruh ke tax | Mengurangi base pajak | Tidak mempengaruhi pajak |
| Input | Kode atau otomatis | Kode spesifik |
| Nilai fleksibel | Ya (%, nominal) | Tetap (sellingPrice group) |
| Reusable | Ya (usageLimit) | Tidak (one-time) |

---

## 2. Hierarki

```
VoucherBrand
└── VoucherGroup (1 brand → N group)
    └── VoucherCode  (1 group → N kode)  [tabel: voucher]
```

### VoucherBrand
Penerbit voucher. Bisa brand sendiri, atau mitra (Ultra Voucher, TADA, dsb).

| Field | Keterangan |
|---|---|
| `name` | Nama brand |
| `logoUrl` | URL logo (opsional) |
| `isActive` | Status aktif |

### VoucherGroup
Mendefinisikan aturan, nilai, dan periode semua kode dalam grup.

| Field | Keterangan |
|---|---|
| `brandId` | FK ke VoucherBrand |
| `purchasePrice` | Harga beli / cost ke merchant |
| `sellingPrice` | Nilai yang diterima pelanggan saat redeem |
| `expiredDate` | Tanggal kadaluarsa. Null = tidak ada batas |
| `validDays` | Hari valid: `MON,TUE,WED,THU,FRI,SAT,SUN`. Null = setiap hari |
| `isRequiredCustomer` | Hanya bisa digunakan pelanggan terdaftar |
| `channel` | `POS` \| `ONLINE` \| `BOTH` |

### VoucherCode (tabel: `voucher`)
Satu baris = satu kode unik yang bisa ditukarkan sekali.

**Status kode:**

| Status | Keterangan |
|---|---|
| `AVAILABLE` | Belum digunakan, siap ditukarkan |
| `USED` | Sudah ditukarkan, tidak bisa digunakan lagi |
| `EXPIRED` | Melewati `expiredDate` group |
| `CANCELLED` | Dibatalkan oleh merchant |

---

## 3. Manajemen Kode (Manual)

Saat ini kode hanya bisa ditambah secara manual. Tidak ada auto-generate.

### Tambah satu kode
```
POST /pos/voucher/code/add
{
  "groupId": 1,
  "code": "VOUCHER-ABC123"
}
```

### Bulk import dari list
```
POST /pos/voucher/code/bulk-import
{
  "groupId": 1,
  "codes": ["VC-001", "VC-002", "VC-003", "VC-DUPLIKAT"]
}
```

**Response:**
```json
{
  "imported": 3,
  "skipped": 1,
  "skippedCodes": ["VC-DUPLIKAT"]
}
```

Kode yang sudah ada di sistem (duplikat) akan dilewati dan dilaporkan di `skippedCodes`.

### Batalkan kode
```
PUT /pos/voucher/code/cancel/{voucherId}
```
Kode dengan status `USED` tidak bisa dibatalkan.

---

## 4. Alur Penggunaan (Redeem)

```
Kasir input kode → validasi → kalkulasi → tandai USED → simpan transaksi
```

### Validasi di TransactionService (`resolveVoucher`)

1. Cari voucher by `code` + `merchantId`
2. Cek `status == "AVAILABLE"`
3. Cek `group.isActive`
4. Cek `group.expiredDate` (jika ada)
5. Cek `group.validDays` (jika ada): hari hari ini harus ada dalam daftar
6. Jika `group.isRequiredCustomer == true`: pastikan `customerId != null`
7. Hitung `voucherAmount = min(group.sellingPrice, totalAmount)`

### Kalkulasi Posisi Voucher dalam Transaksi

```
grossSubTotal = Σ(effectivePrice × qty)          // Layer 1: Price Book
promoAmount   = hasil promosi Layer 2
discountAmount = hasil kode diskon Layer 3

netSubTotal   = grossSubTotal - promoAmount - discountAmount

serviceCharge = netSubTotal × scRate
taxAmount     = (netSubTotal + serviceCharge) × taxRate
rounding      = ...

totalAmount   = netSubTotal + serviceCharge + taxAmount + rounding

// Layer 4: Voucher (payment instrument)
voucherAmount = min(group.sellingPrice, totalAmount)
amountDue     = totalAmount - voucherAmount       // yang harus dibayar tunai/non-tunai
```

### Split Payment (Voucher + Cash/QRIS)

Jika `voucherAmount < totalAmount`, sisa harus dibayar dengan metode lain.

**Contoh:**
```
totalAmount    = 150.000
voucherAmount  = 100.000  (sellingPrice group)
amountDue      = 50.000   → dibayar tunai / QRIS
```

### Pencatatan

Setelah transaksi berhasil:
- `Voucher.status` diubah ke `"USED"`
- `Voucher.usedDate` diisi timestamp
- `Voucher.transactionId` diisi ID transaksi
- Record `VoucherUsage` dibuat sebagai audit trail

---

## 5. API Endpoints

### Brand
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/voucher/brand/list` | List semua brand |
| GET | `/pos/voucher/brand/detail/{brandId}` | Detail brand + jumlah grup |
| POST | `/pos/voucher/brand/add` | Buat brand baru |
| PUT | `/pos/voucher/brand/update` | Update brand |
| DELETE | `/pos/voucher/brand/delete/{brandId}` | Hapus brand |

### Group
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/voucher/group/list` | Semua grup merchant |
| GET | `/pos/voucher/group/list?brandId=X` | Grup dalam brand tertentu |
| GET | `/pos/voucher/group/detail/{groupId}` | Detail grup + statistik kode |
| POST | `/pos/voucher/group/add` | Buat grup baru |
| PUT | `/pos/voucher/group/update` | Update grup |
| DELETE | `/pos/voucher/group/delete/{groupId}` | Hapus grup |

### Code
| Method | Endpoint | Keterangan |
|---|---|---|
| GET | `/pos/voucher/code/list/{groupId}` | List kode dalam grup |
| POST | `/pos/voucher/code/add` | Tambah satu kode |
| POST | `/pos/voucher/code/bulk-import` | Import banyak kode |
| PUT | `/pos/voucher/code/cancel/{voucherId}` | Batalkan kode |

### Gunakan di Transaksi
Field di `CreateTransactionRequest`:
```json
{
  "voucherCode": "VC-001",
  "voucherAmount": 50000
}
```
Server memvalidasi ulang `voucherAmount`. Jika tidak cocok, transaksi ditolak.

---

## 6. Contoh Skenario

### Skenario 1: Voucher cukup menutup seluruh tagihan
```
Total tagihan      = 80.000
Voucher sellingPrice = 100.000
voucherAmount diklaim = 80.000   (capped ke totalAmount)
amountDue          = 0
```

### Skenario 2: Voucher tidak cukup (split payment)
```
Total tagihan      = 200.000
Voucher sellingPrice = 100.000
voucherAmount diklaim = 100.000
amountDue          = 100.000 → bayar cash/QRIS
```

### Skenario 3: Voucher + Diskon
```
grossSubTotal      = 150.000
discountAmount     = 15.000   (promo 10%)
netSubTotal        = 135.000
tax (11%)          = 14.850
totalAmount        = 149.850

voucherAmount      = 100.000
amountDue          = 49.850
```
Tax dihitung dari 135.000 (bukan dari 149.850 - 100.000). Voucher tidak mempengaruhi basis pajak.

---

## 7. Roadmap: Auto-Generate & Distribusi

Fitur berikut **belum diimplementasi** dan dijadikan referensi pengembangan selanjutnya.

### 7.1 Auto-Generate Kode

Tambahkan endpoint:
```
POST /pos/voucher/code/generate
{
  "groupId": 1,
  "quantity": 100,
  "prefix": "VCH-",        // opsional
  "codeLength": 8          // panjang bagian random
}
```

Contoh implementasi generator:
```kotlin
fun generateCode(prefix: String, length: Int): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // hindari 0/O dan 1/I
    val random = (1..length).map { chars.random() }.joinToString("")
    return "$prefix$random"
}
```

Pastikan uniqueness dengan retry loop dan `existsByCode()`.

### 7.2 Export Kode ke CSV

Berguna untuk dicetak atau dikirim ke mitra distribusi:
```
GET /pos/voucher/code/export/{groupId}?format=csv
```

Response: file CSV dengan kolom `code`, `status`, `expiredDate`.

### 7.3 Distribusi via Email

Flow yang direkomendasikan:
1. Merchant upload data pelanggan + assign kode per pelanggan
2. Tambah field `assignedCustomerId` dan `assignedEmail` di tabel `voucher`
3. Trigger email via SMTP atau layanan transaksional (SendGrid, Mailjet, dsb)
4. Status awal sebelum dikirim: `ASSIGNED` → setelah email terkirim: `DISTRIBUTED`

Tambahan tabel yang diperlukan:
```sql
ALTER TABLE voucher ADD COLUMN assigned_customer_id BIGINT;
ALTER TABLE voucher ADD COLUMN assigned_email VARCHAR(255);
ALTER TABLE voucher ADD COLUMN distributed_at TIMESTAMP;
```

### 7.4 Customer Validity Check

Jika `group.isRequiredCustomer = true`, validasi saat redeem:
- Cek `assignedCustomerId == transaction.customerId` (kode hanya untuk pelanggan spesifik)
- Atau cek customer terdaftar saja (tanpa binding ke individu tertentu)

Dua mode ini perlu dikonfigurasikan per group dengan field tambahan `customerScope`:
- `ANY_REGISTERED` — customer terdaftar mana saja boleh
- `ASSIGNED_ONLY` — hanya customer yang di-assign

### 7.5 Voucher untuk Online Channel

Untuk channel `ONLINE` atau `BOTH`, tambahkan:
- Validasi channel di `resolveVoucher()` berdasarkan sumber transaksi
- Field `transactionChannel` di `CreateTransactionRequest`
