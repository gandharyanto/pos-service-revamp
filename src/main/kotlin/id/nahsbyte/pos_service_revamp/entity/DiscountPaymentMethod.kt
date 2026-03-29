package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "discount_payment_method")
class DiscountPaymentMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "payment_method_code", nullable = false)
    var paymentMethodCode: String = ""

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
