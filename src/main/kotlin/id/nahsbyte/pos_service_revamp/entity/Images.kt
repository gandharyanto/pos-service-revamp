package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "images")
class Images {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "source_type")
    var sourceType: String? = null

    @Column(name = "source_id")
    var sourceId: Long? = null

    var filename: String = ""
    var ext: String? = null

    @Column(name = "is_main")
    var isMain: Boolean = false

    var status: String? = null

    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    @Column(name = "uploaded_by")
    var uploadedBy: String? = null
}
