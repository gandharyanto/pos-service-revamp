package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PriceBook
import id.nahsbyte.pos_service_revamp.entity.PriceBookItem
import id.nahsbyte.pos_service_revamp.entity.PriceBookWholesaleTier
import id.nahsbyte.pos_service_revamp.entity.PriceBookOutlet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PriceBookRepository : JpaRepository<PriceBook, Long> {
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<PriceBook>
}

interface PriceBookItemRepository : JpaRepository<PriceBookItem, Long> {
    fun findByPriceBookIdAndProductId(priceBookId: Long, productId: Long): Optional<PriceBookItem>
    fun findByPriceBookIdIn(priceBookIds: List<Long>): List<PriceBookItem>
}

interface PriceBookWholesaleTierRepository : JpaRepository<PriceBookWholesaleTier, Long> {
    fun findByPriceBookIdInAndProductId(priceBookIds: List<Long>, productId: Long): List<PriceBookWholesaleTier>
}

interface PriceBookOutletRepository : JpaRepository<PriceBookOutlet, Long> {
    fun findByPriceBookId(priceBookId: Long): List<PriceBookOutlet>
}
