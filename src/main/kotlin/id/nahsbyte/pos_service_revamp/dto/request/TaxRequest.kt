package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreateTaxRequest(
    val name: String,
    val percentage: BigDecimal,
    val isDefault: Boolean = false
)

data class UpdateTaxRequest(
    val taxId: Long,
    val name: String,
    val percentage: BigDecimal,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
