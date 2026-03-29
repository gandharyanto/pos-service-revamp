package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_modifier")
class ProductModifier : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "modifier_group_id", nullable = false)
    var modifierGroupId: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    @Column(name = "additional_price")
    var additionalPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "display_order")
    var displayOrder: Int? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
