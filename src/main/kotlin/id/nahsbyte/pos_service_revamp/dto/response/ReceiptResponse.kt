package id.nahsbyte.pos_service_revamp.dto.response

data class ReceiptResponse(
    val id: Long,
    val outletId: Long?,
    val header: String?,
    val footer: String?,
    val showTax: Boolean,
    val showServiceCharge: Boolean,
    val showRounding: Boolean,
    val showLogo: Boolean,
    val logoUrl: String?,
    val showQueueNumber: Boolean,
    val paperSize: String?
)
