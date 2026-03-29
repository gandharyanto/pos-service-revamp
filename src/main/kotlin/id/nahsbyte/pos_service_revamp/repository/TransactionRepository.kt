package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Transaction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByIdAndMerchantId(id: Long, merchantId: Long): Optional<Transaction>
    fun findByTrxIdAndMerchantId(trxId: String, merchantId: Long): Optional<Transaction>
    fun findAllByMerchantIdAndCreatedDateBetween(
        merchantId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Transaction>
}
