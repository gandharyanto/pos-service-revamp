package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "user_roles")
class UserRole : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "user_id", nullable = false)
    var userId: Long = 0

    @Column(name = "role_id", nullable = false)
    var roleId: Long = 0

    @Column(name = "scope_level")
    var scopeLevel: String? = null

    @Column(name = "scope_id")
    var scopeId: Long? = null

    @Column(name = "application_type")
    var applicationType: String? = null
}
