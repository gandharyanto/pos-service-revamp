# Dokumen Spesifikasi Teknis (TSD)
# POS Service Revamp — Phase 2

**Versi:** 2.0.0
**Tanggal:** 31 Maret 2026
**Penulis:** Tim Backend Engineering
**Status:** Final

---

## Daftar Isi

1. [Ringkasan Teknis & Stack](#1-ringkasan-teknis--stack)
2. [Arsitektur Sistem](#2-arsitektur-sistem)
3. [Struktur Package & Naming Convention](#3-struktur-package--naming-convention)
4. [Perubahan Database Phase 2](#4-perubahan-database-phase-2)
5. [Desain API](#5-desain-api)
6. [Desain Service Layer](#6-desain-service-layer)
7. [Pola Desain yang Digunakan](#7-pola-desain-yang-digunakan)
8. [Algoritma Kalkulasi Transaksi 5 Layer](#8-algoritma-kalkulasi-transaksi-5-layer)
9. [Keamanan & Autentikasi](#9-keamanan--autentikasi)
10. [Strategi Migrasi Database](#10-strategi-migrasi-database)
11. [Monitoring & Error Handling](#11-monitoring--error-handling)
12. [Panduan Pengembangan Lanjutan](#12-panduan-pengembangan-lanjutan)

---

## 1. Ringkasan Teknis & Stack

### 1.1 Tujuan Phase 2

Phase 2 merupakan ekspansi besar dari POS Service Revamp, menambahkan modul-modul utama yang diperlukan untuk operasional POS penuh: manajemen pajak, template struk, pengaturan printer, aturan disbursement, manajemen kasir, serta laporan keuangan komprehensif. Phase 2 juga memperkenalkan kalkulasi transaksi 5 layer yang mencakup price book, promosi, diskon, voucher, dan loyalty point.

### 1.2 Stack Teknologi

| Komponen | Teknologi | Versi |
|---|---|---|
| Bahasa Pemrograman | Kotlin | 1.9.x |
| Java Toolchain | JDK | 17 |
| Framework | Spring Boot | 4.0.5 |
| Build Tool | Gradle (Kotlin DSL) | 8.x |
| Database | PostgreSQL | 15+ |
| ORM | Spring Data JPA / Hibernate | sesuai Spring Boot 4 |
| DDL Strategy | `ddl-auto=update` | — |
| Security | Spring Security + JWT custom | — |
| Monitoring | Spring Boot Actuator | — |
| Base Package | `id.nahsbyte.pos_service_revamp` | — |

### 1.3 Kotlin Plugin Penting

```kotlin
// build.gradle.kts
plugins {
    kotlin("plugin.allopen")   // Menghilangkan kebutuhan modifier 'open' pada entity JPA
    kotlin("plugin.jpa")       // Menghasilkan no-arg constructor otomatis untuk JPA
}

compileKotlin {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"  // Enforce nullability annotations Java
    }
}
```

- **kotlin-allopen**: Semua class yang dianotasi `@Entity`, `@Component`, `@Service`, `@Repository`, dan `@Controller` otomatis bersifat `open` tanpa perlu menulis keyword `open` secara eksplisit.
- **kotlin-jpa**: Menghasilkan konstruktor tanpa argumen yang dibutuhkan Hibernate untuk instansiasi entity.
- **-Xjsr305=strict**: Kompiler Kotlin memperlakukan anotasi `@Nullable`/`@NotNull` dari library Java secara ketat, sehingga potensi NullPointerException terdeteksi saat kompilasi.

### 1.4 Konfigurasi Utama (`application.properties`)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pos_db
spring.datasource.username=pos_user
spring.datasource.password=<secret>
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

jwt.secret=<base64-encoded-256-bit-secret>
jwt.expiration=86400000

spring.security.user.name=admin
spring.security.user.password=<secret>
```

---

## 2. Arsitektur Sistem

### 2.1 Diagram Komponen (ASCII)

```
+--------------------------------------------------------------------------------------------------+
|                                       POS SERVICE REVAMP                                         |
|                                                                                                  |
|  +-----------+    HTTP/HTTPS    +------------------------------------------------------------+  |
|  |  POS App  | <--------------> |                   SPRING BOOT 4.0.5                        |  |
|  | (Client)  |                  |                                                            |  |
|  +-----------+                  |  +------------------+   +------------------------------+  |  |
|                                 |  | Security Filter  |   |  Spring Boot Actuator        |  |  |
|  +-----------+                  |  | Chain            |   |  /actuator/health             |  |  |
|  |  Backoffice|<--------------> |  | - JwtAuthFilter  |   |  /actuator/metrics            |  |  |
|  | (Client)  |                  |  | - SecurityConfig |   +------------------------------+  |  |
|  +-----------+                  |  +--------+---------+                                     |  |
|                                 |           |                                                |  |
|                                 |  +--------v-------------------------------------------------+  |
|                                 |  |                    CONTROLLER LAYER                      |  |
|                                 |  |                                                          |  |
|                                 |  | AuthController  TaxController    ReceiptController       |  |
|                                 |  | PrinterController  DisbController CashierMgmtController  |  |
|                                 |  | ReportController   TransactionController  ...            |  |
|                                 |  +--------+-----------------------------------------+------+  |
|                                 |           |                                         |          |
|                                 |  +--------v-----------+   +------------------------v------+  |
|                                 |  |   SERVICE LAYER    |   |   Response Wrapper             |  |
|                                 |  |                    |   |   ApiResponse.ok(data)         |  |
|                                 |  | TaxService         |   |   { success, message, data }   |  |
|                                 |  | ReceiptService     |   +--------------------------------+  |
|                                 |  | PrinterService     |                                        |
|                                 |  | DisbursementService|   +--------------------------------+  |
|                                 |  | CashierService     |   |   Exception Handler            |  |
|                                 |  | ReportService      |   |   ResourceNotFoundException    |  |
|                                 |  | TransactionService |   |   BusinessException            |  |
|                                 |  | ...                |   +--------------------------------+  |
|                                 |  +--------+-----------+                                        |
|                                 |           |                                                    |
|                                 |  +--------v-----------+                                        |
|                                 |  |  REPOSITORY LAYER  |                                        |
|                                 |  |  (Spring Data JPA) |                                        |
|                                 |  |                    |                                        |
|                                 |  | TaxRepository      |                                        |
|                                 |  | ReceiptRepository  |                                        |
|                                 |  | PrinterRepository  |                                        |
|                                 |  | DisbursementRepo   |                                        |
|                                 |  | UserDetailRepo     |                                        |
|                                 |  | TransactionRepo    |                                        |
|                                 |  | ...                |                                        |
|                                 |  +--------+-----------+                                        |
|                                 |           |                                                    |
|                                 +------------------------------------------------------------+  |
|                                             |                                                    |
|                                  +----------v-----------+                                        |
|                                  |  PostgreSQL Database |                                        |
|                                  |  (38+ tables)        |                                        |
|                                  +----------------------+                                        |
+--------------------------------------------------------------------------------------------------+
```

### 2.2 Alur Request Tipikal

```
Client Request
    |
    v
[JwtAuthenticationFilter]
    - Ekstrak header Authorization: Bearer <token>
    - Validasi JWT signature & expiry
    - Set SecurityContext dengan UserDetails + merchantId claim
    |
    v
[Controller]
    - Terima @RequestHeader("Authorization") auth
    - Ekstrak merchantId: jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
    - Validasi input via @Valid DTO
    - Delegasi ke Service
    |
    v
[Service]
    - Business logic
    - Query Repository dengan filter merchantId (multi-tenant isolation)
    - Map Entity -> ResponseDto via extension function toResponse()
    - Return data / throw ResourceNotFoundException / BusinessException
    |
    v
[Controller]
    - Wrap dengan ApiResponse.ok(data)
    - Return ResponseEntity
    |
    v
Client Response: { "success": true, "message": "...", "data": {...} }
```

### 2.3 Diagram Kalkulasi Transaksi

```
TransactionRequest
    |
    +---> [Layer 1] PriceBookService.resolveEffectivePrice()
    |         Per item: cek price book aktif -> PRODUCT/ORDER_TYPE/CATEGORY/WHOLESALE
    |
    +---> [Layer 2] PromotionService.evaluateEligiblePromos()
    |         Auto-apply semua promo aktif, sort by priority
    |
    +---> [Layer 3] DiscountService.validateAndApplyCode()
    |         Validasi kode diskon, hitung discountAmount
    |
    +---> grossAmount = Σ(effectivePrice × qty)
    |     netAmount   = grossAmount - promoAmount - discountAmount
    |
    +---> [Layer SC] ServiceChargeCalculation
    |         BEFORE_TAX | AFTER_TAX | DPP | AFTER_DISCOUNT
    |
    +---> [Layer Tax] TaxCalculation
    |         isPriceIncludeTax ? (netAmount / (1+rate)) * rate : netAmount * rate
    |
    +---> [Layer Round] RoundingCalculation
    |         UP | DOWN | STANDARD, rounded to roundingTarget
    |
    |     totalAmount = netAmount + SC + tax + rounding
    |
    +---> [Layer 4] VoucherService.applyVoucher()
    |         voucherAmount mengurangi totalAmount
    |
    +---> [Layer 5] LoyaltyService.redeemPoints()
              loyaltyRedeemAmount mengurangi totalAmount
              amountDue = totalAmount - voucherAmount - loyaltyRedeemAmount
```

---

## 3. Struktur Package & Naming Convention

### 3.1 Struktur Direktori Lengkap

```
src/main/kotlin/id/nahsbyte/pos_service_revamp/
├── PosServiceRevampApplication.kt          # Entry point
│
├── config/
│   ├── SecurityConfig.kt                   # Spring Security & CORS config
│   └── JwtUtil.kt                          # JWT helper (sign, extract, validate)
│
├── entity/                                 # JPA @Entity classes
│   ├── User.kt
│   ├── UserDetail.kt                       # + pin (BCrypt), outletId
│   ├── Merchant.kt                         # + languageCode
│   ├── Outlet.kt                           # + defaultOrderTypeId, languageCode
│   ├── Product.kt                          # + productType, displayOrder, isActive
│   ├── ProductCategory.kt
│   ├── ProductVariantGroup.kt              # NEW Phase 2
│   ├── ProductVariant.kt                   # NEW Phase 2
│   ├── ProductModifierGroup.kt             # NEW Phase 2
│   ├── ProductModifier.kt                  # NEW Phase 2
│   ├── PaymentSetting.kt                   # + outletId, serviceChargeSource
│   ├── Transaction.kt                      # + 17 kolom baru
│   ├── TransactionItem.kt                  # + 4 kolom baru
│   ├── TransactionModifier.kt              # NEW Phase 2
│   ├── Tax.kt                              # NEW Phase 2
│   ├── ReceiptTemplate.kt                  # NEW Phase 2
│   ├── PrinterSetting.kt                   # NEW Phase 2
│   ├── DisbursementRule.kt                 # NEW Phase 2
│   ├── DisbursementLog.kt                  # NEW Phase 2
│   ├── OrderType.kt                        # NEW Phase 2
│   ├── Customer.kt                         # NEW Phase 2
│   ├── CashierShift.kt                     # NEW Phase 2
│   ├── Discount.kt                         # NEW Phase 2
│   ├── DiscountProduct.kt                  # NEW Phase 2
│   ├── DiscountCategory.kt                 # NEW Phase 2
│   ├── DiscountOutlet.kt                   # NEW Phase 2
│   ├── DiscountCustomer.kt                 # NEW Phase 2
│   ├── DiscountPaymentMethod.kt            # NEW Phase 2
│   ├── Promotion.kt                        # NEW Phase 2
│   ├── PromotionProduct.kt                 # NEW Phase 2
│   ├── PromotionRewardProduct.kt           # NEW Phase 2
│   ├── PromotionOutlet.kt                  # NEW Phase 2
│   ├── PromotionCustomer.kt                # NEW Phase 2
│   ├── PriceBook.kt                        # NEW Phase 2
│   ├── PriceBookItem.kt                    # NEW Phase 2
│   ├── PriceBookWholesaleTier.kt           # NEW Phase 2
│   ├── PriceBookOutlet.kt                  # NEW Phase 2
│   ├── VoucherBrand.kt                     # NEW Phase 2
│   ├── VoucherGroup.kt                     # NEW Phase 2
│   ├── Voucher.kt                          # + groupId, status, usedDate, transactionId
│   ├── VoucherUsage.kt                     # NEW Phase 2
│   ├── LoyaltyProgram.kt                   # NEW Phase 2
│   ├── LoyaltyRedemptionRule.kt            # NEW Phase 2
│   ├── LoyaltyTransaction.kt               # NEW Phase 2
│   ├── ProductLoyaltySetting.kt            # NEW Phase 2
│   ├── Refund.kt                           # NEW Phase 2
│   ├── NotificationSetting.kt              # NEW Phase 2
│   └── MerchantConfig.kt                   # NEW Phase 2
│
├── repository/                             # Spring Data JPA Repositories
│   ├── TaxRepository.kt
│   ├── ReceiptRepository.kt
│   ├── PrinterRepository.kt
│   ├── DisbursementRuleRepository.kt
│   ├── DisbursementLogRepository.kt
│   ├── UserDetailRepository.kt             # Extended
│   ├── PaymentSettingRepository.kt         # Extended
│   ├── TransactionRepository.kt            # Extended
│   ├── TransactionItemRepository.kt        # Updated JPQL
│   └── ...
│
├── service/                                # Business logic layer
│   ├── TaxService.kt
│   ├── ReceiptService.kt
│   ├── PrinterService.kt
│   ├── DisbursementService.kt
│   ├── CashierService.kt
│   ├── ReportService.kt                    # Extended
│   └── ...
│
├── controller/                             # REST Controllers
│   ├── TaxController.kt
│   ├── ReceiptController.kt
│   ├── PrinterController.kt
│   ├── DisbursementController.kt
│   ├── CashierManagementController.kt
│   ├── ReportController.kt                 # Extended
│   └── ...
│
├── dto/                                    # Data Transfer Objects (Request + Response)
│   ├── ApiResponse.kt                      # Wrapper { success, message, data }
│   └── ...
│
└── exception/
    ├── ResourceNotFoundException.kt        # 404
    └── BusinessException.kt                # 400
```

### 3.2 Naming Convention

| Artefak | Convention | Contoh |
|---|---|---|
| Entity | PascalCase, singular | `Tax`, `PrinterSetting` |
| Repository | `<Entity>Repository` | `TaxRepository` |
| Service | `<Domain>Service` | `TaxService`, `DisbursementService` |
| Controller | `<Domain>Controller` | `TaxController` |
| Request DTO | `<Action><Domain>Request` | `CreateTaxRequest`, `UpdatePrinterRequest` |
| Response DTO | `<Domain>Response` | `TaxResponse`, `DisbursementRuleResponse` |
| Extension fn | `fun Entity.toResponse()` | `fun Tax.toResponse(): TaxResponse` |
| URL prefix | `/pos/<resource>` | `/pos/tax`, `/pos/printer` |
| Enum | SCREAMING_SNAKE_CASE | `RECEIPT`, `KITCHEN`, `NETWORK` |

### 3.3 Konvensi Audit Fields

Semua entity yang memerlukan audit trail mengikuti pola:

```kotlin
@Column(nullable = false, updatable = false)
val createdDate: LocalDateTime = LocalDateTime.now()

@Column(nullable = false, updatable = false)
val createdBy: String = ""  // diisi dari JWT username

var modifiedDate: LocalDateTime? = null
var modifiedBy: String? = null
```

---

## 4. Perubahan Database Phase 2

### 4.1 Tabel yang Dimodifikasi

#### `transaction` (+17 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `customer_id` | UUID | YES | FK ke tabel customer |
| `order_type_id` | UUID | YES | FK ke tabel order_type |
| `shift_id` | UUID | YES | FK ke tabel cashier_shift |
| `discount_id` | UUID | YES | FK ke tabel discount |
| `discount_amount` | NUMERIC(15,2) | YES | Jumlah diskon kode |
| `promo_id` | UUID | YES | FK ke tabel promotion |
| `promo_amount` | NUMERIC(15,2) | YES | Jumlah diskon promo |
| `voucher_amount` | NUMERIC(15,2) | YES | Jumlah redeem voucher |
| `gross_amount` | NUMERIC(15,2) | YES | Total sebelum diskon |
| `net_amount` | NUMERIC(15,2) | YES | Total setelah diskon |
| `refund_amount` | NUMERIC(15,2) | YES | Jumlah refund |
| `refund_reason` | TEXT | YES | Alasan refund |
| `refund_by` | VARCHAR | YES | Username yang refund |
| `refund_date` | TIMESTAMP | YES | Waktu refund |
| `loyalty_points_earned` | INT | YES | Poin loyalty diperoleh |
| `loyalty_points_redeemed` | INT | YES | Poin loyalty ditukar |
| `loyalty_redeem_amount` | NUMERIC(15,2) | YES | Nilai redeem loyalty |

#### `transaction_items` (+4 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `variant_id` | UUID | YES | FK ke product_variant |
| `original_price` | NUMERIC(15,2) | YES | Harga sebelum price book |
| `discount_amount` | NUMERIC(15,2) | YES | Diskon per item |
| `price_book_item_id` | UUID | YES | Price book item yang dipakai |

#### `payment_setting` (+2 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `outlet_id` | UUID | YES | NULL = default merchant, nilai = override per outlet |
| `service_charge_source` | VARCHAR | YES | Enum: `BEFORE_TAX`, `AFTER_TAX`, `DPP`, `AFTER_DISCOUNT` |

#### `user_detail` (+2 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `pin` | VARCHAR(60) | YES | BCrypt hash PIN 6 digit |
| `outlet_id` | UUID | YES | Outlet assignment kasir |

#### `outlet` (+2 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `default_order_type_id` | UUID | YES | Order type default outlet |
| `language_code` | VARCHAR(10) | YES | Bahasa antarmuka outlet |

#### `merchant` (+1 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `language_code` | VARCHAR(10) | YES | Bahasa antarmuka merchant |

#### `product` (+3 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `product_type` | VARCHAR | YES | Enum: `REGULAR`, `COMPOSITE`, `SERVICE` |
| `display_order` | INT | YES | Urutan tampil di POS |
| `is_active` | BOOLEAN | YES | Soft delete flag |

#### `voucher` (+4 kolom)

| Kolom Baru | Tipe | Nullable | Keterangan |
|---|---|---|---|
| `group_id` | UUID | YES | FK ke voucher_group |
| `status` | VARCHAR | YES | Enum: `AVAILABLE`, `USED`, `EXPIRED` |
| `used_date` | TIMESTAMP | YES | Waktu pemakaian |
| `transaction_id` | UUID | YES | FK ke transaction |

### 4.2 Tabel Baru Phase 2 (38 Tabel)

#### Grup: Order & Customer Management

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `order_type` | id, merchantId, name, description, isActive | Tipe pesanan (Dine-in, Takeaway, Delivery, dll) |
| `customer` | id, merchantId, name, phone, email, address, loyaltyPoints, isActive | Data pelanggan |
| `cashier_shift` | id, merchantId, outletId, cashierId, openTime, closeTime, openCash, closeCash, status | Sesi shift kasir |

#### Grup: Price Book

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `price_book` | id, merchantId, name, type (PRODUCT/ORDER_TYPE/CATEGORY/WHOLESALE), startDate, endDate, isActive | Master price book |
| `price_book_item` | id, priceBookId, productId, price | Harga produk per price book |
| `price_book_wholesale_tier` | id, priceBookItemId, minQty, price | Tier harga grosir |
| `price_book_outlet` | id, priceBookId, outletId | Outlet yang menggunakan price book |

#### Grup: Product Variant & Modifier

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `product_variant_group` | id, productId, name | Grup variant (Size, Color, dll) |
| `product_variant` | id, variantGroupId, name, additionalPrice, sku, stock | Pilihan variant |
| `product_modifier_group` | id, productId, name, minSelect, maxSelect, isRequired | Grup modifier (Topping, Add-on) |
| `product_modifier` | id, modifierGroupId, name, additionalPrice | Pilihan modifier |
| `transaction_modifier` | id, transactionItemId, modifierId, name, price, qty | Modifier yang dipilih di transaksi |

#### Grup: Discount

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `discount` | id, merchantId, name, code, type, valueType, value, minPurchase, maxDiscount, startDate, endDate, isActive | Master diskon |
| `discount_product` | id, discountId, productId | Produk yang ter-cover diskon |
| `discount_category` | id, discountId, categoryId | Kategori yang ter-cover diskon |
| `discount_outlet` | id, discountId, outletId | Outlet yang berlaku |
| `discount_customer` | id, discountId, customerId | Pelanggan khusus |
| `discount_payment_method` | id, discountId, paymentSettingId | Metode pembayaran syarat diskon |

#### Grup: Promotion

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `promotion` | id, merchantId, name, type, triggerType, triggerValue, rewardType, rewardValue, priority, isActive | Master promosi |
| `promotion_product` | id, promotionId, productId | Produk trigger promo |
| `promotion_reward_product` | id, promotionId, productId, qty, price | Produk reward promo |
| `promotion_outlet` | id, promotionId, outletId | Outlet yang berlaku |
| `promotion_customer` | id, promotionId, customerId | Pelanggan target promo |

#### Grup: Voucher

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `voucher_brand` | id, merchantId, name, logoUrl | Brand/penerbit voucher |
| `voucher_group` | id, merchantId, brandId, name, amount, expiryDate, maxUsage | Grup voucher |
| `voucher_usage` | id, voucherId, transactionId, usedDate, usedBy | Riwayat pemakaian voucher |

#### Grup: Loyalty

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `loyalty_program` | id, merchantId, name, pointsPerAmount, minRedeemPoints, pointValueInCurrency, isActive | Program loyalty |
| `loyalty_redemption_rule` | id, loyaltyProgramId, minPoints, maxRedeemPercent | Aturan redeem |
| `loyalty_transaction` | id, merchantId, customerId, transactionId, points, type (EARN/REDEEM), createdDate | Riwayat poin |
| `product_loyalty_setting` | id, productId, multiplier, isExcluded | Pengaturan poin per produk |

#### Grup: Refund & Notification

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `refund` | id, transactionId, merchantId, amount, reason, refundBy, refundDate, status | Detail refund transaksi |
| `notification_setting` | id, merchantId, type, isEnabled, config | Pengaturan notifikasi |

#### Grup: Tax, Printer, Receipt, Disbursement

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `tax` | id, merchantId, name, percentage, isActive, isDefault, createdDate, modifiedDate | Konfigurasi pajak |
| `receipt_template` | id, merchantId, outletId, header, footer, showTax, showServiceCharge, showRounding, showLogo, logoUrl, showQueueNumber, paperSize | Template struk |
| `printer_setting` | id, merchantId, outletId, type, name, connectionType, ipAddress, port, paperSize, isDefault, isActive | Pengaturan printer |
| `disbursement_rule` | id, merchantId, name, layer, recipientId, recipientName, percentage, source, productTypeFilter, displayOrder, isActive | Aturan disbursement |
| `disbursement_log` | id, transactionId, ruleId, merchantId, recipientId, recipientName, layer, baseAmount, percentage, amount, status, createdDate, settledDate | Log disbursement |

#### Grup: Config

| Tabel | Kolom Utama | Keterangan |
|---|---|---|
| `merchant_config` | id, merchantId, configKey, configValue, createdDate | Konfigurasi merchant generik |

---

## 5. Desain API

### 5.1 Ringkasan Endpoint (108 Total)

#### Auth (1 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| POST | `/auth/login` | Login dan dapatkan JWT token |

#### Category (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/category` | List semua kategori |
| GET | `/pos/category/{id}` | Detail kategori |
| POST | `/pos/category` | Buat kategori baru |
| PUT | `/pos/category/{id}` | Update kategori |
| DELETE | `/pos/category/{id}` | Hapus kategori |

#### Product (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/product` | List semua produk |
| GET | `/pos/product/{id}` | Detail produk |
| POST | `/pos/product` | Buat produk baru |
| PUT | `/pos/product/{id}` | Update produk |
| DELETE | `/pos/product/{id}` | Hapus produk |

#### Stock (2 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/stock` | List stok produk |
| PUT | `/pos/stock/{productId}` | Update stok produk |

#### Payment Setting (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/payment-setting` | List semua pengaturan pembayaran |
| GET | `/pos/payment-setting/{id}` | Detail pengaturan pembayaran |
| POST | `/pos/payment-setting` | Buat pengaturan pembayaran |
| PUT | `/pos/payment-setting/{id}` | Update pengaturan pembayaran |
| DELETE | `/pos/payment-setting/{id}` | Hapus pengaturan pembayaran |

#### Tax (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/tax` | List semua pajak |
| GET | `/pos/tax/{id}` | Detail pajak |
| POST | `/pos/tax` | Buat pajak baru |
| PUT | `/pos/tax/{id}` | Update pajak |
| DELETE | `/pos/tax/{id}` | Soft delete pajak |

#### Transaction (4 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/transaction` | List transaksi (dengan filter tanggal) |
| GET | `/pos/transaction/{id}` | Detail transaksi |
| POST | `/pos/transaction` | Buat transaksi baru (5-layer calc) |
| POST | `/pos/transaction/{id}/refund` | Refund transaksi |

#### Reports — Legacy (2 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/summary-report` | Laporan ringkasan (legacy) |
| GET | `/pos/summary-report/top-products` | Top produk terlaris (legacy) |

#### Reports — Extended (4 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/report/financial-summary` | Ringkasan keuangan (revenue, SC, tax, rounding) |
| GET | `/pos/report/payment-methods` | Breakdown metode pembayaran |
| GET | `/pos/report/top-products` | Top produk dengan filter |
| GET | `/pos/report/outlet-breakdown` | Perbandingan performa antar outlet |

#### Images (1 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| POST | `/pos/image/upload` | Upload gambar produk/logo |

#### Customer (7 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/customer` | List semua pelanggan |
| GET | `/pos/customer/{id}` | Detail pelanggan |
| POST | `/pos/customer` | Buat pelanggan baru |
| PUT | `/pos/customer/{id}` | Update pelanggan |
| DELETE | `/pos/customer/{id}` | Soft delete pelanggan |
| GET | `/pos/customer/{id}/loyalty` | Riwayat poin loyalty pelanggan |
| POST | `/pos/customer/{id}/loyalty/adjust` | Adjust manual poin loyalty |

#### Order Type (4 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/order-type` | List tipe pesanan |
| POST | `/pos/order-type` | Buat tipe pesanan |
| PUT | `/pos/order-type/{id}` | Update tipe pesanan |
| DELETE | `/pos/order-type/{id}` | Soft delete tipe pesanan |

#### Cashier Shift (3 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| POST | `/pos/shift/open` | Buka shift kasir |
| POST | `/pos/shift/{id}/close` | Tutup shift kasir |
| GET | `/pos/shift/{id}` | Detail shift kasir |

#### Discount (6 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/discount` | List semua diskon |
| GET | `/pos/discount/{id}` | Detail diskon |
| POST | `/pos/discount` | Buat diskon baru |
| PUT | `/pos/discount/{id}` | Update diskon |
| DELETE | `/pos/discount/{id}` | Soft delete diskon |
| POST | `/pos/discount/validate` | Validasi kode diskon |

#### Promotion (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/promotion` | List semua promosi |
| GET | `/pos/promotion/{id}` | Detail promosi |
| POST | `/pos/promotion` | Buat promosi baru |
| PUT | `/pos/promotion/{id}` | Update promosi |
| DELETE | `/pos/promotion/{id}` | Soft delete promosi |

#### Price Book (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/price-book` | List semua price book |
| GET | `/pos/price-book/{id}` | Detail price book |
| POST | `/pos/price-book` | Buat price book baru |
| PUT | `/pos/price-book/{id}` | Update price book |
| DELETE | `/pos/price-book/{id}` | Soft delete price book |

#### Voucher (12 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/voucher-brand` | List voucher brand |
| POST | `/pos/voucher-brand` | Buat voucher brand |
| PUT | `/pos/voucher-brand/{id}` | Update voucher brand |
| DELETE | `/pos/voucher-brand/{id}` | Hapus voucher brand |
| GET | `/pos/voucher-group` | List voucher group |
| POST | `/pos/voucher-group` | Buat voucher group |
| PUT | `/pos/voucher-group/{id}` | Update voucher group |
| GET | `/pos/voucher` | List voucher |
| POST | `/pos/voucher/generate` | Generate batch voucher |
| POST | `/pos/voucher/validate` | Validasi kode voucher |
| POST | `/pos/voucher/redeem` | Redeem voucher di transaksi |
| GET | `/pos/voucher/{code}` | Detail voucher by kode |

#### Loyalty (7 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/loyalty` | Detail program loyalty |
| POST | `/pos/loyalty` | Buat/update program loyalty |
| GET | `/pos/loyalty/rules` | List aturan redemption |
| POST | `/pos/loyalty/rules` | Tambah aturan redemption |
| DELETE | `/pos/loyalty/rules/{id}` | Hapus aturan redemption |
| GET | `/pos/loyalty/transactions` | Riwayat transaksi poin |
| POST | `/pos/loyalty/calculate` | Hitung estimasi poin |

#### Cashier Management (7 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/cashier` | List semua kasir |
| GET | `/pos/cashier/{id}` | Detail kasir |
| POST | `/pos/cashier` | Buat kasir baru |
| PUT | `/pos/cashier/{id}` | Update kasir |
| DELETE | `/pos/cashier/{id}` | Soft delete kasir |
| POST | `/pos/cashier/{id}/pin` | Set PIN kasir |
| POST | `/pos/cashier/{id}/reset-password` | Reset password kasir |

#### Receipt Template (6 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/receipt-template` | List semua template struk |
| GET | `/pos/receipt-template/{id}` | Detail template struk |
| GET | `/pos/receipt-template/outlet/{outletId}` | Template efektif per outlet (dengan fallback) |
| POST | `/pos/receipt-template` | Buat template struk |
| PUT | `/pos/receipt-template/{id}` | Update template struk |
| DELETE | `/pos/receipt-template/{id}` | Hard delete template struk |

#### Printer (5 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/printer` | List printer (opsional filter outletId) |
| GET | `/pos/printer/{id}` | Detail printer |
| POST | `/pos/printer` | Tambah printer baru |
| PUT | `/pos/printer/{id}` | Update printer |
| DELETE | `/pos/printer/{id}` | Hard delete printer |

#### Disbursement (6 endpoint)

| Method | Path | Deskripsi |
|---|---|---|
| GET | `/pos/disbursement/rule` | List aturan disbursement |
| GET | `/pos/disbursement/rule/{id}` | Detail aturan |
| POST | `/pos/disbursement/rule` | Buat aturan disbursement |
| PUT | `/pos/disbursement/rule/{id}` | Update aturan |
| DELETE | `/pos/disbursement/rule/{id}` | Soft delete aturan |
| GET | `/pos/disbursement/log` | List log disbursement (dengan filter tanggal) |

### 5.2 Standar Format Response

#### Success Response

```json
{
  "success": true,
  "message": "Data berhasil diambil",
  "data": { ... }
}
```

#### Error Response (400 Bad Request — BusinessException)

```json
{
  "success": false,
  "message": "Kode diskon tidak valid atau sudah kadaluarsa",
  "data": null
}
```

#### Error Response (404 Not Found — ResourceNotFoundException)

```json
{
  "success": false,
  "message": "Tax dengan id abc123 tidak ditemukan",
  "data": null
}
```

### 5.3 Header Request

Semua endpoint (kecuali `/auth/login`) memerlukan:

```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

---

## 6. Desain Service Layer

### 6.1 TaxService

**Tanggung Jawab:** Manajemen konfigurasi pajak per merchant.

**Dependensi:** `TaxRepository`

**Method Utama:**

| Method | Deskripsi | Catatan |
|---|---|---|
| `list(merchantId)` | Ambil semua tax aktif | Filter `isActive=true` opsional |
| `detail(merchantId, id)` | Ambil 1 tax | Throw `ResourceNotFoundException` jika tidak ada |
| `create(merchantId, request)` | Buat tax baru | Jika `isDefault=true`, clear semua default lain milik merchant |
| `update(merchantId, id, request)` | Update tax | Validasi kepemilikan dengan merchantId |
| `delete(merchantId, id)` | Soft delete | Set `isActive=false` |

**Pola clearDefault:**

```kotlin
if (request.isDefault == true) {
    taxRepository.findAllByMerchantId(merchantId)
        .filter { it.isDefault == true }
        .forEach { it.isDefault = false; taxRepository.save(it) }
}
```

### 6.2 ReceiptService

**Tanggung Jawab:** Manajemen template struk dengan support per-outlet override.

**Dependensi:** `ReceiptRepository`

**Method Utama:**

| Method | Deskripsi | Catatan |
|---|---|---|
| `list(merchantId)` | Semua template merchant | Termasuk default dan per-outlet |
| `detail(merchantId, id)` | Detail 1 template | Validasi merchantId |
| `getByOutlet(merchantId, outletId)` | Template efektif outlet | Fallback ke default jika outlet tidak ada override |
| `create(merchantId, request)` | Buat template baru | `outletId=null` = template default |
| `update(merchantId, id, request)` | Update template | — |
| `delete(merchantId, id)` | Hard delete | Langsung hapus dari DB |

**Pola Fallback Per-Outlet:**

```kotlin
fun getByOutlet(merchantId: UUID, outletId: UUID): ReceiptTemplate {
    return receiptRepository.findByMerchantIdAndOutletId(merchantId, outletId)
        .or { receiptRepository.findByMerchantIdAndOutletIdIsNull(merchantId) }
        .orElseThrow { ResourceNotFoundException("Receipt template tidak ditemukan") }
}
```

### 6.3 PrinterService

**Tanggung Jawab:** Manajemen pengaturan printer fisik (receipt, kitchen, order display).

**Dependensi:** `PrinterRepository`

**Method Utama:**

| Method | Deskripsi | Catatan |
|---|---|---|
| `list(merchantId, outletId?)` | List printer | Filter opsional per outlet |
| `detail(merchantId, id)` | Detail 1 printer | — |
| `create(merchantId, request)` | Tambah printer | Jika `isDefault=true`, clear default lain dengan `type+outletId` sama |
| `update(merchantId, id, request)` | Update printer | — |
| `delete(merchantId, id)` | Hard delete | — |

**Logika clearDefault per Type+Outlet:**

```kotlin
if (request.isDefault == true) {
    printerRepository.findAllByMerchantIdAndOutletId(merchantId, request.outletId)
        .filter { it.type == request.type && it.isDefault == true }
        .forEach { it.isDefault = false; printerRepository.save(it) }
}
```

### 6.4 DisbursementService

**Tanggung Jawab:** Manajemen aturan pembagian pendapatan (multi-layer disbursement) dan log eksekusinya.

**Dependensi:** `DisbursementRuleRepository`, `DisbursementLogRepository`

**Method Utama:**

| Method | Deskripsi | Catatan |
|---|---|---|
| `listRules(merchantId, activeOnly)` | List aturan disbursement | Filter aktif opsional |
| `detailRule(merchantId, id)` | Detail 1 aturan | — |
| `createRule(merchantId, request)` | Buat aturan baru | Validasi total persentase per layer |
| `updateRule(merchantId, id, request)` | Update aturan | — |
| `deleteRule(merchantId, id)` | Soft delete | Set `isActive=false` |
| `listLogs(merchantId, startDate, endDate)` | List log disbursement | Filter rentang tanggal |
| `getDisbursementSummary(merchantId, startDate, endDate)` | Summary disbursement | Agregasi per recipient |

**Konsep Layer:**
- `layer=1`: Pembagian utama (mis. platform fee, merchant take)
- `layer=2`: Pembagian sub (mis. komisioner)
- Setiap transaksi men-trigger eksekusi semua aturan aktif dan mencatat ke `disbursement_log`

### 6.5 CashierService

**Tanggung Jawab:** Manajemen akun kasir (kombinasi `User` + `UserDetail`).

**Dependensi:** `UserRepository`, `UserDetailRepository`, `PasswordEncoder` (BCrypt)

**Method Utama:**

| Method | Deskripsi | Catatan |
|---|---|---|
| `list(merchantId)` | List kasir aktif merchant | Filter `isActive=true` pada UserDetail |
| `detail(merchantId, id)` | Detail 1 kasir | Return gabungan User+UserDetail |
| `create(merchantId, request)` | Buat kasir baru | `@Transactional`: insert User + insert UserDetail |
| `update(merchantId, id, request)` | Update data kasir | Update UserDetail fields |
| `delete(merchantId, id)` | Soft delete kasir | Set `isActive=false` di UserDetail |
| `setPin(merchantId, id, pin)` | Set/update PIN | BCrypt hash PIN sebelum simpan, response: `{ hasPin: true }` |
| `resetPassword(merchantId, id, newPassword)` | Reset password | BCrypt hash password baru |

**@Transactional pada create:**

```kotlin
@Transactional
fun create(merchantId: UUID, request: CreateCashierRequest): CashierResponse {
    val user = userRepository.save(User(
        username = request.username,
        password = passwordEncoder.encode(request.password),
        role = "CASHIER"
    ))
    val userDetail = userDetailRepository.save(UserDetail(
        userId = user.id,
        merchantId = merchantId,
        outletId = request.outletId,
        isActive = true
    ))
    return buildCashierResponse(user, userDetail)
}
```

### 6.6 ReportService (Extended)

**Tanggung Jawab:** Laporan keuangan dan operasional POS.

**Dependensi:** `TransactionRepository`, `TransactionItemRepository`, `DisbursementLogRepository`, `PaymentSettingRepository`

**Method Utama (Phase 2 — Baru):**

| Method | Deskripsi | Return |
|---|---|---|
| `getFinancialSummary(merchantId, start, end)` | Ringkasan pendapatan, SC, pajak, rounding, diskon | `FinancialSummaryResponse` |
| `getPaymentMethodBreakdown(merchantId, start, end)` | Breakdown jumlah & nilai per metode bayar | `List<PaymentBreakdownResponse>` |
| `getTopProducts(merchantId, start, end, limit)` | Top N produk berdasarkan qty/revenue | `List<TopProductResponse>` |
| `getOutletBreakdown(merchantId, start, end)` | Perbandingan revenue antar outlet | `List<OutletBreakdownResponse>` |
| `getDisbursementSummary(merchantId, start, end)` | Total disbursement per penerima | `DisbursementSummaryResponse` |

**Query JPQL Top Products (diperbarui di Phase 2):**

```kotlin
// TransactionItemRepository
@Query("""
    SELECT ti.productName, SUM(ti.qty), SUM(ti.totalPrice)
    FROM TransactionItem ti
    JOIN ti.transaction t
    WHERE t.merchantId = :merchantId
    AND t.createdDate BETWEEN :start AND :end
    GROUP BY ti.productName
    ORDER BY SUM(ti.qty) DESC
""")
fun findTopProducts(merchantId: UUID, start: LocalDateTime, end: LocalDateTime): List<Array<Any>>
```

---

## 7. Pola Desain yang Digunakan

### 7.1 Per-Outlet Override Pattern

Pola ini digunakan untuk konfigurasi yang bisa diset secara global (merchant-level) namun dapat di-override per outlet.

**Entitas yang menggunakan pola ini:**
- `ReceiptTemplate` (outletId nullable)
- `PaymentSetting` (outletId nullable)
- `PrinterSetting` (outletId wajib, tapi list global via outletId=null query)

**Mekanisme:**

```
outletId = NULL  → Konfigurasi default merchant-level
outletId = <UUID> → Override khusus outlet tersebut

Query pattern:
  1. Coba temukan record dengan (merchantId, outletId=specificId)
  2. Jika tidak ada, fallback ke (merchantId, outletId=NULL)
  3. Jika tidak ada juga, throw ResourceNotFoundException
```

**Implementasi dengan `Optional.or {}`:**

```kotlin
// Contoh di ReceiptService
val template = receiptRepository.findByMerchantIdAndOutletId(merchantId, outletId)
    .or { receiptRepository.findByMerchantIdAndOutletIdIsNull(merchantId) }
    .orElseThrow { ResourceNotFoundException("Template tidak ditemukan") }
```

### 7.2 Soft Delete Pattern

Menghindari penghapusan data permanen untuk menjaga integritas referensial dan audit trail.

**Entitas dengan Soft Delete:**

| Entitas | Flag Field |
|---|---|
| `Tax` | `isActive: Boolean` |
| `DisbursementRule` | `isActive: Boolean` |
| `Discount` | `isActive: Boolean` |
| `Promotion` | `isActive: Boolean` |
| `UserDetail` (kasir) | `isActive: Boolean` |
| `Customer` | `isActive: Boolean` |
| `OrderType` | `isActive: Boolean` |

**Entitas dengan Hard Delete:**

| Entitas | Alasan |
|---|---|
| `PrinterSetting` | Tidak ada foreign key ke entity lain |
| `ReceiptTemplate` | Template independen, tidak direferensi |

**Implementasi:**

```kotlin
// Soft delete
fun delete(merchantId: UUID, id: UUID) {
    val entity = repository.findByMerchantIdAndId(merchantId, id)
        .orElseThrow { ResourceNotFoundException("...") }
    entity.isActive = false
    repository.save(entity)
}

// Hard delete
fun delete(merchantId: UUID, id: UUID) {
    val entity = repository.findByMerchantIdAndId(merchantId, id)
        .orElseThrow { ResourceNotFoundException("...") }
    repository.delete(entity)
}
```

### 7.3 clearDefault Pattern

Memastikan hanya satu record yang berstatus `isDefault=true` dalam scope tertentu.

**Digunakan pada:**
- `TaxService.create/update` — clearDefault scope: `merchantId`
- `PrinterService.create/update` — clearDefault scope: `merchantId + outletId + type`

**Implementasi:**

```kotlin
private fun clearDefault(merchantId: UUID, outletId: UUID, type: PrinterType) {
    printerRepository.findAllByMerchantIdAndOutletId(merchantId, outletId)
        .filter { it.type == type && it.isDefault }
        .forEach {
            it.isDefault = false
            printerRepository.save(it)
        }
}
```

### 7.4 @Transactional Multi-Table Pattern

Digunakan ketika satu operasi bisnis melibatkan penulisan ke beberapa tabel sekaligus dan harus bersifat atomik.

**Kasus penggunaan:**
- `CashierService.create`: Insert `user` + insert `user_detail` (atomic)
- `TransactionService.create`: Insert `transaction` + insert `transaction_items` + insert `transaction_modifier` + update stock + create `disbursement_log` + create `loyalty_transaction`

**Prinsip:**
- Semua operasi dalam satu method `@Transactional` di-commit bersama
- Jika salah satu gagal, seluruh operasi di-rollback
- Hindari memanggil method `@Transactional` dari method `@Transactional` lain dalam kelas yang sama (Spring AOP proxy limitation)

### 7.5 Extension Function toResponse() Pattern

Setiap Service memiliki private extension function untuk mapping Entity ke Response DTO, menjaga separation of concerns.

```kotlin
// Di dalam TaxService.kt
private fun Tax.toResponse(): TaxResponse = TaxResponse(
    id = this.id,
    merchantId = this.merchantId,
    name = this.name,
    percentage = this.percentage,
    isActive = this.isActive,
    isDefault = this.isDefault,
    createdDate = this.createdDate,
    modifiedDate = this.modifiedDate
)
```

**Keuntungan:**
- Response DTO bisa berbeda struktur dari Entity (mis. tidak expose field sensitif)
- Mapping tersentralisasi di Service, Controller tetap bersih
- PIN hash tidak pernah masuk ke response (hanya `hasPin: Boolean`)

### 7.6 ApiResponse Wrapper Pattern

Semua response dari Controller dibungkus dalam `ApiResponse`:

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> ok(data: T, message: String = "Berhasil"): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun error(message: String): ApiResponse<Nothing> =
            ApiResponse(success = false, message = message, data = null)
    }
}
```

---

## 8. Algoritma Kalkulasi Transaksi 5 Layer

### 8.1 Overview

Kalkulasi transaksi berjalan secara berurutan melalui 5 layer. Setiap layer dapat mengurangi atau memodifikasi `amountDue`.

```
INPUT: TransactionRequest {
    items: List<{ productId, variantId?, qty, modifiers? }>,
    orderTypeId?: UUID,
    customerId?: UUID,
    discountCode?: String,
    voucherCode?: String,
    loyaltyPointsToRedeem?: Int,
    paymentMethodId: UUID,
    outletId: UUID
}
```

### 8.2 Layer 1 — Price Book Resolution

**Tujuan:** Menentukan `effectivePrice` per item berdasarkan price book yang aktif.

```
FUNCTION resolveEffectivePrice(item, orderTypeId, outletId, qty):
    
    // Cek apakah outlet terdaftar di price book manapun
    activePriceBooks = getPriceBooksForOutlet(outletId)
        .filter { isActive AND startDate <= today AND endDate >= today }
        .sortedBy { priority }
    
    FOR EACH priceBook IN activePriceBooks:
        
        IF priceBook.type == PRODUCT:
            item = priceBookItemRepo.findByPriceBookIdAndProductId(priceBook.id, item.productId)
            IF item EXISTS:
                RETURN item.price  // Override harga produk spesifik
        
        ELSE IF priceBook.type == ORDER_TYPE AND orderTypeId != null:
            IF priceBook.orderTypeId == orderTypeId:
                item = priceBookItemRepo.findByPriceBookIdAndProductId(priceBook.id, item.productId)
                IF item EXISTS:
                    RETURN item.price  // Harga khusus tipe pesanan
        
        ELSE IF priceBook.type == CATEGORY:
            product = productRepo.findById(item.productId)
            IF product.categoryId MATCHES priceBook.categoryFilter:
                RETURN product.basePrice * priceBook.discountPercentage
        
        ELSE IF priceBook.type == WHOLESALE:
            tiers = priceBookWholesaleTierRepo.findByPriceBookItemId(priceBook.id, item.productId)
            applicableTier = tiers.filter { minQty <= qty }.maxByOrNull { minQty }
            IF applicableTier EXISTS:
                RETURN applicableTier.price  // Harga grosir berdasarkan kuantitas
    
    RETURN product.basePrice  // Fallback: harga normal produk
```

### 8.3 Layer 2 — Promotion Auto-Evaluation

**Tujuan:** Menerapkan semua promosi yang eligible secara otomatis.

```
FUNCTION evaluatePromotions(items, customerId, orderTypeId, outletId):
    
    totalPromoAmount = 0
    appliedPromoId = null
    
    activePromos = promotionRepo.findAllByMerchantIdAndIsActiveTrue(merchantId)
        .filter { isValidDateRange() }
        .filter { outletId IN promo.outlets OR promo.outlets.isEmpty() }
        .filter { customerId IN promo.customers OR promo.customers.isEmpty() }
        .sortedBy { priority ASC }  // Priority lebih rendah = lebih utama
    
    FOR EACH promo IN activePromos:
        
        IF promo.triggerType == MINIMUM_PURCHASE:
            subtotal = Σ(effectivePrice × qty) for all items
            IF subtotal >= promo.triggerValue:
                promoAmount = calculateReward(promo, items, subtotal)
                totalPromoAmount += promoAmount
                appliedPromoId = promo.id
                IF NOT promo.isStackable: BREAK
        
        ELSE IF promo.triggerType == BUY_X_GET_Y:
            triggerProducts = promo.triggerProducts
            triggerQty = items.filter { productId IN triggerProducts }.sumOf { qty }
            IF triggerQty >= promo.triggerValue:
                // Add reward products to order at discounted/free price
                rewardItems = promo.rewardProducts
                promoAmount = Σ(rewardItem.price × rewardItem.qty) for free items
                totalPromoAmount += promoAmount
                appliedPromoId = promo.id
                IF NOT promo.isStackable: BREAK
        
        ELSE IF promo.triggerType == PRODUCT_QUANTITY:
            qualifyingItems = items.filter { productId IN promo.triggerProducts }
            IF qualifyingItems.sumOf { qty } >= promo.triggerValue:
                promoAmount = calculateReward(promo, qualifyingItems, null)
                totalPromoAmount += promoAmount
                appliedPromoId = promo.id
                IF NOT promo.isStackable: BREAK
    
    RETURN { promoAmount: totalPromoAmount, promoId: appliedPromoId }

FUNCTION calculateReward(promo, items, subtotal):
    IF promo.rewardType == PERCENTAGE_DISCOUNT:
        base = subtotal OR Σ(qualifying items)
        RETURN MIN(base * promo.rewardValue / 100, promo.maxDiscount ?? ∞)
    ELSE IF promo.rewardType == FIXED_DISCOUNT:
        RETURN MIN(promo.rewardValue, promo.maxDiscount ?? ∞)
    ELSE IF promo.rewardType == FREE_ITEM:
        RETURN Σ(rewardItem.price × rewardItem.qty)
```

### 8.4 Layer 3 — Discount Code Validation & Application

**Tujuan:** Validasi kode diskon yang diinput pengguna dan hitung nilai diskonnya.

```
FUNCTION validateAndApplyDiscount(discountCode, items, customerId, paymentMethodId, outletId):
    
    IF discountCode IS NULL: RETURN { discountAmount: 0, discountId: null }
    
    discount = discountRepo.findByMerchantIdAndCodeAndIsActiveTrue(merchantId, discountCode)
    IF NOT EXISTS: THROW BusinessException("Kode diskon tidak valid")
    
    // Validasi tanggal berlaku
    IF today < discount.startDate OR today > discount.endDate:
        THROW BusinessException("Kode diskon sudah kadaluarsa")
    
    // Validasi outlet
    IF discount.outlets NOT EMPTY AND outletId NOT IN discount.outlets:
        THROW BusinessException("Diskon tidak berlaku di outlet ini")
    
    // Validasi pelanggan
    IF discount.customers NOT EMPTY AND customerId NOT IN discount.customers:
        THROW BusinessException("Diskon tidak berlaku untuk pelanggan ini")
    
    // Validasi metode pembayaran
    IF discount.paymentMethods NOT EMPTY AND paymentMethodId NOT IN discount.paymentMethods:
        THROW BusinessException("Diskon tidak berlaku untuk metode pembayaran ini")
    
    // Hitung eligible amount
    IF discount.products NOT EMPTY OR discount.categories NOT EMPTY:
        eligibleItems = items.filter {
            productId IN discount.products OR categoryId IN discount.categories
        }
        baseAmount = Σ(effectivePrice × qty) for eligibleItems
    ELSE:
        baseAmount = Σ(effectivePrice × qty) for all items
    
    // Validasi minimum pembelian
    IF baseAmount < discount.minPurchase:
        THROW BusinessException("Minimum pembelian Rp ${discount.minPurchase} untuk menggunakan diskon ini")
    
    // Hitung nilai diskon
    IF discount.valueType == PERCENTAGE:
        discountAmount = MIN(baseAmount * discount.value / 100, discount.maxDiscount ?? ∞)
    ELSE IF discount.valueType == FIXED:
        discountAmount = MIN(discount.value, baseAmount)
    
    RETURN { discountAmount, discountId: discount.id }
```

### 8.5 Kalkulasi Utama (grossAmount & netAmount)

```
grossAmount = Σ(item.effectivePrice × item.qty) for all items
            + Σ(modifier.additionalPrice × modifier.qty) for all modifiers

netAmount = grossAmount - promoAmount - discountAmount
```

### 8.6 Kalkulasi Service Charge

```
FUNCTION calculateServiceCharge(netAmount, grossAmount, paymentSetting):
    
    scRate = paymentSetting.serviceChargePercentage / 100
    
    IF NOT paymentSetting.isServiceChargeEnabled: RETURN 0
    
    SWITCH paymentSetting.serviceChargeSource:
        CASE BEFORE_TAX:
            RETURN grossAmount * scRate  // SC dihitung dari gross sebelum diskon
        
        CASE AFTER_DISCOUNT:
            RETURN netAmount * scRate    // SC dihitung dari net setelah diskon
        
        CASE AFTER_TAX:
            // SC dihitung setelah tax, memerlukan iterasi atau formula langsung
            // Jika tax = T% dan SC = S%:
            // SC = netAmount * S / (1 - S) saat keduanya saling bergantung
            // Implementasi: hitung tax dulu (tanpa SC), lalu SC dari (net + tax)
            taxTemp = calculateTax(netAmount, 0, paymentSetting.taxRate)
            RETURN (netAmount + taxTemp) * scRate
        
        CASE DPP:
            // DPP (Dasar Pengenaan Pajak) — basis yang sama dengan PPN
            dpp = netAmount / (1 + paymentSetting.taxRate / 100)
            RETURN dpp * scRate
```

### 8.7 Kalkulasi Pajak

```
FUNCTION calculateTax(netAmount, serviceCharge, taxPercentage, isPriceIncludeTax):
    
    taxBase = netAmount + serviceCharge
    taxRate = taxPercentage / 100
    
    IF isPriceIncludeTax:
        // Harga sudah termasuk pajak, ekstrak pajak dari harga
        taxAmount = taxBase - (taxBase / (1 + taxRate))
        netBeforeTax = taxBase / (1 + taxRate)
    ELSE:
        // Harga belum termasuk pajak, tambahkan pajak
        taxAmount = taxBase * taxRate
    
    RETURN taxAmount
```

### 8.8 Kalkulasi Rounding

```
FUNCTION calculateRounding(amount, roundingType, roundingTarget):
    
    // roundingTarget: nilai kelipatan target (mis. 100, 500, 1000)
    
    SWITCH roundingType:
        CASE UP:
            remainder = amount % roundingTarget
            IF remainder == 0: RETURN 0
            RETURN roundingTarget - remainder  // Selalu naik ke kelipatan berikutnya
        
        CASE DOWN:
            remainder = amount % roundingTarget
            RETURN -remainder  // Potong ke kelipatan di bawahnya (nilai negatif)
        
        CASE STANDARD:
            remainder = amount % roundingTarget
            IF remainder >= roundingTarget / 2:
                RETURN roundingTarget - remainder  // Naik
            ELSE:
                RETURN -remainder  // Turun

totalAmount = netAmount + serviceCharge + taxAmount + roundingAmount
```

### 8.9 Layer 4 — Voucher Application

```
FUNCTION applyVoucher(voucherCode, totalAmount):
    
    IF voucherCode IS NULL: RETURN { voucherAmount: 0, voucherId: null }
    
    voucher = voucherRepo.findByCode(voucherCode)
    IF NOT EXISTS: THROW BusinessException("Kode voucher tidak valid")
    IF voucher.status != AVAILABLE: THROW BusinessException("Voucher sudah digunakan atau kadaluarsa")
    IF voucher.expiryDate < today: THROW BusinessException("Voucher sudah kadaluarsa")
    
    voucherAmount = MIN(voucher.amount, totalAmount)  // Tidak bisa melebihi total
    
    RETURN { voucherAmount, voucherId: voucher.id }
```

### 8.10 Layer 5 — Loyalty Point Redemption

```
FUNCTION redeemLoyaltyPoints(customerId, pointsToRedeem, totalAmount):
    
    IF pointsToRedeem == 0 OR customerId IS NULL: 
        RETURN { redeemAmount: 0, pointsRedeemed: 0 }
    
    customer = customerRepo.findById(customerId)
    loyaltyProgram = loyaltyRepo.findByMerchantIdAndIsActiveTrue(merchantId)
    
    IF customer.loyaltyPoints < pointsToRedeem:
        THROW BusinessException("Poin tidak mencukupi")
    
    redemptionRule = loyaltyRedemptionRuleRepo.findApplicableRule(loyaltyProgram.id, pointsToRedeem)
    IF redemptionRule EXISTS AND pointsToRedeem > totalAmount * redemptionRule.maxRedeemPercent / 100:
        maxPoints = (totalAmount * redemptionRule.maxRedeemPercent / 100) / loyaltyProgram.pointValueInCurrency
        THROW BusinessException("Maksimum redeem ${maxPoints} poin untuk transaksi ini")
    
    redeemAmount = pointsToRedeem * loyaltyProgram.pointValueInCurrency
    redeemAmount = MIN(redeemAmount, totalAmount)  // Tidak bisa melebihi total
    
    RETURN { redeemAmount, pointsRedeemed: pointsToRedeem }
```

### 8.11 Final Calculation & Commit

```
amountDue = totalAmount - voucherAmount - loyaltyRedeemAmount

@Transactional
FUNCTION commitTransaction:
    
    1. Simpan Transaction dengan semua field kalkulasi
    2. Simpan TransactionItems (with variantId, priceBookItemId, discountAmount per item)
    3. Simpan TransactionModifiers
    4. Update stock per item (stock -= qty) IF product.isStockTracked
    5. Mark Voucher sebagai USED, isi usedDate + transactionId
    6. Deduct customer loyaltyPoints, catat LoyaltyTransaction (REDEEM)
    7. Earn loyalty points: earnedPoints = floor(netAmount / loyaltyProgram.pointsPerAmount)
       Update customer.loyaltyPoints += earnedPoints
       Catat LoyaltyTransaction (EARN)
    8. Buat DisbursementLog entries sesuai aturan aktif
    9. Increment Discount usage counter (jika ada max usage)
    
    RETURN TransactionResponse
```

---

## 9. Keamanan & Autentikasi

### 9.1 JWT Flow

```
CLIENT                          SERVER
  |                                |
  |-- POST /auth/login ----------->|
  |   { username, password }       |
  |                                |-- Validasi credentials di DB
  |                                |-- Generate JWT:
  |                                |   {
  |                                |     sub: "username",
  |                                |     merchantId: "uuid",
  |                                |     roles: ["MERCHANT"],
  |                                |     iat: <timestamp>,
  |                                |     exp: <timestamp + 86400s>
  |                                |   }
  |<-- { token: "eyJ..." } --------|
  |                                |
  |-- GET /pos/tax ---------------->|
  |   Authorization: Bearer eyJ... |
  |                                |-- JwtAuthFilter:
  |                                |   1. resolveToken(auth) -> raw JWT
  |                                |   2. validateToken(token) -> true/false
  |                                |   3. extractUsername(token) -> "username"
  |                                |   4. extractMerchantId(token) -> UUID
  |                                |   5. Set SecurityContextHolder
  |                                |
  |                                |-- Controller:
  |                                |   merchantId = jwtUtil.extractMerchantId(
  |                                |       jwtUtil.resolveToken(auth))
  |                                |
  |<-- 200 { success, data } ------|
```

### 9.2 JWT Token Structure

```
Header: { alg: HS256, typ: JWT }
Payload: {
    sub: "username",
    merchantId: "550e8400-e29b-41d4-a716-446655440000",
    roles: ["MERCHANT", "ADMIN"],
    iat: 1711900000,
    exp: 1711986400
}
Signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
```

### 9.3 BCrypt Usage

```kotlin
// Inisialisasi (Spring Security auto-config atau manual):
val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()

// Saat create/update password atau PIN:
val hashed = passwordEncoder.encode(plainText)  // Work factor default: 10
entity.password = hashed  // Simpan hash, BUKAN plaintext

// Saat verifikasi:
val isMatch = passwordEncoder.matches(inputPlainText, storedHash)

// PIN response — TIDAK PERNAH expose hash:
data class CashierResponse(
    val id: UUID,
    val username: String,
    val hasPin: Boolean,  // true/false berdasarkan pin != null
    // ... field lain
    // TIDAK ADA field 'pin'
)
```

### 9.4 Multi-Tenant Isolation

**Prinsip:** Setiap query ke database WAJIB menyertakan `merchantId` yang diambil dari JWT, bukan dari request body/path.

```kotlin
// BENAR — merchantId dari JWT (tidak bisa dimanipulasi klien)
fun list(auth: String): List<TaxResponse> {
    val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
    return taxRepository.findAllByMerchantId(merchantId).map { it.toResponse() }
}

// SALAH — merchantId dari request parameter (rentan manipulasi)
fun list(merchantId: UUID): List<TaxResponse> {  // JANGAN LAKUKAN INI
    return taxRepository.findAllByMerchantId(merchantId).map { it.toResponse() }
}
```

**Checklist Keamanan:**
- [ ] Setiap repository method menyertakan parameter `merchantId`
- [ ] Controller selalu ekstrak `merchantId` dari JWT, bukan dari path/body
- [ ] Endpoint `/auth/login` adalah satu-satunya yang tidak memerlukan JWT
- [ ] PIN dan password selalu di-hash BCrypt sebelum disimpan
- [ ] Response DTO tidak pernah menyertakan password hash atau PIN hash
- [ ] Token expiry dikonfigurasi (default 86400s = 24 jam)
- [ ] JWT secret menggunakan nilai yang cukup panjang dan random (minimal 256-bit)

### 9.5 Role-Based Access Control

```kotlin
// SecurityConfig.kt
http.authorizeHttpRequests { auth ->
    auth
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/pos/**").hasAnyRole("MERCHANT", "CASHIER", "ADMIN")
        .anyRequest().authenticated()
}
```

---

## 10. Strategi Migrasi Database

### 10.1 Pendekatan: `ddl-auto=update`

Hibernate `ddl-auto=update` berarti:
- Saat aplikasi start, Hibernate membandingkan skema entity dengan skema DB aktual
- Hibernate **hanya menambahkan** kolom dan tabel baru, **tidak pernah menghapus** kolom/tabel yang ada
- Operasi yang aman: `CREATE TABLE`, `ALTER TABLE ADD COLUMN`
- Operasi yang **tidak** dilakukan: `DROP TABLE`, `DROP COLUMN`, `ALTER COLUMN TYPE`

### 10.2 Mengapa `ddl-auto=update` Aman di Phase 2

| Kondisi | Status | Alasan |
|---|---|---|
| Semua kolom baru nullable | AMAN | Tidak akan break existing rows |
| Tidak ada kolom yang dihapus | AMAN | Update tidak menghapus kolom |
| Tidak ada perubahan tipe kolom | AMAN | Update tidak mengubah tipe kolom existing |
| Tidak ada rename kolom | AMAN | Tidak ada operasi DROP+ADD tersembunyi |
| Tidak ada constraint NOT NULL baru | AMAN | Existing rows tidak ter-violasi |

### 10.3 Potensi Risiko & Mitigasi

| Risiko | Mitigasi |
|---|---|
| Perubahan tipe kolom tidak ter-apply | Gunakan migration script manual (Flyway/Liquibase) jika perlu ubah tipe |
| DROP/RENAME tidak otomatis | Jalankan SQL manual di luar deploy jika perlu |
| Race condition saat multi-instance deploy | Pastikan hanya satu instance yang start bersamaan, atau gunakan Flyway |
| Schema drift antara dev/staging/prod | Maintain `schema.txt` sebagai source of truth |

### 10.4 Rollback Plan

Karena `ddl-auto=update` tidak bisa otomatis rollback:

```sql
-- Rollback manual Phase 2: hapus tabel baru jika perlu rollback
-- PERHATIAN: Eksekusi ini akan MENGHAPUS DATA!

DROP TABLE IF EXISTS tax CASCADE;
DROP TABLE IF EXISTS receipt_template CASCADE;
DROP TABLE IF EXISTS printer_setting CASCADE;
DROP TABLE IF EXISTS disbursement_rule CASCADE;
DROP TABLE IF EXISTS disbursement_log CASCADE;
-- ... (tabel baru lainnya)

-- Hapus kolom baru dari tabel yang dimodifikasi
ALTER TABLE transaction
    DROP COLUMN IF EXISTS customer_id,
    DROP COLUMN IF EXISTS order_type_id,
    -- ... kolom baru lainnya

ALTER TABLE user_detail
    DROP COLUMN IF EXISTS pin,
    DROP COLUMN IF EXISTS outlet_id;
```

**Prosedur Rollback yang Aman:**
1. Ambil snapshot database sebelum deploy Phase 2
2. Jika rollback diperlukan, restore snapshot
3. Deploy versi Phase 1 kembali
4. Verifikasi aplikasi berjalan normal

### 10.5 Rekomendasi untuk Production

Untuk lingkungan production yang lebih ketat, pertimbangkan migrasi ke **Flyway**:

```kotlin
// build.gradle.kts
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-database-postgresql")
```

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate  # Ganti update dengan validate
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

Dengan Flyway, setiap perubahan skema didefinisikan dalam file SQL bernomor versi:
```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__phase1_additions.sql
└── V3__phase2_additions.sql
```

---

## 11. Monitoring & Error Handling

### 11.1 Spring Boot Actuator

Endpoint monitoring yang tersedia:

| Endpoint | URL | Fungsi |
|---|---|---|
| Health check | `GET /actuator/health` | Status aplikasi (UP/DOWN) |
| Metrics | `GET /actuator/metrics` | JVM, HTTP, custom metrics |
| Info | `GET /actuator/info` | Info aplikasi (versi, build) |
| Beans | `GET /actuator/beans` | Daftar Spring beans (dev only) |
| Env | `GET /actuator/env` | Environment properties (dev only) |

```properties
# Expose hanya health dan metrics di production
management.endpoints.web.exposure.include=health,metrics,info
management.endpoint.health.show-details=when-authorized
```

### 11.2 Exception Hierarchy

```
RuntimeException
├── ResourceNotFoundException (HTTP 404)
│   Contoh: "Tax dengan id {id} tidak ditemukan untuk merchant {merchantId}"
│
└── BusinessException (HTTP 400)
    Contoh: "Kode diskon tidak valid"
            "Minimum pembelian tidak terpenuhi"
            "Stok tidak mencukupi"
            "Voucher sudah digunakan"
```

### 11.3 Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.message ?: "Resource tidak ditemukan"))

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.message ?: "Request tidak valid"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errors))
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ResponseEntity<ApiResponse<Nothing>> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Terjadi kesalahan internal server"))
}
```

### 11.4 Logging Strategy

```kotlin
// Gunakan SLF4J logger via Kotlin extension
private val log = LoggerFactory.getLogger(javaClass)

// Di Service layer:
log.info("Creating tax for merchant: $merchantId")
log.warn("Default tax cleared for merchant: $merchantId")
log.error("Failed to process transaction: ${ex.message}", ex)
```

**Level logging yang disarankan:**
- `INFO`: Operasi bisnis berhasil (create, update, delete)
- `WARN`: Kondisi tidak normal tapi masih bisa dilanjutkan
- `ERROR`: Exception yang tidak terduga, harus diinvestigasi
- `DEBUG`: Detail teknis (hanya untuk development)

---

## 12. Panduan Pengembangan Lanjutan

### 12.1 Template: Menambahkan Modul Baru

Untuk menambahkan fitur/modul baru ke sistem ini, ikuti pola berikut:

#### Step 1: Buat Entity

```kotlin
// src/main/kotlin/.../entity/FeatureBaru.kt
@Entity
@Table(name = "feature_baru")
class FeatureBaru(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val merchantId: UUID,

    @Column(nullable = false)
    var name: String,

    // Soft delete flag (jika diperlukan)
    var isActive: Boolean = true,

    // Audit fields
    @Column(nullable = false, updatable = false)
    val createdDate: LocalDateTime = LocalDateTime.now(),
    val createdBy: String = "",
    var modifiedDate: LocalDateTime? = null,
    var modifiedBy: String? = null
)
```

#### Step 2: Buat Repository

```kotlin
// src/main/kotlin/.../repository/FeatureBaruRepository.kt
@Repository
interface FeatureBaruRepository : JpaRepository<FeatureBaru, UUID> {
    fun findAllByMerchantId(merchantId: UUID): List<FeatureBaru>
    fun findByMerchantIdAndId(merchantId: UUID, id: UUID): Optional<FeatureBaru>
    // Tambahkan query method sesuai kebutuhan
}
```

#### Step 3: Buat DTO

```kotlin
// Request DTO
data class CreateFeatureBaruRequest(
    @field:NotBlank val name: String,
    // ... field lainnya
)

// Response DTO
data class FeatureBaruResponse(
    val id: UUID,
    val merchantId: UUID,
    val name: String,
    val isActive: Boolean,
    val createdDate: LocalDateTime
)
```

#### Step 4: Buat Service

```kotlin
// src/main/kotlin/.../service/FeatureBaruService.kt
@Service
class FeatureBaruService(
    private val featureBaruRepository: FeatureBaruRepository
) {
    fun list(merchantId: UUID): List<FeatureBaruResponse> =
        featureBaruRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: UUID, id: UUID): FeatureBaruResponse =
        featureBaruRepository.findByMerchantIdAndId(merchantId, id)
            .orElseThrow { ResourceNotFoundException("FeatureBaru dengan id $id tidak ditemukan") }
            .toResponse()

    fun create(merchantId: UUID, request: CreateFeatureBaruRequest): FeatureBaruResponse {
        val entity = FeatureBaru(merchantId = merchantId, name = request.name)
        return featureBaruRepository.save(entity).toResponse()
    }

    fun update(merchantId: UUID, id: UUID, request: CreateFeatureBaruRequest): FeatureBaruResponse {
        val entity = featureBaruRepository.findByMerchantIdAndId(merchantId, id)
            .orElseThrow { ResourceNotFoundException("...") }
        entity.name = request.name
        entity.modifiedDate = LocalDateTime.now()
        return featureBaruRepository.save(entity).toResponse()
    }

    fun delete(merchantId: UUID, id: UUID) {
        val entity = featureBaruRepository.findByMerchantIdAndId(merchantId, id)
            .orElseThrow { ResourceNotFoundException("...") }
        entity.isActive = false  // Soft delete
        featureBaruRepository.save(entity)
    }

    // Private extension function — mapping
    private fun FeatureBaru.toResponse(): FeatureBaruResponse = FeatureBaruResponse(
        id = this.id,
        merchantId = this.merchantId,
        name = this.name,
        isActive = this.isActive,
        createdDate = this.createdDate
    )
}
```

#### Step 5: Buat Controller

```kotlin
// src/main/kotlin/.../controller/FeatureBaruController.kt
@RestController
@RequestMapping("/pos/feature-baru")
class FeatureBaruController(
    private val featureBaruService: FeatureBaruService,
    private val jwtUtil: JwtUtil
) {
    @GetMapping
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<List<FeatureBaruResponse>>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(featureBaruService.list(merchantId)))
    }

    @GetMapping("/{id}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<FeatureBaruResponse>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(featureBaruService.detail(merchantId, id)))
    }

    @PostMapping
    fun create(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: CreateFeatureBaruRequest
    ): ResponseEntity<ApiResponse<FeatureBaruResponse>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(featureBaruService.create(merchantId, request), "Berhasil dibuat"))
    }

    @PutMapping("/{id}")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateFeatureBaruRequest
    ): ResponseEntity<ApiResponse<FeatureBaruResponse>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(featureBaruService.update(merchantId, id, request)))
    }

    @DeleteMapping("/{id}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: UUID
    ): ResponseEntity<ApiResponse<Nothing>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        featureBaruService.delete(merchantId, id)
        return ResponseEntity.ok(ApiResponse.ok(null, "Berhasil dihapus"))
    }
}
```

### 12.2 Checklist Review sebelum Merge

```
[ ] Semua query repository menyertakan merchantId filter
[ ] Controller mengambil merchantId dari JWT, bukan dari request body/path
[ ] Field sensitif (password, pin) tidak masuk ke Response DTO
[ ] Operasi multi-tabel menggunakan @Transactional
[ ] Entity memiliki audit fields (createdDate, modifiedDate, createdBy, modifiedBy)
[ ] Soft delete menggunakan isActive=false, bukan DELETE SQL
[ ] Hard delete hanya untuk entity yang tidak direferensikan tabel lain
[ ] Response dibungkus ApiResponse.ok()
[ ] Exception menggunakan ResourceNotFoundException (404) atau BusinessException (400)
[ ] Tidak ada logika bisnis di Controller (hanya delegasi ke Service)
[ ] Extension function toResponse() adalah private di dalam Service class
[ ] URL prefix mengikuti konvensi /pos/<resource>
[ ] Nama entity singular, repository <Entity>Repository, service <Domain>Service
```

### 12.3 Panduan Testing

```bash
# Jalankan semua test
./gradlew test

# Jalankan test spesifik
./gradlew test --tests "id.nahsbyte.pos_service_revamp.service.TaxServiceTest"

# Build lengkap dengan test
./gradlew clean build

# Skip test (tidak disarankan untuk CI/CD)
./gradlew build -x test
```

**Struktur Test yang Disarankan:**

```kotlin
// Unit test untuk Service
@ExtendWith(MockitoExtension::class)
class TaxServiceTest {

    @Mock lateinit var taxRepository: TaxRepository
    @InjectMocks lateinit var taxService: TaxService

    @Test
    fun `create tax should clear existing defaults when isDefault is true`() {
        // Given
        val merchantId = UUID.randomUUID()
        val existingDefault = Tax(merchantId = merchantId, name = "PPN", isDefault = true)
        whenever(taxRepository.findAllByMerchantId(merchantId)).thenReturn(listOf(existingDefault))
        
        // When
        taxService.create(merchantId, CreateTaxRequest(name = "PPN Baru", percentage = 11.0, isDefault = true))
        
        // Then
        verify(taxRepository).save(argThat { !isDefault })  // Default lama harus di-clear
    }
}
```

### 12.4 Konvensi Commit & Branch

```
Feature branch: feature/<module>-<description>
  Contoh: feature/tax-management
          feature/receipt-template
          feature/disbursement-rules

Commit message format:
  feat: add tax management endpoints
  fix: correct fallback logic in receipt template service
  refactor: extract toResponse() as private extension function
  test: add unit tests for CashierService.setPin
  docs: update TSD with disbursement calculation details
```

---

*Dokumen ini merupakan referensi teknis resmi untuk POS Service Revamp Phase 2. Setiap perubahan arsitektur atau pola desain yang signifikan harus diperbarui dalam dokumen ini sebelum implementasi.*

*Terakhir diperbarui: 31 Maret 2026*
