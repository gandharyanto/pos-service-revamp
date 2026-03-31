# Functional Specification Document (FSD)
# POS Service Revamp — Phase 2

**Versi Dokumen:** 1.0.0
**Tanggal:** 31 Maret 2026
**Disusun oleh:** Tim Backend Engineering
**Status:** Final

---

## Daftar Isi

1. [Pendahuluan & Ruang Lingkup](#1-pendahuluan--ruang-lingkup)
2. [Daftar Fitur & Modul](#2-daftar-fitur--modul)
3. [Spesifikasi Fitur](#3-spesifikasi-fitur)
   - [3.1 Pajak (Tax)](#31-pajak-tax)
   - [3.2 Service Charge (Payment Setting)](#32-service-charge-payment-setting)
   - [3.3 Diskon Berbasis Kode (Discount)](#33-diskon-berbasis-kode-discount)
   - [3.4 Promosi Otomatis (Promotion)](#34-promosi-otomatis-promotion)
   - [3.5 Price Book](#35-price-book)
   - [3.6 Voucher](#36-voucher)
   - [3.7 CRM Customer](#37-crm-customer)
   - [3.8 Cashier Management](#38-cashier-management)
   - [3.9 Receipt Template & Printer Settings](#39-receipt-template--printer-settings)
   - [3.10 Disbursement (Revenue Sharing)](#310-disbursement-revenue-sharing)
   - [3.11 Financial Report](#311-financial-report)
   - [3.12 Order Type & POS Language Setting](#312-order-type--pos-language-setting)
   - [3.13 Workshift & Cash Register](#313-workshift--cash-register)
   - [3.14 Refund at POS](#314-refund-at-pos)
   - [3.15 Loyalty Program & Redemption Rule](#315-loyalty-program--redemption-rule)
   - [3.16 MDR Setting](#316-mdr-setting)
   - [3.17 Notification Setting](#317-notification-setting)
   - [3.18 Product Variant & Modifier](#318-product-variant--modifier)
4. [Alur Transaksi End-to-End](#4-alur-transaksi-end-to-end)
5. [Business Rules Lintas Fitur](#5-business-rules-lintas-fitur)
6. [Manajemen State & Status](#6-manajemen-state--status)
7. [Aturan Keamanan & Otorisasi](#7-aturan-keamanan--otorisasi)
8. [Error Handling Standard](#8-error-handling-standard)

---

## 1. Pendahuluan & Ruang Lingkup

### 1.1 Latar Belakang

POS Service Revamp adalah proyek pembuatan ulang layanan backend Point-of-Sale (POS) yang dirancang untuk mendukung operasional ritel dan F&B skala kecil hingga menengah. Phase 1 mencakup fitur inti seperti manajemen produk, outlet, dan transaksi dasar. Phase 2 memperluas cakupan dengan menambahkan fitur-fitur komersial yang lebih canggih: perpajakan, service charge, sistem diskon berlapis, promosi otomatis, buku harga, voucher, CRM pelanggan, manajemen kasir, pengaturan receipt/printer, pembagian pendapatan (disbursement), dan laporan keuangan.

### 1.2 Tujuan Dokumen

Dokumen ini mendefinisikan secara fungsional seluruh fitur Phase 2 sehingga dapat dijadikan acuan bagi:
- Tim backend dalam mengimplementasikan logic bisnis
- Tim QA dalam menyusun skenario pengujian
- Tim produk dalam memvalidasi kesesuaian requirement
- Stakeholder bisnis dalam memahami kapabilitas sistem

### 1.3 Ruang Lingkup

Dokumen ini mencakup 18 modul fungsional Phase 2. Semua fitur berjalan di atas infrastruktur berikut:

| Aspek | Detail |
|---|---|
| Framework |  |
| Database |  |
| Autentikasi | JWT Bearer Token |
| Multi-tenancy | `merchantId` diekstrak dari setiap request via JWT |
| Format Response | `{ "success": true, "message": "...", "data": {...} }` |
| Base URL | `/pos` |

### 1.4 Asumsi & Batasan

- Setiap request sudah terautentikasi dan membawa `merchantId` yang valid dalam JWT.
- Satu merchant dapat memiliki banyak outlet; kebijakan dapat di-set per outlet atau default untuk semua outlet.
- Semua penghapusan data sensitif menggunakan soft delete (`isActive = false`) kecuali dinyatakan eksplisit sebagai hard delete.
- Kalkulasi harga menggunakan 5 layer terurut dan tidak dapat diubah urutannya.
- Waktu yang digunakan adalah waktu server (UTC).

### 1.5 Konvensi Penamaan FSD vs Schema

Dokumen FSD ini menggunakan istilah bisnis yang lebih mudah dibaca seperti `grossSubTotal`, `netSubTotal`, `voucher_code`, atau `loyalty_history`. Saat implementasi, tim backend WAJIB mengikuti nama tabel/kolom aktual pada schema migration Phase 2. Mapping utamanya adalah:

| Istilah di FSD | Nama di Schema Migration | Catatan |
|---|---|---|
| `grossSubTotal` | `transaction.gross_amount` | Nama response/API boleh tetap `grossSubTotal` |
| `netSubTotal` | `transaction.net_amount` | Nama response/API boleh tetap `netSubTotal` |
| `refundAmount` | `transaction.refund_amount` | Snapshot di tabel transaksi |
| `refundReason` | `transaction.refund_reason` | Snapshot di tabel transaksi |
| `refundBy` | `transaction.refund_by` | Snapshot di tabel transaksi |
| `refundDate` | `transaction.refund_date` | Snapshot di tabel transaksi |
| `voucher_code` | tabel `voucher` yang direpurpose | Secara bisnis tetap disebut voucher code |
| `loyalty_history` | tabel `loyalty_transaction` | FSD lama memakai istilah generik riwayat poin |
| `printer` | tabel `printer_setting` | Istilah bisnis tetap printer |

Jika ada konflik antara istilah bisnis FSD dan nama persistence schema, implementasi entity/repository mengikuti schema, sedangkan contract API dapat tetap memakai istilah bisnis yang lebih jelas.

---

## 2. Daftar Fitur & Modul

| No | Modul | Deskripsi Singkat | Tipe |
|---|---|---|---|
| 1 | Pajak (Tax) | Manajemen tarif pajak merchant; default tax; apply di level item dan transaksi | Master Data |
| 2 | Service Charge | Pengaturan biaya layanan per merchant/outlet; empat mode sumber perhitungan | Konfigurasi |
| 3 | Diskon Kode | Kode diskon dengan scope, validitas, channel, dan quota | Promosi |
| 4 | Promosi Otomatis | Auto-apply promotion berdasarkan kondisi cart; BUY_X_GET_Y, order discount | Promosi |
| 5 | Price Book | Override harga satuan berdasarkan produk, kategori, tipe order, atau tier qty | Harga |
| 6 | Voucher | Voucher fisik/digital dengan hierarki Brand → Group → Code | Pembayaran |
| 7 | CRM Customer | Data pelanggan, loyalty point, riwayat transaksi | CRM |
| 8 | Cashier Management | Manajemen akun kasir, PIN otorisasi, reset password | SDM |
| 9 | Receipt & Printer | Template struk per merchant/outlet; pengaturan printer fisik | Konfigurasi |
| 10 | Disbursement | Aturan bagi hasil pendapatan; log per transaksi; laporan rekap | Keuangan |
| 11 | Financial Report | Laporan ringkasan, metode pembayaran, produk terlaris, per outlet, disbursement | Pelaporan |
| 12 | Order Type & POS Language | Master tipe pesanan dan pengaturan bahasa merchant/outlet | Konfigurasi |
| 13 | Workshift & Cash Register | Buka/tutup shift, opening/closing cash, cash adjustment, rekap shift | Operasional |
| 14 | Refund at POS | Refund penuh/sebagian dengan approval dan audit trail | Operasional |
| 15 | Loyalty Program | Konfigurasi earn/redeem point, expiry, rule penukaran, override per produk | CRM |
| 16 | MDR Setting | Konfigurasi fee metode pembayaran dan dampaknya ke settlement | Keuangan |
| 17 | Notification Setting | Konfigurasi notifikasi operasional dan rekap berkala | Operasional |
| 18 | Product Variant & Modifier | Variasi produk, opsi tambahan, snapshot pilihan ke transaksi | Katalog / Operasional |

---

## 3. Spesifikasi Fitur

---

### 3.1 Pajak (Tax)

#### 3.1.1 User Story

> Sebagai pemilik merchant, saya ingin mendefinisikan satu atau lebih tarif pajak (misalnya PPN 11%, PPh 1%), menandai salah satunya sebagai default, sehingga saat kasir membuat transaksi, pajak yang benar otomatis diterapkan tanpa input manual.

#### 3.1.2 Model Data

**Tabel: `merchant_tax`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama tarif pajak (contoh: "PPN 11%") |
| percentage | Decimal | Persentase pajak (contoh: 11.00) |
| isDefault | Boolean | Apakah tarif ini default |
| isActive | Boolean | Soft delete flag |
| createdAt | Timestamp | Waktu pembuatan |
| updatedAt | Timestamp | Waktu terakhir diubah |

**Data pajak di level transaksi:**

| Kolom | Level | Keterangan |
|---|---|---|
| taxId | Transaction | ID tarif pajak yang diterapkan |
| taxName | Transaction | Snapshot nama tarif saat transaksi dibuat |
| taxPercentage | Transaction | Snapshot persentase saat transaksi dibuat |
| totalTax | Transaction | Total nilai pajak seluruh transaksi |
| taxAmount | TransactionItem | Nilai pajak per item |

#### 3.1.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/tax/list` | Daftar semua tarif pajak aktif merchant |
| GET | `/pos/tax/detail/{id}` | Detail satu tarif pajak |
| POST | `/pos/tax/add` | Tambah tarif pajak baru |
| PUT | `/pos/tax/update` | Update tarif pajak (id wajib di body) |
| DELETE | `/pos/tax/delete/{id}` | Soft delete tarif pajak |

#### 3.1.4 Alur Fungsional

**Tambah Tarif Pajak:**
```
Kasir/Admin → POST /pos/tax/add
    ↓
Validasi input (name tidak kosong, percentage 0-100)
    ↓
Jika isDefault = true:
    UPDATE semua tax lain milik merchant → isDefault = false
    ↓
INSERT record baru dengan isDefault = true
    ↓
Return data tax baru
```

**Set Default Tax:**
```
Admin → PUT /pos/tax/update (isDefault: true)
    ↓
Cari tax lama yang isDefault = true milik merchant yang sama
    ↓
Set isDefault = false pada tax lama
    ↓
Set isDefault = true pada tax yang di-update
    ↓
Return data tax ter-update
```

**Aplikasi Pajak di Transaksi:**
```
Sistem ambil tax default merchant (isDefault = true, isActive = true)
    ↓
Snapshot taxId, taxName, taxPercentage ke header transaksi
    ↓
Untuk setiap item: taxAmount = itemSubTotal × (taxPercentage / 100)
    ↓
totalTax = SUM(taxAmount semua item)
```

#### 3.1.5 Business Rules

1. **Uniqueness Default:** Pada satu merchant, hanya boleh ada satu tarif dengan `isDefault = true` pada satu waktu. Sistem secara otomatis unset default lama saat default baru ditetapkan.
2. **Snapshot Pajak:** Nilai `taxName` dan `taxPercentage` di transaksi adalah snapshot saat transaksi dibuat. Perubahan tarif pajak di kemudian hari tidak mempengaruhi transaksi historis.
3. **Soft Delete:** Menghapus tarif pajak hanya mengubah `isActive = false`. Tarif yang dihapus tidak muncul di list, tetapi data historis di transaksi tetap utuh.
4. **Pajak pada Non-Active Tax:** Jika tarif default di-nonaktifkan tanpa menetapkan default baru, transaksi baru tidak akan memiliki pajak (totalTax = 0). Sistem tidak error, tetapi merchant harus menetapkan default baru.
5. **Basis Kalkulasi Pajak:** Pajak dihitung dari `netSubTotal` (setelah price book, promo, dan diskon kode diterapkan), kecuali jika `serviceChargeSource = BEFORE_TAX` — lihat detail di Bagian 3.2.

#### 3.1.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| `percentage` di luar rentang 0–100 | `400 Bad Request` — "Persentase pajak harus antara 0 dan 100" |
| `name` kosong atau null | `400 Bad Request` — "Nama tarif pajak tidak boleh kosong" |
| DELETE tax yang sedang jadi default | Soft delete berhasil; merchant tidak lagi punya default tax |
| GET detail tax milik merchant lain | `404 Not Found` |
| Merchant belum punya tax | Transaksi berjalan normal tanpa pajak (totalTax = 0) |

---

### 3.2 Service Charge (Payment Setting)

#### 3.2.1 User Story

> Sebagai pemilik merchant, saya ingin mengaktifkan biaya layanan (service charge) dengan persentase tertentu, mengonfigurasi cara penghitungannya (sebelum pajak, sesudah pajak, dari DPP, atau sesudah diskon), dan mengizinkan outlet tertentu memiliki konfigurasi yang berbeda dari default merchant.

#### 3.2.2 Model Data

**Tabel: `payment_setting`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| outletId | UUID / null | null = pengaturan default merchant; diisi = override per outlet |
| isServiceCharge | Boolean | Apakah service charge aktif |
| serviceChargePercentage | Decimal | Persentase service charge |
| serviceChargeAmount | Decimal | Nilai SC dalam amount absolut (jika digunakan) |
| serviceChargeSource | Enum | `BEFORE_TAX` \| `AFTER_TAX` \| `DPP` \| `AFTER_DISCOUNT` |
| isPriceIncludeTax | Boolean | Flag display — harga sudah termasuk pajak (tidak ubah kalkulasi) |
| createdAt | Timestamp | Waktu pembuatan |
| updatedAt | Timestamp | Waktu terakhir diubah |

#### 3.2.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/payment-setting` | Default payment setting merchant (outletId = null) |
| GET | `/pos/payment-setting/list` | Semua payment setting merchant (default + semua outlet) |
| GET | `/pos/payment-setting/outlet/{outletId}` | Setting spesifik outlet; fallback ke default jika tidak ada |
| POST | `/pos/payment-setting/create` | Buat payment setting baru |
| PUT | `/pos/payment-setting/update` | Update payment setting yang ada |

#### 3.2.4 Alur Fungsional

**Resolusi Setting per Outlet:**
```
GET /pos/payment-setting/outlet/{outletId}
    ↓
Cari record dengan merchantId = current AND outletId = {outletId}
    ↓
    [Ditemukan] → Return setting outlet-specific
    [Tidak ditemukan] → Cari record dengan merchantId = current AND outletId = null
        ↓
        [Ditemukan] → Return setting default merchant
        [Tidak ditemukan] → Return default kosong (SC tidak aktif)
```

**Kalkulasi Service Charge berdasarkan Source:**

| `serviceChargeSource` | Formula SC |
|---|---|
| `BEFORE_TAX` | `netSubTotal × serviceChargePercentage / 100` (SC dihitung sebelum pajak, pajak dihitung dari netSubTotal + SC) |
| `AFTER_TAX` | `(netSubTotal + totalTax) × serviceChargePercentage / 100` |
| `DPP` | `(netSubTotal / (1 + taxPercentage/100)) × serviceChargePercentage / 100` (dari Dasar Pengenaan Pajak) |
| `AFTER_DISCOUNT` | `(grossSubTotal - discountAmount) × serviceChargePercentage / 100` (sebelum pajak, tapi sesudah diskon kode saja) |

#### 3.2.5 Business Rules

1. **Hierarki Override:** Setting outlet-specific selalu menggantikan setting default merchant. Jika outlet tidak punya setting sendiri, sistem fallback ke default merchant.
2. **isPriceIncludeTax:** Flag ini murni untuk keperluan tampilan di UI (contoh: menampilkan "Harga sudah termasuk PPN"). Flag ini tidak mengubah cara kalkulasi apapun di backend.
3. **Service Charge vs. Amount Absolut:** Sistem mendukung dua mode: persentase (`serviceChargePercentage`) atau jumlah tetap (`serviceChargeAmount`). Jika keduanya diisi, persentase yang diprioritaskan. Logic ini harus dikonfirmasi dengan product owner.
4. **SC dan Pajak:** Urutan kalkulasi SC dan pajak bergantung pada `serviceChargeSource`. Lihat Bagian 5 untuk detail kalkulasi lintas fitur.
5. **Satu Default per Merchant:** Hanya boleh ada satu record dengan `outletId = null` per merchant.

#### 3.2.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| `serviceChargePercentage` negatif | `400 Bad Request` |
| Buat setting untuk outletId yang sudah ada | `409 Conflict` — "Payment setting untuk outlet ini sudah ada" |
| GET outlet yang tidak ada di sistem | `404 Not Found` |
| `serviceChargeSource` bukan nilai enum valid | `400 Bad Request` — "serviceChargeSource tidak valid" |

---

### 3.3 Diskon Berbasis Kode (Discount)

#### 3.3.1 User Story

> Sebagai pemilik merchant, saya ingin membuat kode diskon yang dapat dimasukkan pelanggan saat checkout, dengan kemampuan membatasi diskon pada produk atau kategori tertentu, outlet tertentu, saluran penjualan tertentu, serta mengelola kuota pemakaian dan masa berlaku.

#### 3.3.2 Model Data

**Tabel: `discount`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| code | String | Kode unik diskon (case-insensitive) |
| name | String | Nama deskriptif |
| valueType | Enum | `PERCENTAGE` \| `AMOUNT` |
| value | Decimal | Nilai diskon (% atau amount) |
| maxDiscountAmount | Decimal / null | Batas maksimal diskon (untuk valueType PERCENTAGE) |
| minPurchase | Decimal / null | Minimum subtotal untuk kode berlaku |
| scope | Enum | `ALL` \| `PRODUCT` \| `CATEGORY` |
| visibility | Enum | `ALL_OUTLET` \| `SPECIFIC_OUTLET` |
| channel | Enum | `POS` \| `ONLINE` \| `BOTH` |
| startDate | Timestamp / null | Tanggal mulai berlaku |
| endDate | Timestamp / null | Tanggal berakhir |
| usageLimit | Integer / null | Total kuota pemakaian (null = tidak terbatas) |
| usagePerCustomer | Integer / null | Kuota per pelanggan (null = tidak terbatas) |
| usageCount | Integer | Jumlah pemakaian saat ini |
| isActive | Boolean | Soft delete flag |

**Tabel: `discount_product`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| discountId | UUID | FK ke discount |
| productId | UUID | FK ke product |

**Tabel: `discount_category`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| discountId | UUID | FK ke discount |
| categoryId | UUID | FK ke category |

**Tabel: `discount_outlet`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| discountId | UUID | FK ke discount |
| outletId | UUID | FK ke outlet |

#### 3.3.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/discount/list` | Daftar semua diskon aktif merchant |
| GET | `/pos/discount/detail/{id}` | Detail satu diskon beserta binding produk/kategori/outlet |
| POST | `/pos/discount/add` | Tambah diskon baru |
| PUT | `/pos/discount/update` | Update diskon |
| DELETE | `/pos/discount/delete/{id}` | Soft delete diskon |
| POST | `/pos/discount/validate` | Validasi kode diskon saat checkout |

#### 3.3.4 Alur Fungsional

**Validasi Kode Diskon:**
```
Kasir → POST /pos/discount/validate { code, cartTotal, customerId?, outletId, channel }
    ↓
Cari discount dengan code (case-insensitive) AND merchantId = current AND isActive = true
    [Tidak ditemukan] → 404 "Kode diskon tidak ditemukan"
    ↓
Cek channel: apakah channel request sesuai? (POS/ONLINE/BOTH)
    [Tidak sesuai] → 400 "Kode tidak berlaku untuk channel ini"
    ↓
Cek visibility: jika SPECIFIC_OUTLET → cek outletId ada di discount_outlet
    [Tidak ada] → 400 "Kode tidak berlaku untuk outlet ini"
    ↓
Cek startDate/endDate (jika diisi)
    [Di luar rentang] → 400 "Kode diskon belum/sudah tidak berlaku"
    ↓
Cek usageLimit: usageCount < usageLimit (jika usageLimit tidak null)
    [Kuota habis] → 400 "Kuota kode diskon telah habis"
    ↓
Cek usagePerCustomer (jika customerId tersedia)
    [Melebihi batas] → 400 "Anda telah mencapai batas pemakaian kode ini"
    ↓
Cek minPurchase: cartTotal >= minPurchase (jika minPurchase tidak null)
    [Tidak memenuhi] → 400 "Minimum pembelian tidak terpenuhi"
    ↓
Hitung discountAmount:
    Jika valueType = PERCENTAGE:
        discountAmount = cartTotal × (value / 100)
        Jika maxDiscountAmount diisi: discountAmount = MIN(discountAmount, maxDiscountAmount)
    Jika valueType = AMOUNT:
        discountAmount = value
    ↓
Return { isValid: true, discountAmount, discount }
```

**Scope Filtering:**
```
Jika scope = PRODUCT:
    Hitung subtotal hanya dari item yang productId ada di discount_product
    discountAmount dihitung dari subtotal item-item tersebut

Jika scope = CATEGORY:
    Hitung subtotal item yang categoryId ada di discount_category
    discountAmount dihitung dari subtotal item-item tersebut

Jika scope = ALL:
    discountAmount dihitung dari total keseluruhan cart
```

#### 3.3.5 Business Rules

1. **Kode Unik per Merchant:** Tidak boleh ada dua kode diskon aktif dengan kode yang sama (case-insensitive) dalam satu merchant.
2. **Increment usageCount:** Setelah transaksi dengan kode diskon mencapai status `PAID`, sistem otomatis mengincrementkan `usageCount`.
3. **maxDiscountAmount:** Hanya berlaku untuk `valueType = PERCENTAGE`. Jika tidak diisi, diskon tidak dibatasi.
4. **Scope Binding:** Saat `scope = PRODUCT`, setidaknya satu `discount_product` binding harus ada. Begitu pula untuk `scope = CATEGORY`.
5. **Channel POS:** Kode yang dibuat dari POS app dengan channel `POS` tidak bisa digunakan di platform online, dan sebaliknya.

#### 3.3.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Kode duplikat (case-insensitive) | `409 Conflict` — "Kode diskon sudah digunakan" |
| scope = PRODUCT tanpa binding produk | `400 Bad Request` |
| value negatif | `400 Bad Request` |
| PERCENTAGE > 100 | `400 Bad Request` |
| cartTotal setelah SC/tax lebih rendah dari minPurchase | Evaluasi minPurchase harus menggunakan grossSubTotal pre-tax, harus dikonfirmasi |

---

### 3.4 Promosi Otomatis (Promotion)

#### 3.4.1 User Story

> Sebagai pemilik merchant, saya ingin membuat promosi yang otomatis diterapkan saat kondisi cart memenuhi syarat — tanpa pelanggan perlu memasukkan kode — dengan kemampuan mendefinisikan prioritas, kombinabilitas, dan tipe reward yang fleksibel.

#### 3.4.2 Model Data

**Tabel: `promotion`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama promosi |
| type | Enum | `DISCOUNT_BY_ORDER` \| `BUY_X_GET_Y` \| `DISCOUNT_BY_ITEM_SUBTOTAL` |
| priority | Integer | Angka lebih kecil = lebih diprioritaskan |
| canCombine | Boolean | Bisa digabung dengan promosi lain |
| startDate | Timestamp / null | Tanggal mulai |
| endDate | Timestamp / null | Tanggal berakhir |
| validDays | Array\<String\> | Hari-hari aktif: `["MON","TUE","WED","THU","FRI","SAT","SUN"]` |
| minPurchase | Decimal / null | Minimum subtotal |
| isActive | Boolean | Soft delete flag |

**Konfigurasi Kondisi dan Reward (BUY_X_GET_Y):**

| Kolom | Tipe | Keterangan |
|---|---|---|
| buyProductId | UUID | Produk yang harus dibeli |
| buyQty | Integer | Jumlah yang harus dibeli |
| getProductId | UUID | Produk yang mendapat reward |
| getQty | Integer | Jumlah produk reward |
| rewardType | Enum | `FREE` \| `PERCENTAGE` \| `AMOUNT` \| `FIXED_PRICE` |
| rewardValue | Decimal | Nilai reward (untuk non-FREE) |
| allowMultiple | Boolean | Reward berlipat per kelipatan buyQty |

#### 3.4.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/promotion/list` | Daftar semua promosi aktif |
| GET | `/pos/promotion/detail/{id}` | Detail promosi |
| POST | `/pos/promotion/add` | Tambah promosi baru |
| PUT | `/pos/promotion/update` | Update promosi |
| DELETE | `/pos/promotion/delete/{id}` | Soft delete promosi |

#### 3.4.4 Alur Fungsional

**Evaluasi Promosi Saat Transaksi Dibuat:**
```
Sistem ambil semua promosi aktif merchant (isActive = true)
    ↓
Filter berdasarkan validitas waktu:
    - Hari ini berada di antara startDate dan endDate (jika diisi)
    - Hari ini adalah hari yang ada di validDays (jika diisi)
    ↓
Urutkan berdasarkan priority (ASC)
    ↓
Untuk setiap promosi (dalam urutan prioritas):
    Evaluasi kondisi (minPurchase, buyQty, dll.)
    Jika terpenuhi:
        Hitung promoAmount
        Jika canCombine = false: terapkan promosi ini saja, hentikan loop
        Jika canCombine = true: terapkan dan lanjut ke promosi berikutnya
    ↓
Total promoAmount = SUM semua reward yang berhasil diterapkan
```

**Kalkulasi per Tipe Promosi:**

**DISCOUNT_BY_ORDER:**
```
Jika cartSubTotal >= minPurchase:
    promoAmount = cartSubTotal × (discountPercentage / 100)
    Atau: promoAmount = discountAmount (fixed)
```

**BUY_X_GET_Y:**
```
Cek apakah buyProductId ada di cart dengan qty >= buyQty
    ↓
Jika allowMultiple = true:
    multiplier = FLOOR(cartQtyOfBuyProduct / buyQty)
Jika allowMultiple = false:
    multiplier = 1
    ↓
Berdasarkan rewardType:
    FREE: promoAmount += getProductPrice × getQty × multiplier
    PERCENTAGE: promoAmount += getProductPrice × (rewardValue/100) × getQty × multiplier
    AMOUNT: promoAmount += rewardValue × getQty × multiplier
    FIXED_PRICE: promoAmount += (getProductPrice - rewardValue) × getQty × multiplier
```

**DISCOUNT_BY_ITEM_SUBTOTAL:**
```
Untuk setiap item yang memenuhi kondisi:
    promoAmount += itemSubTotal × (discountPercentage / 100)
```

#### 3.4.5 Business Rules

1. **Auto-Apply:** Promosi tidak memerlukan kode. Sistem mengevaluasi semua promosi yang memenuhi syarat secara otomatis setiap kali transaksi dibuat atau di-recalculate.
2. **Priority dan Kombinasi:** Jika ada dua promosi aktif di waktu yang sama, promosi dengan priority lebih kecil dievaluasi lebih dulu. Jika `canCombine = false`, setelah promosi tersebut diterapkan, evaluasi berhenti — promosi dengan priority lebih rendah tidak dievaluasi lagi.
3. **validDays:** Format hari menggunakan singkatan 3 huruf Inggris: `MON`, `TUE`, `WED`, `THU`, `FRI`, `SAT`, `SUN`. Jika `validDays` kosong, promosi berlaku setiap hari.
4. **Reward FREE:** Ketika `rewardType = FREE`, sistem menambahkan item reward ke cart dengan harga 0 atau mengurangi harga item reward dari total.
5. **Snapshot:** Nilai `promoAmount` di transaksi adalah snapshot kalkulasi saat transaksi dibuat.

#### 3.4.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Dua promosi aktif dengan canCombine = false dan priority sama | Sistem ambil satu secara acak (behavior ini perlu dikonfirmasi / sebaiknya validasi saat pembuatan) |
| buyProductId tidak ada di master produk | `400 Bad Request` saat membuat promosi |
| FIXED_PRICE rewardValue > harga produk | promoAmount bisa negatif — perlu cap di 0 |
| Cart tidak memenuhi kondisi promosi manapun | promoAmount = 0, transaksi tetap valid |

---

### 3.5 Price Book

#### 3.5.1 User Story

> Sebagai pemilik merchant, saya ingin menetapkan harga khusus yang berbeda dari harga standar produk — misalnya harga grosir untuk pembelian banyak, harga berbeda untuk takeaway vs dine-in, atau harga promosi untuk kategori tertentu — yang diterapkan otomatis sebelum perhitungan lainnya.

#### 3.5.2 Model Data

**Tabel: `price_book`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama price book |
| type | Enum | `PRODUCT` \| `ORDER_TYPE` \| `CATEGORY` \| `WHOLESALE` |
| visibility | Enum | `ALL_OUTLET` \| `SPECIFIC_OUTLET` |
| isDefault | Boolean | Aktif secara default |
| isActive | Boolean | Soft delete flag |
| startDate | Timestamp / null | Tanggal mulai |
| endDate | Timestamp / null | Tanggal berakhir |

**Tabel: `price_book_item`** (untuk PRODUCT dan CATEGORY)

| Kolom | Tipe | Keterangan |
|---|---|---|
| priceBookId | UUID | FK ke price_book |
| productId / categoryId | UUID | FK ke produk atau kategori |
| adjustType | Enum | `FIXED` \| `PERCENTAGE_INCREASE` \| `PERCENTAGE_DECREASE` \| `AMOUNT_INCREASE` \| `AMOUNT_DECREASE` |
| adjustValue | Decimal | Nilai penyesuaian |

**Tabel: `price_book_wholesale_tier`** (untuk WHOLESALE)

| Kolom | Tipe | Keterangan |
|---|---|---|
| priceBookId | UUID | FK ke price_book |
| productId | UUID | FK ke produk |
| minQty | Integer | Minimum qty untuk tier ini |
| price | Decimal | Harga per unit untuk tier ini |

#### 3.5.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/price-book/list` | Daftar semua price book |
| GET | `/pos/price-book/detail/{id}` | Detail price book beserta items |
| POST | `/pos/price-book/add` | Tambah price book baru |
| PUT | `/pos/price-book/update` | Update price book |
| DELETE | `/pos/price-book/delete/{id}` | Soft delete price book |

#### 3.5.4 Alur Fungsional

**Resolusi Harga dengan Price Book:**
```
Saat transaksi dibuat, untuk setiap item:
    ↓
Cari price book yang aktif untuk outlet/merchant:
    - isActive = true
    - isDefault = true ATAU sesuai kondisi yang di-apply
    - Jika startDate/endDate diisi: tanggal sekarang dalam rentang
    ↓
Terapkan berdasarkan type:

    [PRODUCT]
        Cari price_book_item dengan productId = item.productId
        Hitung harga baru berdasarkan adjustType dan adjustValue

    [CATEGORY]
        Cari price_book_item dengan categoryId = item.categoryId
        Hitung harga baru berdasarkan adjustType dan adjustValue

    [ORDER_TYPE]
        Cek orderType transaksi (DINE_IN / TAKE_AWAY)
        Ambil harga sesuai orderType dari konfigurasi price book

    [WHOLESALE]
        Cari tier yang sesuai: MAX tier di mana minQty <= item.qty
        Gunakan price dari tier tersebut sebagai harga satuan
    ↓
unitPrice item = harga hasil price book (menggantikan harga standar)
    ↓
itemSubTotal = unitPrice × qty
```

#### 3.5.5 Business Rules

1. **Layer Pertama:** Price Book adalah layer pertama dalam kalkulasi 5-layer. Harga hasil Price Book menjadi basis untuk layer selanjutnya (promosi, diskon, SC, pajak).
2. **isDefault:** Price book yang `isDefault = true` diterapkan otomatis ke semua transaksi tanpa memerlukan seleksi manual.
3. **Prioritas jika Multiple:** Jika ada beberapa price book aktif untuk item yang sama, ambil price book dengan prioritas tertinggi (atau yang paling spesifik: PRODUCT > CATEGORY > ORDER_TYPE).
4. **Wholesale Tier:** Untuk WHOLESALE, jika qty item tidak memenuhi tier manapun (lebih kecil dari minQty tier terendah), gunakan harga standar.
5. **visibility:** Jika `visibility = SPECIFIC_OUTLET`, price book hanya berlaku di outlet yang terdaftar di binding `price_book_outlet`.

#### 3.5.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| adjustType = FIXED_PRICE menghasilkan harga negatif | Harga dikap di 0 |
| PERCENTAGE_DECREASE > 100% | `400 Bad Request` |
| Tidak ada tier WHOLESALE yang cocok dengan qty | Gunakan harga standar produk |
| Price book aktif tapi startDate di masa depan | Price book tidak diterapkan |

---

### 3.6 Voucher

#### 3.6.1 User Story

> Sebagai pemilik merchant, saya ingin mengelola voucher (fisik atau digital) yang dapat digunakan pelanggan sebagai alat pembayaran parsial, dengan kemampuan mengorganisir voucher dalam hierarki brand → grup → kode individual, dan melakukan import kode secara massal.

#### 3.6.2 Model Data

**Hierarki:**
```
VoucherBrand (1) → VoucherGroup (banyak) → VoucherCode (banyak)
```

**Tabel: `voucher_brand`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama brand voucher |
| description | String / null | Deskripsi |
| isActive | Boolean | Soft delete flag |

**Tabel: `voucher_group`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| voucherBrandId | UUID | FK ke voucher_brand |
| name | String | Nama grup |
| purchasePrice | Decimal | Harga beli voucher (biaya merchant) |
| sellingPrice | Decimal | Nilai nominal voucher (digunakan saat transaksi) |
| expiredDate | Timestamp / null | Tanggal kadaluarsa |
| validDays | Integer / null | Masa berlaku dalam hari dari tanggal aktivasi |
| isRequiredCustomer | Boolean | Apakah wajib tautkan ke customer |
| isActive | Boolean | Soft delete flag |

**Tabel bisnis: `voucher_code`**

Catatan implementasi: pada schema migration, entitas ini dipersist ke tabel `voucher` yang direpurpose menjadi kode voucher individual.

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| voucherGroupId | UUID | FK ke voucher_group |
| code | String | Kode unik voucher |
| status | Enum | `AVAILABLE` \| `USED` \| `EXPIRED` \| `CANCELLED` |
| customerId | UUID / null | Customer yang memegang voucher (jika isRequiredCustomer) |
| usedDate | Timestamp / null | Tanggal digunakan |
| transactionId | UUID / null | FK ke transaksi penggunaan |

#### 3.6.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/voucher/brand/list` | Daftar voucher brand |
| POST | `/pos/voucher/brand/add` | Tambah brand |
| PUT | `/pos/voucher/brand/update` | Update brand |
| DELETE | `/pos/voucher/brand/delete/{id}` | Soft delete brand |
| GET | `/pos/voucher/group/list` | Daftar voucher group |
| POST | `/pos/voucher/group/add` | Tambah group |
| PUT | `/pos/voucher/group/update` | Update group |
| DELETE | `/pos/voucher/group/delete/{id}` | Soft delete group |
| GET | `/pos/voucher/code/list` | Daftar kode voucher |
| POST | `/pos/voucher/code/add` | Tambah kode individual |
| POST | `/pos/voucher/code/bulk-import` | Import banyak kode sekaligus |
| PUT | `/pos/voucher/code/cancel/{id}` | Cancel kode voucher |
| POST | `/pos/voucher/validate` | Validasi kode sebelum digunakan di transaksi |

#### 3.6.4 Alur Fungsional

**Validasi dan Penggunaan Voucher:**
```
Kasir → POST /pos/voucher/validate { code, customerId? }
    ↓
Cari voucher_code dengan code = input AND status = AVAILABLE
    [Tidak ditemukan / sudah USED/EXPIRED/CANCELLED] → 400 Error
    ↓
Ambil voucher_group dari voucher_code
    Cek expiredDate (jika diisi): tanggal sekarang < expiredDate
    Jika validDays diisi dan voucher sudah diaktifkan: cek tanggal aktivasi + validDays
    ↓
Cek isRequiredCustomer:
    Jika true dan customerId tidak diberikan → 400 "Voucher ini memerlukan data customer"
    Jika true dan customerId tidak cocok dengan yang tautkan → 400 "Voucher bukan milik customer ini"
    ↓
Return { isValid: true, voucherAmount: sellingPrice, voucherCode }
    ↓
Saat transaksi selesai (PAID):
    UPDATE voucher_code: status = USED, usedDate = now, transactionId = transaksiId
```

**Bulk Import:**
```
POST /pos/voucher/code/bulk-import { voucherGroupId, codes: ["CODE1","CODE2",...] }
    ↓
Validasi: voucherGroupId valid dan milik merchant
    ↓
Untuk setiap code:
    Cek duplikat dalam batch dan dengan data yang ada
    Insert jika unik, skip jika duplikat (dengan log)
    ↓
Return { imported: N, skipped: M, skippedCodes: [...] }
```

#### 3.6.5 Business Rules

1. **voucherAmount di Transaksi:** Nilai voucher (`sellingPrice` dari voucher_group) mengurangi `totalAmount` di Layer 4, setelah SC dan pajak dihitung.
2. **Satu Voucher per Transaksi:** Dalam satu transaksi, hanya satu kode voucher yang dapat digunakan (kecuali ada kebijakan khusus).
3. **Status Immutability:** Voucher yang sudah `USED` atau `CANCELLED` tidak dapat kembali ke `AVAILABLE`.
4. **Kode Unik Global:** Kode voucher harus unik di seluruh sistem (atau minimal per merchant).
5. **Expired Voucher:** Sistem secara periodik (atau saat validasi) mengubah status voucher yang melewati `expiredDate` menjadi `EXPIRED`.

#### 3.6.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Kode sudah USED | `400 Bad Request` — "Voucher sudah digunakan" |
| Kode EXPIRED | `400 Bad Request` — "Voucher sudah kadaluarsa" |
| Bulk import dengan semua kode duplikat | `200 OK` dengan `imported: 0, skipped: N` |
| sellingPrice > totalAmount transaksi | voucherAmount dikap di totalAmount (tidak menghasilkan kembalian) |

---

### 3.7 CRM Customer

#### 3.7.1 User Story

> Sebagai pemilik merchant, saya ingin menyimpan data pelanggan, melacak riwayat transaksi dan total pengeluaran mereka, serta memberikan program loyalty point yang dapat ditukarkan saat checkout. Saya juga ingin bisa menyesuaikan poin pelanggan secara manual jika diperlukan.

#### 3.7.2 Model Data

**Tabel: `customer`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama pelanggan |
| phone | String / null | Nomor telepon (unik per merchant) |
| email | String / null | Email (unik per merchant) |
| address | String / null | Alamat |
| gender | Enum / null | `MALE` \| `FEMALE` \| `OTHER` |
| loyaltyPoints | Integer | Saldo poin saat ini (default 0) |
| totalTransaction | Integer | Jumlah transaksi yang sudah PAID |
| totalSpend | Decimal | Total nilai transaksi yang sudah PAID |
| isActive | Boolean | Soft delete flag |
| createdAt | Timestamp | Waktu registrasi |

**Tabel bisnis: `loyalty_history`**

Catatan implementasi: pada schema migration, audit trail poin disimpan di tabel `loyalty_transaction`.

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| customerId | UUID | FK ke customer |
| merchantId | UUID | FK ke merchant |
| type | Enum | `EARN` \| `REDEEM` \| `ADJUST` \| `EXPIRE` |
| points | Integer | Jumlah poin (positif = earn, negatif = redeem/adjust negatif) |
| transactionId | UUID / null | FK ke transaksi terkait |
| notes | String / null | Catatan untuk penyesuaian manual |
| createdAt | Timestamp | Waktu mutasi |

#### 3.7.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/customer/list` | Daftar pelanggan; query params: `phone`, `email` |
| GET | `/pos/customer/detail/{id}` | Detail pelanggan + statistik |
| POST | `/pos/customer/add` | Tambah pelanggan baru |
| PUT | `/pos/customer/update` | Update data pelanggan |
| DELETE | `/pos/customer/delete/{id}` | Soft delete pelanggan |
| GET | `/pos/customer/{id}/loyalty-history` | Riwayat mutasi poin |
| PUT | `/pos/customer/loyalty/adjust` | Penyesuaian poin manual |

#### 3.7.4 Alur Fungsional

**Earn Points Otomatis:**
```
Transaksi berhasil (status → PAID)
    ↓
Sistem baca konfigurasi loyalty merchant (berapa poin per Rp X)
    ↓
earnPoints = FLOOR(amountDue / loyaltyEarnRate)
    ↓
UPDATE customer: loyaltyPoints += earnPoints, totalTransaction++, totalSpend += amountDue
    ↓
INSERT loyalty_history: type = EARN, points = earnPoints, transactionId = id
```

**Redeem Points saat Checkout:**
```
Kasir → request transaksi dengan loyaltyRedeemPoints = N
    ↓
Cek: customer.loyaltyPoints >= N
    [Tidak cukup] → 400 "Poin tidak mencukupi"
    ↓
loyaltyRedeemAmount = N × loyaltyRedeemRate (nilai Rupiah per poin)
    ↓
UPDATE customer: loyaltyPoints -= N
    ↓
INSERT loyalty_history: type = REDEEM, points = -N, transactionId = id
    ↓
amountDue = totalAmount - voucherAmount - loyaltyRedeemAmount
```

**Penyesuaian Manual:**
```
Admin → PUT /pos/customer/loyalty/adjust { customerId, points, notes }
    ↓
UPDATE customer: loyaltyPoints += points (bisa negatif untuk pengurangan)
    ↓
INSERT loyalty_history: type = ADJUST, points = points, notes = notes
    ↓
Jika loyaltyPoints < 0 setelah adjust → dikap di 0 atau ditolak (tergantung kebijakan)
```

#### 3.7.5 Business Rules

1. **Uniqueness per Merchant:** `phone` dan `email` masing-masing harus unik dalam satu merchant (jika diisi).
2. **Statistik Otomatis:** `totalTransaction` dan `totalSpend` tidak boleh diubah secara manual via API. Keduanya hanya diupdate oleh sistem saat transaksi PAID.
3. **Poin Tidak Boleh Negatif:** Saldo `loyaltyPoints` tidak boleh di bawah 0. Jika adjust manual menghasilkan nilai negatif, sistem menolak atau mengap di 0.
4. **Riwayat Immutable:** `loyalty_history` adalah log audit — tidak ada endpoint untuk mengubah atau menghapus riwayat.
5. **Search:** GET list dengan parameter `phone` atau `email` menggunakan pencarian exact match atau LIKE, tergantung kebutuhan UX.

#### 3.7.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Redeem poin lebih dari saldo | `400 Bad Request` — "Poin tidak mencukupi" |
| Duplikat phone dalam merchant | `409 Conflict` |
| Duplikat email dalam merchant | `409 Conflict` |
| Adjust poin menghasilkan negatif | `400 Bad Request` atau cap ke 0 |
| Customer tidak aktif mencoba redeem | `400 Bad Request` — "Customer tidak aktif" |

---

### 3.8 Cashier Management

#### 3.8.1 User Story

> Sebagai manajer toko, saya ingin mengelola akun kasir — menambah, menonaktifkan, mengatur PIN untuk otorisasi operasi sensitif (seperti refund atau override diskon), dan mereset password — tanpa mengekspos hash PIN ke klien.

#### 3.8.2 Model Data

**Tabel: `users`** (data autentikasi)

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| outletId | UUID / null | FK ke outlet (kasir terikat outlet) |
| username | String | Username unik |
| password | String | BCrypt hash password |
| role | Enum | `ADMIN` \| `CASHIER` \| `MANAGER` |
| isActive | Boolean | Soft delete flag |

**Tabel: `user_detail`** (data profil dan PIN)

| Kolom | Tipe | Keterangan |
|---|---|---|
| userId | UUID | FK ke users (1-to-1) |
| fullName | String | Nama lengkap kasir |
| phone | String / null | Nomor telepon |
| pin | String / null | BCrypt hash PIN (4-6 digit) |
| hasPin | Boolean | Flag apakah PIN sudah diset |

#### 3.8.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/cashier/list` | Daftar kasir aktif milik merchant |
| GET | `/pos/cashier/detail/{id}` | Detail kasir (tanpa PIN hash) |
| POST | `/pos/cashier/add` | Tambah kasir (create User + UserDetail) |
| PUT | `/pos/cashier/update` | Update data kasir |
| DELETE | `/pos/cashier/delete/{id}` | Soft delete (isActive = false) |
| POST | `/pos/cashier/set-pin` | Set atau update PIN kasir |
| POST | `/pos/cashier/verify-pin` | Verifikasi PIN untuk operasi sensitif |
| PUT | `/pos/cashier/reset-password` | Reset password kasir |

#### 3.8.4 Alur Fungsional

**Tambah Kasir:**
```
Admin → POST /pos/cashier/add { username, password, fullName, phone?, outletId? }
    ↓
Validasi: username unik dalam merchant
    ↓
Hash password dengan BCrypt
    ↓
INSERT users (merchantId, outletId, username, hashedPassword, role = CASHIER, isActive = true)
    ↓
INSERT user_detail (userId, fullName, phone, pin = null, hasPin = false)
    ↓
Return data kasir (tanpa password hash)
```

**Set PIN:**
```
Admin/Kasir → POST /pos/cashier/set-pin { cashierId, pin }
    ↓
Validasi: pin hanya angka, 4-6 digit
    ↓
Hash PIN dengan BCrypt
    ↓
UPDATE user_detail: pin = hashedPin, hasPin = true
    ↓
Return { success: true, message: "PIN berhasil disimpan" } (PIN tidak dikembalikan)
```

**Verifikasi PIN untuk Operasi Sensitif:**
```
Kasir → POST /pos/cashier/verify-pin { cashierId, pin }
    ↓
Ambil user_detail.pin untuk cashierId
    ↓
BCrypt.verify(inputPin, storedHashedPin)
    ↓
    [Match] → Return { verified: true, token: operationToken (short-lived) }
    [No match] → Return { verified: false } atau 401
```

#### 3.8.5 Business Rules

1. **PIN Tidak Pernah Diekspos:** API tidak pernah mengembalikan nilai field `pin` (hash). Yang dikembalikan hanya `hasPin: true/false`.
2. **Soft Delete:** Menghapus kasir hanya mengubah `isActive = false`. Kasir tidak aktif tidak bisa login tetapi data dan riwayat transaksinya tetap tersimpan.
3. **Scope Merchant:** Admin hanya bisa mengelola kasir dalam merchantId yang sama. Cross-merchant access dilarang.
4. **Reset Password:** Reset password oleh admin tidak memerlukan password lama. Sistem langsung mengganti dengan password baru yang di-hash.
5. **PIN Otorisasi:** PIN digunakan untuk mengotorisasi operasi sensitif (refund, override diskon, void transaksi). Setiap operasi sensitif harus divalidasi dengan verifikasi PIN terlebih dahulu.

#### 3.8.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Username duplikat dalam merchant | `409 Conflict` |
| PIN bukan angka atau kurang dari 4 digit | `400 Bad Request` |
| Verify PIN tanpa PIN yang di-set | `400 Bad Request` — "Kasir belum memiliki PIN" |
| Admin akses kasir merchant lain | `403 Forbidden` |

---

### 3.9 Receipt Template & Printer Settings

#### 3.9.1 User Story

> Sebagai pemilik merchant, saya ingin mengkustomisasi tampilan struk (header, footer, informasi yang ditampilkan) dan mengkonfigurasi printer fisik per outlet, termasuk menentukan printer default untuk setiap tipe (struk pelanggan, dapur, order).

#### 3.9.2 Model Data

**Tabel: `receipt_template`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| outletId | UUID / null | null = template default merchant; diisi = override outlet |
| header | String / null | Teks header struk |
| footer | String / null | Teks footer struk |
| showTax | Boolean | Tampilkan baris pajak di struk |
| showServiceCharge | Boolean | Tampilkan baris SC di struk |
| showRounding | Boolean | Tampilkan baris pembulatan |
| showLogo | Boolean | Tampilkan logo |
| logoUrl | String / null | URL logo |
| showQueueNumber | Boolean | Tampilkan nomor antrian |
| paperSize | Enum | `58MM` \| `80MM` |

**Tabel bisnis: `printer`**

Catatan implementasi: pada schema migration, entitas ini dipersist ke tabel `printer_setting`.

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| outletId | UUID | FK ke outlet (printer selalu terikat outlet) |
| name | String | Nama printer |
| type | Enum | `RECEIPT` \| `KITCHEN` \| `ORDER` |
| connectionType | Enum | `NETWORK` \| `USB` \| `BLUETOOTH` |
| ipAddress | String / null | IP address (untuk NETWORK) |
| port | Integer / null | Port (untuk NETWORK) |
| usbPath | String / null | Path USB (untuk USB) |
| bluetoothAddress | String / null | MAC address (untuk BLUETOOTH) |
| isDefault | Boolean | Default printer untuk type ini di outlet ini |

#### 3.9.3 Endpoints

**Receipt Template:**

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/receipt/list` | Daftar template (default + semua outlet) |
| GET | `/pos/receipt/detail/{id}` | Detail template |
| GET | `/pos/receipt/outlet/{outletId}` | Template aktif untuk outlet; fallback ke default |
| POST | `/pos/receipt/create` | Buat template baru |
| PUT | `/pos/receipt/update` | Update template |
| DELETE | `/pos/receipt/delete/{id}` | Hard delete template |

**Printer:**

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/printer/list` | Daftar printer milik merchant |
| GET | `/pos/printer/outlet/{outletId}` | Daftar printer per outlet |
| POST | `/pos/printer/add` | Tambah printer baru |
| PUT | `/pos/printer/update` | Update printer |
| DELETE | `/pos/printer/delete/{id}` | Hard delete printer |
| PUT | `/pos/printer/set-default/{id}` | Set printer sebagai default |

#### 3.9.4 Alur Fungsional

**Resolusi Template Struk:**
```
Saat transaksi selesai, sistem ambil template untuk outlet:
    ↓
Cari receipt_template dengan merchantId = current AND outletId = transaksiOutletId
    [Ditemukan] → Gunakan template outlet-specific
    [Tidak ditemukan] → Cari dengan outletId = null (template default)
        [Ditemukan] → Gunakan template default
        [Tidak ditemukan] → Gunakan nilai default system (semua show = true, paperSize = 80MM)
```

**Set Default Printer:**
```
Admin → PUT /pos/printer/set-default/{id}
    ↓
Ambil printer yang di-set: type dan outletId
    ↓
UPDATE semua printer dengan type = {type} AND outletId = {outletId}: isDefault = false
    ↓
UPDATE printer {id}: isDefault = true
    ↓
Return data printer
```

#### 3.9.5 Business Rules

1. **Hard Delete:** Berbeda dengan modul lain, receipt template dan printer menggunakan hard delete. Pertimbangkan dampak jika ada transaksi yang masih mereferensi template.
2. **Fallback Struk:** Jika outlet tidak punya template sendiri, sistem menggunakan template default merchant. Ini konsisten dengan pattern di Payment Setting.
3. **Default Printer per Type per Outlet:** Hanya boleh ada satu default printer untuk setiap kombinasi `(type, outletId)`. Mengeset printer baru sebagai default otomatis membatalkan default sebelumnya.
4. **Printer Terikat Outlet:** Setiap printer harus terikat ke satu outlet. Printer tidak bisa menjadi "global merchant-level".
5. **connectionType dan Field Opsional:** Berdasarkan `connectionType`, field yang relevan adalah: NETWORK → `ipAddress` + `port`; USB → `usbPath`; BLUETOOTH → `bluetoothAddress`.

#### 3.9.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Set default printer non-existent | `404 Not Found` |
| Printer milik outlet yang tidak ada | `400 Bad Request` |
| Delete template yang sudah hard-delete | `404 Not Found` |
| Template outlet dihapus; tidak ada default | Struk menggunakan nilai sistem default |

---

### 3.10 Disbursement (Revenue Sharing)

#### 3.10.1 User Story

> Sebagai operator platform, saya ingin mendefinisikan aturan bagi hasil pendapatan dari setiap transaksi — misalnya platform mengambil 5%, dealer mengambil 3%, sisanya untuk merchant — dan menyimpan log disbursement per transaksi untuk pelaporan dan rekonsiliasi.

#### 3.10.2 Model Data

**Tabel: `disbursement_rule`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| layer | Enum | `PLATFORM` \| `DEALER` \| `MERCHANT` \| `CUSTOM` |
| recipientId | UUID / null | ID penerima (opsional untuk identifikasi) |
| recipientName | String | Nama penerima |
| percentage | Decimal | Persentase dari source |
| source | Enum | `GROSS` \| `NET` \| `NET_AFTER_TAX` \| `NET_AFTER_TAX_SC` |
| productTypeFilter | String / null | Filter jenis produk (null = semua) |
| displayOrder | Integer | Urutan tampil di laporan |
| isActive | Boolean | Soft delete flag |

**Tabel: `disbursement_log`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| transactionId | UUID | FK ke transaksi |
| disbursementRuleId | UUID | FK ke disbursement_rule |
| recipientName | String | Snapshot nama penerima |
| layer | Enum | Snapshot layer |
| baseAmount | Decimal | Nilai dasar yang digunakan (sesuai source) |
| amount | Decimal | Nilai disbursement (baseAmount × percentage) |
| status | Enum | `PENDING` \| `SETTLED` \| `FAILED` |
| settledDate | Timestamp / null | Tanggal settlement |

#### 3.10.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/disbursement/rule/list` | Daftar aturan disbursement |
| POST | `/pos/disbursement/rule/add` | Tambah aturan baru |
| PUT | `/pos/disbursement/rule/update` | Update aturan |
| DELETE | `/pos/disbursement/rule/delete/{id}` | Soft delete aturan |
| GET | `/pos/disbursement/log/list` | Daftar log disbursement (dengan filter tanggal/status) |
| GET | `/pos/disbursement/log/transaction/{transactionId}` | Log disbursement per transaksi |
| PUT | `/pos/disbursement/log/settle/{id}` | Mark disbursement sebagai SETTLED |
| GET | `/pos/disbursement/report` | Rekap per layer/recipient |

#### 3.10.4 Alur Fungsional

**Kalkulasi Disbursement saat Transaksi PAID:**
```
Transaksi → status = PAID
    ↓
Ambil semua disbursement_rule aktif untuk merchantId
    ↓
Untuk setiap rule:
    Tentukan baseAmount berdasarkan source:
        GROSS       → grossSubTotal
        NET         → netSubTotal (setelah promo + diskon)
        NET_AFTER_TAX → netSubTotal + totalTax
        NET_AFTER_TAX_SC → netSubTotal + totalTax + serviceCharge
    ↓
    Jika productTypeFilter diisi:
        Hitung baseAmount hanya dari item dengan productType yang cocok
    ↓
    amount = baseAmount × (percentage / 100)
    ↓
    INSERT disbursement_log { transactionId, ruleId, baseAmount, amount, status = PENDING, ... }
    ↓
Log dibuat per rule, per transaksi
```

**Settlement:**
```
Admin → PUT /pos/disbursement/log/settle/{id}
    ↓
Validasi: log dengan id tersebut ada dan status = PENDING
    ↓
UPDATE disbursement_log: status = SETTLED, settledDate = now
    ↓
Return log ter-update
```

#### 3.10.5 Business Rules

1. **Log Immutable:** Setelah dibuat, `disbursement_log` tidak dapat diedit kecuali perubahan status (PENDING → SETTLED / FAILED).
2. **Snapshot:** Field `recipientName` dan `layer` di log adalah snapshot nilai saat transaksi terjadi, agar perubahan aturan di kemudian hari tidak mempengaruhi log historis.
3. **Soft Delete Rule:** Menonaktifkan aturan (`isActive = false`) tidak menghapus log yang sudah ada. Aturan non-aktif tidak akan memproses transaksi baru.
4. **Jumlah Persentase:** Sistem tidak memvalidasi apakah total persentase semua rule = 100%. Ini memungkinkan skenario di mana sisa dikembalikan ke merchant secara implisit.
5. **productTypeFilter:** Memungkinkan splitting disbursement berdasarkan jenis produk, misal F&B vs non-F&B.

#### 3.10.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Settle log yang sudah SETTLED | `400 Bad Request` — "Sudah di-settle" |
| Rule dengan percentage > 100 | `400 Bad Request` |
| Transaksi tidak ada disbursement rule aktif | Tidak ada log yang dibuat; transaksi tetap valid |
| baseAmount = 0 | Disbursement log tetap dibuat dengan amount = 0 |

---

### 3.11 Financial Report

#### 3.11.1 User Story

> Sebagai pemilik merchant atau manajer, saya ingin melihat laporan keuangan yang komprehensif: ringkasan pendapatan, breakdown per metode pembayaran, produk terlaris, perbandingan antar outlet, dan rekap disbursement — semua dalam satu rentang tanggal yang dapat dikonfigurasi.

#### 3.11.2 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/report/summary` | Ringkasan keuangan keseluruhan |
| GET | `/pos/report/payment-method` | Breakdown per metode pembayaran |
| GET | `/pos/report/top-products` | Produk terlaris |
| GET | `/pos/report/outlet` | Perbandingan antar outlet |
| GET | `/pos/report/disbursement` | Rekap disbursement |

**Query Parameters Umum:**

| Parameter | Tipe | Keterangan |
|---|---|---|
| `startDate` | Date | Tanggal mulai filter (wajib) |
| `endDate` | Date | Tanggal akhir filter (wajib) |
| `outletId` | UUID | Filter per outlet (opsional, kecuali report outlet) |

#### 3.11.3 Spesifikasi Response per Endpoint

**GET `/pos/report/summary`**

Catatan implementasi: nama field response tetap `grossSubTotal` dan `netSubTotal`, tetapi persistence layer mengikuti kolom schema `gross_amount` dan `net_amount`.

```json
{
  "data": {
    "grossSubTotal": 0.00,
    "netSubTotal": 0.00,
    "totalTax": 0.00,
    "totalServiceCharge": 0.00,
    "totalDiscount": 0.00,
    "totalPromo": 0.00,
    "totalVoucher": 0.00,
    "totalLoyaltyRedeem": 0.00,
    "totalRounding": 0.00,
    "totalRefund": 0.00,
    "totalAmountDue": 0.00,
    "transactionCount": 0,
    "period": { "startDate": "...", "endDate": "..." }
  }
}
```

**GET `/pos/report/payment-method`**
```json
{
  "data": [
    {
      "paymentMethod": "CASH",
      "transactionCount": 0,
      "totalAmount": 0.00
    }
  ]
}
```

**GET `/pos/report/top-products`**

Query param: `limit` (default 10)

```json
{
  "data": [
    {
      "rank": 1,
      "productId": "uuid",
      "productName": "Produk A",
      "qtySold": 0,
      "totalRevenue": 0.00
    }
  ]
}
```

**GET `/pos/report/outlet`**
```json
{
  "data": [
    {
      "outletId": "uuid",
      "outletName": "Outlet A",
      "grossSubTotal": 0.00,
      "netSubTotal": 0.00,
      "totalTax": 0.00,
      "totalServiceCharge": 0.00,
      "totalAmountDue": 0.00,
      "transactionCount": 0
    }
  ]
}
```

**GET `/pos/report/disbursement`**
```json
{
  "data": [
    {
      "layer": "PLATFORM",
      "recipientName": "Platform Fee",
      "totalBaseAmount": 0.00,
      "totalAmount": 0.00,
      "transactionCount": 0,
      "settledCount": 0,
      "pendingCount": 0
    }
  ]
}
```

#### 3.11.4 Alur Fungsional

**Kalkulasi Summary Report:**
```
GET /pos/report/summary?startDate=&endDate=&outletId=
    ↓
Query transaksi: merchantId = current AND status = PAID
    AND createdAt BETWEEN startDate AND endDate
    AND (outletId = param JIKA param diisi)
    ↓
Agregasi:
    grossSubTotal = SUM(transaction.grossSubTotal)
    netSubTotal = SUM(transaction.netSubTotal)
    totalTax = SUM(transaction.totalTax)
    totalServiceCharge = SUM(transaction.serviceCharge)
    totalDiscount = SUM(transaction.discountAmount)
    totalPromo = SUM(transaction.promoAmount)
    totalVoucher = SUM(transaction.voucherAmount)
    totalLoyaltyRedeem = SUM(transaction.loyaltyRedeemAmount)
    totalRounding = SUM(transaction.rounding)
    totalRefund = SUM(refund.amount WHERE refund.transactionId IN hasil query)
    totalAmountDue = SUM(transaction.amountDue)
    transactionCount = COUNT(*)
    ↓
Return agregasi
```

#### 3.11.5 Business Rules

1. **Hanya Transaksi PAID:** Semua laporan hanya menghitung transaksi dengan status `PAID`. Transaksi pending, void, atau refund tidak masuk ke kalkulasi utama.
2. **Refund Ditampilkan Terpisah:** `totalRefund` ditampilkan sebagai nilai tersendiri, bukan pengurang dari gross. Ini memungkinkan analisis gross revenue sebelum refund.
3. **Periode Wajib:** `startDate` dan `endDate` wajib diisi untuk semua endpoint report. Tidak ada default "semua waktu" untuk mencegah query berat.
4. **top-products:** Default `limit = 10`. Maksimal limit yang diizinkan adalah 100.
5. **Disbursement Report:** Mengelompokkan berdasarkan `(layer, recipientName)`. Menampilkan `settledCount` vs `pendingCount` untuk memudahkan rekonsiliasi.

#### 3.11.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| startDate > endDate | `400 Bad Request` |
| Rentang tanggal > 1 tahun | `400 Bad Request` — "Rentang maksimal adalah 1 tahun" |
| Tidak ada transaksi di periode tersebut | Return data kosong/nol, bukan error |
| outletId tidak milik merchant | `403 Forbidden` |
| limit > 100 untuk top-products | Dikap di 100 |

---

### 3.12 Order Type & POS Language Setting

#### 3.12.1 User Story

> Sebagai merchant, saya ingin mendefinisikan tipe pesanan seperti dine-in, take away, atau delivery, menetapkan tipe default per outlet, dan mengatur bahasa default merchant/outlet agar perilaku transaksi dan konfigurasi POS konsisten di setiap cabang.

#### 3.12.2 Model Data

**Tabel: `order_type`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama tipe pesanan |
| code | String | Kode unik, contoh: `DINE_IN`, `TAKE_AWAY`, `DELIVERY` |
| isDefault | Boolean | Default tipe pesanan merchant |
| isActive | Boolean | Soft delete flag |

**Field tambahan:**

| Entitas | Kolom | Keterangan |
|---|---|---|
| `outlet` | `defaultOrderTypeId` | Default order type per outlet |
| `transaction` | `orderTypeId` | Snapshot tipe pesanan transaksi |
| `merchant` | `languageCode` | Bahasa default merchant |
| `outlet` | `languageCode` | Override bahasa per outlet |

#### 3.12.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/order-type/list` | Daftar order type aktif merchant |
| GET | `/pos/order-type/detail/{id}` | Detail order type |
| POST | `/pos/order-type/add` | Tambah order type |
| PUT | `/pos/order-type/update` | Update order type |
| DELETE | `/pos/order-type/delete/{id}` | Soft delete order type |
| PUT | `/pos/merchant/language` | Set bahasa default merchant |
| PUT | `/pos/outlet/{outletId}/language` | Set bahasa outlet |
| GET | `/pos/outlet/{outletId}/config` | Ambil konfigurasi outlet termasuk order type default dan language |

#### 3.12.4 Alur Fungsional

**Resolusi Order Type saat Transaksi Baru:**
```
Saat transaksi dibuat:
    Jika request membawa orderTypeId:
        validasi orderType milik merchant dan aktif
        simpan ke transaction.orderTypeId
    Jika request tidak membawa orderTypeId:
        ambil outlet.defaultOrderTypeId
        jika null, fallback ke order_type.isDefault = true
        simpan hasilnya ke transaction.orderTypeId
```

**Resolusi Bahasa POS:**
```
Saat client meminta konfigurasi outlet:
    cari outlet.languageCode
    jika null -> gunakan merchant.languageCode
    jika merchant.languageCode null -> fallback ke "id"
```

#### 3.12.5 Business Rules

1. **Minimal Satu Default Merchant:** Merchant sebaiknya memiliki satu `order_type.isDefault = true`. Jika default lama diganti, default lama otomatis di-unset.
2. **Outlet Override:** `outlet.defaultOrderTypeId` menggantikan default merchant hanya untuk outlet tersebut.
3. **Snapshot Transaksi:** `transaction.orderTypeId` adalah snapshot saat transaksi dibuat; perubahan default di kemudian hari tidak mengubah transaksi historis.
4. **Language Setting adalah Konfigurasi UI:** `languageCode` tidak mengubah kalkulasi backend; fungsinya untuk konfigurasi aplikasi POS/admin client.
5. **Nilai Bahasa Valid:** Minimal mendukung `id` dan `en`. Nilai lain harus ditolak sampai memang disupport product.

#### 3.12.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Set default order type ke record merchant lain | `404 Not Found` |
| Delete order type yang sedang dipakai transaksi baru | Soft delete diperbolehkan; transaksi historis tetap aman |
| outlet.defaultOrderTypeId mengarah ke order type nonaktif | Fallback ke default merchant |
| `languageCode` di luar daftar yang didukung | `400 Bad Request` |

---

### 3.13 Workshift & Cash Register

#### 3.13.1 User Story

> Sebagai kasir atau supervisor outlet, saya ingin membuka dan menutup shift, memasukkan opening cash, mencatat cash in/cash out, dan melihat selisih kas aktual versus expected cash agar kontrol operasional toko lebih rapi dan dapat diaudit.

#### 3.13.2 Model Data

**Tabel: `cashier_shift`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| outletId | UUID | FK ke outlet |
| userId | UUID | FK ke user/kasir |
| username | String | Snapshot username kasir |
| openingCash | Decimal | Saldo kas awal |
| closingCash | Decimal / null | Saldo kas akhir aktual |
| openDate | Timestamp | Waktu shift dibuka |
| closeDate | Timestamp / null | Waktu shift ditutup |
| status | Enum | `OPEN` \| `CLOSED` |
| note | String / null | Catatan shift |
| openedBy | String | Username pembuka |
| closedBy | String / null | Username penutup |

**Tabel tambahan (direkomendasikan): `cashier_shift_adjustment`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| shiftId | UUID | FK ke cashier_shift |
| type | Enum | `CASH_IN` \| `CASH_OUT` \| `ADJUSTMENT` |
| amount | Decimal | Nilai penyesuaian |
| reason | String | Alasan |
| createdBy | String | User yang mencatat |
| createdAt | Timestamp | Waktu pencatatan |

#### 3.13.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| POST | `/pos/shift/open` | Buka shift baru |
| PUT | `/pos/shift/close` | Tutup shift aktif |
| GET | `/pos/shift/current` | Shift aktif user/outlet |
| GET | `/pos/shift/list` | Riwayat shift |
| POST | `/pos/shift/adjustment/add` | Catat cash in/out/adjustment |
| GET | `/pos/shift/detail/{id}` | Detail shift + rekap kas |

#### 3.13.4 Alur Fungsional

**Buka Shift:**
```
Kasir -> POST /pos/shift/open { outletId, openingCash, note? }
    ↓
Validasi belum ada shift OPEN untuk user+outlet yang sama
    ↓
INSERT cashier_shift(status = OPEN, openDate = now, openingCash = value)
    ↓
Return shift aktif
```

**Tutup Shift:**
```
Kasir/Supervisor -> PUT /pos/shift/close { shiftId, closingCash, note? }
    ↓
Validasi shift masih OPEN
    ↓
Hitung expectedCash:
    openingCash
    + total pembayaran CASH pada transaksi shift
    + total CASH_IN
    - total CASH_OUT
    - total refund cash yang disetujui
    ↓
variance = closingCash - expectedCash
    ↓
UPDATE cashier_shift(status = CLOSED, closeDate = now, closingCash = value)
    ↓
Return shift summary
```

#### 3.13.5 Business Rules

1. **Satu Shift Aktif:** Satu user hanya boleh memiliki satu shift `OPEN` per outlet pada satu waktu.
2. **Binding ke Transaksi:** Setiap transaksi POS yang dibuat saat shift aktif harus menyimpan `transaction.shiftId`.
3. **Supervisor Close:** Shift dapat ditutup oleh supervisor/manager bila kasir berhalangan, dan `closedBy` harus merekam siapa yang menutup.
4. **Adjustment Immutable:** Catatan `cashier_shift_adjustment` tidak boleh diubah setelah dibuat; jika salah input, koreksi dilakukan dengan adjustment baru.
5. **Expected Cash Bukan Sumber Kebenaran Tunggal:** Sistem tetap menyimpan `closingCash` aktual walaupun berbeda dari `expectedCash`.

#### 3.13.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Buka shift saat masih ada shift aktif | `409 Conflict` |
| Tutup shift yang sudah CLOSED | `400 Bad Request` |
| Transaksi dibuat tanpa shift aktif | Boleh atau ditolak tergantung policy outlet; default disarankan `400 Bad Request` untuk POS kasir |
| Adjustment amount negatif | `400 Bad Request` |

---

### 3.14 Refund at POS

#### 3.14.1 User Story

> Sebagai supervisor atau kasir yang terotorisasi, saya ingin memproses refund penuh atau sebagian dari transaksi yang sudah dibayar, dengan audit trail yang lengkap dan kontrol approval, sehingga komplain pelanggan dapat ditangani tanpa merusak integritas laporan.

#### 3.14.2 Model Data

**Tabel: `refund`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| transactionId | UUID | FK ke transaksi asli |
| merchantId | UUID | FK ke merchant |
| outletId | UUID / null | FK ke outlet |
| amount | Decimal | Nilai refund |
| reason | String / null | Alasan refund |
| type | Enum | `FULL` \| `PARTIAL` |
| status | Enum | `PENDING` \| `APPROVED` \| `REJECTED` |
| refundBy | String | User pengaju |
| approvedBy | String / null | User approver |
| refundDate | Timestamp | Waktu pengajuan |
| approvedDate | Timestamp / null | Waktu approval |
| note | String / null | Catatan tambahan |

**Kolom snapshot di `transaction`:**

Catatan implementasi: pada schema migration, field snapshot berikut tersimpan sebagai `refund_amount`, `refund_reason`, `refund_by`, dan `refund_date`.

| Kolom | Keterangan |
|---|---|
| `refundAmount` | Akumulasi total refund yang disetujui |
| `refundReason` | Snapshot alasan refund terakhir atau utama |
| `refundBy` | Snapshot operator terakhir |
| `refundDate` | Snapshot waktu refund terakhir |

#### 3.14.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| POST | `/pos/refund/request` | Ajukan refund |
| PUT | `/pos/refund/approve` | Setujui refund |
| PUT | `/pos/refund/reject` | Tolak refund |
| GET | `/pos/refund/list` | Daftar refund |
| GET | `/pos/refund/detail/{id}` | Detail refund |

#### 3.14.4 Alur Fungsional

**Pengajuan Refund:**
```
Kasir/Supervisor -> POST /pos/refund/request { transactionId, amount, type, reason, operationToken }
    ↓
Validasi transaksi status = PAID
    ↓
Validasi amount > 0 dan amount <= refundableAmount
    ↓
INSERT refund(status = PENDING)
    ↓
Jika policy outlet tidak membutuhkan approval tambahan:
    proses approve otomatis
```

**Approval Refund:**
```
Manager/Admin -> PUT /pos/refund/approve { refundId, operationToken }
    ↓
Validasi refund.status = PENDING
    ↓
UPDATE refund.status = APPROVED, approvedDate = now
    ↓
UPDATE transaction.refundAmount += refund.amount
    ↓
Jika total refundAmount = transaction.amountDue:
    transaction.status = REFUNDED
Jika refundAmount < amountDue:
    transaction.status tetap PAID
```

#### 3.14.5 Business Rules

1. **Refundable Amount:** `refundableAmount = amountDue - totalApprovedRefund`.
2. **Full vs Partial:** `FULL` mensyaratkan `amount = refundableAmount` saat pengajuan. `PARTIAL` mensyaratkan `amount < refundableAmount`.
3. **Approval Flow:** Default yang direkomendasikan adalah kasir membuat `PENDING`, manager/admin mengubah ke `APPROVED` atau `REJECTED`.
4. **Laporan:** Refund tidak mengubah nilai gross historis; nilai refund ditampilkan terpisah di report.
5. **Loyalty dan Voucher:** Jika transaksi menghasilkan poin loyalty, sistem harus melakukan reversal proporsional atau penuh sesuai kebijakan merchant. Voucher yang sudah `USED` tidak otomatis dikembalikan ke `AVAILABLE`, kecuali ada kebijakan manual yang eksplisit.
6. **Disbursement:** Refund yang sudah di-approve harus mempengaruhi settlement/disbursement lanjutan, minimal melalui report dan reconciliation process.

#### 3.14.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Refund pada transaksi `PENDING` / `VOID` | `400 Bad Request` |
| amount melebihi refundableAmount | `400 Bad Request` |
| Approve refund yang sudah APPROVED/REJECTED | `400 Bad Request` |
| Multiple partial refund hingga nilai total habis | Diperbolehkan; status transaksi berubah `REFUNDED` saat total refund sama dengan amountDue |

---

### 3.15 Loyalty Program & Redemption Rule

#### 3.15.1 User Story

> Sebagai merchant, saya ingin mengatur cara pelanggan mendapatkan poin, kapan poin kadaluarsa, dan bagaimana poin dapat ditukar menjadi pembayaran, diskon, atau produk gratis, sehingga program loyalitas lebih fleksibel dan bisa disesuaikan dengan model bisnis saya.

#### 3.15.2 Model Data

**Tabel: `loyalty_program`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| name | String | Nama program |
| earnMode | Enum | `RATIO` \| `MULTIPLY` |
| pointsPerAmount | Decimal | Untuk mode `RATIO` |
| earnMultiplier | Decimal | Untuk mode `MULTIPLY` |
| expiryMode | Enum | `NONE` \| `ROLLING_DAYS` \| `FIXED_DATE` |
| expiryDays | Integer / null | Untuk `ROLLING_DAYS` |
| expiryDate | Timestamp / null | Untuk `FIXED_DATE` |
| isActive | Boolean | Status aktif |

**Tabel: `loyalty_redemption_rule`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| loyaltyProgramId | UUID | FK ke loyalty_program |
| merchantId | UUID | FK ke merchant |
| type | Enum | `PAYMENT` \| `DISCOUNT` \| `FREE_PRODUCT` |
| redeemRate | Decimal / null | Nilai uang per poin untuk type `PAYMENT` |
| minRedeemPoints | Decimal / null | Minimum point redeem |
| maxRedeemPoints | Decimal / null | Maksimum point redeem |
| requiredPoints | Decimal / null | Poin yang dibutuhkan untuk rule `DISCOUNT` / `FREE_PRODUCT` |
| discountType | Enum / null | `PERCENTAGE` \| `AMOUNT` |
| discountValue | Decimal / null | Nilai diskon |
| maxDiscountAmount | Decimal / null | Cap diskon |
| rewardProductId | UUID / null | Produk gratis |
| rewardQty | Integer / null | Qty hadiah |
| isActive | Boolean | Status aktif |

**Tabel: `product_loyalty_setting`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| productId | UUID | FK ke product |
| merchantId | UUID | FK ke merchant |
| isLoyaltyEnabled | Boolean | Produk ikut loyalty atau tidak |
| fixedPoints | Decimal / null | Override poin earn untuk produk tertentu |

#### 3.15.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/loyalty/program` | Ambil program loyalty aktif |
| POST | `/pos/loyalty/program/create` | Buat program loyalty |
| PUT | `/pos/loyalty/program/update` | Update program loyalty |
| GET | `/pos/loyalty/redemption-rule/list` | Daftar rule redeem |
| POST | `/pos/loyalty/redemption-rule/add` | Tambah rule redeem |
| PUT | `/pos/loyalty/redemption-rule/update` | Update rule redeem |
| PUT | `/pos/product/{productId}/loyalty-setting` | Set override loyalty per produk |
| POST | `/pos/loyalty/redeem/preview` | Preview nilai redeem sebelum transaksi final |

#### 3.15.4 Alur Fungsional

**Earn Points:**
```
Saat transaksi PAID:
    ambil loyalty_program aktif
    hitung earnPoints dari item yang loyalty-enabled
    jika product_loyalty_setting.fixedPoints ada:
        gunakan fixedPoints × qty
    jika tidak:
        gunakan earnMode global
    insert loyalty_transaction type = EARN
    update customer.loyaltyPoints
```

**Redeem Preview:**
```
Kasir -> POST /pos/loyalty/redeem/preview { customerId, ruleId, requestedPoints?, cart }
    ↓
Validasi customer aktif dan saldo cukup
    ↓
Validasi rule aktif dan syarat minimum terpenuhi
    ↓
Hitung reward:
    PAYMENT -> loyaltyRedeemAmount
    DISCOUNT -> discountAmount tambahan
    FREE_PRODUCT -> reward product
    ↓
Return preview tanpa mengurangi poin
```

#### 3.15.5 Business Rules

1. **Satu Program Aktif:** Dalam satu waktu hanya boleh ada satu `loyalty_program.isActive = true` per merchant.
2. **Product Override:** `product_loyalty_setting` hanya mempengaruhi earn points, bukan redeem rule.
3. **Expiry:** Jika `expiryMode = ROLLING_DAYS`, setiap transaksi poin menyimpan expiry sendiri. Jika `FIXED_DATE`, seluruh poin program mengikuti tanggal yang sama.
4. **Redeem Type PAYMENT:** Nilai redeem mengurangi `amountDue` pada layer loyalty seperti yang sudah dijelaskan di alur transaksi.
5. **Redeem Type DISCOUNT / FREE_PRODUCT:** Jika diaktifkan di POS, sistem harus mengubah kalkulasi cart sebelum pembayaran final dan tetap menyimpan audit trail di `loyalty_transaction`.
6. **Saldo Tidak Negatif:** Pengurangan poin hanya boleh terjadi setelah transaksi final berhasil atau setelah merchant melakukan konfirmasi redeem yang eksplisit.

#### 3.15.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Merchant belum punya program loyalty aktif | Earn/redeem tidak berjalan |
| requestedPoints < minimum rule | `400 Bad Request` |
| requiredPoints melebihi saldo customer | `400 Bad Request` |
| Produk `isLoyaltyEnabled = false` | Tidak menghasilkan poin |

---

### 3.16 MDR Setting

#### 3.16.1 User Story

> Sebagai merchant atau operator platform, saya ingin mengatur fee MDR per metode pembayaran dan menentukan siapa yang menanggung biaya tersebut agar settlement, report, dan disbursement dapat dihitung dengan benar.

#### 3.16.2 Model Data

**Tabel: `mdr_setting`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| paymentMethodCode | String | Kode metode pembayaran |
| percentage | Decimal | Fee persentase |
| flatFee | Decimal | Fee tetap |
| chargedTo | Enum | `MERCHANT` \| `CUSTOMER` |
| isActive | Boolean | Status aktif |

#### 3.16.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/mdr/list` | Daftar MDR setting |
| POST | `/pos/mdr/add` | Tambah MDR setting |
| PUT | `/pos/mdr/update` | Update MDR setting |
| DELETE | `/pos/mdr/delete/{id}` | Soft delete MDR setting |
| POST | `/pos/mdr/preview` | Preview fee berdasarkan metode pembayaran dan amount |

#### 3.16.4 Alur Fungsional

```
Saat payment line dipilih:
    cari mdr_setting aktif untuk paymentMethodCode
    jika tidak ada:
        fee = 0
    jika ada:
        fee = (paymentAmount × percentage / 100) + flatFee

    jika chargedTo = MERCHANT:
        amountPaidByCustomer tetap
        settlementNet = paymentAmount - fee

    jika chargedTo = CUSTOMER:
        surchargeToCustomer = fee
        amountPaidByCustomer = paymentAmount + fee
```

#### 3.16.5 Business Rules

1. **Satu Setting Aktif per Payment Method:** Dalam satu merchant, hanya boleh ada satu MDR aktif per `paymentMethodCode`.
2. **ChargedTo CUSTOMER:** Jika fee dibebankan ke customer, surcharge harus muncul eksplisit di preview dan receipt.
3. **ChargedTo MERCHANT:** Fee tidak menambah `amountDue`, tetapi mempengaruhi settlement/report downstream.
4. **Reportability:** Sistem harus bisa menghitung total fee MDR per periode dan per payment method.
5. **Integrasi Split Payment:** Jika satu transaksi memakai beberapa payment line, MDR dihitung per payment line, bukan di level transaksi total.

#### 3.16.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| percentage negatif | `400 Bad Request` |
| Duplikasi setting aktif untuk payment method yang sama | `409 Conflict` |
| paymentMethodCode tidak dikenal | `400 Bad Request` atau `fee = 0` sesuai policy integrasi |

---

### 3.17 Notification Setting

#### 3.17.1 User Story

> Sebagai merchant atau manajer, saya ingin menerima notifikasi atas kejadian penting seperti ringkasan harian, settlement, atau penutupan shift agar saya dapat memonitor operasional toko tanpa harus selalu membuka dashboard.

#### 3.17.2 Model Data

**Tabel: `notification_setting`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| merchantId | UUID | FK ke merchant |
| emailAddress | String / null | Tujuan notifikasi email |
| isEnabled | Boolean | Notifikasi aktif/nonaktif |
| notifyTypes | Array/String | Contoh: `DAILY_SUMMARY`, `SETTLEMENT`, `SHIFT_CLOSE`, `LOW_STOCK` |
| sendTime | String | Format `HH:mm` untuk notifikasi terjadwal |
| createdAt | Timestamp | Waktu pembuatan |
| updatedAt | Timestamp | Waktu update |

#### 3.17.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/notification-setting` | Ambil setting notifikasi merchant |
| POST | `/pos/notification-setting/create` | Buat setting |
| PUT | `/pos/notification-setting/update` | Update setting |
| POST | `/pos/notification-setting/test` | Kirim notifikasi uji |

#### 3.17.4 Alur Fungsional

```
Scheduler / event publisher berjalan
    ↓
Ambil notification_setting merchant yang isEnabled = true
    ↓
Evaluasi notifyTypes:
    DAILY_SUMMARY -> kirim ringkasan harian pada sendTime
    SETTLEMENT -> kirim saat settlement/disbursement tertentu terjadi
    SHIFT_CLOSE -> kirim saat shift ditutup
    LOW_STOCK -> kirim saat stok melewati threshold
```

#### 3.17.5 Business Rules

1. **Satu Setting per Merchant:** Merchant hanya memiliki satu konfigurasi notifikasi aktif.
2. **Test Notification:** Endpoint test tidak boleh mengirim data bisnis sensitif penuh; cukup payload simulasi.
3. **Graceful Degradation:** Jika kanal notifikasi gagal, event bisnis utama tetap dianggap sukses; kegagalan notifikasi dicatat untuk retry/logging.
4. **Notify Types Parsial:** Merchant dapat memilih sebagian tipe notifikasi tanpa harus mengaktifkan semuanya.

#### 3.17.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| emailAddress kosong saat kanal email diwajibkan | `400 Bad Request` |
| sendTime tidak valid | `400 Bad Request` |
| notifyTypes berisi nilai tak dikenal | `400 Bad Request` |

---

### 3.18 Product Variant & Modifier

#### 3.18.1 User Story

> Sebagai merchant, saya ingin mendefinisikan varian produk seperti ukuran atau warna, serta modifier opsional seperti topping atau add-on, agar kasir dapat menjual produk dengan konfigurasi yang tepat dan sistem menyimpan snapshot pilihan tersebut di transaksi.

#### 3.18.2 Model Data

**Tabel: `product_variant_group`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| productId | UUID | FK ke product |
| merchantId | UUID | FK ke merchant |
| name | String | Nama grup varian, contoh `Ukuran` |
| isRequired | Boolean | Apakah wajib pilih satu varian |
| displayOrder | Integer | Urutan tampil |
| isActive | Boolean | Status aktif |

**Tabel: `product_variant`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| variantGroupId | UUID | FK ke product_variant_group |
| productId | UUID | FK ke product |
| merchantId | UUID | FK ke merchant |
| name | String | Nama varian, contoh `Large` |
| additionalPrice | Decimal | Harga tambahan |
| sku | String / null | SKU opsional |
| displayOrder | Integer | Urutan tampil |
| isActive | Boolean | Status aktif |

**Tabel: `product_modifier_group`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| productId | UUID | FK ke product |
| merchantId | UUID | FK ke merchant |
| name | String | Nama grup modifier |
| minSelect | Integer | Minimum pilihan |
| maxSelect | Integer | Maksimum pilihan |
| isRequired | Boolean | Apakah wajib pilih |
| displayOrder | Integer | Urutan tampil |
| isActive | Boolean | Status aktif |

**Tabel: `product_modifier`**

| Kolom | Tipe | Keterangan |
|---|---|---|
| id | UUID | Primary key |
| modifierGroupId | UUID | FK ke product_modifier_group |
| productId | UUID | FK ke product |
| merchantId | UUID | FK ke merchant |
| name | String | Nama modifier |
| additionalPrice | Decimal | Harga tambahan |
| displayOrder | Integer | Urutan tampil |
| isActive | Boolean | Status aktif |

**Snapshot di Transaksi**

| Entitas | Kolom | Keterangan |
|---|---|---|
| `transaction_items` | `variantId` | Varian yang dipilih |
| `transaction_items` | `originalPrice` | Harga produk sebelum override price book |
| `transaction_modifier` | `modifierName` | Snapshot nama modifier |
| `transaction_modifier` | `additionalPrice` | Snapshot harga modifier |
| `transaction_modifier` | `qty` | Qty modifier di item tersebut |

#### 3.18.3 Endpoints

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/product/{productId}/variant-group/list` | List grup varian produk |
| POST | `/pos/product/{productId}/variant-group/add` | Tambah grup varian |
| PUT | `/pos/product/variant-group/update` | Update grup varian |
| DELETE | `/pos/product/variant-group/delete/{id}` | Soft delete grup varian |
| POST | `/pos/product/{productId}/variant/add` | Tambah variant |
| PUT | `/pos/product/variant/update` | Update variant |
| DELETE | `/pos/product/variant/delete/{id}` | Soft delete variant |
| GET | `/pos/product/{productId}/modifier-group/list` | List grup modifier produk |
| POST | `/pos/product/{productId}/modifier-group/add` | Tambah grup modifier |
| PUT | `/pos/product/modifier-group/update` | Update grup modifier |
| DELETE | `/pos/product/modifier-group/delete/{id}` | Soft delete grup modifier |
| POST | `/pos/product/{productId}/modifier/add` | Tambah modifier |
| PUT | `/pos/product/modifier/update` | Update modifier |
| DELETE | `/pos/product/modifier/delete/{id}` | Soft delete modifier |

#### 3.18.4 Alur Fungsional

**Kalkulasi Harga Item dengan Variant dan Modifier:**
```
Saat item dimasukkan ke cart:
    ambil harga dasar product
    jika variant dipilih:
        validasi variant milik product yang sama dan aktif
        basePrice = product.price + variant.additionalPrice
    jika modifier dipilih:
        validasi semua modifier milik product yang sama dan aktif
        modifierTotal = SUM(modifier.additionalPrice × qtyModifier)
    itemBaseUnitPrice = basePrice + modifierTotal
    simpan originalPrice = itemBaseUnitPrice
    lalu teruskan ke evaluasi Price Book
```

**Snapshot ke Transaksi:**
```
Saat transaksi disimpan:
    transaction_items.variantId = selectedVariantId
    transaction_items.originalPrice = itemBaseUnitPrice
    untuk setiap modifier:
        INSERT transaction_modifier(transactionItemId, modifierId, modifierName, additionalPrice, qty)
```

#### 3.18.5 Business Rules

1. **Varian Bersifat Single Select per Group:** Satu item hanya boleh memilih maksimal satu variant dari satu `product_variant_group`.
2. **Modifier Bersifat Multi Select:** Satu item dapat memilih beberapa modifier selama masih dalam batas `minSelect` dan `maxSelect` group.
3. **Urutan Kalkulasi:** Harga product + variant + modifier dihitung lebih dulu sebagai `originalPrice`, baru setelah itu Price Book dapat mengubah unit price efektif.
4. **Scope Price Book:** Secara default Price Book berlaku pada item setelah harga variant/modifier dibentuk. Jika product owner ingin perilaku lain, harus dinyatakan eksplisit di requirement.
5. **Promo dan Discount:** Evaluasi promo/discount memakai nilai item final setelah varian/modifier dan price book diterapkan.
6. **Snapshot Immutable:** Nama dan harga tambahan modifier yang sudah masuk ke `transaction_modifier` tidak berubah walaupun master modifier diubah atau dinonaktifkan.
7. **Receipt dan Kitchen/Order Print:** Nama variant dan modifier harus ditampilkan sebagai detail item agar operasional outlet tidak kehilangan konteks pesanan.

#### 3.18.6 Kondisi Error & Edge Case

| Skenario | Respons Sistem |
|---|---|
| Variant tidak milik product yang dipilih | `400 Bad Request` |
| Modifier tidak milik product yang dipilih | `400 Bad Request` |
| Group required tapi tidak ada pilihan | `400 Bad Request` |
| Jumlah modifier melebihi `maxSelect` | `400 Bad Request` |
| Variant/modifier nonaktif dipakai di transaksi baru | `400 Bad Request` |

---

## 4. Alur Transaksi End-to-End

### 4.1 Gambaran Umum

Setiap transaksi POS melewati 5 layer kalkulasi secara berurutan. Urutan ini tidak dapat diubah dan merupakan fondasi dari semua fitur pricing yang dijelaskan di Phase 2.

### 4.2 5-Layer Kalkulasi Harga

```
INPUT: Cart berisi item-item dengan harga standar
        ↓
╔══════════════════════════════════════════════════════════╗
║  LAYER 1: PRICE BOOK                                     ║
║  Terapkan override harga dari Price Book aktif           ║
║  → PRODUCT / CATEGORY / ORDER_TYPE / WHOLESALE           ║
║  Output: unitPrice per item (mungkin berbeda dari standar)║
╚══════════════════════════════════════════════════════════╝
        ↓
grossSubTotal = SUM(unitPrice × qty) untuk semua item
        ↓
╔══════════════════════════════════════════════════════════╗
║  LAYER 2: PROMOSI OTOMATIS                               ║
║  Evaluasi semua promotion aktif berdasarkan kondisi cart ║
║  → DISCOUNT_BY_ORDER / BUY_X_GET_Y / DISCOUNT_BY_ITEM   ║
║  Output: promoAmount (total diskon dari promosi)         ║
╚══════════════════════════════════════════════════════════╝
        ↓
╔══════════════════════════════════════════════════════════╗
║  LAYER 3: DISKON KODE                                    ║
║  Jika kode diskon dimasukkan, hitung discountAmount      ║
║  berdasarkan scope (ALL/PRODUCT/CATEGORY)                ║
║  Output: discountAmount                                  ║
╚══════════════════════════════════════════════════════════╝
        ↓
netSubTotal = grossSubTotal - promoAmount - discountAmount
        ↓
╔══════════════════════════════════════════════════════════╗
║  KALKULASI SC & PAJAK                                    ║
║  (urutan SC vs Pajak bergantung serviceChargeSource)     ║
║                                                          ║
║  BEFORE_TAX:                                             ║
║    SC = netSubTotal × scPercentage%                      ║
║    Tax = (netSubTotal + SC) × taxPercentage%             ║
║                                                          ║
║  AFTER_TAX:                                              ║
║    Tax = netSubTotal × taxPercentage%                    ║
║    SC = (netSubTotal + Tax) × scPercentage%              ║
║                                                          ║
║  DPP:                                                    ║
║    DPP = netSubTotal / (1 + taxPercentage%)              ║
║    SC = DPP × scPercentage%                              ║
║    Tax = netSubTotal × taxPercentage%                    ║
║                                                          ║
║  AFTER_DISCOUNT:                                         ║
║    SC = (grossSubTotal - discountAmount) × scPercentage% ║
║    Tax = netSubTotal × taxPercentage%                    ║
╚══════════════════════════════════════════════════════════╝
        ↓
subTotalWithSCAndTax = netSubTotal + SC + Tax
        ↓
rounding = pembulatan ke ratusan/ribuan terdekat (jika aktif)
        ↓
totalAmount = subTotalWithSCAndTax + rounding
        ↓
╔══════════════════════════════════════════════════════════╗
║  LAYER 4: VOUCHER                                        ║
║  Kurangi totalAmount dengan nilai voucher                ║
║  voucherAmount = MIN(voucher.sellingPrice, totalAmount)  ║
╚══════════════════════════════════════════════════════════╝
        ↓
╔══════════════════════════════════════════════════════════╗
║  LAYER 5: LOYALTY REDEEM                                 ║
║  Kurangi dengan nilai loyalty poin yang diredeem         ║
║  loyaltyRedeemAmount = redeemPoints × loyaltyRedeemRate  ║
╚══════════════════════════════════════════════════════════╝
        ↓
amountDue = totalAmount - voucherAmount - loyaltyRedeemAmount
        ↓
OUTPUT: amountDue (jumlah yang harus dibayar pelanggan)
```

### 4.3 Alur Transaksi Lengkap (Narrative)

**Fase 1: Persiapan Cart**

Kasir membuka sesi, memilih produk, dan menambahkannya ke cart. Untuk setiap item, sistem langsung mengevaluasi Price Book yang aktif untuk outlet tersebut — mengganti harga standar dengan harga dari Price Book jika ada yang berlaku (PRODUCT, CATEGORY, ORDER_TYPE, atau WHOLESALE). `grossSubTotal` dihitung dari jumlah seluruh item dengan harga yang sudah di-price-book.

**Fase 2: Evaluasi Promosi**

Setelah cart terbentuk, sistem secara otomatis mengevaluasi seluruh promosi aktif merchant yang berlaku untuk tanggal dan hari saat ini. Promosi diurutkan berdasarkan `priority` (ascending). Promosi yang kondisinya terpenuhi diterapkan. Jika ada promosi dengan `canCombine = false` yang terpenuhi, promosi tersebut diterapkan dan evaluasi berhenti. Total `promoAmount` dijumlahkan dari semua reward promosi yang berhasil diterapkan.

**Fase 3: Input Kode Diskon (Opsional)**

Jika pelanggan memiliki kode diskon, kasir memasukkannya. Sistem melakukan validasi lengkap: keberadaan kode, channel, outlet, tanggal berlaku, kuota, minimum pembelian. Jika valid, `discountAmount` dihitung berdasarkan scope (ALL/PRODUCT/CATEGORY).

**Fase 4: Kalkulasi SC dan Pajak**

Dengan `netSubTotal = grossSubTotal - promoAmount - discountAmount`, sistem menghitung service charge dan pajak. Urutan dan basis kalkulasi bergantung pada `serviceChargeSource` yang dikonfigurasi di Payment Setting outlet (dengan fallback ke default merchant).

**Fase 5: Voucher (Opsional)**

Jika pelanggan memiliki voucher, kasir memindai atau memasukkan kode. Setelah validasi, `voucherAmount` (senilai `sellingPrice` voucher group) dikurangkan dari `totalAmount`.

**Fase 6: Loyalty Redeem (Opsional)**

Jika pelanggan ingin meredeem loyalty point, kasir memasukkan jumlah poin. Sistem mengecek kecukupan saldo. `loyaltyRedeemAmount` dihitung dari `redeemPoints × loyaltyRedeemRate` dan dikurangkan dari total.

**Fase 7: Pembayaran**

Kasir memilih metode pembayaran dan memasukkan jumlah yang dibayarkan. `amountDue` adalah jumlah final yang harus dibayar. Sistem menghitung kembalian jika pembayaran melebihi `amountDue`.

**Fase 8: Finalisasi**

Setelah pembayaran dikonfirmasi, transaksi berubah status menjadi `PAID`. Sistem secara otomatis:
1. Mengincrement `usageCount` kode diskon (jika ada)
2. Mengubah status `voucher_code` menjadi `USED` (jika ada)
3. Mengupdate `customer.loyaltyPoints`, `totalTransaction`, `totalSpend` (jika ada customer)
4. Membuat `loyalty_history` EARN (jika ada customer)
5. Membuat `disbursement_log` untuk setiap rule aktif
6. Mencetak struk ke printer default outlet

### 4.3.1 Klarifikasi Alur untuk Modul Tambahan

Klarifikasi berikut melengkapi narasi pada Bagian 4.3 untuk modul yang ditambahkan setelah sinkronisasi dengan schema migration:

1. **Sebelum Persiapan Cart:** Untuk outlet yang mewajibkan shift, kasir harus memiliki `cashier_shift` berstatus `OPEN` sebelum membuat transaksi baru.
2. **Saat Transaksi Dibuat:** Sistem harus menyelesaikan `orderTypeId` dari request atau default outlet sebelum evaluasi Price Book tipe `ORDER_TYPE`.
3. **Saat Pembayaran:** Jika metode pembayaran memiliki konfigurasi MDR aktif, fee dihitung per payment method dan ditampilkan/diterapkan sesuai `chargedTo`.
4. **Setelah Finalisasi:** Selain post-transaction action yang sudah disebutkan, sistem dapat mem-publish event notifikasi operasional bila merchant mengaktifkannya.
5. **Pasca-Transaksi:** Refund penuh atau sebagian adalah alur terpisah setelah transaksi `PAID`, tidak menjadi bagian dari checkout normal.

### 4.4 Diagram Alur (Teks)

```
[Mulai Transaksi]
        |
        v
[Pilih Produk & Qty]
        |
        v
[Evaluasi Price Book] --> [grossSubTotal]
        |
        v
[Evaluasi Promosi Otomatis] --> [promoAmount]
        |
        v
[Input Kode Diskon?] --No--> [discountAmount = 0]
        |                           |
       Yes                          |
        |                           |
[Validasi Kode Diskon]              |
[Hitung discountAmount]             |
        |___________________________v
        |
        v
[netSubTotal = gross - promo - discount]
        |
        v
[Hitung SC & Tax sesuai serviceChargeSource]
        |
        v
[totalAmount = netSubTotal + SC + Tax + Rounding]
        |
        v
[Input Voucher?] --No--> [voucherAmount = 0]
        |                       |
       Yes                      |
        |                       |
[Validasi Voucher]              |
[voucherAmount = sellingPrice]  |
        |_______________________v
        |
        v
[Redeem Loyalty?] --No--> [loyaltyRedeemAmount = 0]
        |                           |
       Yes                          |
        |                           |
[Cek Saldo Poin]                    |
[Hitung loyaltyRedeemAmount]        |
        |___________________________v
        |
        v
[amountDue = totalAmount - voucher - loyalty]
        |
        v
[Pilih Metode Pembayaran]
        |
        v
[Konfirmasi Pembayaran]
        |
        v
[Status = PAID]
        |
        v
[Post-Transaction Actions]
  - Increment discount usageCount
  - Update voucher_code → USED
  - Earn loyalty points
  - Create disbursement_log
  - Print receipt
        |
        v
[Selesai]
```

### 4.4.1 Tambahan Node Diagram

Jika diagram di atas diimplementasikan ke state machine atau workflow engine, node berikut perlu dianggap bagian dari flow lengkap walaupun belum divisualkan penuh pada diagram utama:

- `Shift Aktif?` sebelum transaksi dibuat, untuk outlet yang mewajibkan shift terbuka.
- `Resolusi Order Type` sebelum evaluasi Price Book.
- `Hitung MDR per Payment Method` pada tahap pembayaran.
- `Send Notification Event` sebagai side effect non-blocking setelah finalisasi.
- `Refund Flow` sebagai workflow terpisah setelah transaksi `PAID`.

---

## 5. Business Rules Lintas Fitur

### 5.1 Kalkulasi Pajak

Pajak selalu dihitung berdasarkan `netSubTotal` (setelah price book, promo, dan diskon kode), **kecuali** dalam skenario berikut:

- **serviceChargeSource = BEFORE_TAX:** SC ditambahkan ke `netSubTotal` sebelum pajak dihitung. Formula: `Tax = (netSubTotal + SC) × taxPercentage%`
- **serviceChargeSource = DPP:** Pajak dihitung dari `netSubTotal` langsung, SC dihitung dari DPP (Dasar Pengenaan Pajak = `netSubTotal / (1 + taxPercentage%)`).

Nilai `taxPercentage` yang digunakan adalah snapshot dari tarif pajak default merchant saat transaksi dibuat.

### 5.2 Kalkulasi Service Charge

SC dihitung berdasarkan `serviceChargeSource`:

| Source | Basis SC |
|---|---|
| `BEFORE_TAX` | `netSubTotal` |
| `AFTER_TAX` | `netSubTotal + totalTax` |
| `DPP` | `netSubTotal / (1 + taxPercentage%)` |
| `AFTER_DISCOUNT` | `grossSubTotal - discountAmount` (bukan promoAmount) |

Jika `isServiceCharge = false` di Payment Setting yang berlaku, SC = 0 dan tidak dihitung sama sekali.

### 5.3 Prioritas Diskon

Urutan layer pricing yang tidak dapat diubah:

1. **Price Book** (mengubah unitPrice) — dieksekusi pertama, menjadi basis kalkulasi
2. **Promosi Otomatis** (menghasilkan promoAmount) — auto-apply, dievaluasi per priority
3. **Kode Diskon** (menghasilkan discountAmount) — manual input oleh kasir/pelanggan
4. **Service Charge & Pajak** — dihitung dari netSubTotal
5. **Voucher** (mengurangi totalAmount) — setelah SC dan pajak
6. **Loyalty Redeem** (mengurangi totalAmount) — layer terakhir sebelum pembayaran

Tidak ada konfigurasi untuk mengubah urutan ini. Merchant tidak dapat memilih untuk menerapkan diskon kode sebelum promosi, misalnya.

### 5.4 Interaksi Kode Diskon dan Promosi

- Kode diskon dan promosi otomatis dapat diterapkan bersamaan pada transaksi yang sama.
- `promoAmount` dan `discountAmount` adalah field terpisah di transaksi.
- Keduanya sama-sama mengurangi `netSubTotal` dari `grossSubTotal`.
- Jika ada promosi dengan `canCombine = false` yang menghasilkan diskon signifikan, kasir/pelanggan tetap bisa menggunakan kode diskon di atas promosi tersebut.

### 5.5 Interaksi Voucher dan Loyalty

- Voucher dan Loyalty Redeem dapat digunakan bersamaan dalam satu transaksi.
- Urutan: Voucher dikurangkan terlebih dahulu, baru Loyalty Redeem.
- `amountDue` tidak boleh negatif. Jika `voucherAmount + loyaltyRedeemAmount > totalAmount`, sistem mengap salah satu di jumlah yang tersisa.
- Skenario: `totalAmount = 100.000`, voucher = 80.000, loyalty = 30.000 → `voucherAmount = 80.000`, `loyaltyRedeemAmount = MIN(30.000, 20.000) = 20.000`, `amountDue = 0`.

### 5.6 Disbursement dan Kalkulasi

Disbursement dihitung setelah seluruh kalkulasi transaksi selesai (status = PAID). Basis perhitungan (`source` pada disbursement rule) mengacu pada field yang sudah tersimpan di transaksi:

| Disbursement Source | Field Transaksi |
|---|---|
| `GROSS` | `grossSubTotal` |
| `NET` | `netSubTotal` |
| `NET_AFTER_TAX` | `netSubTotal + totalTax` |
| `NET_AFTER_TAX_SC` | `netSubTotal + totalTax + serviceCharge` |

### 5.7 Order Type dan Resolusi Harga

- `orderTypeId` di transaksi harus diputuskan sebelum evaluasi Price Book tipe `ORDER_TYPE`.
- Jika transaksi tidak membawa `orderTypeId`, sistem memakai `outlet.defaultOrderTypeId`, lalu fallback ke `order_type.isDefault = true`.
- Perubahan default order type tidak mengubah transaksi historis.

### 5.8 Shift dan Transaksi

- Untuk outlet yang mewajibkan shift, transaksi baru hanya dapat dibuat jika kasir memiliki shift `OPEN`.
- Setiap transaksi harus menyimpan `shiftId` dari shift aktif agar rekap kas dan audit trail akurat.
- Refund tunai yang disetujui pada shift yang sama atau shift berikutnya harus tetap masuk ke rekap shift saat refund diproses, bukan saat transaksi awal dibuat.

### 5.9 Refund terhadap Laporan, Loyalty, dan Disbursement

- Refund yang `APPROVED` tidak mengubah gross historis transaksi, tetapi menambah nilai `totalRefund` di report.
- Jika transaksi sebelumnya menghasilkan loyalty point, sistem harus melakukan reversal poin berdasarkan kebijakan merchant: penuh untuk full refund, proporsional untuk partial refund.
- Disbursement yang sudah dibuat dari transaksi yang kemudian direfund tidak otomatis dihapus; penyesuaian dilakukan melalui proses rekonsiliasi atau settlement adjustment.

### 5.10 Loyalty Program terhadap Kalkulasi Transaksi

- Rule loyalty type `PAYMENT` mengurangi `amountDue` pada layer terakhir, setelah voucher.
- Rule loyalty type `DISCOUNT` mengurangi nilai transaksi dan harus diperlakukan sebagai komponen diskon terpisah agar audit trail jelas.
- Rule loyalty type `FREE_PRODUCT` menambahkan reward ke cart atau menurunkan nilai item terkait, tergantung desain checkout client.
- Preview redeem tidak boleh mengurangi saldo poin; pengurangan final hanya boleh dilakukan setelah transaksi selesai atau redeem dikonfirmasi secara eksplisit.

### 5.11 MDR dan Settlement

- MDR dihitung per payment method yang digunakan, bukan hanya di level transaksi total.
- Jika `chargedTo = CUSTOMER`, surcharge MDR harus terlihat di preview checkout, receipt, dan detail transaksi.
- Jika `chargedTo = MERCHANT`, fee tidak menambah `amountDue`, tetapi harus muncul di report settlement atau rekonsiliasi pembayaran.

### 5.12 Notification sebagai Side Effect

- Pengiriman notifikasi tidak boleh menjadi bagian dari transaksi database utama yang membuat operasi bisnis gagal.
- Jika notifikasi gagal terkirim, status transaksi/refund/shift tetap dianggap sukses selama operasi bisnis utama berhasil.

### 5.13 Variant, Modifier, dan Pricing Layer

- Variant dan modifier membentuk `originalPrice` item sebelum Price Book diterapkan.
- `transaction_items.originalPrice` menyimpan harga hasil product + variant + modifier sebelum override dari Price Book.
- Jika Price Book mengubah harga item, nilai override tersebut menjadi basis untuk promo, diskon, pajak, dan service charge.
- Report produk minimum harus tetap dapat mengatribusikan penjualan ke `productId`; report lanjutan per variant dapat ditambahkan kemudian jika dibutuhkan.

---

## 6. Manajemen State & Status

### 6.1 Status Transaksi

```
[PENDING] ──────────────────────────────── [VOID]
    |
    |──── Pembayaran dikonfirmasi ────────► [PAID]
                                               |
                                               |──── Refund sebagian/penuh ──► [REFUNDED]
```

| Status | Deskripsi |
|---|---|
| `PENDING` | Transaksi dibuat, belum ada pembayaran |
| `PAID` | Pembayaran diterima, transaksi selesai |
| `VOID` | Transaksi dibatalkan sebelum pembayaran |
| `REFUNDED` | Pembayaran dikembalikan (sebagian atau penuh) |

**Transisi yang Valid:**
- `PENDING` → `PAID` (pembayaran dikonfirmasi)
- `PENDING` → `VOID` (dibatalkan sebelum bayar)
- `PAID` → `REFUNDED` (proses refund)

**Transisi yang Tidak Valid:**
- `VOID` → apapun (tidak bisa direaktivasi)
- `REFUNDED` → apapun (status final)
- `PAID` → `PENDING` (tidak bisa dibatalkan setelah bayar)

### 6.2 Status Voucher Code

```
[AVAILABLE] ──── Digunakan dalam transaksi PAID ──► [USED]
    |
    |──── Kadaluarsa (expiredDate terlewati) ──────► [EXPIRED]
    |
    |──── Dibatalkan secara manual ─────────────────► [CANCELLED]
```

| Status | Deskripsi |
|---|---|
| `AVAILABLE` | Voucher siap digunakan |
| `USED` | Voucher sudah digunakan dalam transaksi |
| `EXPIRED` | Voucher sudah melewati tanggal kadaluarsa |
| `CANCELLED` | Voucher dibatalkan secara manual |

**Semua status kecuali `AVAILABLE` adalah final** — tidak dapat dikembalikan ke status sebelumnya.

### 6.3 Status Disbursement Log

```
[PENDING] ──── Settlement dikonfirmasi ──► [SETTLED]
    |
    └──── Terjadi kegagalan ─────────────► [FAILED]
```

| Status | Deskripsi |
|---|---|
| `PENDING` | Log dibuat, belum di-settle |
| `SETTLED` | Dana sudah ditransfer/dikonfirmasi |
| `FAILED` | Proses disbursement gagal |

### 6.4 Status Refund

```
[PENDING] ──── Disetujui ───► [APPROVED]
    │
    └──── Ditolak ─────────► [REJECTED]
```

| Status | Deskripsi |
|---|---|
| `PENDING` | Refund sudah diajukan, menunggu approval |
| `APPROVED` | Refund disetujui dan nilai refund masuk ke perhitungan transaksi/report |
| `REJECTED` | Refund ditolak dan tidak mempengaruhi transaksi |

### 6.5 Status Cashier Shift

```
[OPEN] ──── Shift ditutup ───► [CLOSED]
```

| Status | Deskripsi |
|---|---|
| `OPEN` | Shift sedang aktif dan dapat menerima transaksi |
| `CLOSED` | Shift selesai; tidak menerima transaksi baru |

### 6.6 Status Entitas dengan Soft Delete

Entitas berikut menggunakan `isActive` untuk soft delete:

| Entitas | Field | Perilaku |
|---|---|---|
| Tax | `isActive` | Tarif tidak aktif tidak muncul di list, tidak diterapkan di transaksi baru |
| Discount | `isActive` | Kode tidak aktif tidak bisa divalidasi |
| Promotion | `isActive` | Promosi tidak aktif tidak dievaluasi |
| Price Book | `isActive` | Price book tidak aktif tidak diterapkan |
| Voucher Brand/Group | `isActive` | Group tidak aktif: kodenya tidak bisa digunakan |
| Customer | `isActive` | Customer tidak aktif tidak bisa earn/redeem loyalty |
| Cashier (User) | `isActive` | Kasir tidak aktif tidak bisa login |
| Disbursement Rule | `isActive` | Rule tidak aktif tidak memproses transaksi baru |

### 6.7 Status Loyalty History Type

| Type | Deskripsi | Nilai Points |
|---|---|---|
| `EARN` | Poin diperoleh dari transaksi | Positif |
| `REDEEM` | Poin digunakan dalam transaksi | Negatif |
| `ADJUST` | Penyesuaian manual oleh admin | Positif atau Negatif |
| `EXPIRE` | Poin kadaluarsa (jika ada policy expiry) | Negatif |

---

## 7. Aturan Keamanan & Otorisasi

### 7.1 Autentikasi

Semua endpoint `/pos/**` memerlukan JWT Bearer Token yang valid. Token dikirimkan via header `Authorization: Bearer <token>`. JWT mengandung:

- `merchantId` — ID merchant pemilik data
- `userId` — ID user yang melakukan request
- `role` — Role user (`ADMIN`, `CASHIER`, `MANAGER`)
- `outletId` — Outlet yang terikat (jika kasir)

### 7.2 Isolasi Data Multi-Tenant

**Aturan paling kritis:** Setiap query ke database WAJIB menyertakan `merchantId = JWT.merchantId` sebagai filter. Tidak ada satu pun endpoint yang boleh mengembalikan data milik merchant lain.

```kotlin
// Contoh pattern yang benar:
fun getTaxList(merchantId: UUID) = taxRepository.findByMerchantIdAndIsActiveTrue(merchantId)

// Pattern yang SALAH (tidak ada merchantId filter):
fun getTaxList() = taxRepository.findAll()
```

### 7.3 Aturan Akses per Role

| Operasi | ADMIN | MANAGER | CASHIER |
|---|---|---|---|
| CRUD Master Data (tax, SC, discount, promo, price book) | ✅ | ✅ | ❌ |
| CRUD Kasir | ✅ | ❌ | ❌ |
| Reset Password Kasir | ✅ | ❌ | ❌ |
| Set/Reset PIN Kasir | ✅ | ✅ (diri sendiri) | ✅ (diri sendiri) |
| Buat Transaksi | ✅ | ✅ | ✅ |
| Void Transaksi | ✅ | ✅ | ❌ (perlu PIN override) |
| Refund Transaksi | ✅ | ✅ | ❌ (perlu PIN override) |
| Lihat Laporan | ✅ | ✅ | ❌ |
| Kelola Disbursement Rule | ✅ | ❌ | ❌ |
| Settle Disbursement | ✅ | ❌ | ❌ |
| CRUD Voucher | ✅ | ✅ | ❌ |
| Bulk Import Voucher | ✅ | ✅ | ❌ |

### 7.4 PIN Otorisasi untuk Operasi Sensitif

**Tambahan akses untuk modul baru Phase 2:**

| Operasi Tambahan | ADMIN | MANAGER | CASHIER |
|---|---|---|---|
| CRUD Order Type | ✅ | ✅ | ❌ |
| Set Language Merchant/Outlet | ✅ | ✅ | ❌ |
| Open / Close Shift | ✅ | ✅ | ✅ |
| Cash Adjustment Shift | ✅ | ✅ | ✅ |
| Approve Refund | ✅ | ✅ | ❌ |
| Kelola Loyalty Program | ✅ | ✅ | ❌ |
| Kelola MDR Setting | ✅ | ❌ | ❌ |
| Kelola Notification Setting | ✅ | ✅ | ❌ |

Operasi berikut memerlukan verifikasi PIN kasir sebelum dieksekusi:

1. **Refund transaksi** — kasir yang melakukan refund harus verifikasi PIN-nya
2. **Void transaksi** — setelah kasir mencapai batas waktu sesi atau di luar jam kerja
3. **Override diskon** — memberikan diskon manual di luar yang terkonfigurasi di sistem
4. **Edit transaksi yang sudah PAID**

Mekanisme:
1. Kasir memanggil `POST /pos/cashier/verify-pin { cashierId, pin }`
2. Sistem memverifikasi dan mengembalikan short-lived `operationToken` (misal berlaku 5 menit)
3. Operasi sensitif dieksekusi dengan menyertakan `operationToken` tersebut

### 7.5 Keamanan Data Sensitif

- **PIN:** Disimpan sebagai BCrypt hash. Tidak pernah dikembalikan di response API manapun.
- **Password:** Disimpan sebagai BCrypt hash. Tidak pernah dikembalikan di response API.
- **`hasPin`:** Hanya mengembalikan boolean, tidak pernah hash.
- **Kode Voucher:** Kode voucher yang belum digunakan harus dibatasi aksesnya — hanya admin yang bisa melihat daftar semua kode.
- **Data Customer:** Phone dan email adalah data PII (Personally Identifiable Information). Akses ke list customer dibatasi per role.

### 7.6 Rate Limiting (Rekomendasi)

| Endpoint | Batas |
|---|---|
| `POST /pos/discount/validate` | 30 req/menit per user |
| `POST /pos/voucher/validate` | 30 req/menit per user |
| `POST /pos/cashier/verify-pin` | 5 req/menit per user (anti brute-force) |
| `GET /pos/report/**` | 10 req/menit per user |

---

## 8. Error Handling Standard

### 8.1 Format Response Error

Semua error menggunakan format response yang konsisten:

```json
{
  "success": false,
  "message": "Deskripsi error yang jelas dalam Bahasa Indonesia",
  "data": null,
  "errorCode": "ERROR_CODE_CONSTANT",
  "timestamp": "2026-03-31T10:00:00Z"
}
```

### 8.2 HTTP Status Code

| Status Code | Penggunaan |
|---|---|
| `200 OK` | Request berhasil (GET, PUT yang berhasil) |
| `201 Created` | Resource baru berhasil dibuat (POST add/create) |
| `400 Bad Request` | Input tidak valid, validasi gagal, kondisi bisnis tidak terpenuhi |
| `401 Unauthorized` | Token JWT tidak ada atau tidak valid |
| `403 Forbidden` | Token valid tapi tidak punya izin untuk resource ini |
| `404 Not Found` | Resource tidak ditemukan (atau milik merchant lain) |
| `409 Conflict` | Duplikat data (kode unik sudah ada) |
| `422 Unprocessable Entity` | Data valid secara format tapi tidak bisa diproses secara bisnis |
| `500 Internal Server Error` | Error tidak terduga di server |

### 8.3 Error Codes per Modul

**Tax:**

| Error Code | Deskripsi |
|---|---|
| `TAX_NOT_FOUND` | Tarif pajak tidak ditemukan |
| `TAX_PERCENTAGE_INVALID` | Persentase pajak di luar rentang 0-100 |
| `TAX_NAME_REQUIRED` | Nama tarif pajak wajib diisi |

**Discount:**

| Error Code | Deskripsi |
|---|---|
| `DISCOUNT_NOT_FOUND` | Kode diskon tidak ditemukan |
| `DISCOUNT_EXPIRED` | Kode diskon sudah kadaluarsa |
| `DISCOUNT_QUOTA_EXCEEDED` | Kuota kode diskon sudah habis |
| `DISCOUNT_CHANNEL_MISMATCH` | Kode tidak berlaku untuk channel ini |
| `DISCOUNT_OUTLET_MISMATCH` | Kode tidak berlaku untuk outlet ini |
| `DISCOUNT_MIN_PURCHASE_NOT_MET` | Minimum pembelian belum terpenuhi |
| `DISCOUNT_CUSTOMER_QUOTA_EXCEEDED` | Batas pemakaian per customer terlampaui |
| `DISCOUNT_CODE_DUPLICATE` | Kode diskon sudah digunakan |

**Voucher:**

| Error Code | Deskripsi |
|---|---|
| `VOUCHER_NOT_FOUND` | Kode voucher tidak ditemukan |
| `VOUCHER_ALREADY_USED` | Voucher sudah digunakan |
| `VOUCHER_EXPIRED` | Voucher sudah kadaluarsa |
| `VOUCHER_CANCELLED` | Voucher telah dibatalkan |
| `VOUCHER_CUSTOMER_REQUIRED` | Voucher memerlukan data customer |
| `VOUCHER_CUSTOMER_MISMATCH` | Voucher bukan milik customer ini |

**Customer / Loyalty:**

| Error Code | Deskripsi |
|---|---|
| `CUSTOMER_NOT_FOUND` | Customer tidak ditemukan |
| `CUSTOMER_PHONE_DUPLICATE` | Nomor telepon sudah terdaftar |
| `CUSTOMER_EMAIL_DUPLICATE` | Email sudah terdaftar |
| `LOYALTY_INSUFFICIENT_POINTS` | Poin loyalty tidak mencukupi |
| `LOYALTY_POINTS_NEGATIVE` | Penyesuaian menghasilkan saldo negatif |

**Cashier:**

| Error Code | Deskripsi |
|---|---|
| `CASHIER_NOT_FOUND` | Kasir tidak ditemukan |
| `CASHIER_USERNAME_DUPLICATE` | Username sudah digunakan |
| `CASHIER_PIN_NOT_SET` | Kasir belum memiliki PIN |
| `CASHIER_PIN_INVALID` | PIN tidak cocok |
| `CASHIER_PIN_FORMAT_INVALID` | Format PIN tidak valid (harus 4-6 digit angka) |

**Disbursement:**

| Error Code | Deskripsi |
|---|---|
| `DISBURSEMENT_RULE_NOT_FOUND` | Aturan disbursement tidak ditemukan |
| `DISBURSEMENT_LOG_NOT_FOUND` | Log disbursement tidak ditemukan |
| `DISBURSEMENT_ALREADY_SETTLED` | Disbursement sudah di-settle |

**Order Type / Language:**

| Error Code | Deskripsi |
|---|---|
| `ORDER_TYPE_NOT_FOUND` | Order type tidak ditemukan |
| `ORDER_TYPE_DUPLICATE_CODE` | Kode order type sudah digunakan |
| `ORDER_TYPE_INACTIVE` | Order type tidak aktif |
| `LANGUAGE_CODE_INVALID` | Kode bahasa tidak didukung |

**Shift:**

| Error Code | Deskripsi |
|---|---|
| `SHIFT_ALREADY_OPEN` | Masih ada shift aktif untuk user/outlet ini |
| `SHIFT_NOT_FOUND` | Shift tidak ditemukan |
| `SHIFT_ALREADY_CLOSED` | Shift sudah ditutup |
| `SHIFT_REQUIRED` | Outlet mensyaratkan shift aktif sebelum transaksi |
| `SHIFT_ADJUSTMENT_INVALID` | Nilai atau tipe adjustment shift tidak valid |

**Refund:**

| Error Code | Deskripsi |
|---|---|
| `REFUND_NOT_FOUND` | Refund tidak ditemukan |
| `REFUND_AMOUNT_INVALID` | Nilai refund tidak valid |
| `REFUND_AMOUNT_EXCEEDED` | Nilai refund melebihi refundable amount |
| `REFUND_STATUS_INVALID` | Status refund tidak mengizinkan aksi ini |
| `REFUND_TRANSACTION_NOT_REFUNDABLE` | Transaksi tidak dapat direfund |

**Loyalty Program:**

| Error Code | Deskripsi |
|---|---|
| `LOYALTY_PROGRAM_NOT_FOUND` | Program loyalty tidak ditemukan |
| `LOYALTY_PROGRAM_INACTIVE` | Program loyalty tidak aktif |
| `LOYALTY_REDEMPTION_RULE_NOT_FOUND` | Rule redeem loyalty tidak ditemukan |
| `LOYALTY_REDEMPTION_RULE_INVALID` | Rule redeem tidak valid untuk request ini |
| `LOYALTY_PRODUCT_OVERRIDE_INVALID` | Konfigurasi loyalty produk tidak valid |

**MDR:**

| Error Code | Deskripsi |
|---|---|
| `MDR_SETTING_NOT_FOUND` | Setting MDR tidak ditemukan |
| `MDR_SETTING_DUPLICATE` | Setting MDR aktif untuk payment method ini sudah ada |
| `MDR_PERCENTAGE_INVALID` | Persentase MDR tidak valid |
| `MDR_PAYMENT_METHOD_INVALID` | Payment method untuk MDR tidak dikenali |

**Notification:**

| Error Code | Deskripsi |
|---|---|
| `NOTIFICATION_SETTING_NOT_FOUND` | Konfigurasi notifikasi tidak ditemukan |
| `NOTIFICATION_TYPE_INVALID` | Tipe notifikasi tidak valid |
| `NOTIFICATION_SEND_TIME_INVALID` | Format waktu kirim tidak valid |
| `NOTIFICATION_DESTINATION_REQUIRED` | Tujuan notifikasi wajib diisi |

**Variant / Modifier:**

| Error Code | Deskripsi |
|---|---|
| `VARIANT_GROUP_NOT_FOUND` | Grup variant tidak ditemukan |
| `VARIANT_NOT_FOUND` | Variant tidak ditemukan |
| `VARIANT_PRODUCT_MISMATCH` | Variant tidak milik produk ini |
| `MODIFIER_GROUP_NOT_FOUND` | Grup modifier tidak ditemukan |
| `MODIFIER_NOT_FOUND` | Modifier tidak ditemukan |
| `MODIFIER_PRODUCT_MISMATCH` | Modifier tidak milik produk ini |
| `MODIFIER_SELECTION_INVALID` | Pilihan modifier tidak memenuhi aturan min/max |

**Report:**

| Error Code | Deskripsi |
|---|---|
| `REPORT_DATE_RANGE_INVALID` | startDate lebih besar dari endDate |
| `REPORT_DATE_RANGE_TOO_WIDE` | Rentang tanggal melebihi batas maksimal |
| `REPORT_OUTLET_FORBIDDEN` | Outlet tidak milik merchant ini |

### 8.4 Validasi Input Umum

Seluruh endpoint yang menerima input wajib menerapkan validasi berikut sebelum meneruskan ke business logic:

1. **UUID Fields:** Validasi format UUID yang benar untuk semua field ID.
2. **Mandatory Fields:** Field wajib harus terisi dan tidak null/blank.
3. **Enum Fields:** Nilai harus salah satu dari nilai enum yang terdefinisi.
4. **Decimal Precision:** Field finansial menggunakan maksimal 2 desimal.
5. **String Length:** Nama dan deskripsi dibatasi maksimal 255 karakter kecuali dinyatakan lain.
6. **Date Format:** ISO 8601 (`yyyy-MM-ddTHH:mm:ssZ`) untuk semua field timestamp.

### 8.5 Penanganan Error Database

| Kondisi | Respons |
|---|---|
| Unique constraint violation | `409 Conflict` dengan pesan yang deskriptif |
| Foreign key constraint violation | `400 Bad Request` — "Data referensi tidak ditemukan" |
| Optimistic locking failure | `409 Conflict` — "Data diubah bersamaan, coba lagi" |
| Connection timeout | `503 Service Unavailable` |

### 8.6 Logging Standar

Setiap error `5xx` wajib di-log dengan informasi berikut:
- `timestamp`
- `merchantId` (dari JWT)
- `userId` (dari JWT)
- `requestPath`
- `requestMethod`
- `errorMessage`
- `stackTrace`

Error `4xx` cukup di-log tanpa stack trace (untuk efisiensi log), kecuali error yang mengindikasikan potensi serangan (contoh: PIN brute-force).

### 8.7 Catatan Implementasi Persistence

Untuk menghindari mismatch antara kontrak API dan schema migration, implementasi disarankan memisahkan:

- **Nama field API/DTO**: boleh memakai istilah bisnis seperti `grossSubTotal`, `netSubTotal`, `refundAmount`, `loyaltyHistory`.
- **Nama field entity/database**: harus mengikuti schema migration aktual.

Mapping minimal yang wajib diperhatikan:

| Layer API / FSD | Layer Database |
|---|---|
| `grossSubTotal` | `transaction.gross_amount` |
| `netSubTotal` | `transaction.net_amount` |
| `refundAmount` | `transaction.refund_amount` |
| `refundReason` | `transaction.refund_reason` |
| `refundBy` | `transaction.refund_by` |
| `refundDate` | `transaction.refund_date` |
| `voucher_code` | tabel `voucher` |
| `loyalty_history` | tabel `loyalty_transaction` |
| `printer` | tabel `printer_setting` |

Jika diperlukan, backend boleh membuat mapper atau projection khusus agar naming response tetap konsisten dengan FSD tanpa mengubah schema persistence.

### 8.8 Contoh Request / Response

Contoh di bawah ini bersifat referensial untuk membantu implementasi client dan backend. Field tambahan seperti audit info, pagination, atau metadata boleh ditambahkan selama tidak mengubah kontrak inti.

#### 8.8.1 Order Type

**POST `/pos/order-type/add`**
```json
{
  "name": "Dine In",
  "code": "DINE_IN",
  "isDefault": true
}
```

**Response**
```json
{
  "success": true,
  "message": "Order type berhasil dibuat",
  "data": {
    "id": "8c9f9d47-8f54-4a85-90b8-fb0f8cf26b08",
    "name": "Dine In",
    "code": "DINE_IN",
    "isDefault": true,
    "isActive": true
  }
}
```

#### 8.8.2 Shift Open

**POST `/pos/shift/open`**
```json
{
  "outletId": "5dc11534-9316-49d2-bf51-0d7a1bdf28a9",
  "openingCash": 500000,
  "note": "Shift pagi"
}
```

**Response**
```json
{
  "success": true,
  "message": "Shift berhasil dibuka",
  "data": {
    "id": "f722c9d2-6a31-4a7d-b1d3-6b4298546c24",
    "status": "OPEN",
    "openingCash": 500000,
    "openDate": "2026-03-31T08:00:00Z",
    "outletId": "5dc11534-9316-49d2-bf51-0d7a1bdf28a9"
  }
}
```

#### 8.8.3 Refund Request

**POST `/pos/refund/request`**
```json
{
  "transactionId": "72b0f29d-98f0-4cb7-bdd2-b69578b5c433",
  "amount": 25000,
  "type": "PARTIAL",
  "reason": "Produk salah input",
  "operationToken": "op_token_xxx"
}
```

**Response**
```json
{
  "success": true,
  "message": "Refund berhasil diajukan",
  "data": {
    "id": "d6866446-5f35-446f-aa87-b575b4b19b6f",
    "transactionId": "72b0f29d-98f0-4cb7-bdd2-b69578b5c433",
    "amount": 25000,
    "type": "PARTIAL",
    "status": "PENDING",
    "reason": "Produk salah input"
  }
}
```

#### 8.8.4 Loyalty Program

**POST `/pos/loyalty/program/create`**
```json
{
  "name": "Loyalty Reguler",
  "earnMode": "RATIO",
  "pointsPerAmount": 10000,
  "expiryMode": "ROLLING_DAYS",
  "expiryDays": 365
}
```

**Response**
```json
{
  "success": true,
  "message": "Program loyalty berhasil dibuat",
  "data": {
    "id": "8bf80984-6b3a-45c3-88f2-0bd211f66f77",
    "name": "Loyalty Reguler",
    "earnMode": "RATIO",
    "pointsPerAmount": 10000,
    "expiryMode": "ROLLING_DAYS",
    "expiryDays": 365,
    "isActive": true
  }
}
```

#### 8.8.5 MDR Preview

**POST `/pos/mdr/preview`**
```json
{
  "paymentMethodCode": "CREDIT_CARD_BCA",
  "amount": 100000
}
```

**Response**
```json
{
  "success": true,
  "message": "Preview MDR berhasil",
  "data": {
    "paymentMethodCode": "CREDIT_CARD_BCA",
    "amount": 100000,
    "percentage": 2.5,
    "flatFee": 0,
    "chargedTo": "MERCHANT",
    "feeAmount": 2500,
    "customerPays": 100000,
    "settlementNet": 97500
  }
}
```

#### 8.8.6 Notification Setting

**POST `/pos/notification-setting/create`**
```json
{
  "emailAddress": "owner@merchant.com",
  "isEnabled": true,
  "notifyTypes": ["DAILY_SUMMARY", "SHIFT_CLOSE"],
  "sendTime": "21:00"
}
```

**Response**
```json
{
  "success": true,
  "message": "Setting notifikasi berhasil dibuat",
  "data": {
    "id": "a3040740-6d56-4cb1-9a22-0682f32e5861",
    "emailAddress": "owner@merchant.com",
    "isEnabled": true,
    "notifyTypes": ["DAILY_SUMMARY", "SHIFT_CLOSE"],
    "sendTime": "21:00"
  }
}
```

#### 8.8.7 Product Variant Group

**POST `/pos/product/{productId}/variant-group/add`**
```json
{
  "name": "Ukuran",
  "isRequired": true,
  "displayOrder": 1,
  "variants": [
    { "name": "Regular", "additionalPrice": 0, "displayOrder": 1 },
    { "name": "Large", "additionalPrice": 5000, "displayOrder": 2 }
  ]
}
```

**Response**
```json
{
  "success": true,
  "message": "Grup variant berhasil dibuat",
  "data": {
    "id": "f57b2f27-c0fc-4717-9b57-7b360ca66490",
    "name": "Ukuran",
    "isRequired": true,
    "variants": [
      { "id": "ca6c8817-31ce-4b3e-90c6-64c421fcf6dd", "name": "Regular", "additionalPrice": 0 },
      { "id": "7bd94f68-3f7f-4ba0-a6cb-4612b2cb130f", "name": "Large", "additionalPrice": 5000 }
    ]
  }
}
```

#### 8.8.8 Product Modifier Group

**POST `/pos/product/{productId}/modifier-group/add`**
```json
{
  "name": "Topping",
  "minSelect": 0,
  "maxSelect": 3,
  "isRequired": false,
  "displayOrder": 1,
  "modifiers": [
    { "name": "Extra Shot", "additionalPrice": 4000, "displayOrder": 1 },
    { "name": "Whipped Cream", "additionalPrice": 3000, "displayOrder": 2 }
  ]
}
```

**Response**
```json
{
  "success": true,
  "message": "Grup modifier berhasil dibuat",
  "data": {
    "id": "709fd0df-63e5-43ab-80e2-4874b6963c85",
    "name": "Topping",
    "minSelect": 0,
    "maxSelect": 3,
    "modifiers": [
      { "id": "2d6626d1-c1e0-4c8a-9ba1-c59fdb8a4dfe", "name": "Extra Shot", "additionalPrice": 4000 },
      { "id": "f86f6948-bcb0-4f16-a806-f54afd5b6ebc", "name": "Whipped Cream", "additionalPrice": 3000 }
    ]
  }
}
```

#### 8.8.9 Create Transaction dengan Variant dan Modifier

**POST `/pos/transaction/create`**
```json
{
  "outletId": "5dc11534-9316-49d2-bf51-0d7a1bdf28a9",
  "customerId": "e864714c-efb5-40a2-92f5-135246f3e8c6",
  "orderTypeId": "8c9f9d47-8f54-4a85-90b8-fb0f8cf26b08",
  "shiftId": "f722c9d2-6a31-4a7d-b1d3-6b4298546c24",
  "items": [
    {
      "productId": "f9be39bf-5de3-4f4c-96aa-1eb41877e2ea",
      "qty": 2,
      "variantId": "7bd94f68-3f7f-4ba0-a6cb-4612b2cb130f",
      "modifiers": [
        { "modifierId": "2d6626d1-c1e0-4c8a-9ba1-c59fdb8a4dfe", "qty": 1 },
        { "modifierId": "f86f6948-bcb0-4f16-a806-f54afd5b6ebc", "qty": 1 }
      ]
    }
  ],
  "discountCode": "HEMAT10",
  "voucherCode": "VCR-2026-0001",
  "loyaltyRedeemPoints": 100
}
```

**Response**
```json
{
  "success": true,
  "message": "Transaksi berhasil dibuat",
  "data": {
    "transactionId": "72b0f29d-98f0-4cb7-bdd2-b69578b5c433",
    "status": "PENDING",
    "grossSubTotal": 44000,
    "promoAmount": 0,
    "discountAmount": 4400,
    "netSubTotal": 39600,
    "serviceCharge": 1980,
    "totalTax": 4356,
    "voucherAmount": 10000,
    "loyaltyRedeemAmount": 10000,
    "amountDue": 25936,
    "items": [
      {
        "productId": "f9be39bf-5de3-4f4c-96aa-1eb41877e2ea",
        "qty": 2,
        "variantId": "7bd94f68-3f7f-4ba0-a6cb-4612b2cb130f",
        "originalPrice": 22000,
        "unitPrice": 22000,
        "modifiers": [
          { "modifierName": "Extra Shot", "additionalPrice": 4000, "qty": 1 },
          { "modifierName": "Whipped Cream", "additionalPrice": 3000, "qty": 1 }
        ]
      }
    ]
  }
}
```

#### 8.8.10 Error Response Contoh

```json
{
  "success": false,
  "message": "Variant tidak milik produk ini",
  "data": null,
  "errorCode": "VARIANT_PRODUCT_MISMATCH",
  "timestamp": "2026-03-31T10:00:00Z"
}
```

---

*Dokumen ini adalah referensi fungsional hidup. Setiap perubahan requirement harus didokumentasikan dengan nomor versi dan catatan perubahan.*

*Versi 1.0.0 — Ditetapkan 31 Maret 2026*
