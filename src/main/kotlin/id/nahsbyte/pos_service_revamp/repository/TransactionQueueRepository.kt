package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.TransactionQueue
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface TransactionQueueRepository : JpaRepository<TransactionQueue, Long> {
    fun countByMerchantIdAndOutletIdAndQueueDate(merchantId: Long, outletId: Long, date: LocalDate): Long
}
