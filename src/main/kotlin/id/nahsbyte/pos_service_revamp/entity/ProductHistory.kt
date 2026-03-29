package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_histories")
class ProductHistory : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "change_type")
    var changeType: String? = null

    @Column(columnDefinition = "text")
    var snapshot: String? = null

    // action: enum (CREATE, UPDATE, DELETE)
    var action: String? = null

    @Column(name = "before_snapshot", columnDefinition = "json")
    var beforeSnapshot: String? = null

    @Column(name = "after_snapshot", columnDefinition = "json")
    var afterSnapshot: String? = null
}
