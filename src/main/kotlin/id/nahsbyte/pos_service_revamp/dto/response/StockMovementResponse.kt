package id.nahsbyte.pos_service_revamp.dto.response

import java.time.LocalDateTime

data class StockMovementResponse(
    val id: Long,
    val productId: Long,
    val qty: Int,
    val movementType: String,
    val movementReason: String?,
    val note: String?,
    val createdBy: String?,
    val createdDate: LocalDateTime?
)
