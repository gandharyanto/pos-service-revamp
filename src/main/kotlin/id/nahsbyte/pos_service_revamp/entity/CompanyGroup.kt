package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "company_group")
class CompanyGroup : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    var code: String? = null
    var name: String = ""
    var description: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "is_system")
    var isSystem: Boolean = false
}
