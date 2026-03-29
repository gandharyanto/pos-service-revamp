package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class VoucherBrandResponse(
    val id: Long,
    val name: String,
    val logoUrl: String?,
    val isActive: Boolean,
    val groups: List<VoucherGroupResponse> = emptyList(),
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)

data class VoucherGroupResponse(
    val id: Long,
    val brandId: Long,
    val brandName: String,
    val name: String,
    val purchasePrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val expiredDate: LocalDateTime?,
    val validDays: String?,
    val isRequiredCustomer: Boolean,
    val channel: String,
    val isActive: Boolean,
    val totalCodes: Int,
    val availableCodes: Int,
    val usedCodes: Int,
    val createdDate: LocalDateTime?,
    val modifiedDate: LocalDateTime?
)

data class VoucherCodeResponse(
    val id: Long,
    val code: String,
    val groupId: Long?,
    val status: String,
    val usedDate: LocalDateTime?,
    val transactionId: Long?,
    val isActive: Boolean,
    val createdDate: LocalDateTime?
)

data class BulkImportResult(
    val imported: Int,
    val skipped: Int,
    val skippedCodes: List<String>
)
