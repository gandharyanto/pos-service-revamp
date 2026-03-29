package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "payment_method")
class PaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(unique = true, nullable = false)
    var code: String = ""

    var name: String = ""
    var category: String? = null

    @Column(name = "payment_type")
    var paymentType: String? = null

    var provider: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
}
