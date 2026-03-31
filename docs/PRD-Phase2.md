# PRD — POS Service Revamp Phase 2

**Dokumen:** Product Requirements Document  
**Versi:** 1.0  
**Tanggal:** 31 Maret 2026  
**Status:** In Progress  
**Penulis:** Tim Product & Engineering  
**Proyek:** POS Service Revamp — Phase 2

---

## Daftar Isi

1. [Ringkasan Eksekutif](#1-ringkasan-eksekutif)
2. [Latar Belakang & Konteks](#2-latar-belakang--konteks)
3. [Masalah yang Diselesaikan](#3-masalah-yang-diselesaikan)
4. [Visi & Tujuan Produk](#4-visi--tujuan-produk)
5. [Target Pengguna & Persona](#5-target-pengguna--persona)
6. [Daftar Fitur Phase 2](#6-daftar-fitur-phase-2)
7. [User Journey Utama](#7-user-journey-utama)
8. [Success Metrics & KPI](#8-success-metrics--kpi)
9. [Batasan & Out of Scope](#9-batasan--out-of-scope)
10. [Asumsi & Dependency](#10-asumsi--dependency)
11. [Risiko & Mitigasi](#11-risiko--mitigasi)
12. [Timeline & Milestone](#12-timeline--milestone)

---

## 1. Ringkasan Eksekutif

POS Service Revamp Phase 2 adalah lanjutan dari Phase 1 yang telah membangun fondasi sistem kasir berbasis cloud, mencakup autentikasi, manajemen produk & kategori, stok, pengaturan metode pembayaran, transaksi dasar, dan laporan ringkasan. Phase 2 bertujuan mengangkat sistem ini dari sekadar alat kasir sederhana menjadi platform commerce yang komprehensif dan siap skala enterprise.

Dalam Phase 2, 14 fitur strategis direncanakan untuk diimplementasikan. Per tanggal dokumen ini, 10 fitur telah selesai diimplementasikan dengan total 108 endpoint REST API aktif. Empat fitur tersisa — POS Language Setting, Refund at POS, True Split Payment, dan Notification Settings — sedang dalam antrean pengembangan.

Sistem dibangun di atas arsitektur multi-tenant dan multi-outlet: setiap merchant beroperasi dalam isolasi data yang ketat, dan setiap merchant dapat memiliki satu atau lebih outlet yang masing-masing memiliki konfigurasi independen. Stack teknologi yang digunakan adalah Spring Boot 4.0.5 dengan Kotlin, PostgreSQL sebagai database utama, dan JWT untuk autentikasi stateless.

Keberhasilan Phase 2 diukur dari tiga dimensi: (1) kelengkapan fitur dibanding target Excel planning, (2) adopsi fitur oleh merchant aktif, dan (3) kualitas sistem — performa, keandalan, dan kemudahan integrasi oleh klien front-end (kasir web/mobile).

---

## 2. Latar Belakang & Konteks

### 2.1 Kondisi Sebelum Proyek

Sebelum proyek revamp ini dimulai, sistem POS yang ada berjalan pada arsitektur lama yang monolitik dan sulit di-maintain. Beberapa masalah utama yang teridentifikasi:

- **Coupling tinggi:** Logika bisnis, data access, dan presentasi bercampur sehingga setiap perubahan fitur berisiko merusak fitur lain.
- **Tidak ada multi-tenancy:** Sistem lama menggunakan database terpisah per merchant, menyulitkan skalabilitas dan operasi.
- **Tidak ada multi-outlet:** Merchant dengan lebih dari satu cabang harus menggunakan instance terpisah, tanpa konsolidasi laporan.
- **Keterbatasan fitur komersial:** Tidak ada dukungan diskon fleksibel, program loyalitas, split payment, atau revenue sharing.
- **Integrasi pembayaran terbatas:** Hanya mendukung satu atau dua metode pembayaran statis.

### 2.2 Apa yang Dibangun di Phase 1

Phase 1 membangun ulang fondasi sistem dari nol dengan standar arsitektur modern:

- **Autentikasi & Otorisasi:** JWT-based, dengan klaim `merchantId` dan `outletId` tertanam di token sehingga setiap request secara otomatis ter-scope ke tenant yang benar.
- **Manajemen Produk & Kategori:** CRUD lengkap dengan hierarki kategori dan dukungan gambar produk.
- **Manajemen Stok:** Tracking stok per outlet, history mutasi, dan alert stok minimum.
- **Payment Settings:** Konfigurasi metode pembayaran yang aktif per merchant.
- **Transaksi Dasar:** Flow kasir standar — buat keranjang, tambah item, proses pembayaran, cetak struk.
- **Laporan Ringkasan:** Laporan harian sederhana per outlet (total penjualan, jumlah transaksi, breakdown produk).

### 2.3 Mengapa Phase 2 Diperlukan

Merchant yang menggunakan sistem Phase 1 segera merasakan kebutuhan fitur yang lebih canggih:

- Merchant ritel membutuhkan program promosi dan diskon yang fleksibel untuk meningkatkan konversi.
- Merchant F&B membutuhkan service charge dan pajak yang dapat dikonfigurasi per item atau per order.
- Merchant dengan jaringan cabang membutuhkan laporan keuangan terkonsolidasi dan pengaturan revenue sharing.
- Merchant enterprise membutuhkan manajemen kasir (user POS) dengan kontrol akses berbasis PIN.
- Semua merchant membutuhkan program loyalitas pelanggan untuk meningkatkan retensi.

Phase 2 menjawab semua kebutuhan ini secara sistematis berdasarkan prioritas bisnis dan feedback merchant aktif.

### 2.4 Konteks Teknis

```
Stack:
  - Language    : Kotlin (JVM 17)
  - Framework   : Spring Boot 4.0.5
  - Database    : PostgreSQL (Spring Data JPA)
  - Auth        : JWT (stateless, merchantId embedded in claims)
  - Build       : Gradle Kotlin DSL
  - Monitoring  : Spring Boot Actuator

Arsitektur Multi-Tenant:
  - Setiap request membawa JWT dengan klaim merchantId
  - Semua entitas di-scope ke merchantId secara otomatis di service layer
  - Multi-outlet: merchant → 1..N outlet, konfigurasi independen per outlet

Pendekatan desain endpoint:
  - RESTful dengan konvensi /api/v1/{resource}
  - Response envelope: { success, message, data, errors }
  - Pagination: ?page=0&size=20&sort=createdAt,desc
```

---

## 3. Masalah yang Diselesaikan

Phase 2 menyelesaikan 14 kelompok masalah bisnis dan operasional yang nyata dihadapi oleh merchant:

### 3.1 Kompleksitas Perpajakan dan Biaya Tambahan
Merchant harus mematuhi regulasi pajak (PPN, pajak daerah, dll.) dan menerapkan service charge sebagai bagian dari model bisnis mereka, terutama di segmen F&B dan hospitality. Sistem lama tidak memiliki engine kalkulasi pajak yang fleksibel. Phase 2 menghadirkan:
- Multiple tax rate per merchant (misalnya PPN 11% + Pajak Daerah 2%)
- Service charge dengan 4 basis kalkulasi: flat amount, percentage of subtotal, percentage of tax, percentage of subtotal+tax
- Konfigurasi pajak inklusif atau eksklusif per produk

### 3.2 Ketiadaan Program Promosi dan Diskon
Tanpa sistem promosi, merchant tidak memiliki alat untuk mendorong penjualan, menghabiskan stok lama, atau merespons persaingan. Phase 2 menghadirkan dua mekanisme diskon:
- **Discount Code:** Kode kupon dengan scope produk/kategori/semua, batas penggunaan (total dan per pelanggan), window validitas tanggal.
- **Automatic Promotion:** Auto-apply tanpa kode dengan tipe DISCOUNT_BY_ORDER, BUY_X_GET_Y, DISCOUNT_BY_ITEM_SUBTOTAL.

### 3.3 Harga Tidak Fleksibel
Satu harga per produk tidak memadai untuk model bisnis yang beragam. Merchant membutuhkan:
- Harga khusus per tipe order (dine-in vs takeaway vs delivery)
- Harga grosir (wholesale) berdasarkan kuantitas
- Override harga per outlet untuk merchant multi-cabang
- Price book yang dapat diaktifkan/dinonaktifkan kapan saja

### 3.4 Pengalaman Struk yang Generik
Merchant membutuhkan struk yang mencerminkan identitas brand mereka dan memenuhi kebutuhan kepatuhan (menampilkan NPWP, nama outlet, QR code untuk pembayaran digital, dll.). Phase 2 menghadirkan konfigurasi template struk per merchant dan per outlet.

### 3.5 Kontrol Akses Kasir yang Lemah
Tanpa manajemen user kasir yang proper, merchant tidak dapat mengontrol siapa yang boleh melakukan transaksi sensitif (refund, diskon manual, void transaksi). Phase 2 memperkenalkan:
- CRUD user kasir dengan role berbeda
- PIN otorisasi untuk aksi sensitif
- Audit trail per kasir

### 3.6 Tidak Ada Alat Bayar Voucher
Merchant yang menjalankan program gift voucher atau top-up credit tidak memiliki infrastruktur untuk mengelola dan memvalidasi voucher saat checkout. Phase 2 membangun sistem voucher dengan hierarki brand → group → kode voucher individual.

### 3.7 Tidak Ada CRM Pelanggan
Merchant tidak memiliki visibilitas terhadap pelanggan mereka: siapa yang sering datang, berapa total belanja, produk apa yang disukai. Phase 2 menghadirkan database pelanggan terintegrasi dengan riwayat transaksi dan program loyalitas berbasis poin atau stamp.

### 3.8 Laporan Keuangan yang Terbatas
Laporan ringkasan Phase 1 tidak cukup untuk kebutuhan analitik bisnis. Merchant membutuhkan dashboard keuangan yang komprehensif: breakdown per metode bayar, top produk, komparasi antar outlet, proyeksi disbursement.

### 3.9 Tidak Ada Mekanisme Revenue Sharing
Untuk model bisnis platform (SAAS reseller, franchise, dealer), sistem harus mampu menghitung dan mengeksekusi pembagian pendapatan secara otomatis antar layer: platform, dealer, merchant, dan custom party.

### 3.10 Ketiadaan Fitur Operasional Penting (Backlog)
Empat fitur operasional krusial yang belum diimplementasikan akan menghambat penggunaan di lapangan:
- **Refund:** Tanpa alur refund, kasir harus melakukan workaround manual yang berisiko dan tidak ter-audit.
- **Split Payment:** Pelanggan yang membayar sebagian tunai dan sebagian QRIS tidak dapat dilayani.
- **Notifikasi:** Merchant tidak mendapatkan alert real-time untuk transaksi penting atau kejadian sistem.
- **Pengaturan Bahasa:** Interface kasir harus dapat disesuaikan bahasanya untuk merchant yang beroperasi di pasar non-Indonesia.

---

## 4. Visi & Tujuan Produk

### 4.1 Visi

> **"Menjadi platform kasir berbasis cloud yang paling fleksibel dan dapat dikonfigurasi di pasar Indonesia — memungkinkan merchant dari skala warung hingga jaringan franchise untuk beroperasi dengan satu sistem terpadu."**

### 4.2 Tujuan Strategis

| ID | Tujuan | Indikator Keberhasilan |
|----|--------|----------------------|
| G1 | Melengkapi seluruh 14 fitur Phase 2 | 14/14 fitur ter-deploy di production |
| G2 | Meningkatkan ARPU merchant | Minimal 30% merchant aktif menggunakan fitur berbayar Phase 2 |
| G3 | Mengurangi churn merchant | Churn rate turun 20% karena stickiness fitur loyalitas & CRM |
| G4 | Mempercepat onboarding partner | Dokumentasi API lengkap, waktu integrasi front-end < 5 hari kerja |
| G5 | Mendukung skalabilitas | Sistem mampu menangani 10.000 transaksi/hari tanpa degradasi performa |

### 4.3 Non-Goal (Bukan Tujuan Phase 2)

- Membangun aplikasi front-end kasir (Phase 2 adalah pure backend/API)
- Integrasi dengan platform e-commerce (Tokopedia, Shopee) — dijadwalkan Phase 3
- Modul akuntansi (jurnal, neraca, laporan laba rugi) — dijadwalkan Phase 3
- Hardware integration (printer thermal, barcode scanner) — tanggung jawab client app

---

## 5. Target Pengguna & Persona

### 5.1 Persona 1: Pemilik Usaha (Merchant Owner)

**Nama Representatif:** Budi, 38 tahun, pemilik jaringan kafe 3 outlet di Jakarta  
**Kebutuhan Utama:**
- Melihat laporan konsolidasi dari semua outlet dalam satu dashboard
- Mengatur promosi seasonal (hari raya, ulang tahun toko) tanpa bantuan IT
- Memastikan kasir tidak bisa memberikan diskon sembarangan tanpa otorisasi
- Mendapatkan notifikasi jika ada transaksi di atas threshold tertentu

**Pain Points:**
- Harus buka 3 sistem berbeda untuk melihat laporan semua outlet
- Promosi saat ini diatur manual di masing-masing kasir, rawan error
- Tidak tahu berapa banyak pelanggan loyal yang dimiliki

**Fitur Phase 2 yang Relevan:** Financial Reporting & Analytics, Automatic Promotions, User Management POS, Notification Settings, CRM Customer

---

### 5.2 Persona 2: Kasir / Operator POS

**Nama Representatif:** Sari, 23 tahun, kasir full-time di minimarket  
**Kebutuhan Utama:**
- Proses transaksi cepat tanpa hambatan
- Dapat menerima berbagai metode pembayaran termasuk kombinasi (split)
- Dapat memproses refund dengan mudah jika pelanggan complaint
- Interface yang intuitif dan dalam bahasa Indonesia

**Pain Points:**
- Saat ini tidak bisa terima bayar sebagian tunai sebagian transfer — harus tolak pelanggan
- Refund harus manual via owner, merusak pengalaman pelanggan
- Antarmuka dalam bahasa Inggris membingungkan

**Fitur Phase 2 yang Relevan:** True Split Payment, Refund at POS, POS Language Setting, User Management POS

---

### 5.3 Persona 3: Platform/Reseller (Dealer)

**Nama Representatif:** PT. Mitra Digital, perusahaan reseller SAAS POS  
**Kebutuhan Utama:**
- Mengelola ratusan merchant sebagai sub-tenant
- Menerima revenue sharing otomatis dari setiap transaksi
- Dapat branding platform dengan nama sendiri
- Laporan agregat semua merchant yang dinaungi

**Pain Points:**
- Pembagian revenue saat ini dilakukan manual setiap bulan, rawan sengketa
- Tidak ada visibilitas real-time ke performa merchant binaannya

**Fitur Phase 2 yang Relevan:** Split Disbursement (Revenue Sharing), Financial Reporting & Analytics

---

### 5.4 Persona 4: End Customer (Pelanggan Toko)

**Nama Representatif:** Dewi, 30 tahun, pelanggan setia kafe langganan  
**Kebutuhan Utama:**
- Mendapatkan reward atas kesetiaan belanja
- Bisa bayar menggunakan voucher gift dari teman
- Struk yang informatif (ada detail pajak, nominal SC, QR untuk feedback)

**Pain Points:**
- Tidak tahu apakah poinnya sudah terakumulasi atau belum
- Voucher hadiah tidak bisa digunakan karena sistem tidak mendukung

**Fitur Phase 2 yang Relevan:** CRM Customer & Loyalty, Voucher, Receipt View Settings

---

### 5.5 Persona 5: Finance/Accounting Staff

**Nama Representatif:** Rina, 35 tahun, staf keuangan jaringan restoran  
**Kebutuhan Utama:**
- Export laporan keuangan untuk rekonsiliasi
- Melihat proyeksi disbursement dari payment gateway
- Breakdown penjualan per metode bayar per hari

**Pain Points:**
- Harus export dari banyak sistem lalu gabungkan di Excel
- Tidak bisa lihat berapa yang masih tertahan di payment gateway

**Fitur Phase 2 yang Relevan:** Financial Reporting & Analytics, Split Disbursement

---

## 6. Daftar Fitur Phase 2

### 6.1 Tabel Master Fitur

| ID | Nama Fitur | Deskripsi Singkat | Prioritas | Status |
|----|-----------|-------------------|-----------|--------|
| F01 | Pajak & Biaya Lainnya | Engine kalkulasi pajak (multiple rate) dan service charge dengan 4 basis kalkulasi. Dapat dikonfigurasi per merchant dan override per outlet. | P0 - Critical | **Done** |
| F02 | Diskon Berbasis Kode | Kode diskon dengan scope produk/kategori/semua, batas penggunaan total dan per pelanggan, validitas tanggal, dan stack behavior dengan promosi lain. | P0 - Critical | **Done** |
| F03 | Promosi Otomatis | Auto-apply promotion tanpa kode: DISCOUNT_BY_ORDER (diskon total order), BUY_X_GET_Y (beli X gratis Y), DISCOUNT_BY_ITEM_SUBTOTAL (diskon berdasarkan subtotal item tertentu). | P0 - Critical | **Done** |
| F04 | Price Book & Daftar Harga | Multiple price list per merchant: PRODUCT (harga default), ORDER_TYPE (dine-in/takeaway/delivery), CATEGORY (override per kategori), WHOLESALE (harga grosir by quantity). Override per outlet. | P1 - High | **Done** |
| F05 | Tampilan Struk / Receipt Settings | Template struk per merchant/outlet: header, footer, logo, show/hide tax, service charge, QR payment, QR feedback. Support format thermal 58mm dan 80mm. | P1 - High | **Done** |
| F06 | User Management POS/Kasir | CRUD user kasir dengan role (OWNER, MANAGER, CASHIER). PIN 6 digit untuk otorisasi aksi sensitif. Reset password flow. Audit trail per kasir. | P0 - Critical | **Done** |
| F07 | Voucher | Hierarki Brand → Group → Kode Voucher. Voucher sebagai alat bayar saat checkout. Validitas, nominal, batas penggunaan per kode. | P1 - High | **Done** |
| F08 | CRM Customer | Database pelanggan (nama, kontak, tanggal lahir). Riwayat transaksi per pelanggan. Program loyalitas: POINT_BASED (akumulasi poin per rupiah) dan STAMP_BASED (cap per transaksi). Reward redemption. | P1 - High | **Done** |
| F09 | Financial Reporting & Analytics | Dashboard keuangan: ringkasan omzet, breakdown per metode bayar, top 10 produk, komparasi outlet, grafik tren harian/mingguan/bulanan, proyeksi disbursement. Export CSV/PDF. | P1 - High | **Done** |
| F10 | Split Disbursement (Revenue Sharing) | Aturan bagi hasil per layer: PLATFORM (pemilik platform), DEALER (reseller), MERCHANT (pemilik toko), CUSTOM (pihak ketiga). Konfigurasi persentase dan nominal minimum. | P2 - Medium | **Done** |
| F11 | POS Language Setting | Pengaturan bahasa interface POS per merchant dan per outlet. Mendukung Bahasa Indonesia dan Inggris di Phase 2, dapat diperluas. | P2 - Medium | **Pending** |
| F12 | Refund at POS | Alur refund dengan otorisasi PIN manager. Tipe FULL (refund seluruh transaksi) dan PARTIAL (refund sebagian item). Pencatatan alasan refund dan audit trail. | P0 - Critical | **Pending** |
| F13 | True Split Payment | Satu transaksi dibayar dengan dua atau lebih metode pembayaran berbeda. Validasi total split harus sama dengan total transaksi. | P0 - Critical | **Pending** |
| F14 | Notification Settings | Konfigurasi notifikasi per event: transaksi baru, refund, stok hampir habis, login mencurigakan. Channel: email dan push notification. Per merchant/outlet. | P2 - Medium | **Pending** |

### 6.2 Distribusi Status

- **Done:** 10 fitur (F01–F10)
- **Pending:** 4 fitur (F11–F14)
- **Total Endpoint Aktif:** 108 endpoints REST API

### 6.3 Detail Fitur Pending

**F12 — Refund at POS** mendapat prioritas tertinggi di antara fitur pending karena langsung berdampak pada operasional kasir sehari-hari. Ketiadaan fitur ini memaksa kasir melakukan workaround yang tidak ter-audit. Flow yang direncanakan:
1. Kasir pilih transaksi yang akan di-refund
2. Sistem minta PIN otorisasi dari Manager/Owner
3. Kasir pilih tipe: FULL atau PARTIAL (pilih item)
4. Input alasan refund (dropdown + opsional text bebas)
5. Konfirmasi dan eksekusi — stok dikembalikan secara otomatis
6. Struk refund dicetak

**F13 — True Split Payment** juga kritis karena menjadi blocker untuk kasus penggunaan yang sangat umum (misalnya bayar sebagian tunai + sisanya QRIS). Flow:
1. Saat checkout, kasir pilih "Split Payment"
2. Input nominal untuk masing-masing metode (minimal 2, maksimal sesuai konfigurasi)
3. Sistem validasi total split = total transaksi
4. Proses masing-masing metode secara sekuensial
5. Transaksi dianggap selesai hanya jika semua split berhasil

---

## 7. User Journey Utama

### 7.1 Journey: Pelanggan Loyal Berbelanja dengan Voucher dan Program Poin

Berikut adalah narasi lengkap alur transaksi yang menggabungkan berbagai fitur Phase 2 secara terintegrasi.

---

**Tahap 1: Pelanggan Tiba di Outlet**

Dewi datang ke kafe langganannya. Kasir (Sari) membuka aplikasi POS di tablet. Sari sudah login dengan akun kasirnya — sistem mengenali outlet tempat ia bertugas dari JWT token yang di-issue saat login.

Karena ini adalah kafe F&B, price book yang aktif adalah ORDER_TYPE. Sari menanyakan Dewi: "Dine-in atau takeaway?" Dewi memilih dine-in. Sistem secara otomatis akan menggunakan daftar harga khusus dine-in.

---

**Tahap 2: Identifikasi Pelanggan (CRM)**

Sari mengetik nomor HP Dewi di field pencarian pelanggan. Sistem menemukan profil Dewi di database CRM: nama, total kunjungan (47 kali), dan saldo poin loyalitas (2.350 poin). Sistem juga menampilkan status stamp: Dewi sudah punya 9 dari 10 stamp untuk program "Beli 10 Gratis 1."

Profil pelanggan terhubung ke transaksi ini. Setiap poin atau stamp yang dihasilkan transaksi ini akan otomatis dikreditkan ke akun Dewi.

---

**Tahap 3: Membangun Keranjang Belanja**

Sari menambahkan item ke keranjang:
- 1x Cold Brew Coffee (Rp 45.000 — harga dine-in)
- 1x Croissant (Rp 25.000 — harga dine-in)
- 2x Mineral Water (Rp 8.000/pc — harga normal, tidak ada override ORDER_TYPE)

Subtotal: Rp 86.000

---

**Tahap 4: Kalkulasi Otomatis — Promosi, Diskon, dan Pajak**

Sistem menjalankan engine kalkulasi multi-layer:

**Layer 1 — Price Book:** Harga yang ditampilkan sudah menggunakan price book ORDER_TYPE (dine-in), tidak ada perubahan lebih lanjut di layer ini.

**Layer 2 — Automatic Promotion:** Sistem memeriksa semua promosi aktif untuk outlet ini. Ditemukan promosi DISCOUNT_BY_ORDER: "Diskon 10% untuk semua order di atas Rp 75.000." Karena subtotal (Rp 86.000) memenuhi syarat, diskon Rp 8.600 otomatis diterapkan. Tidak perlu kode apapun.

Subtotal setelah promosi: Rp 77.400

**Layer 3 — Pajak dan Service Charge:**
- Service Charge 5% dari subtotal: Rp 3.870
- PPN 11% (eksklusif, dihitung dari subtotal setelah SC): (77.400 + 3.870) × 11% = Rp 8.940

Total akhir: Rp 77.400 + Rp 3.870 + Rp 8.940 = **Rp 90.210**

---

**Tahap 5: Dewi Ingin Menggunakan Voucher**

Dewi mengeluarkan voucher digital senilai Rp 50.000 yang didapat sebagai hadiah ulang tahun dari temannya. Sari menginput kode voucher di field pembayaran. Sistem memvalidasi:
- Kode voucher valid dan belum pernah digunakan
- Nominal voucher: Rp 50.000
- Berlaku di outlet ini (scope voucher mencakup semua outlet merchant)

Voucher diterima. Sisa yang harus dibayar: Rp 90.210 − Rp 50.000 = **Rp 40.210**

---

**Tahap 6: Pembayaran Split (True Split Payment)**

Dewi ingin bayar sisanya: Rp 20.000 tunai + Rp 20.210 via QRIS.

Sari memilih "Split Payment":
- Input: Tunai Rp 20.000
- Input: QRIS Rp 20.210

Sistem memvalidasi: Rp 50.000 (voucher) + Rp 20.000 (tunai) + Rp 20.210 (QRIS) = Rp 90.210. Validasi berhasil.

QRIS ditampilkan di layar. Dewi scan dan membayar. Sistem menerima konfirmasi pembayaran dari payment gateway. Transaksi selesai.

---

**Tahap 7: Akumulasi Loyalitas**

Setelah transaksi selesai, sistem secara otomatis:
- Menambahkan 1 stamp ke akun Dewi → sekarang 10/10 stamp → **stamp card penuh, reward "1 free coffee" di-queue untuk kunjungan berikutnya**
- Mengkreditkan poin loyalitas: setiap Rp 1.000 belanja = 1 poin → Rp 77.400 (subtotal sebelum pajak) ÷ 1.000 = 77 poin dikreditkan → saldo poin baru Dewi: 2.427 poin

---

**Tahap 8: Struk Dicetak**

Sistem mencetak struk sesuai template konfigurasi outlet:
- Logo kafe di header
- Nama outlet, alamat, nomor NPWP
- Detail item dengan harga satuan dan subtotal
- Breakdown: Promosi (−Rp 8.600), Voucher (−Rp 50.000), Service Charge (Rp 3.870), PPN 11% (Rp 8.940)
- Total bayar: Rp 90.210
- Rincian pembayaran: Voucher Rp 50.000, Tunai Rp 20.000, QRIS Rp 20.210
- QR code feedback/rating di footer
- Poin loyalitas yang diperoleh: 77 poin (saldo: 2.427 poin)

Dewi pergi dengan puas. Seluruh transaksi ter-audit di sistem.

---

**Tahap 9: Dashboard Pemilik (Background Process)**

Di sisi pemilik kafe (Budi), transaksi Dewi secara real-time masuk ke:
- Dashboard Financial Reporting: total omzet hari ini bertambah Rp 77.400 (net setelah diskon, tidak termasuk pajak)
- Breakdown metode bayar: Voucher +Rp 50.000, Tunai +Rp 20.000, QRIS +Rp 20.210
- Notifikasi (jika dikonfigurasi): "Transaksi baru #TRX-2026-XXXXX — Rp 90.210 di Outlet Jakarta Selatan"
- Revenue sharing: jika dealer menerima 2% dari setiap transaksi, maka engine Split Disbursement mencatat alokasi Rp 1.804 untuk akun dealer

---

## 8. Success Metrics & KPI

### 8.1 Metrics Teknis (Engineering)

| Metrik | Target | Cara Ukur |
|--------|--------|-----------|
| API Response Time (p95) | < 200ms untuk semua endpoint | APM / Actuator metrics |
| API Error Rate | < 0.1% dari total request | Log aggregation |
| Uptime | > 99.9% per bulan | Healthcheck monitoring |
| Test Coverage | > 80% untuk service layer | JaCoCo report |
| Endpoint Completion | 14/14 fitur ter-deploy | Manual tracking |
| Database Query Performance | Tidak ada query > 500ms di production | Slow query log |

### 8.2 Metrics Produk (Product)

| Metrik | Target (6 bulan post-launch) | Cara Ukur |
|--------|------------------------------|-----------|
| Merchant yang Aktif Pakai Promosi | 50% dari total merchant | Query ke DB |
| Merchant yang Aktif Pakai CRM | 40% dari total merchant | Query ke DB |
| Merchant yang Konfigurasi Pajak | 70% dari merchant F&B | Query ke DB |
| Rata-rata Fitur Digunakan per Merchant | ≥ 5 dari 14 fitur | Feature usage tracking |
| Jumlah Pelanggan Ter-register di CRM | 10.000 pelanggan unik | Query ke DB |
| Transaksi dengan Split Payment | 15% dari total transaksi | Query ke DB |

### 8.3 Metrics Bisnis (Business)

| Metrik | Target | Cara Ukur |
|--------|--------|-----------|
| Merchant Retention Rate (12 bulan) | > 85% | Cohort analysis |
| Pengurangan Churn Dibanding Phase 1 | −20% | Cohort comparison |
| ARPU (Average Revenue Per User) | +30% dibanding Phase 1 | Billing data |
| Waktu Onboarding Merchant Baru | < 1 jam untuk konfigurasi dasar | Support ticket data |
| NPS (Net Promoter Score) | ≥ 40 | Survey per kuartal |

### 8.4 Metrics Operasional

| Metrik | Target | Cara Ukur |
|--------|--------|-----------|
| Waktu Rata-rata per Transaksi (kasir) | < 90 detik | Transaction timestamp analysis |
| Jumlah Support Ticket per 100 Merchant | < 5 per bulan | Helpdesk system |
| Waktu Resolusi Bug Critical | < 4 jam | Bug tracking |
| Waktu Integrasi Front-end Baru | < 5 hari kerja | Partner onboarding log |

---

## 9. Batasan & Out of Scope

### 9.1 Batasan Teknis Phase 2

- **Backend only:** Phase 2 adalah pure API/backend service. Tidak ada UI yang dibangun dalam scope ini. Semua fitur diakses oleh front-end melalui REST API yang terdokumentasi.
- **Single region:** Deployment untuk satu region (Indonesia) tanpa multi-region failover. Disaster recovery di-handle di level infrastruktur, bukan aplikasi.
- **PostgreSQL only:** Tidak ada dukungan database lain. Migrasi schema dilakukan via Liquibase/Flyway jika diperlukan, atau manual SQL migration untuk saat ini.
- **Push notification:** Implementasi push notification (F14) di Phase 2 hanya mendukung web push (via FCM/APNS token yang dikirim oleh client). Implementasi native mobile notification adalah tanggung jawab aplikasi mobile client.
- **Email notification:** Hanya transactional email (bukan marketing bulk email). Infrastruktur SMTP atau email service provider (SendGrid/Mailgun) dikonfigurasi di level environment, tidak di-manage dalam kode aplikasi.

### 9.2 Out of Scope (Dijadwalkan Phase 3 atau Proyek Lain)

- **Aplikasi kasir front-end** (web/mobile) — Phase 2 hanya menyediakan API
- **Integrasi marketplace** (Tokopedia, Shopee, GrabFood, GoFood) — Phase 3
- **Modul akuntansi** (jurnal umum, neraca, laporan laba rugi) — Phase 3
- **Manajemen supplier dan purchase order** — Phase 3
- **Manajemen karyawan dan penggajian** — Phase 3
- **Integrasi hardware kasir** (printer, barcode scanner, cash drawer) — tanggung jawab client app
- **Multi-currency** — tidak di-support, semua dalam IDR
- **Pajak internasional** (GST, VAT non-Indonesia) — tidak di-scope
- **GDPR / compliance data internasional** — hanya memenuhi regulasi Indonesia (UU PDP)
- **Fraud detection engine** — di-plan sebagai standalone microservice Phase 4

---

## 10. Asumsi & Dependency

### 10.1 Asumsi Bisnis

1. Setiap merchant telah memiliki akun dan minimal satu outlet yang ter-setup sebelum menggunakan fitur Phase 2.
2. Integrasi payment gateway (untuk memvalidasi pembayaran QRIS, transfer, dsb.) sudah ada dan berfungsi dari Phase 1 atau di-provide oleh sistem eksternal. Phase 2 hanya mengelola record transaksi, bukan mengeksekusi payment gateway secara langsung.
3. Merchant bertanggung jawab atas kebenaran konfigurasi pajak mereka sendiri (tarif, tipe inklusif/eksklusif). Platform tidak memberikan konsultasi pajak.
4. Program loyalitas (poin/stamp) dijalankan sepenuhnya di dalam sistem ini — tidak ada sinkronisasi dengan program loyalitas eksternal di Phase 2.
5. Voucher bersifat single-use per kode kecuali dikonfigurasi otherwise. Nilai voucher tidak dapat di-split menjadi saldo.

### 10.2 Asumsi Teknis

1. Infrastruktur PostgreSQL yang digunakan support JSONB untuk konfigurasi fleksibel (receipt template, notification settings) tanpa perlu skema yang terlalu kaku.
2. JWT yang di-issue saat login selalu membawa klaim `merchantId` yang valid. Tidak ada mekanisme untuk request tanpa konteks tenant.
3. Clock server dan client ter-sinkronisasi dengan toleransi ±30 detik untuk validasi window waktu promo/voucher.
4. Semua monetary value disimpan dalam integer (cent/rupiah tanpa desimal) untuk menghindari floating point error dalam kalkulasi.
5. Concurrency pada update stok dan penggunaan voucher/kode diskon di-handle dengan optimistic locking atau database-level constraints untuk mencegah race condition.

### 10.3 Dependency Eksternal

| Dependency | Tipe | Dampak Jika Tidak Tersedia |
|-----------|------|---------------------------|
| PostgreSQL | Database | Blocker — aplikasi tidak bisa jalan |
| Payment Gateway (eksternal) | API Eksternal | Split payment dan konfirmasi pembayaran digital tidak bisa diproses |
| SMTP / Email Service | Notifikasi | Fitur notifikasi email (F14) tidak bisa berfungsi |
| FCM / APNS Token | Push Notifikasi | Fitur push notification (F14) tidak bisa berfungsi |
| Front-end Client (Web/Mobile) | Konsumer API | Fitur tidak dapat diakses oleh end-user |
| Dokumentasi API (Postman/Swagger) | Developer Experience | Integrasi front-end melambat, bergantung pada komunikasi manual |

---

## 11. Risiko & Mitigasi

### 11.1 Risiko Teknis

**R01 — Kompleksitas Engine Kalkulasi Diskon/Pajak**
- **Deskripsi:** Engine kalkulasi yang menggabungkan price book, kode diskon, promosi otomatis, pajak, dan service charge memiliki banyak permutasi. Urutan kalkulasi yang salah dapat menghasilkan harga akhir yang tidak konsisten atau tidak sesuai ekspektasi merchant.
- **Probabilitas:** Tinggi (sudah terjadi beberapa kali di testing internal)
- **Dampak:** Tinggi — menyebabkan kerugian finansial merchant atau ketidakpercayaan sistem
- **Mitigasi:**
  - Mendefinisikan urutan kalkulasi secara eksplisit dan tidak dapat dikonfigurasi ulang tanpa approval engineering lead: Price Book → Auto Promotion → Discount Code → Service Charge → Tax
  - Unit test exhaustive untuk setiap kombinasi diskon yang mungkin (tersedia di `discount-simulation.md`)
  - Integration test end-to-end untuk user journey lengkap sebelum setiap release

**R02 — Race Condition pada Stok dan Penggunaan Voucher/Kode**
- **Deskripsi:** Dua kasir di outlet berbeda menggunakan kode diskon yang sama secara bersamaan, atau dua transaksi bersamaan mengurangi stok yang sama.
- **Probabilitas:** Sedang
- **Dampak:** Sedang — over-redemption voucher atau overselling stok
- **Mitigasi:**
  - Database-level unique constraint dan row-level locking pada tabel `voucher_code` dan `discount_code_usage`
  - Optimistic locking (version field) pada entitas stok
  - Idempotency key pada endpoint transaksi

**R03 — Data Leak Antar Tenant**
- **Deskripsi:** Bug pada layer filtering `merchantId` menyebabkan data satu merchant terekspos ke merchant lain.
- **Probabilitas:** Rendah (arsitektur sudah dirancang dengan filter di service layer)
- **Dampak:** Sangat Tinggi — pelanggaran privasi, potensi masalah hukum
- **Mitigasi:**
  - Code review mandatory untuk semua perubahan di layer repository dan service
  - Automated test yang memverifikasi isolasi data antar tenant
  - Regular security audit untuk endpoint-endpoint yang mengembalikan list data

### 11.2 Risiko Produk

**R04 — Kompleksitas Konfigurasi Menghalangi Adopsi**
- **Deskripsi:** Fitur seperti price book, tax engine, dan split disbursement memiliki banyak parameter konfigurasi. Merchant awam teknologi mungkin kesulitan dan tidak mengaktifkan fitur ini.
- **Probabilitas:** Tinggi
- **Dampak:** Sedang — KPI adopsi fitur tidak tercapai
- **Mitigasi:**
  - Menyediakan default value yang sensible untuk setiap konfigurasi
  - Template preset (misalnya "Kafe Standard" yang sudah include SC 10% + PPN 11%)
  - Onboarding guide dan tooltip di front-end (tanggung jawab tim front-end)

**R05 — Ketidaksesuaian Aturan Pajak dengan Regulasi**
- **Deskripsi:** Tarif dan aturan PPN/pajak daerah dapat berubah mengikuti regulasi pemerintah.
- **Probabilitas:** Sedang (tarif PPN terakhir berubah dari 10% ke 11% pada 2022)
- **Dampak:** Tinggi — merchant menggunakan tarif lama yang tidak comply
- **Mitigasi:**
  - Tarif pajak bersifat fully configurable per merchant, tidak di-hardcode
  - Notifikasi sistem jika ada perubahan regulasi (manual dari tim operasional)

### 11.3 Risiko Jadwal

**R06 — Fitur Pending (F11–F14) Tidak Selesai Tepat Waktu**
- **Deskripsi:** Empat fitur masih belum diimplementasikan dan memiliki dependensi yang tidak trivial (Refund membutuhkan integrasi dengan stok dan payment; Split Payment membutuhkan koordinasi dengan payment gateway).
- **Probabilitas:** Sedang
- **Dampak:** Tinggi untuk F12 (Refund) dan F13 (Split Payment) yang merupakan blocker operasional
- **Mitigasi:**
  - Prioritaskan F12 dan F13 sebagai sprint pertama setelah dokumen ini disetujui
  - F11 dan F14 dapat di-defer ke early Phase 3 jika kapasitas tidak memadai
  - Buffer 2 minggu di akhir timeline untuk fitur berisiko tinggi

---

## 12. Timeline & Milestone

### 12.1 Status Saat Ini (Per 31 Maret 2026)

```
Phase 2 Overall Progress:
  Fitur Selesai  : 10 / 14  (71%)
  Endpoint Aktif : 108
  Fitur Pending  : 4 (F11, F12, F13, F14)
```

### 12.2 Rencana Penyelesaian Fitur Pending

| Fitur | Sprint | Estimasi Selesai | Owner | Dependensi |
|-------|--------|-----------------|-------|------------|
| F12 — Refund at POS | Sprint 1 (April W1–W2) | 14 April 2026 | Backend Engineer | Stok service, Payment record |
| F13 — True Split Payment | Sprint 1 (April W1–W2) | 14 April 2026 | Backend Engineer | Payment gateway config |
| F11 — POS Language Setting | Sprint 2 (April W3) | 21 April 2026 | Backend Engineer | - |
| F14 — Notification Settings | Sprint 2 (April W3–W4) | 30 April 2026 | Backend Engineer | SMTP config, FCM setup |

### 12.3 Milestone Phase 2

| Milestone | Tanggal Target | Deliverable |
|-----------|---------------|-------------|
| **M1 — Core Commerce Complete** | 31 Maret 2026 | F01–F10 done, 108 endpoint aktif, Postman collection updated | ✅ Done |
| **M2 — Refund & Split Payment** | 14 April 2026 | F12 & F13 done, alur transaksi lengkap dari buka s/d refund | 🔄 In Progress |
| **M3 — Phase 2 Feature Complete** | 30 April 2026 | F11 & F14 done, seluruh 14 fitur ter-deploy | 🔲 Planned |
| **M4 — Stabilization & QA** | 15 Mei 2026 | Semua bug P0/P1 resolved, test coverage ≥ 80%, load test passed | 🔲 Planned |
| **M5 — Phase 2 Production Release** | 31 Mei 2026 | Sistem live di production, onboarding merchant pilot batch | 🔲 Planned |
| **M6 — Phase 3 Kickoff** | 15 Juni 2026 | PRD Phase 3 disetujui, sprint planning Phase 3 dimulai | 🔲 Planned |

### 12.4 Rencana Stabilisasi (Mei 2026)

Setelah semua fitur selesai, dua minggu terakhir bulan Mei dialokasikan untuk:

1. **Regression Testing:** Menjalankan seluruh test suite (unit + integration) dan memastikan tidak ada regresi dari penambahan fitur baru.
2. **Load Testing:** Simulasi 10.000 transaksi/hari untuk mengidentifikasi bottleneck performa, terutama di engine kalkulasi diskon dan endpoint laporan.
3. **Security Review:** Audit manual pada endpoint autentikasi, manajemen kasir PIN, dan data pelanggan CRM.
4. **Documentation Update:** Postman collection, API documentation, dan schema migration guide harus up-to-date sebelum production release.
5. **Merchant Pilot:** Onboarding 3–5 merchant pilot untuk validasi real-world usage sebelum general availability.

### 12.5 Kriteria Go/No-Go Production Release

Sebelum Phase 2 di-release ke production, seluruh kriteria berikut harus terpenuhi:

- [ ] Seluruh 14 fitur telah diimplementasikan dan di-test
- [ ] Tidak ada bug dengan severity P0 yang open
- [ ] Test coverage service layer ≥ 80%
- [ ] Load test lulus (p95 response time < 200ms pada 10.000 req/hari)
- [ ] Security review selesai tanpa temuan critical
- [ ] Postman collection dan dokumentasi API up-to-date
- [ ] Database migration script sudah di-review dan di-test di staging
- [ ] Merchant pilot memberikan feedback positif (NPS ≥ 30 dari pilot group)
- [ ] Rollback plan terdokumentasi dan di-test

---

## Lampiran

### A. Referensi Dokumen Terkait

| Dokumen | Lokasi |
|---------|--------|
| API Documentation | `docs/api-documentation.md` |
| Schema Migration Phase 2 | `docs/schema-migration-phase2.md` |
| Discount Simulation | `docs/discount-simulation.md` |
| Loyalty Program Detail | `docs/loyalty.md` |
| Voucher System Detail | `docs/voucher.md` |
| Product & Transaction Flow | `docs/product-and-transaction.md` |
| Postman Collection | `docs/POS-Service-Revamp.postman_collection.json` |

### B. Glosarium

| Istilah | Definisi |
|---------|----------|
| Multi-tenant | Arsitektur di mana satu instance aplikasi melayani banyak merchant dengan isolasi data penuh |
| Multi-outlet | Kemampuan satu merchant memiliki banyak lokasi/cabang dalam satu akun |
| JWT | JSON Web Token — token autentikasi stateless yang membawa klaim merchantId |
| Price Book | Kumpulan harga alternatif yang dapat diaktifkan untuk menggantikan harga default produk |
| SC | Service Charge — biaya layanan yang ditambahkan ke subtotal |
| PPN | Pajak Pertambahan Nilai |
| CRM | Customer Relationship Management |
| Split Disbursement | Mekanisme pembagian pendapatan otomatis ke beberapa pihak |
| Void | Pembatalan transaksi sebelum settlement |
| Refund | Pengembalian dana setelah transaksi selesai |
| P0/P1/P2 | Tingkat prioritas: P0 = critical/must-have, P1 = high/should-have, P2 = medium/nice-to-have |

---

*Dokumen ini dibuat pada 31 Maret 2026. Setiap perubahan scope atau timeline harus disetujui oleh Product Owner dan Tech Lead sebelum dieksekusi.*
