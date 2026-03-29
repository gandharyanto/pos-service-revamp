package id.nahsbyte.pos_service_revamp.dto.response

import java.time.LocalDateTime

data class OrderTypeResponse(
    val id: Long,
    val name: String,
    val code: String?,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)
