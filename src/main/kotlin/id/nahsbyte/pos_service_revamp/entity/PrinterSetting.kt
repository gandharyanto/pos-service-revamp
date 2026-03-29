package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "printer_setting")
class PrinterSetting : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "outlet_id")
    var outletId: Long? = null

    /** RECEIPT | KITCHEN | ORDER */
    var type: String = "RECEIPT"

    var name: String = ""

    /** NETWORK | USB | BLUETOOTH */
    @Column(name = "connection_type")
    var connectionType: String? = null

    @Column(name = "ip_address")
    var ipAddress: String? = null

    var port: Int? = null

    @Column(name = "paper_size")
    var paperSize: String? = null

    @Column(name = "is_default")
    var isDefault: Boolean = false

    @Column(name = "is_active")
    var isActive: Boolean = true
}
