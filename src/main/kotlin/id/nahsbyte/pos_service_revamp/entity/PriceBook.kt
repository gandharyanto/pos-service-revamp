package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

/**
 * Price Book — penyesuaian harga dasar sebelum diskon/promosi dihitung.
 *
 * Tipe price book:
 *   PRODUCT    — override harga per produk (lihat tabel price_book_item)
 *   CATEGORY   — adjust harga semua produk dalam kategori
 *   WHOLESALE  — harga bertingkat berdasarkan qty per produk (lihat tabel price_book_wholesale_tier)
 *   ORDER_TYPE — harga berbeda per tipe pesanan (Dine In, Take Away, dll)
 *
 * Adjustment type (untuk CATEGORY):
 *   PERCENTAGE_OFF — diskon persen dari harga normal
 *   AMOUNT_OFF     — diskon nominal dari harga normal
 *   SPECIAL_PRICE  — harga tetap untuk semua produk di kategori ini
 */
@Entity
@Table(name = "price_book")
class PriceBook : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** PRODUCT | CATEGORY | WHOLESALE | ORDER_TYPE */
    var type: String = "PRODUCT"

    /** Untuk type=ORDER_TYPE: tipe pesanan yang berlaku */
    @Column(name = "order_type_id")
    var orderTypeId: Long? = null

    /** Untuk type=CATEGORY: kategori yang berlaku */
    @Column(name = "category_id")
    var categoryId: Long? = null

    /** Untuk type=CATEGORY: PERCENTAGE_OFF | AMOUNT_OFF | SPECIAL_PRICE */
    @Column(name = "adjustment_type")
    var adjustmentType: String? = null

    /** Untuk type=CATEGORY: nilai penyesuaian harga */
    @Column(name = "adjustment_value")
    var adjustmentValue: BigDecimal? = null

    /** ALL_OUTLET | SPECIFIC_OUTLET */
    var visibility: String = "ALL_OUTLET"

    @Column(name = "is_default")
    var isDefault: Boolean = false

    @Column(name = "is_active")
    var isActive: Boolean = true
}
