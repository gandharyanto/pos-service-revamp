package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.PrinterSetting
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PrinterRepository : JpaRepository<PrinterSetting, Long> {
    fun findAllByMerchantId(merchantId: Long): List<PrinterSetting>
    fun findAllByMerchantIdAndOutletId(merchantId: Long, outletId: Long): List<PrinterSetting>
    fun findByMerchantIdAndId(merchantId: Long, id: Long): Optional<PrinterSetting>
}
