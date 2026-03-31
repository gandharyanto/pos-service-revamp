package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal

data class TaxResponse(
    val id: Long,
    val name: String,
    val percentage: BigDecimal,
    val isActive: Boolean,
    val isDefault: Boolean
)
