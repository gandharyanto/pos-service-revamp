package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal
import java.time.LocalDateTime

data class TransactionSummaryResponse(
    val id: Long,
    val trxId: String?,
    val status: String,
    val paymentMethod: String?,
    val totalAmount: BigDecimal,
    val createdDate: LocalDateTime?
)

data class TransactionItemResponse(
    val id: Long,
    val productId: Long?,
    val productName: String?,
    val price: BigDecimal,
    val qty: Int,
    val totalPrice: BigDecimal,
    val taxName: String?,
    val taxPercentage: BigDecimal?,
    val taxAmount: BigDecimal?
)

data class PaymentDetailResponse(
    val id: Long,
    val paymentMethod: String?,
    val amountPaid: BigDecimal,
    val status: String?,
    val paymentReference: String?,
    val paymentDate: LocalDateTime?
)

data class TransactionDetailResponse(
    val transactionId: Long,
    val code: String?,
    val paymentMethod: String?,
    val status: String,
    val subTotal: BigDecimal,
    val totalTax: BigDecimal?,
    val totalServiceCharge: BigDecimal?,
    val totalRounding: BigDecimal?,
    val totalAmount: BigDecimal,
    val cashTendered: BigDecimal?,
    val cashChange: BigDecimal?,
    val taxName: String?,
    val taxPercentage: BigDecimal?,
    val serviceChargeAmount: BigDecimal?,
    val serviceChargePercentage: BigDecimal?,
    val roundingTarget: String?,
    val roundingType: String?,
    val transactionDate: LocalDateTime?,
    val queueNumber: String?,
    val transactionItems: List<TransactionItemResponse>,
    val payments: List<PaymentDetailResponse>
)

data class CreateTransactionResponse(
    val id: Long,
    val trxId: String?,
    val queueNumber: String?
)
