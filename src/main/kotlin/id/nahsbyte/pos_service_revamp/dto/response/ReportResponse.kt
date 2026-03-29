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
