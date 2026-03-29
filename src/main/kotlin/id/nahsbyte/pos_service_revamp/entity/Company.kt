package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "company")
class Company : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "group_id")
    var groupId: Long? = null

    var code: String? = null
    var name: String = ""

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "is_system")
    var isSystem: Boolean = false
}
