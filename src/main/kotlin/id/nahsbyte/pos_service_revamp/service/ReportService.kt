package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.repository.DisbursementLogRepository
import id.nahsbyte.pos_service_revamp.repository.PaymentRepository
import id.nahsbyte.pos_service_revamp.repository.TransactionItemRepository
import id.nahsbyte.pos_service_revamp.repository.TransactionRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class ReportService(
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val paymentRepository: PaymentRepository,
    private val disbursementLogRepository: DisbursementLogRepository
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

    fun getFinancialSummary(merchantId: Long, startDate: String, endDate: String, outletId: Long?): FinancialSummaryResponse {
        val (from, to) = parseDateRange(startDate, endDate)
        val transactions = fetchTransactions(merchantId, outletId, from, to)
        val paid = transactions.filter { it.status == "PAID" }
        val refunded = transactions.filter { it.refundDate != null }

        return FinancialSummaryResponse(
            period = ReportPeriod(startDate, endDate),
            outletId = outletId,
            totalTransactions = transactions.size,
            paidTransactions = paid.size,
            refundedTransactions = refunded.size,
            grossRevenue = paid.sumOf { it.grossAmount ?: it.subTotal },
            totalDiscount = paid.sumOf { it.discountAmount ?: BigDecimal.ZERO },
            totalPromo = paid.sumOf { it.promoAmount ?: BigDecimal.ZERO },
            totalVoucher = paid.sumOf { it.voucherAmount ?: BigDecimal.ZERO },
            totalLoyaltyRedeem = paid.sumOf { it.loyaltyRedeemAmount ?: BigDecimal.ZERO },
            netRevenue = paid.sumOf { it.netAmount ?: it.subTotal },
            totalTax = paid.sumOf { it.totalTax ?: BigDecimal.ZERO },
            totalServiceCharge = paid.sumOf { it.totalServiceCharge ?: BigDecimal.ZERO },
            totalRounding = paid.sumOf { it.totalRounding ?: BigDecimal.ZERO },
            totalAmount = paid.sumOf { it.totalAmount },
            totalRefund = refunded.sumOf { it.refundAmount ?: BigDecimal.ZERO }
        )
    }

    fun getPaymentMethodBreakdown(merchantId: Long, startDate: String, endDate: String, outletId: Long?): List<PaymentMethodBreakdownResponse> {
        val (from, to) = parseDateRange(startDate, endDate)
        val transactions = fetchTransactions(merchantId, outletId, from, to)
        return transactions
            .filter { it.status == "PAID" }
            .groupBy { it.paymentMethod ?: "UNKNOWN" }
            .map { (method, trxList) ->
                PaymentMethodBreakdownResponse(
                    paymentMethod = method,
                    transactionCount = trxList.size,
                    totalAmount = trxList.sumOf { it.totalAmount }
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun getTopProducts(merchantId: Long, startDate: String, endDate: String, limit: Int): List<TopProductResponse> {
        val (from, to) = parseDateRange(startDate, endDate)
        val rows = transactionItemRepository.findTopProductsByMerchantAndDateRange(merchantId, from, to)
        return rows.take(limit).mapIndexed { index, row ->
            TopProductResponse(
                rank = index + 1,
                productName = row[0] as? String ?: "-",
                qtySold = (row[1] as? Number)?.toLong() ?: 0L,
                totalRevenue = (row[2] as? BigDecimal) ?: BigDecimal.ZERO
            )
        }
    }

    fun getOutletBreakdown(merchantId: Long, startDate: String, endDate: String): List<OutletBreakdownResponse> {
        val (from, to) = parseDateRange(startDate, endDate)
        val transactions = transactionRepository.findAllByMerchantIdAndCreatedDateBetween(merchantId, from, to)
        return transactions
            .filter { it.status == "PAID" }
            .groupBy { it.outletId }
            .map { (outletId, trxList) ->
                OutletBreakdownResponse(
                    outletId = outletId,
                    totalTransactions = trxList.size,
                    grossRevenue = trxList.sumOf { it.grossAmount ?: it.subTotal },
                    netRevenue = trxList.sumOf { it.netAmount ?: it.subTotal },
                    totalTax = trxList.sumOf { it.totalTax ?: BigDecimal.ZERO },
                    totalServiceCharge = trxList.sumOf { it.totalServiceCharge ?: BigDecimal.ZERO },
                    totalAmount = trxList.sumOf { it.totalAmount }
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    fun getDisbursementSummary(merchantId: Long, startDate: String, endDate: String): List<DisbursementSummaryResponse> {
        val (from, to) = parseDateRange(startDate, endDate)
        val logs = disbursementLogRepository.findAllByMerchantIdAndCreatedDateBetween(merchantId, from, to)
        return logs
            .groupBy { "${it.layer}|${it.ruleId}" }
            .map { (_, logList) ->
                val first = logList.first()
                DisbursementSummaryResponse(
                    layer = first.layer,
                    recipientName = first.recipientName,
                    percentage = first.percentage,
                    totalBaseAmount = logList.sumOf { it.baseAmount },
                    totalAmount = logList.sumOf { it.amount },
                    transactionCount = logList.map { it.transactionId }.distinct().size,
                    settledCount = logList.count { it.status == "SETTLED" },
                    pendingCount = logList.count { it.status == "PENDING" }
                )
            }
            .sortedBy { it.layer }
    }

    private fun parseDateRange(startDate: String, endDate: String): Pair<LocalDateTime, LocalDateTime> =
        LocalDate.parse(startDate).atStartOfDay() to LocalDate.parse(endDate).atTime(23, 59, 59)

    private fun fetchTransactions(merchantId: Long, outletId: Long?, from: LocalDateTime, to: LocalDateTime) =
        if (outletId != null)
            transactionRepository.findAllByMerchantIdAndOutletIdAndCreatedDateBetween(merchantId, outletId, from, to)
        else
            transactionRepository.findAllByMerchantIdAndCreatedDateBetween(merchantId, from, to)
}
