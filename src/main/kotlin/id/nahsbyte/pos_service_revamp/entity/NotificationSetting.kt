package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "notification_setting")
class NotificationSetting : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "email_address")
    var emailAddress: String? = null

    @Column(name = "is_enabled")
    var isEnabled: Boolean = false

    /** Comma-separated: DAILY_SUMMARY,SETTLEMENT,SHIFT_CLOSE */
    @Column(name = "notify_types")
    var notifyTypes: String? = null

    /** Waktu pengiriman notifikasi harian (HH:mm) */
    @Column(name = "send_time")
    var sendTime: String? = null
}
