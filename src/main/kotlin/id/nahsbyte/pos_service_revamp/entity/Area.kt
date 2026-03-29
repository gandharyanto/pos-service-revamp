package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "area")
class Area : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "company_id")
    var companyId: Long? = null

    var code: String? = null
    var name: String = ""
    var description: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "is_system")
    var isSystem: Boolean = false
}
