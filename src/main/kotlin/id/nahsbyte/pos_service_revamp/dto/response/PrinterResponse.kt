package id.nahsbyte.pos_service_revamp.dto.response

data class PrinterResponse(
    val id: Long,
    val outletId: Long?,
    val type: String,
    val name: String,
    val connectionType: String?,
    val ipAddress: String?,
    val port: Int?,
    val paperSize: String?,
    val isDefault: Boolean,
    val isActive: Boolean
)
