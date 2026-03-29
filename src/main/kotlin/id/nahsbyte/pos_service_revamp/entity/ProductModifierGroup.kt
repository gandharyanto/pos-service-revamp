package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_modifier_group")
class ProductModifierGroup : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    @Column(name = "min_select")
    var minSelect: Int = 0

    @Column(name = "max_select")
    var maxSelect: Int = 1

    @Column(name = "is_required")
    var isRequired: Boolean = false

    @Column(name = "display_order")
    var displayOrder: Int? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
