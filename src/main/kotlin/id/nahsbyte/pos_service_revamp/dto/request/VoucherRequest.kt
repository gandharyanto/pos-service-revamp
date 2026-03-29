package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateVoucherBrandRequest(
    val name: String,
    val logoUrl: String? = null,
    val isActive: Boolean = true
)

data class UpdateVoucherBrandRequest(
    val id: Long,
    val name: String,
    val logoUrl: String? = null,
    val isActive: Boolean = true
)

data class CreateVoucherGroupRequest(
    val brandId: Long,
    val name: String,
    val purchasePrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val expiredDate: LocalDateTime? = null,
    /** Comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN */
    val validDays: String? = null,
    val isRequiredCustomer: Boolean = false,
    /** POS | ONLINE | BOTH */
    val channel: String = "BOTH",
    val isActive: Boolean = true
)

data class UpdateVoucherGroupRequest(
    val id: Long,
    val name: String,
    val purchasePrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val expiredDate: LocalDateTime? = null,
    val validDays: String? = null,
    val isRequiredCustomer: Boolean = false,
    val channel: String = "BOTH",
    val isActive: Boolean = true
)

/** Tambah satu kode voucher secara manual */
data class AddVoucherCodeRequest(
    val groupId: Long,
    val code: String
)

/** Bulk import kode voucher dari list */
data class BulkImportVoucherCodesRequest(
    val groupId: Long,
    val codes: List<String>
)
