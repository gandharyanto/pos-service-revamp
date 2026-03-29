package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreatePriceBookRequest(
    val name: String,
    /** PRODUCT | CATEGORY | WHOLESALE | ORDER_TYPE */
    val type: String,
    val orderTypeId: Long? = null,
    val categoryId: Long? = null,
    /** PERCENTAGE_OFF | AMOUNT_OFF | SPECIAL_PRICE (untuk type=CATEGORY) */
    val adjustmentType: String? = null,
    val adjustmentValue: BigDecimal? = null,
    /** ALL_OUTLET | SPECIFIC_OUTLET */
    val visibility: String = "ALL_OUTLET",
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
    val outletIds: List<Long> = emptyList()
)

data class UpdatePriceBookRequest(
    val id: Long,
    val name: String,
    val type: String,
    val orderTypeId: Long? = null,
    val categoryId: Long? = null,
    val adjustmentType: String? = null,
    val adjustmentValue: BigDecimal? = null,
    val visibility: String = "ALL_OUTLET",
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)

data class AddPriceBookItemRequest(
    val productId: Long,
    val price: BigDecimal,
    val isActive: Boolean = true
)

data class AddWholesaleTierRequest(
    val productId: Long,
    val minQty: Int,
    val maxQty: Int? = null,
    val price: BigDecimal,
    val displayOrder: Int? = null
)

data class UpdateWholesaleTierRequest(
    val tierId: Long,
    val minQty: Int,
    val maxQty: Int? = null,
    val price: BigDecimal,
    val displayOrder: Int? = null
)
