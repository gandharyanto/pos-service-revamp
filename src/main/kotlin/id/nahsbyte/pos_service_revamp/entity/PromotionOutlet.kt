package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "promotion_outlet")
class PromotionOutlet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "promotion_id", nullable = false)
    var promotionId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
