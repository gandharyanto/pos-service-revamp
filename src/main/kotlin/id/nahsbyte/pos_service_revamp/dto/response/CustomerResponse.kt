package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class CustomerResponse(
    val id: Long,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val gender: String?,
    val loyaltyPoints: BigDecimal,
    val totalTransaction: Int,
    val totalSpend: BigDecimal,
    val isActive: Boolean,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)

data class LoyaltyTransactionResponse(
    val id: Long,
    val customerId: Long,
    val transactionId: Long?,
    val points: BigDecimal,
    val type: String,
    val note: String?,
    val createdDate: LocalDateTime?
)
