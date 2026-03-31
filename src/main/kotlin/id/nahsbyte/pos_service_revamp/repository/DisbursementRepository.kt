package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.DisbursementLog
import id.nahsbyte.pos_service_revamp.entity.DisbursementRule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.Optional

interface DisbursementRuleRepository : JpaRepository<DisbursementRule, Long> {
    fun findAllByMerchantId(merchantId: Long): List<DisbursementRule>
    fun findAllByMerchantIdAndIsActiveTrue(merchantId: Long): List<DisbursementRule>
    fun findByMerchantIdAndId(merchantId: Long, id: Long): Optional<DisbursementRule>
}

interface DisbursementLogRepository : JpaRepository<DisbursementLog, Long> {
    fun findAllByMerchantId(merchantId: Long): List<DisbursementLog>
    fun findAllByMerchantIdAndCreatedDateBetween(
        merchantId: Long, from: LocalDateTime, to: LocalDateTime
    ): List<DisbursementLog>
    fun findAllByTransactionId(transactionId: Long): List<DisbursementLog>
}
