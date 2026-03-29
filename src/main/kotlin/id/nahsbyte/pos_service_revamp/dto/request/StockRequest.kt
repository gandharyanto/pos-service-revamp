package id.nahsbyte.pos_service_revamp.dto.request

import jakarta.validation.constraints.NotNull

enum class StockUpdateType { ADD, SUBTRACT, SET }

data class UpdateStockRequest(
    @field:NotNull val productId: Long,
    @field:NotNull val qty: Int,
    @field:NotNull val updateType: StockUpdateType
)
