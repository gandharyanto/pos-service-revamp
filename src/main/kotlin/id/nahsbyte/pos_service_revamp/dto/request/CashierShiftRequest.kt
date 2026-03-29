package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class OpenShiftRequest(
    val outletId: Long,
    val openingCash: BigDecimal = BigDecimal.ZERO,
    val note: String? = null
)

data class CloseShiftRequest(
    val shiftId: Long,
    val closingCash: BigDecimal,
    val note: String? = null
)
