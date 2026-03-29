package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Images
import org.springframework.data.jpa.repository.JpaRepository

interface ImagesRepository : JpaRepository<Images, Long> {
    fun findAllByMerchantIdAndSourceTypeAndSourceId(merchantId: Long, sourceType: String, sourceId: Long): List<Images>
}
