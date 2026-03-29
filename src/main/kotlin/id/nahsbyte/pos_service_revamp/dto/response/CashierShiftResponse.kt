package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class CashierShiftResponse(
    val id: Long,
    val merchantId: Long,
    val outletId: Long,
    val userId: Long,
    val username: String?,
    val openingCash: BigDecimal,
    val closingCash: BigDecimal?,
    val openDate: LocalDateTime,
    val closeDate: LocalDateTime?,
    val status: String,
    val note: String?,
    val openedBy: String?,
    val closedBy: String?
)
