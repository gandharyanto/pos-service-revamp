package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "customer")
class Customer : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""
    var phone: String? = null
    var email: String? = null
    var address: String? = null
    var gender: String? = null

    @Column(name = "loyalty_points")
    var loyaltyPoints: BigDecimal = BigDecimal.ZERO

    @Column(name = "total_transaction")
    var totalTransaction: Int = 0

    @Column(name = "total_spend")
    var totalSpend: BigDecimal = BigDecimal.ZERO

    @Column(name = "is_active")
    var isActive: Boolean = true
}
