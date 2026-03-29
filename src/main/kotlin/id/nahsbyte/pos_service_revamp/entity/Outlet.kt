package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "outlet")
class Outlet : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var code: String? = null
    var name: String = ""
    var address: String? = null
    var phone: String? = null

    @Column(name = "is_default")
    var isDefault: Boolean = false

    @Column(name = "is_active")
    var isActive: Boolean = true
}
