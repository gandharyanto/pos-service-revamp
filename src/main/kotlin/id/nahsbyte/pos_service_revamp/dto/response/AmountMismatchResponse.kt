package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal

data class MismatchDetail(
    val field: String,
    val fromRequest: BigDecimal,
    val calculated: BigDecimal
)

data class AmountMismatchResponse(
    val mismatches: List<MismatchDetail>
)
