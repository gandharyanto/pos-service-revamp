package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Stock
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface StockRepository : JpaRepository<Stock, Long> {
    fun findByProductId(productId: Long): Optional<Stock>
}
