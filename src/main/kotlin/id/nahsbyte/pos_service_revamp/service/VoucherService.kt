package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.Voucher
import id.nahsbyte.pos_service_revamp.entity.VoucherBrand
import id.nahsbyte.pos_service_revamp.entity.VoucherGroup
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoucherService(
    private val voucherBrandRepository: VoucherBrandRepository,
    private val voucherGroupRepository: VoucherGroupRepository,
    private val voucherRepository: VoucherRepository
) {

    // ── Brand ──────────────────────────────────────────────────────────────

    fun listBrands(merchantId: Long): List<VoucherBrandResponse> {
        val brands = voucherBrandRepository.findByMerchantId(merchantId)
        val allGroups = voucherGroupRepository.findByMerchantId(merchantId)
            .groupBy { it.brandId }
        return brands.map { brand ->
            brand.toResponse(allGroups[brand.id]?.map { it.toResponse(merchantId) } ?: emptyList())
        }
    }

    fun detailBrand(merchantId: Long, brandId: Long): VoucherBrandResponse {
        val brand = voucherBrandRepository.findById(brandId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherBrand not found") }
        val groups = voucherGroupRepository.findByBrandIdAndMerchantId(brandId, merchantId)
            .map { it.toResponse(merchantId) }
        return brand.toResponse(groups)
    }

    @Transactional
    fun createBrand(merchantId: Long, request: CreateVoucherBrandRequest): VoucherBrandResponse {
        val brand = VoucherBrand().apply {
            this.merchantId = merchantId
            name = request.name
            logoUrl = request.logoUrl
            isActive = request.isActive
        }
        return voucherBrandRepository.save(brand).toResponse(emptyList())
    }

    @Transactional
    fun updateBrand(merchantId: Long, request: UpdateVoucherBrandRequest): VoucherBrandResponse {
        val brand = voucherBrandRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherBrand not found") }
        brand.apply {
            name = request.name
            logoUrl = request.logoUrl
            isActive = request.isActive
        }
        val groups = voucherGroupRepository.findByBrandIdAndMerchantId(request.id, merchantId).map { it.toResponse(merchantId) }
        return voucherBrandRepository.save(brand).toResponse(groups)
    }

    @Transactional
    fun deleteBrand(merchantId: Long, brandId: Long) {
        val brand = voucherBrandRepository.findById(brandId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherBrand not found") }
        voucherBrandRepository.delete(brand)
    }

    // ── Group ──────────────────────────────────────────────────────────────

    @Transactional
    fun createGroup(merchantId: Long, request: CreateVoucherGroupRequest): VoucherGroupResponse {
        voucherBrandRepository.findById(request.brandId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherBrand not found") }

        val group = VoucherGroup().apply {
            this.merchantId = merchantId
            brandId = request.brandId
            name = request.name
            purchasePrice = request.purchasePrice
            sellingPrice = request.sellingPrice
            expiredDate = request.expiredDate
            validDays = request.validDays
            isRequiredCustomer = request.isRequiredCustomer
            channel = request.channel
            isActive = request.isActive
        }
        return voucherGroupRepository.save(group).toResponse(merchantId)
    }

    @Transactional
    fun updateGroup(merchantId: Long, request: UpdateVoucherGroupRequest): VoucherGroupResponse {
        val group = voucherGroupRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherGroup not found") }
        group.apply {
            name = request.name
            purchasePrice = request.purchasePrice
            sellingPrice = request.sellingPrice
            expiredDate = request.expiredDate
            validDays = request.validDays
            isRequiredCustomer = request.isRequiredCustomer
            channel = request.channel
            isActive = request.isActive
        }
        return voucherGroupRepository.save(group).toResponse(merchantId)
    }

    @Transactional
    fun deleteGroup(merchantId: Long, groupId: Long) {
        val group = voucherGroupRepository.findById(groupId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherGroup not found") }
        voucherGroupRepository.delete(group)
    }

    // ── Code ──────────────────────────────────────────────────────────────

    fun listCodes(merchantId: Long, groupId: Long): List<VoucherCodeResponse> {
        // Verify group belongs to merchant
        voucherGroupRepository.findById(groupId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherGroup not found") }
        return voucherRepository.findByGroupIdAndMerchantId(groupId, merchantId).map { it.toCodeResponse() }
    }

    @Transactional
    fun addCode(merchantId: Long, request: AddVoucherCodeRequest): VoucherCodeResponse {
        voucherGroupRepository.findById(request.groupId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherGroup not found") }

        if (voucherRepository.existsByCode(request.code)) {
            throw IllegalArgumentException("Voucher code '${request.code}' already exists")
        }

        val voucher = Voucher().apply {
            this.merchantId = merchantId
            code = request.code
            groupId = request.groupId
            status = "AVAILABLE"
            isActive = true
        }
        return voucherRepository.save(voucher).toCodeResponse()
    }

    @Transactional
    fun bulkImport(merchantId: Long, request: BulkImportVoucherCodesRequest): BulkImportResult {
        voucherGroupRepository.findById(request.groupId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("VoucherGroup not found") }

        val skippedCodes = mutableListOf<String>()
        var imported = 0

        for (code in request.codes.map { it.trim() }.filter { it.isNotBlank() }) {
            if (voucherRepository.existsByCode(code)) {
                skippedCodes.add(code)
                continue
            }
            voucherRepository.save(Voucher().apply {
                this.merchantId = merchantId
                this.code = code
                groupId = request.groupId
                status = "AVAILABLE"
                isActive = true
            })
            imported++
        }

        return BulkImportResult(imported = imported, skipped = skippedCodes.size, skippedCodes = skippedCodes)
    }

    @Transactional
    fun cancelCode(merchantId: Long, voucherId: Long): VoucherCodeResponse {
        val voucher = voucherRepository.findById(voucherId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Voucher not found") }
        if (voucher.status == "USED") throw IllegalStateException("Cannot cancel a USED voucher")
        voucher.status = "CANCELLED"
        voucher.isActive = false
        return voucherRepository.save(voucher).toCodeResponse()
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private fun VoucherBrand.toResponse(groups: List<VoucherGroupResponse>) = VoucherBrandResponse(
        id = id,
        name = name,
        logoUrl = logoUrl,
        isActive = isActive,
        groups = groups,
        createdDate = createdDate,
        modifiedDate = modifiedDate
    )

    private fun VoucherGroup.toResponse(merchantId: Long): VoucherGroupResponse {
        val brand = voucherBrandRepository.findById(brandId).orElse(null)
        val codes = voucherRepository.findByGroupIdAndMerchantId(id, merchantId)
        val total = codes.size
        val used = codes.count { it.status == "USED" }
        val available = codes.count { it.status == "AVAILABLE" }
        return VoucherGroupResponse(
            id = id,
            brandId = brandId,
            brandName = brand?.name ?: "",
            name = name,
            purchasePrice = purchasePrice,
            sellingPrice = sellingPrice,
            expiredDate = expiredDate,
            validDays = validDays,
            isRequiredCustomer = isRequiredCustomer,
            channel = channel,
            isActive = isActive,
            totalCodes = total,
            availableCodes = available,
            usedCodes = used,
            createdDate = createdDate,
            modifiedDate = modifiedDate
        )
    }

    private fun Voucher.toCodeResponse() = VoucherCodeResponse(
        id = id,
        code = code,
        groupId = groupId,
        status = status ?: if (isActive) "AVAILABLE" else "CANCELLED",
        usedDate = usedDate,
        transactionId = transactionId,
        isActive = isActive,
        createdDate = createdDate
    )
}
