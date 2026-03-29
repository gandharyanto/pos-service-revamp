package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "permissions")
class Permission : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(unique = true, nullable = false)
    var code: String = ""

    var name: String = ""
    var description: String? = null

    @Column(name = "menu_key")
    var menuKey: String? = null

    @Column(name = "menu_label")
    var menuLabel: String? = null
}
