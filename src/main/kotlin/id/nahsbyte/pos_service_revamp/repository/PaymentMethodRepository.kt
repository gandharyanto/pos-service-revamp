package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PaymentMethod
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentMethodRepository : JpaRepository<PaymentMethod, Long> {
    fun findAllByIsActiveTrue(): List<PaymentMethod>
}
