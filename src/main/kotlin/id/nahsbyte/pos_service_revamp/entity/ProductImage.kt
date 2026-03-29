package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "product_images")
class ProductImage : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    var filename: String = ""
    var ext: String? = null

    @Column(name = "is_main")
    var isMain: Boolean = false
}
