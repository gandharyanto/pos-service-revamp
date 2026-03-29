package id.nahsbyte.pos_service_revamp.dto.request

data class CreateOrderTypeRequest(
    val name: String,
    val code: String? = null,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)

data class UpdateOrderTypeRequest(
    val id: Long,
    val name: String,
    val code: String? = null,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
