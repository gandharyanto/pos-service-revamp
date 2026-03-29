package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseAuditEntity {
    @Column(name = "created_by")
    var createdBy: String? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null

    @Column(name = "modified_by")
    var modifiedBy: String? = null

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
}
