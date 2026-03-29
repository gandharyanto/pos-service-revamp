package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "global_parameter")
class GlobalParameter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "param_group")
    var paramGroup: String? = null

    @Column(name = "param_name", nullable = false)
    var paramName: String = ""

    @Column(name = "param_value", columnDefinition = "text")
    var paramValue: String? = null

    var description: String? = null

    @Column(name = "created_date")
    var createdDate: LocalDateTime? = null

    @Column(name = "modified_date")
    var modifiedDate: LocalDateTime? = null
}
