package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "role_permissions")
class RolePermission : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "role_id", nullable = false)
    var roleId: Long = 0

    @Column(name = "permission_id", nullable = false)
    var permissionId: Long = 0
}
