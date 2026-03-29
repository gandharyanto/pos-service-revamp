package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "user_detail")
class UserDetail : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id")
    var merchantId: Long? = null

    @Column(name = "merchant_pos_id")
    var merchantPosId: Long? = null

    @Column(name = "merchant_vapn_id")
    var merchantVapnId: Long? = null

    @Column(name = "merchant_ng_id")
    var merchantNgId: Long? = null

    @Column(nullable = false)
    var username: String = ""
}
