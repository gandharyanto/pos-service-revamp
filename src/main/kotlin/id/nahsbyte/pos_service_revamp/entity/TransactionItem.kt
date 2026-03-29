package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "transaction_items")
class TransactionItem : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "transaction_id", nullable = false)
    var transactionId: Long = 0

    @Column(name = "product_id")
    var productId: Long? = null

    @Column(name = "product_name")
    var productName: String? = null

    var price: BigDecimal = BigDecimal.ZERO
    var qty: Int = 1

    @Column(name = "total_price")
    var totalPrice: BigDecimal = BigDecimal.ZERO

    @Column(name = "product_snapshot", columnDefinition = "text")
    var productSnapshot: String? = null

    @Column(name = "tax_id")
    var taxId: Long? = null

    @Column(name = "tax_name")
    var taxName: String? = null

    @Column(name = "tax_percentage")
    var taxPercentage: BigDecimal? = null

    @Column(name = "tax_amount")
    var taxAmount: BigDecimal? = null

    // --- Columns added for revamp features ---

    @Column(name = "variant_id")
    var variantId: Long? = null

    @Column(name = "original_price")
    var originalPrice: BigDecimal? = null

    @Column(name = "discount_amount")
    var discountAmount: BigDecimal? = null

    @Column(name = "price_book_item_id")
    var priceBookItemId: Long? = null
}
