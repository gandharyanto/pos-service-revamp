package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.response.PaymentSummaryItem
import id.nahsbyte.pos_service_revamp.dto.response.ProductSaleItem
import id.nahsbyte.pos_service_revamp.dto.response.SummaryReportResponse
import id.nahsbyte.pos_service_revamp.repository.PaymentRepository
import id.nahsbyte.pos_service_revamp.repository.TransactionItemRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ReportService(
    private val transactionItemRepository: TransactionItemRepository,
    private val paymentRepository: PaymentRepository
) {

    fun getSummary(merchantId: Long, startDate: String, endDate: String): SummaryReportResponse {
        val start = LocalDate.parse(startDate).atStartOfDay()
        val end = LocalDate.parse(endDate).atTime(23, 59, 59)

        val productRows = transactionItemRepository.findTopProductsByMerchantAndDateRange(merchantId, start, end)
        val productList = productRows.map {
            ProductSaleItem(
                productName = it[0] as? String,
                totalSaleItems = (it[1] as? Number)?.toLong() ?: 0L
            )
        }

        val paymentRows = paymentRepository.sumByPaymentMethodAndMerchant(merchantId, start, end)
        val paymentList = paymentRows.map {
            PaymentSummaryItem(
                paymentMethod = it[0] as? String,
                totalAmount = (it[1] as? BigDecimal) ?: BigDecimal.ZERO
            )
        }

        // Split by method prefix: CASH/CARD = internal, others = external
        val internalKeywords = setOf("CASH", "CARD", "DEBIT", "CREDIT")
        val (internal, external) = paymentList.partition { item ->
            internalKeywords.any { item.paymentMethod?.uppercase()?.contains(it) == true }
        }

        return SummaryReportResponse(
            productList = productList,
            paymentListInternal = internal,
            paymentListExternal = external
        )
    }
}
