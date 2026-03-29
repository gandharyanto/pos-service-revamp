package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "stock")
class Stock : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false, unique = true)
    var productId: Long = 0

    var qty: Int = 0
}
