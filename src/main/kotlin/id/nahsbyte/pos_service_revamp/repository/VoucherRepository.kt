package id.nahsbyte.pos_service_revamp.repository

import id.nahsbyte.pos_service_revamp.entity.Voucher
import id.nahsbyte.pos_service_revamp.entity.VoucherBrand
import id.nahsbyte.pos_service_revamp.entity.VoucherGroup
import id.nahsbyte.pos_service_revamp.entity.VoucherUsage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface VoucherBrandRepository : JpaRepository<VoucherBrand, Long> {
    fun findByMerchantIdAndIsActiveTrue(merchantId: Long): List<VoucherBrand>
    fun findByMerchantId(merchantId: Long): List<VoucherBrand>
}

interface VoucherGroupRepository : JpaRepository<VoucherGroup, Long> {
    fun findByBrandIdAndMerchantId(brandId: Long, merchantId: Long): List<VoucherGroup>
    fun findByMerchantId(merchantId: Long): List<VoucherGroup>
}

interface VoucherRepository : JpaRepository<Voucher, Long> {
    fun findByCodeAndMerchantId(code: String, merchantId: Long): Optional<Voucher>
    fun findByGroupIdAndMerchantId(groupId: Long, merchantId: Long): List<Voucher>
    fun findByGroupId(groupId: Long): List<Voucher>
    fun existsByCode(code: String): Boolean
}

interface VoucherUsageRepository : JpaRepository<VoucherUsage, Long> {
    fun findByVoucherId(voucherId: Long): List<VoucherUsage>
    fun findByTransactionId(transactionId: Long): List<VoucherUsage>
}
