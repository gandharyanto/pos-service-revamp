package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
class User : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "full_name")
    var fullName: String? = null

    @Column(unique = true, nullable = false)
    var username: String = ""

    @Column(nullable = false)
    var password: String = ""

    var email: String? = null

    @Column(name = "employee_code")
    var employeeCode: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    @Column(name = "is_system")
    var isSystem: Boolean = false
}
