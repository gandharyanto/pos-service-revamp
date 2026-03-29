package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface ProductRepository : JpaRepository<Product, Long> {

    fun findByIdAndMerchantIdAndDeletedDateIsNull(id: Long, merchantId: Long): Optional<Product>

    @Query("""
        SELECT p FROM Product p
        WHERE p.merchantId = :merchantId
          AND p.deletedDate IS NULL
          AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:sku IS NULL OR p.sku = :sku)
          AND (:upc IS NULL OR p.upc = :upc)
          AND (:startDate IS NULL OR p.createdDate >= :startDate)
          AND (:endDate IS NULL OR p.createdDate <= :endDate)
          AND (:categoryId IS NULL OR EXISTS (
              SELECT 1 FROM p.categories c WHERE c.id = :categoryId
          ))
    """)
    fun searchProducts(
        @Param("merchantId") merchantId: Long,
        @Param("keyword") keyword: String?,
        @Param("sku") sku: String?,
        @Param("upc") upc: String?,
        @Param("categoryId") categoryId: Long?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<Product>
}
