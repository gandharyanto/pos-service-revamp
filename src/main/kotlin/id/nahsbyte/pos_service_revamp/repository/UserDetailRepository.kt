package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.UserDetail
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserDetailRepository : JpaRepository<UserDetail, Long> {
    fun findByUsername(username: String): Optional<UserDetail>
    fun findAllByMerchantId(merchantId: Long): List<UserDetail>
    fun findByMerchantIdAndId(merchantId: Long, id: Long): Optional<UserDetail>
}
