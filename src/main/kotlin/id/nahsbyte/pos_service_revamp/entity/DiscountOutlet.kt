package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "discount_outlet")
class DiscountOutlet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
