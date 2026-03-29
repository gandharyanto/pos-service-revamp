package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.StockMovement
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface StockMovementRepository : JpaRepository<StockMovement, Long> {
    fun findAllByProductIdAndCreatedDateBetween(
        productId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<StockMovement>
}
