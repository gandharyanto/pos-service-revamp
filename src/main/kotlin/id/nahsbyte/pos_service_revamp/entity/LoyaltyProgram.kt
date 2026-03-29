package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "loyalty_program")
class LoyaltyProgram : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** Setiap kelipatan Rp X mendapat 1 poin */
    @Column(name = "points_per_amount")
    var pointsPerAmount: BigDecimal = BigDecimal.ZERO

    /** 1 poin = Rp X saat redeem */
    @Column(name = "redeem_rate")
    var redeemRate: BigDecimal = BigDecimal.ZERO

    @Column(name = "min_redeem_points")
    var minRedeemPoints: BigDecimal? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
