package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.TransactionItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface TransactionItemRepository : JpaRepository<TransactionItem, Long> {

    fun findAllByTransactionId(transactionId: Long): List<TransactionItem>

    @Query("""
        SELECT ti.productName, SUM(ti.qty), SUM(ti.totalPrice)
        FROM TransactionItem ti
        WHERE ti.transactionId IN (
            SELECT t.id FROM Transaction t
            WHERE t.merchantId = :merchantId
              AND t.createdDate BETWEEN :startDate AND :endDate
              AND t.status = 'PAID'
        )
        GROUP BY ti.productId, ti.productName
        ORDER BY SUM(ti.qty) DESC
    """)
    fun findTopProductsByMerchantAndDateRange(
        @Param("merchantId") merchantId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
}
