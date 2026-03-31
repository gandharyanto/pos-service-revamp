package id.nahsbyte.pos_service_revamp.dto.request

data class CreateReceiptRequest(
    /** Null = berlaku semua outlet (default). Isi untuk per-outlet override. */
    val outletId: Long? = null,
    val header: String? = null,
    val footer: String? = null,
    val showTax: Boolean = true,
    val showServiceCharge: Boolean = true,
    val showRounding: Boolean = true,
    val showLogo: Boolean = false,
    val logoUrl: String? = null,
    val showQueueNumber: Boolean = true,
    /** Ukuran kertas: 58mm | 80mm */
    val paperSize: String? = null
)

data class UpdateReceiptRequest(
    val receiptId: Long,
    val header: String? = null,
    val footer: String? = null,
    val showTax: Boolean = true,
    val showServiceCharge: Boolean = true,
    val showRounding: Boolean = true,
    val showLogo: Boolean = false,
    val logoUrl: String? = null,
    val showQueueNumber: Boolean = true,
    val paperSize: String? = null
)
