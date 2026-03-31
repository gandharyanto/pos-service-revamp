package id.nahsbyte.pos_service_revamp.dto.request

data class CreateCashierRequest(
    val username: String,
    val password: String,
    val fullName: String? = null,
    val email: String? = null,
    val employeeCode: String? = null,
    val outletId: Long? = null
)

data class UpdateCashierRequest(
    val cashierId: Long,
    val fullName: String? = null,
    val email: String? = null,
    val employeeCode: String? = null,
    val outletId: Long? = null,
    val isActive: Boolean = true
)

data class SetCashierPinRequest(
    val cashierId: Long,
    val pin: String
)

data class ResetCashierPasswordRequest(
    val cashierId: Long,
    val newPassword: String
)
