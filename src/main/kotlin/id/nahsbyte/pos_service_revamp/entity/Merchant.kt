package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

@Entity
@Table(name = "merchant")
class Merchant : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "area_id")
    var areaId: Long? = null

    @Column(name = "merchant_name")
    var merchantName: String? = null

    var name: String = ""

    @Column(unique = true)
    var code: String? = null

    @Column(name = "merchant_unique_code", unique = true)
    var merchantUniqueCode: String? = null

    @Column(name = "source_directory")
    var sourceDirectory: String? = null

    @Column(name = "is_active")
    var isActive: Boolean = true

    var description: String? = null
    var address: String? = null
    var phone: String? = null
    var email: String? = null

    @Column(name = "merchant_ng_id")
    var merchantNgId: Long? = null

    @Column(name = "merchant_vapn_id")
    var merchantVapnId: Long? = null

    @Column(name = "merchant_pos_id")
    var merchantPosId: Long? = null

    // --- Columns added for revamp features ---

    @Column(name = "language_code")
    var languageCode: String? = null

    /**
     * Strategi pemilihan/pengurutan diskon ketika ada lebih dari satu diskon yang berlaku.
     * HIGHEST_VALUE  — utamakan diskon dengan nilai terbesar
     * LOWEST_VALUE   — utamakan diskon dengan nilai terkecil
     * DISPLAY_ORDER  — utamakan berdasarkan display_order di tabel discount (ascending)
     */
    @Column(name = "discount_priority")
    var discountPriority: String? = null
}
