package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "order_type")
class OrderType : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""
    var code: String? = null

    @Column(name = "is_default")
    var isDefault: Boolean = false

    @Column(name = "is_active")
    var isActive: Boolean = true
}
