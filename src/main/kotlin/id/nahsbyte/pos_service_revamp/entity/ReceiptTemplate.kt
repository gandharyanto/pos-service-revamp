package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "receipt_template")
class ReceiptTemplate : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    /** Null = berlaku untuk semua outlet */
    @Column(name = "outlet_id")
    var outletId: Long? = null

    @Column(columnDefinition = "text")
    var header: String? = null

    @Column(columnDefinition = "text")
    var footer: String? = null

    @Column(name = "show_tax")
    var showTax: Boolean = true

    @Column(name = "show_service_charge")
    var showServiceCharge: Boolean = true

    @Column(name = "show_rounding")
    var showRounding: Boolean = true

    @Column(name = "show_logo")
    var showLogo: Boolean = false

    @Column(name = "logo_url", columnDefinition = "text")
    var logoUrl: String? = null

    @Column(name = "show_queue_number")
    var showQueueNumber: Boolean = true

    @Column(name = "paper_size")
    var paperSize: String? = null
}
