package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface PaymentRepository : JpaRepository<Payment, Long> {
    fun findAllByTransactionId(transactionId: Long): List<Payment>

    @Query("""
        SELECT p.paymentMethod, SUM(p.amountPaid)
        FROM Payment p
        WHERE p.transactionId IN (
            SELECT t.id FROM Transaction t
            WHERE t.merchantId = :merchantId
              AND t.createdDate BETWEEN :startDate AND :endDate
        )
        AND p.isEffective = true
        GROUP BY p.paymentMethod
    """)
    fun sumByPaymentMethodAndMerchant(
        @Param("merchantId") merchantId: Long,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>
}
