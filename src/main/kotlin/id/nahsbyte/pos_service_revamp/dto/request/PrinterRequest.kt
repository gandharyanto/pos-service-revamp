package id.nahsbyte.pos_service_revamp.dto.request

data class CreatePrinterRequest(
    val outletId: Long? = null,
    /** RECEIPT | KITCHEN | ORDER */
    val type: String = "RECEIPT",
    val name: String,
    /** NETWORK | USB | BLUETOOTH */
    val connectionType: String? = null,
    val ipAddress: String? = null,
    val port: Int? = null,
    /** 58mm | 80mm */
    val paperSize: String? = null,
    val isDefault: Boolean = false
)

data class UpdatePrinterRequest(
    val printerId: Long,
    val outletId: Long? = null,
    val type: String = "RECEIPT",
    val name: String,
    val connectionType: String? = null,
    val ipAddress: String? = null,
    val port: Int? = null,
    val paperSize: String? = null,
    val isDefault: Boolean = false,
    val isActive: Boolean = true
)
