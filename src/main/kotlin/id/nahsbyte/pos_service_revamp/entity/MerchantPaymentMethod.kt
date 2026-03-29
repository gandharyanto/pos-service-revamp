package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "merchant_payment_method")
class MerchantPaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "payment_method_id", nullable = false)
    var paymentMethodId: Long = 0

    @Column(name = "is_enabled")
    var isEnabled: Boolean = true

    @Column(name = "display_order")
    var displayOrder: Int = 0

    @Column(name = "config_json", columnDefinition = "json")
    var configJson: String? = null

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
}
