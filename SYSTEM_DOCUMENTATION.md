# POS Service Revamp — System Documentation

## Table of Contents
1. [System Overview](#1-system-overview)
2. [Technology Stack](#2-technology-stack)
3. [Architecture](#3-architecture)
4. [Project Structure](#4-project-structure)
5. [Database Schema](#5-database-schema)
6. [Authentication & Security](#6-authentication--security)
7. [API Reference](#7-api-reference)
8. [Configuration](#8-configuration)
9. [Running the Application](#9-running-the-application)

---

## 1. System Overview

POS Service Revamp adalah backend REST API untuk sistem Point-of-Sale (POS) berbasis Spring Boot. Sistem ini mendukung manajemen produk, kategori, stok, transaksi, dan laporan penjualan untuk ekosistem multi-merchant.

**Scope fungsional:**
- Autentikasi dan otorisasi berbasis JWT
- Manajemen produk & kategori (CRUD + soft delete)
- Manajemen stok (ADD / SUBTRACT / SET + riwayat movement)
- Pengaturan pembayaran per merchant (pajak, service charge, rounding)
- Transaksi POS (buat, update status, detail, list)
- Laporan penjualan (produk terlaris + ringkasan metode pembayaran)
- Upload gambar

---

## 2. Technology Stack

| Komponen | Teknologi |
|----------|-----------|
| Language | Kotlin 2.2.21 |
| Runtime | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Build Tool | Gradle 9.4.1 (Kotlin DSL) |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security 7 + JJWT 0.12.6 |
| Serialization | Jackson 3.x (tools.jackson) |
| Validation | Jakarta Bean Validation |

---

## 3. Architecture

### Layered Architecture
```
Request
  │
  ▼
[Controller]          — menerima HTTP request, ekstrak JWT context
  │
  ▼
[Service]             — business logic, transaction management
  │
  ▼
[Repository]          — Spring Data JPA query ke database
  │
  ▼
[PostgreSQL DB]       — 157.10.161.207:5432/pos-db-revamp
```

### Security Flow
```
Request
  │
  ▼
JwtAuthFilter         — baca header "Authorization: Bearer <token>"
  │                   — validasi JWT, set SecurityContext
  ▼
SecurityFilterChain   — endpoint publik: /pos/auth/**, /images/upload, /actuator/**
  │                   — endpoint lain: wajib authenticated
  ▼
Controller            — ekstrak merchantId & username dari JWT via JwtUtil.resolveToken()
```

### Auth Flow (Login)
```
POST /pos/auth/login
  │
  ├─ AuthenticationManager.authenticate(username, password)
  │
  ├─ Lookup user_detail → ambil merchantId
  │
  ├─ Lookup merchant → ambil merchantUniqueCode
  │
  └─ Return:
       token    = JWT(username, merchantId, merchantUniqueCode)
       posToken = JWT(merchantId, type="pos")
       posKey   = merchantUniqueCode
```

### Multi-Tenant Context
Setiap request yang terautentikasi membawa `merchantId` di dalam JWT. Controller mengekstrak `merchantId` dari token untuk memastikan setiap query hanya mengakses data merchant yang bersangkutan — tanpa perlu parameter `merchantId` di request body/param.

---

## 4. Project Structure

```
src/main/kotlin/id/nahsbyte/pos_service_revamp/
│
├── config/
│   └── SecurityConfig.kt              # Spring Security chain, BCrypt, AuthManager
│
├── controller/
│   ├── AuthController.kt              # POST /pos/auth/login
│   ├── ProductController.kt           # /pos/product/**
│   ├── CategoryController.kt          # /pos/category/**
│   ├── StockController.kt             # /pos/stock/**, /pos/stock-movement/**
│   ├── PaymentSettingController.kt    # /pos/payment-setting/**, /pos/payment-method/**
│   ├── TransactionController.kt       # /pos/transaction/**
│   ├── ReportController.kt            # /pos/summary-report/**
│   └── ImageController.kt             # /images/upload
│
├── dto/
│   ├── request/                       # Data kelas untuk HTTP request body
│   └── response/                      # Data kelas untuk HTTP response body
│
├── entity/                            # 33 JPA entity (lihat §5)
│
├── exception/
│   └── GlobalExceptionHandler.kt      # @RestControllerAdvice: ResourceNotFoundException,
│                                      #   BusinessException, BadCredentials, Validation errors
│
├── repository/                        # 18 Spring Data JPA repository interface
│
├── security/
│   ├── JwtUtil.kt                     # Generate & parse JWT token
│   ├── JwtAuthFilter.kt               # OncePerRequestFilter: validasi token per request
│   └── UserDetailsServiceImpl.kt      # Load user dari tabel `users`
│
└── service/
    ├── AuthService.kt
    ├── ProductService.kt
    ├── CategoryService.kt
    ├── StockService.kt
    ├── PaymentSettingService.kt
    ├── TransactionService.kt
    ├── ReportService.kt
    └── ImageService.kt
```

---

## 5. Database Schema

### Hierarki Organisasi
```
company_group
    └── company
            └── area
                    └── merchant
                            └── outlet
```

### Entity Relationship (Ringkasan)

```
users ──────────── user_detail ──── merchant
  │                                     │
  └── user_roles ── roles               ├── outlet
        │               │               │
        └──────── role_permissions      ├── product ──── product_categories ──── category
                    │                   │        │
                  permissions           │        ├── stock
                                        │        ├── stock_movement
                                        │        ├── product_outlet
                                        │        ├── product_images
                                        │        └── product_histories
                                        │
                                        ├── payment_setting
                                        ├── merchant_payment_method ── payment_method
                                        ├── tax
                                        │
                                        └── transaction ── transaction_items
                                                    │
                                                    ├── payment
                                                    └── transaction_queue
```

### Tabel — Ringkasan Kolom Utama

#### users
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| id | bigint | PK |
| username | varchar | unique, login credential |
| password | varchar | BCrypt hashed |
| full_name | varchar | |
| is_active | boolean | |

#### user_detail
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| username | varchar | FK ke users.username |
| merchant_id | bigint | FK ke merchant.id |
| merchant_pos_id | bigint | ID merchant di sistem POS lama |

#### merchant
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| id | bigint | PK |
| merchant_unique_code | varchar | Digunakan sebagai `posKey` |
| name | varchar | |
| is_active | boolean | |

#### product
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| id | bigint | PK |
| merchant_id | bigint | FK ke merchant |
| name | varchar | |
| price | decimal | Harga jual |
| sku / upc | varchar | Kode produk |
| stock_mode | enum | Mode pengelolaan stok |
| is_taxable | boolean | |
| tax_id | bigint | FK ke tax |
| deleted_date | timestamp | Soft delete marker |

#### transaction
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| id | bigint | PK |
| trx_id | varchar | Kode unik (TRX-XXXXXXXX) |
| merchant_id | bigint | FK ke merchant |
| outlet_id | bigint | FK ke outlet |
| status | varchar | PAID / PENDING / CANCELLED |
| payment_method | varchar | |
| sub_total | decimal | |
| total_amount | decimal | Sub total + tax + service charge + rounding |
| total_tax | decimal | |
| total_service_charge | decimal | |
| total_rounding | decimal | |
| cash_tendered / cash_change | decimal | Untuk pembayaran tunai |
| queue_id | bigint | FK ke transaction_queue |

#### payment_setting
| Kolom | Tipe | Keterangan |
|-------|------|-----------|
| merchant_id | bigint | FK ke merchant (1:1) |
| is_price_include_tax | boolean | |
| is_rounding | boolean | |
| rounding_type / rounding_target | varchar/int | Metode pembulatan |
| is_service_charge | boolean | |
| service_charge_percentage | decimal | |
| is_tax | boolean | |
| tax_percentage | decimal | |
| tax_name | varchar | |

---

## 6. Authentication & Security

### JWT Token

Sistem menggunakan **tiga token** yang dikembalikan saat login:

| Token | Isi | Digunakan sebagai |
|-------|-----|-------------------|
| `token` | username, merchantId, merchantUniqueCode | Header `Authorization: Bearer <token>` |
| `posToken` | merchantId, type="pos" | Header `pos-token` |
| `posKey` | = merchantUniqueCode | Header `pos-key` |

Konfigurasi JWT di `application.properties`:
```properties
jwt.secret=<min 32 karakter>
jwt.expiration-ms=86400000   # 24 jam
```

### Header yang Diperlukan (semua endpoint kecuali login & upload)
```
Authorization: Bearer <token>
pos-token: <posToken>
pos-key: <posKey>
version-id: <app version>
device-id: <device serial>
```

> **Catatan:** Validasi `pos-token`, `pos-key`, `version-id`, dan `device-id` saat ini **tidak divalidasi di server** — hanya `Authorization` (JWT) yang diverifikasi. Header lain diteruskan untuk kompatibilitas client.

### Endpoint Publik (tanpa auth)
```
POST /pos/auth/login
POST /images/upload
GET  /actuator/**
```

### Password Hashing
Semua password dienkripsi dengan **BCrypt**. Saat seeding user pertama, gunakan BCrypt encoder:
```kotlin
BCryptPasswordEncoder().encode("password_plain")
```

---

## 7. API Reference

Base URL: `http://<host>:<port>`

### 7.1 Auth

#### POST `/pos/auth/login`
**Request:**
```json
{
  "username": "string",
  "password": "string"
}
```
**Response:**
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "token": "eyJ...",
    "posToken": "eyJ...",
    "posKey": "MERCHANT-001"
  }
}
```

---

### 7.2 Products

#### GET `/pos/product/list`
| Parameter | Tipe | Default | Keterangan |
|-----------|------|---------|-----------|
| page | int | 0 | |
| size | int | 20 | |
| keyword | string | - | Cari nama produk |
| categoryId | long | - | Filter per kategori |
| sku | string | - | |
| upc | string | - | |
| startDate | string | - | Format: `yyyy-MM-dd` |
| endDate | string | - | Format: `yyyy-MM-dd` |
| sortBy | string | createdDate | |
| sortDir | string | DESC | ASC / DESC |

#### GET `/pos/product/detail/{productId}`

#### POST `/pos/product/add`
```json
{
  "name": "string",
  "price": 10000,
  "sku": "string",
  "upc": "string",
  "imageUrl": "string",
  "imageThumbUrl": "string",
  "description": "string",
  "qty": 10,
  "categoryIds": [1, 2]
}
```

#### PUT `/pos/product/update`
```json
{
  "productId": 1,
  "name": "string",
  "price": 10000,
  "sku": "string",
  "upc": "string",
  "imageUrl": "string",
  "imageThumbUrl": "string",
  "description": "string",
  "categoryIds": [1, 2]
}
```

#### DELETE `/pos/product/delete/{productId}`
> Soft delete — mengisi `deleted_date`, data tidak dihapus dari DB.

---

### 7.3 Categories

#### GET `/pos/category/list`
| Parameter | Tipe | Default |
|-----------|------|---------|
| page | int | 0 |
| size | int | 100 |

#### GET `/pos/category/detail/{categoryId}`

#### POST `/pos/category/single/add`
```json
{
  "name": "string",
  "image": "string",
  "description": "string"
}
```

#### PUT `/pos/category/update`
```json
{
  "categoryId": 1,
  "name": "string",
  "image": "string",
  "description": "string"
}
```

#### DELETE `/pos/category/delete/{categoryId}`

---

### 7.4 Stock

#### PUT `/pos/stock/update`
```json
{
  "productId": 1,
  "qty": 10,
  "updateType": "ADD"
}
```
`updateType`: `ADD` | `SUBTRACT` | `SET`

> Setiap perubahan stok otomatis mencatat entri ke tabel `stock_movement`.

#### GET `/pos/stock-movement/product/list`
| Parameter | Tipe | Wajib |
|-----------|------|-------|
| productId | long | ✓ |
| startDate | string (yyyy-MM-dd) | ✓ |
| endDate | string (yyyy-MM-dd) | ✓ |

---

### 7.5 Payment Settings

#### GET `/pos/payment-setting`

#### POST `/pos/payment-setting/create`
```json
{
  "isPriceIncludeTax": false,
  "isRounding": true,
  "roundingTarget": 100,
  "roundingType": "FLOOR",
  "isServiceCharge": true,
  "serviceChargePercentage": 5.00,
  "serviceChargeAmount": null,
  "isTax": true,
  "taxPercentage": 11.00,
  "taxName": "PPN"
}
```
> Hanya bisa dibuat sekali per merchant. Gunakan endpoint update untuk perubahan berikutnya.

#### PUT `/pos/payment-setting/update`
> Body sama seperti create, ditambah `paymentSettingId: long`.

#### GET `/pos/payment-method/merchant/list`
**Response:**
```json
{
  "data": {
    "internalPayments": [
      { "code": "CASH", "name": "Tunai", "category": "INTERNAL", "paymentType": "...", "provider": null }
    ],
    "externalPayments": [
      { "code": "QRIS", "name": "QRIS", "category": "EXTERNAL", "paymentType": "...", "provider": "..." }
    ]
  }
}
```
> Logika pembagian internal/external: method yang mengandung kata CASH, CARD, DEBIT, CREDIT → internal; lainnya → external.

---

### 7.6 Transactions

#### GET `/pos/transaction/list`
| Parameter | Tipe | Wajib | Default |
|-----------|------|-------|---------|
| page | int | - | 0 |
| size | int | - | 20 |
| startDate | string | ✓ | |
| endDate | string | ✓ | |
| sortBy | string | - | createdDate |
| sortType | string | - | DESC |

#### GET `/pos/transaction/detail/{transactionId}`
**Response mencakup:** detail transaksi + daftar item + daftar pembayaran + nomor antrian.

#### POST `/pos/transaction/create`
```json
{
  "subTotal": "50000",
  "totalServiceCharge": "2500",
  "totalTax": "5500",
  "totalRounding": "0",
  "totalAmount": "58000",
  "paymentMethod": "CASH",
  "cashTendered": "60000",
  "cashChange": "2000",
  "priceIncludeTax": false,
  "queueNumber": null,
  "transactionItems": [
    {
      "productId": 1,
      "productName": "Kopi Susu",
      "price": "25000",
      "qty": 2,
      "totalPrice": "50000",
      "taxId": 1,
      "taxAmount": "5500"
    }
  ]
}
```
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

#### PUT `/pos/transaction/update/{merchantTrxId}`
Path variable `merchantTrxId` = nilai `trxId` dari response create (contoh: `TRX-A1B2C3D4`).
```json
{
  "paymentTrxId": "EXT-TRX-001",
  "paymentMethod": "QRIS",
  "amountPaid": 58000,
  "status": "PAID",
  "paymentReference": "REF-12345",
  "paymentDate": "2026-03-29T10:30:00"
}
```

---

### 7.7 Reports

#### GET `/pos/summary-report/list`
| Parameter | Tipe | Wajib |
|-----------|------|-------|
| startDate | string (yyyy-MM-dd) | ✓ |
| endDate | string (yyyy-MM-dd) | ✓ |

**Response:**
```json
{
  "data": {
    "productList": [
      { "productName": "Kopi Susu", "totalSaleItems": 42 }
    ],
    "paymentListInternal": [
      { "paymentMethod": "CASH", "totalAmount": 500000 }
    ],
    "paymentListExternal": [
      { "paymentMethod": "QRIS", "totalAmount": 200000 }
    ]
  }
}
```

---

### 7.8 Images

#### POST `/images/upload`
- Content-Type: `multipart/form-data`
- Field: `file` (jpg, jpeg, png, gif, webp — maks 10MB)

**Response:**
```json
{
  "data": {
    "urlFull": "uploads/<uuid>.jpg",
    "urlThumb": "uploads/<uuid>_thumb.jpg"
  }
}
```

---

### Response Wrapper (Semua Endpoint)
```json
{
  "success": true | false,
  "message": "Success" | "pesan error",
  "data": { ... } | null
}
```

### HTTP Status Codes
| Status | Keterangan |
|--------|-----------|
| 200 | OK |
| 400 | Bad Request / BusinessException / Validation error |
| 401 | Unauthorized (kredensial salah) |
| 404 | Resource tidak ditemukan |
| 500 | Internal Server Error |

---

## 8. Configuration

### application.properties
```properties
# App
spring.application.name=pos-service-revamp

# Database
spring.datasource.url=jdbc:postgresql://157.10.161.207:5432/pos-db-revamp
spring.datasource.username=postgres
spring.datasource.password=<password>
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update        # buat/update tabel otomatis saat startup
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.show-sql=false

# JWT
jwt.secret=<min 32 karakter, ganti di production>
jwt.expiration-ms=86400000                  # 24 jam

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
app.upload.dir=uploads                      # path relatif, ganti ke absolute di production
```

### Catatan Production
- Ganti `jwt.secret` dengan string acak minimal 32 karakter
- Ganti `app.upload.dir` dengan path absolut atau implementasi cloud storage (S3/GCS)
- Pertimbangkan set `spring.jpa.hibernate.ddl-auto=validate` setelah skema stabil

---

## 9. Running the Application

### Prasyarat
- Java 17+
- PostgreSQL running di `157.10.161.207:5432`
- Database `pos-db-revamp` sudah dibuat

### Build & Run
```bash
# Run langsung
./gradlew bootRun

# Build JAR
./gradlew bootJar
java -jar build/libs/pos-service-revamp-0.0.1-SNAPSHOT.jar

# Build tanpa test
./gradlew build -x test

# Run single test class
./gradlew test --tests "id.nahsbyte.pos_service_revamp.SomeTestClass"
```

### Seed Data Minimal
Sebelum bisa login, database harus memiliki minimal:

```sql
-- 1. Merchant
INSERT INTO merchant (id, name, merchant_unique_code, is_active, created_date)
VALUES (1, 'Toko Demo', 'DEMO-001', true, NOW());

-- 2. Outlet default
INSERT INTO outlet (id, merchant_id, name, is_default, is_active, created_date)
VALUES (1, 1, 'Outlet Utama', true, true, NOW());

-- 3. User (password = BCrypt dari "password123")
INSERT INTO users (id, username, password, full_name, is_active, created_date)
VALUES (1, 'admin', '$2a$10$...', 'Admin', true, NOW());

-- 4. User detail
INSERT INTO user_detail (id, username, merchant_id, created_date)
VALUES (1, 'admin', 1, NOW());
```

> Generate BCrypt hash: gunakan [https://bcrypt-generator.com](https://bcrypt-generator.com) atau jalankan `BCryptPasswordEncoder().encode("password123")` di Kotlin REPL.

### Test Login (Postman / curl)
```bash
curl -X POST http://localhost:8080/pos/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}'
```
