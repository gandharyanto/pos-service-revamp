package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Category
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findAllByMerchantId(merchantId: Long, pageable: Pageable): Page<Category>
    fun findByIdAndMerchantId(id: Long, merchantId: Long): Optional<Category>
}
