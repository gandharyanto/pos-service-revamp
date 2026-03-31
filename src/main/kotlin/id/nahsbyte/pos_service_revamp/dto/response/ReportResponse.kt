package id.nahsbyte.pos_service_revamp.dto.response

import java.math.BigDecimal

data class ProductSaleItem(
    val productName: String?,
    val totalSaleItems: Long
)

data class PaymentSummaryItem(
    val paymentMethod: String?,
    val totalAmount: BigDecimal
)

data class SummaryReportResponse(
    val productList: List<ProductSaleItem>,
    val paymentListInternal: List<PaymentSummaryItem>,
    val paymentListExternal: List<PaymentSummaryItem>
)

// --- Financial Report DTOs ---

data class ReportPeriod(val startDate: String, val endDate: String)

data class FinancialSummaryResponse(
    val period: ReportPeriod,
    val outletId: Long?,
    val totalTransactions: Int,
    val paidTransactions: Int,
    val refundedTransactions: Int,
    val grossRevenue: BigDecimal,
    val totalDiscount: BigDecimal,
    val totalPromo: BigDecimal,
    val totalVoucher: BigDecimal,
    val totalLoyaltyRedeem: BigDecimal,
    val netRevenue: BigDecimal,
    val totalTax: BigDecimal,
    val totalServiceCharge: BigDecimal,
    val totalRounding: BigDecimal,
    val totalAmount: BigDecimal,
    val totalRefund: BigDecimal
)

data class PaymentMethodBreakdownResponse(
    val paymentMethod: String,
    val transactionCount: Int,
    val totalAmount: BigDecimal
)

data class TopProductResponse(
    val rank: Int,
    val productName: String,
    val qtySold: Long,
    val totalRevenue: BigDecimal
)

data class OutletBreakdownResponse(
    val outletId: Long,
    val totalTransactions: Int,
    val grossRevenue: BigDecimal,
    val netRevenue: BigDecimal,
    val totalTax: BigDecimal,
    val totalServiceCharge: BigDecimal,
    val totalAmount: BigDecimal
)

data class DisbursementSummaryResponse(
    val layer: String,
    val recipientName: String?,
    val percentage: BigDecimal,
    val totalBaseAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val transactionCount: Int,
    val settledCount: Int,
    val pendingCount: Int
)
