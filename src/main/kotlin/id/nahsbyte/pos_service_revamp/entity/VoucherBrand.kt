package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Brand penerbit voucher.
 * Satu merchant bisa punya beberapa brand (misal: brand sendiri, Ultra Voucher, TADA).
 *
 * Hierarki:
 *   VoucherBrand → VoucherGroup → VoucherCode
 */
@Entity
@Table(name = "voucher_brand")
class VoucherBrand : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    @Column(name = "logo_url", columnDefinition = "text")
    var logoUrl: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
