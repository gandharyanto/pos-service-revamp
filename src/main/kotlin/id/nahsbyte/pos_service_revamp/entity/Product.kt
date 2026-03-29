package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "product")
class Product : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(name = "merchant_unique_code")
    var merchantUniqueCode: String? = null

    var name: String = ""
    var price: BigDecimal = BigDecimal.ZERO
    var sku: String? = null
    var upc: String? = null

    @Column(name = "image_url", columnDefinition = "text")
    var imageUrl: String? = null

    @Column(name = "image_thumb_url", columnDefinition = "text")
    var imageThumbUrl: String? = null

    var description: String? = null

    @Column(name = "product_hash")
    var productHash: String? = null

    @Column(name = "stock_mode")
    var stockMode: String? = null

    @Column(name = "base_price")
    var basePrice: BigDecimal? = null

    @Column(name = "deleted_by")
    var deletedBy: String? = null

    @Column(name = "deleted_date")
    var deletedDate: LocalDateTime? = null

    @Column(name = "is_taxable")
    var isTaxable: Boolean = false

    @Column(name = "tax_id")
    var taxId: Long? = null

    // --- Columns added for revamp features ---

    /** SIMPLE | VARIANT | MODIFIER */
    @Column(name = "product_type")
    var productType: String? = null

    @Column(name = "display_order")
    var displayOrder: Int? = null

    @Column(name = "is_active")
    var isActive: Boolean? = null

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_categories",
        joinColumns = [JoinColumn(name = "product_id")],
        inverseJoinColumns = [JoinColumn(name = "category_id")]
    )
    var categories: MutableSet<Category> = mutableSetOf()
}
