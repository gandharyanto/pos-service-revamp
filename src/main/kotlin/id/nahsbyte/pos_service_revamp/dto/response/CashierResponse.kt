package id.nahsbyte.pos_service_revamp.dto.response

import java.time.LocalDateTime

data class CashierResponse(
    val id: Long,
    val username: String,
    val fullName: String?,
    val email: String?,
    val employeeCode: String?,
    val outletId: Long?,
    val isActive: Boolean,
    val hasPin: Boolean,
    val createdDate: LocalDateTime?
)
