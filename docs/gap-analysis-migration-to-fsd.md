# Gap Analysis: Schema Migration Phase 2 vs FSD Phase 2

**Tanggal:** 31 Maret 2026  
**Tujuan:** Mengidentifikasi entitas/aturan yang sudah muncul di `schema-migration-phase2.md` tetapi belum, belum lengkap, atau belum eksplisit dijelaskan di `FSD-Phase2.md`.  
**Pendekatan:**  
- `schema-migration-phase2.md` diperlakukan sebagai indikator cakupan implementasi data model  
- `FSD-Phase2.md` diperlakukan sebagai sumber requirement fungsional utama  
- Website publik iSeller dipakai sebagai benchmark untuk menjelaskan kenapa gap tersebut penting secara bisnis

---

## 1. Ringkasan Eksekutif

Secara umum, **schema migration Phase 2 lebih luas daripada FSD Phase 2**. FSD sudah cukup kuat untuk modul pricing, voucher, CRM dasar, cashier account, receipt/printer, disbursement, dan report. Namun migration menunjukkan ada beberapa kapabilitas yang level data model-nya sudah disiapkan, sementara FSD belum memberi spesifikasi fungsional yang setara.

Gap terbesar ada pada:

1. **Refund**  
2. **Cashier shift / workshift**  
3. **Notification setting**  
4. **MDR setting**  
5. **Order type dan language setting**  
6. **Loyalty program yang lebih kaya daripada sekadar earn/redeem sederhana**  
7. **Product variant / modifier snapshot di transaksi**  
8. **Discount / promotion eligibility yang lebih granular**

Dengan kata lain, migration merepresentasikan arah sistem yang lebih dekat ke POS modern, sementara FSD masih bias ke modul inti back-office dan pricing engine.

---

## 2. Temuan Gap

| Area | Ada di Migration | Ada di FSD | Status Gap | Kenapa Penting |
|---|---|---|---|---|
| Refund | `refund` table + kolom refund di `transaction` | Hanya disebut di status, PIN, dan report | **High** | Tanpa spesifikasi refund, data model ada tapi perilaku bisnis belum jelas |
| Cashier shift / workshift | `cashier_shift`, `transaction.shift_id` | Tidak ada modul khusus | **High** | Operasional kasir harian tidak cukup dijelaskan dengan modul cashier account saja |
| Notification settings | `notification_setting` | Tidak ada | **High** | Fitur alert operasional tidak terdokumentasi |
| MDR | `mdr_setting` | Tidak ada | **High** | Dampak biaya payment gateway ke settlement/report belum terdefinisi |
| Order type | `order_type`, `outlet.default_order_type_id`, `transaction.order_type_id` | Hanya tersirat di Price Book | **Medium-High** | Order type mempengaruhi pricing, workflow, dan reporting |
| Language setting | `merchant.language_code`, `outlet.language_code` | Tidak ada modul/aturan | **Medium** | Sudah disiapkan di schema, belum ada kontrak konfigurasi |
| Loyalty program advanced | `loyalty_program`, `loyalty_redemption_rule`, `product_loyalty_setting`, `loyalty_transaction` | Hanya loyalty earn/redeem sederhana di CRM | **High** | FSD belum menjelaskan sumber aturan loyalty yang dipakai saat transaksi |
| Product variants & modifiers | `product_variant_group`, `product_variant`, `product_modifier_group`, `product_modifier`, `transaction_modifier`, `transaction_items.variant_id` | Tidak ada | **Medium-High** | Schema sudah mendukung POS retail/F&B yang lebih kaya, FSD belum |
| Discount customer/payment-method eligibility | `discount_customer`, `discount_payment_method` | Tidak ada | **Medium** | Restriksi diskon per customer/metode bayar belum terdokumentasi |
| Promotion customer eligibility | `promotion_customer` | Tidak ada | **Medium** | Promosi targeted customer belum tercakup |
| Voucher usage audit | `voucher_usage` | Tidak ada tabel audit terpisah | **Medium** | Audit penggunaan voucher akan dibutuhkan untuk rekonsiliasi |
| Printer setting persistence | `printer_setting` | Ada sebagai `printer` | **Low** | Lebih ke penyelarasan naming dan field persistensi |

---

## 3. Penjelasan Gap per Area

### 3.1 Refund: schema ada, FSD belum punya modul

**Indikasi di migration**
- Tabel `refund`
- Kolom `refund_amount`, `refund_reason`, `refund_by`, `refund_date` di `transaction`

**Kondisi di FSD**
- FSD hanya menyebut status `REFUNDED`, kebutuhan PIN override, dan `totalRefund` di laporan.
- Belum ada user story, endpoint, business rule, state approval, partial/full refund, atau dampaknya ke transaksi asal.

**Gap**
- Tidak jelas apakah refund bisa parsial atau penuh
- Tidak jelas kapan `transaction.status` menjadi `REFUNDED`
- Tidak jelas bagaimana hubungan refund dengan loyalty, voucher, disbursement, dan inventory

**Penjelasan dengan benchmark iSeller**
- Listing App Store iSeller POS Retail menonjolkan `refund or void orders` dan `complete or partial refunds supporting partial quantity refunds`.
- Ini menunjukkan refund bukan sekadar status akhir, tetapi workflow operasional inti di POS.

**Rekomendasi**
- Tambah modul FSD terpisah: `Refund at POS`
- Minimum isi: endpoint create/approve/refuse refund, tipe FULL/PARTIAL, reason, approval PIN, pengaruh ke laporan, stok, loyalty, voucher, dan disbursement

### 3.2 Cashier Shift / Workshift: migration sudah siap, FSD belum

**Indikasi di migration**
- Tabel `cashier_shift`
- `transaction.shift_id`

**Kondisi di FSD**
- Modul cashier hanya membahas akun kasir, password, PIN, role, dan outlet binding.
- Tidak ada konsep buka/tutup shift, opening cash, closing cash, atau rekonsiliasi uang kas.

**Gap**
- Akun kasir dan shift operasional masih tercampur
- Tidak ada definisi ringkasan shift per kasir/register/outlet
- Tidak ada relasi operasional antara transaksi dengan shift yang aktif

**Penjelasan dengan benchmark iSeller**
- Website iSeller menonjolkan kategori `Workshifts`.
- Deskripsi publik iSeller juga menyebut pengelolaan jam kerja, pergantian shift, dan pada listing App Store ada `register shift workflow`, `cash adjustments`, `shift closing`, dan `expected cash count`.

**Rekomendasi**
- Tambah modul FSD: `Workshift & Cash Register`
- Minimum isi: open shift, close shift, cash adjustment, expected vs actual cash, aturan satu shift aktif per kasir/register

### 3.3 Notification Setting: ada di migration, hilang di FSD

**Indikasi di migration**
- Tabel `notification_setting`

**Kondisi di FSD**
- Tidak ada modul, endpoint, business rule, maupun event yang memicu notifikasi.

**Gap**
- Tidak jelas jenis notifikasi, channel, jadwal kirim, dan siapa penerimanya

**Penjelasan dengan benchmark iSeller**
- Halaman fitur iSeller menonjolkan notifikasi penjualan dan peringatan stok rendah di Admin App.
- Ini mengindikasikan notifikasi diposisikan sebagai fitur operasional, bukan tambahan minor.

**Rekomendasi**
- Tambah modul FSD: `Notification Settings`
- Minimum isi: recipient, notify type, schedule, outlet scope, event source

### 3.4 MDR Setting: data model ada, konsekuensi bisnis belum dijelaskan

**Indikasi di migration**
- Tabel `mdr_setting`

**Kondisi di FSD**
- Tidak ada pembahasan merchant discount rate, siapa yang menanggung fee, atau efeknya ke net settlement/report/disbursement.

**Gap**
- Tidak jelas apakah MDR mengurangi gross, net, atau hanya dipakai untuk reporting
- Tidak jelas interaksinya dengan split payment, payment method, dan disbursement

**Penjelasan dengan benchmark iSeller**
- iSeller memasarkan payment flexibility, kartu kredit, dan channel pembayaran yang beragam.
- Begitu payment method makin beragam, biaya per metode bayar menjadi concern nyata untuk merchant dan settlement.

**Rekomendasi**
- Tambah modul FSD: `MDR / Payment Fee Setting`
- Minimum isi: basis fee, charged-to, impact ke report dan disbursement

### 3.5 Order Type: penting di schema, belum dimodelkan penuh di FSD

**Indikasi di migration**
- Tabel `order_type`
- `outlet.default_order_type_id`
- `transaction.order_type_id`

**Kondisi di FSD**
- FSD membahas Price Book type `ORDER_TYPE`, tetapi tidak ada modul yang mendefinisikan master `order_type`.
- Tidak ada endpoint untuk CRUD order type atau aturan default outlet.

**Gap**
- Price Book bergantung pada order type, tetapi master data order type belum dijelaskan
- Reporting dan transaksi yang memakai dine-in/take-away/delivery belum punya definisi formal

**Penjelasan dengan benchmark iSeller**
- iSeller publik menonjolkan use case retail, F&B, dan service business. Semua segmen itu sangat bergantung pada variasi order context.

**Rekomendasi**
- Tambah modul kecil atau sub-bab: `Order Type Management`
- Minimum isi: CRUD order type, default outlet, penggunaan di transaksi dan pricing

### 3.6 Language Setting: schema siap, FSD belum

**Indikasi di migration**
- `merchant.language_code`
- `outlet.language_code`

**Kondisi di FSD**
- Tidak ada modul bahasa.

**Gap**
- Tidak ada definisi fallback merchant -> outlet
- Tidak ada daftar value valid, perilaku default, atau apakah ini hanya UI preference

**Penjelasan dengan benchmark iSeller**
- Halaman fitur iSeller menyebut fleksibilitas bahasa, zona waktu, dan konfigurasi lintas outlet.

**Rekomendasi**
- Tambah modul ringkas: `POS Language Setting`
- Kalau memang belum jadi scope, tandai eksplisit sebagai backlog agar tidak menjadi orphan schema

### 3.7 Loyalty: FSD terlalu sederhana dibanding schema

**Indikasi di migration**
- `loyalty_program`
- `loyalty_redemption_rule`
- `loyalty_transaction`
- `product_loyalty_setting`

**Kondisi di FSD**
- Hanya ada earn sederhana dengan `loyaltyEarnRate`
- Redeem sederhana dengan `loyaltyRedeemRate`
- Tidak ada entitas konfigurasi loyalty global
- Tidak ada rule type `PAYMENT`, `DISCOUNT`, `FREE_PRODUCT`
- Tidak ada product-level override

**Gap**
- FSD memakai istilah rate, tetapi tidak mendefinisikan dari mana rate itu berasal
- Tidak ada aturan expiry points
- Tidak ada multiple redemption rule

**Penjelasan dengan benchmark iSeller**
- Halaman fitur iSeller menyebut loyalty yang fleksibel, multi-level, dan product-specific.
- Ini sejalan dengan schema migration yang memang sudah menyiapkan rule loyalty yang lebih kaya.

**Rekomendasi**
- Perluasan modul `CRM Customer` atau modul baru `Loyalty Program`
- Minimum isi: earn mode, expiry, redemption rule, product override, audit trail poin

### 3.8 Product Variant / Modifier: schema sudah mendukung, FSD belum memakai

**Indikasi di migration**
- `product_variant_group`, `product_variant`
- `product_modifier_group`, `product_modifier`
- `transaction_modifier`
- `transaction_items.variant_id`

**Kondisi di FSD**
- Tidak ada pembahasan variant/modifier dalam transaksi
- Tidak ada aturan bagaimana promo, price book, tax, dan report berinteraksi dengan variant/modifier

**Gap**
- Data transaksi berpotensi menyimpan variant/modifier, tetapi FSD belum mendefinisikan perilakunya
- Ini akan memengaruhi subtotal item, snapshot harga, receipt, kitchen/order print, dan report produk

**Penjelasan dengan benchmark iSeller**
- iSeller publik menonjolkan varian produk, modifier produk, dan combo set terutama untuk retail/F&B.
- Artinya, fitur ini bukan edge case tetapi bagian inti dari modeling produk modern.

**Rekomendasi**
- Tambah sub-bab dependensi di FSD atau referensi eksplisit ke dokumen product/transaction Phase 1
- Minimum isi: kalkulasi harga variant/modifier, snapshot transaksi, kompatibilitas dengan promo/discount

### 3.9 Discount dan Promotion: schema lebih granular daripada FSD

**Indikasi di migration**
- `discount_customer`
- `discount_payment_method`
- `promotion_customer`

**Kondisi di FSD**
- Discount hanya dibatasi oleh scope produk/kategori/outlet/channel
- Promotion hanya dibatasi oleh waktu, hari, outlet, dan kondisi cart

**Gap**
- Tidak ada targeted discount/promotion berdasarkan customer
- Tidak ada discount khusus payment method

**Penjelasan dengan benchmark iSeller**
- POS modern biasa memakai promo yang ditujukan ke customer tertentu atau payment method tertentu untuk mendorong adopsi channel bayar.

**Rekomendasi**
- Tambah business rules dan binding table terkait ke modul discount/promotion di FSD

### 3.10 Voucher Usage Audit: schema punya log khusus, FSD belum

**Indikasi di migration**
- `voucher_usage`

**Kondisi di FSD**
- FSD hanya menyebut update status `voucher_code` menjadi `USED`.

**Gap**
- Tidak ada audit log penggunaan voucher per transaksi/customer/amount

**Penjelasan dengan benchmark iSeller**
- Saat voucher/gift card menjadi alat bayar, audit trail penting untuk rekonsiliasi dan dispute handling.

**Rekomendasi**
- Tambah tabel/log audit ke spesifikasi voucher

---

## 4. Prioritas Revisi FSD

Urutan revisi yang paling rasional:

1. `Refund at POS`
2. `Workshift & Cash Register`
3. `Notification Settings`
4. `MDR / Payment Fee`
5. `Order Type Management`
6. `Loyalty Program`
7. `Variant / Modifier Transaction Rules`
8. Penyempurnaan `Discount` dan `Promotion`

---

## 5. Kesimpulan

Jika dilihat dari migration, sistem sebenarnya sedang bergerak ke arah POS yang lebih lengkap daripada yang tergambar di FSD saat ini. Benchmark iSeller menguatkan bahwa gap-gap tersebut memang penting secara bisnis, terutama pada area refund, shift/workshift, loyalty yang fleksibel, notification, dan product modeling yang lebih kaya.

Artinya, problem utamanya bukan schema terlalu jauh, tetapi **FSD belum mengejar level detail implementasi yang sudah dipersiapkan oleh migration**.

---

## 6. Referensi Benchmark iSeller

- `https://www.isellercommerce.com/id/features`
- `https://www.isellercommerce.com/features`
- `https://www.isellercommerce.com/pos`
- `https://www.isellercommerce.com/id/pos/retail`
- `https://www.isellercommerce.com/faq`
- `https://apps.apple.com/us/app/iseller-pos-retail/id1478510428`
