package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class DisbursementRuleResponse(
    val id: Long,
    val name: String,
    val layer: String,
    val recipientId: Long?,
    val recipientName: String?,
    val percentage: BigDecimal,
    val source: String,
    val productTypeFilter: String?,
    val isActive: Boolean,
    val displayOrder: Int?
)

data class DisbursementLogResponse(
    val id: Long,
    val transactionId: Long,
    val ruleId: Long,
    val recipientId: Long?,
    val recipientName: String?,
    val layer: String,
    val baseAmount: BigDecimal,
    val percentage: BigDecimal,
    val amount: BigDecimal,
    val status: String,
    val createdDate: LocalDateTime,
    val settledDate: LocalDateTime?
)
