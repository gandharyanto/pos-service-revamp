package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val sku: String?,
    val upc: String?,
    val imageUrl: String?,
    val imageThumbUrl: String?,
    val description: String?,
    val stockQty: Int,
    val isTaxable: Boolean,
    val taxId: Long?,
    val categories: List<CategoryResponse>,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)
