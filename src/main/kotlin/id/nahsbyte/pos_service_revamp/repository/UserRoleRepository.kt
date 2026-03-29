package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleRepository : JpaRepository<UserRole, Long> {
    fun findAllByUserId(userId: Long): List<UserRole>
    fun findAllByUserIdAndApplicationType(userId: Long, applicationType: String): List<UserRole>
}
