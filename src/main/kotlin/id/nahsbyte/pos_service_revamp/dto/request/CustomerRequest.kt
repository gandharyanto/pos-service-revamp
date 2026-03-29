package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class CreateCustomerRequest(
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val gender: String? = null
)

data class UpdateCustomerRequest(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val isActive: Boolean = true
)

data class AdjustLoyaltyPointsRequest(
    val customerId: Long,
    val points: BigDecimal,
    /** GET | REDEEM | ADJUSTMENT */
    val type: String = "ADJUSTMENT",
    val note: String? = null
)
