package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreateDisbursementRuleRequest(
    val name: String,
    /** PLATFORM | DEALER | MERCHANT | CUSTOM */
    val layer: String = "MERCHANT",
    val recipientId: Long? = null,
    val recipientName: String? = null,
    val percentage: BigDecimal,
    /** GROSS | NET | NET_AFTER_TAX | NET_AFTER_TAX_SC */
    val source: String = "NET",
    /** Filter by product type. Null = berlaku semua produk. */
    val productTypeFilter: String? = null,
    val displayOrder: Int? = null
)

data class UpdateDisbursementRuleRequest(
    val ruleId: Long,
    val name: String,
    val layer: String = "MERCHANT",
    val recipientId: Long? = null,
    val recipientName: String? = null,
    val percentage: BigDecimal,
    val source: String = "NET",
    val productTypeFilter: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean = true
)
