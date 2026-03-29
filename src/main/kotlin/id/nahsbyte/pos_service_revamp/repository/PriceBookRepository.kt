package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PriceBook
import id.nahsbyte.pos_service_revamp.entity.PriceBookItem
import id.nahsbyte.pos_service_revamp.entity.PriceBookWholesaleTier
import id.nahsbyte.pos_service_revamp.entity.PriceBookOutlet
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PriceBookRepository : JpaRepository<PriceBook, Long> {
    fun findByMerchantId(merchantId: Long): List<PriceBook>
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<PriceBook>
    fun findByMerchantIdAndTypeAndIsDefaultTrue(merchantId: Long, type: String): List<PriceBook>
}

interface PriceBookItemRepository : JpaRepository<PriceBookItem, Long> {
    fun findByPriceBookId(priceBookId: Long): List<PriceBookItem>
    fun findByPriceBookIdAndProductId(priceBookId: Long, productId: Long): Optional<PriceBookItem>
    fun findByPriceBookIdIn(priceBookIds: List<Long>): List<PriceBookItem>
    fun deleteAllByPriceBookId(priceBookId: Long)
}

interface PriceBookWholesaleTierRepository : JpaRepository<PriceBookWholesaleTier, Long> {
    fun findByPriceBookId(priceBookId: Long): List<PriceBookWholesaleTier>
    fun findByPriceBookIdIn(priceBookIds: List<Long>): List<PriceBookWholesaleTier>
    fun findByPriceBookIdInAndProductId(priceBookIds: List<Long>, productId: Long): List<PriceBookWholesaleTier>
    fun deleteAllByPriceBookId(priceBookId: Long)
}

interface PriceBookOutletRepository : JpaRepository<PriceBookOutlet, Long> {
    fun findByPriceBookId(priceBookId: Long): List<PriceBookOutlet>
    fun findByPriceBookIdIn(priceBookIds: List<Long>): List<PriceBookOutlet>
    fun deleteAllByPriceBookId(priceBookId: Long)
}
