package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "disbursement_rule")
class DisbursementRule : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /**
     * Layer penerima:
     * PLATFORM | DEALER | MERCHANT | CUSTOM
     */
    var layer: String = "MERCHANT"

    /** ID entitas penerima (userId, dealerId, dll) */
    @Column(name = "recipient_id")
    var recipientId: Long? = null

    @Column(name = "recipient_name")
    var recipientName: String? = null

    /** Persentase dari base amount */
    var percentage: BigDecimal = BigDecimal.ZERO

    /**
     * Base kalkulasi:
     * GROSS | NET | NET_AFTER_TAX | NET_AFTER_TAX_SC
     */
    var source: String = "NET"

    @Column(name = "product_type_filter")
    var productTypeFilter: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "display_order")
    var displayOrder: Int? = null
}
