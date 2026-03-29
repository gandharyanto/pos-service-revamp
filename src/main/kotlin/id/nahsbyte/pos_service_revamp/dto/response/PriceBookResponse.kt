package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class PriceBookResponse(
    val id: Long,
    val name: String,
    val type: String,
    val orderTypeId: Long?,
    val categoryId: Long?,
    val adjustmentType: String?,
    val adjustmentValue: BigDecimal?,
    val visibility: String,
    val isDefault: Boolean,
    val isActive: Boolean,
    val outlets: List<Long>,
    val items: List<PriceBookItemResponse>,
    val wholesaleTiers: List<WholesaleTierResponse>,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)

data class PriceBookItemResponse(
    val id: Long,
    val productId: Long,
    val price: BigDecimal,
    val isActive: Boolean
)

data class WholesaleTierResponse(
    val id: Long,
    val productId: Long,
    val minQty: Int,
    val maxQty: Int?,
    val price: BigDecimal,
    val displayOrder: Int?
)
