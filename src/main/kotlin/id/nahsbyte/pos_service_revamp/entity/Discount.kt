package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Diskon berbasis kode — kasir atau pelanggan input kode saat checkout.
 *
 * Scope (cakupan produk yang didiskon):
 *   ALL      — berlaku untuk seluruh transaksi
 *   PRODUCT  — hanya untuk produk tertentu (lihat tabel discount_product)
 *   CATEGORY — hanya untuk kategori tertentu (lihat tabel discount_category)
 *
 * Channel:
 *   POS    — hanya di kasir
 *   ONLINE — hanya di online store
 *   BOTH   — keduanya
 */
@Entity
@Table(name = "discount")
class Discount : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** Kode yang diinput kasir/pelanggan. Null = predefined (tap langsung tanpa kode) */
    var code: String? = null

    /** PERCENTAGE | AMOUNT */
    @Column(name = "value_type")
    var valueType: String = "PERCENTAGE"

    var value: BigDecimal = BigDecimal.ZERO

    /** Batas maksimum nilai diskon (untuk valueType=PERCENTAGE). Null = tidak ada batas */
    @Column(name = "max_discount_amount")
    var maxDiscountAmount: BigDecimal? = null

    /** Minimum total transaksi agar diskon berlaku */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    /** ALL | PRODUCT | CATEGORY */
    var scope: String = "ALL"

    /** POS | ONLINE | BOTH */
    var channel: String = "BOTH"

    /** ALL_OUTLET | SPECIFIC_OUTLET */
    var visibility: String = "ALL_OUTLET"

    /** Total batas pemakaian kode ini. Null = tidak terbatas */
    @Column(name = "usage_limit")
    var usageLimit: Int? = null

    /** Batas pemakaian per pelanggan. Null = tidak terbatas */
    @Column(name = "usage_per_customer")
    var usagePerCustomer: Int? = null

    @Column(name = "start_date")
    var startDate: LocalDateTime? = null

    @Column(name = "end_date")
    var endDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
