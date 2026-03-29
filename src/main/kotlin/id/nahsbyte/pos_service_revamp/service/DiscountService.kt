package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.DiscountResponse
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DiscountService(
    private val discountRepository: DiscountRepository,
    private val discountOutletRepository: DiscountOutletRepository,
    private val discountProductRepository: DiscountProductRepository,
    private val discountCategoryRepository: DiscountCategoryRepository,
    private val discountCustomerRepository: DiscountCustomerRepository
) {

    fun list(
        merchantId: Long,
        isActive: Boolean? = null,
        channel: String? = null,
        scope: String? = null,
        keyword: String? = null
    ): List<DiscountResponse> {
        var discounts = discountRepository.findByMerchantId(merchantId)
        if (isActive != null) discounts = discounts.filter { it.isActive == isActive }
        if (channel != null) discounts = discounts.filter { it.channel == channel }
        if (scope != null) discounts = discounts.filter { it.scope == scope }
        if (keyword != null) discounts = discounts.filter { it.name.contains(keyword, ignoreCase = true) }

        if (discounts.isEmpty()) return emptyList()

        val ids = discounts.map { it.id }
        val outletMap = discountOutletRepository.findByDiscountIdIn(ids).groupBy { it.discountId }
        val productMap = discountProductRepository.findByDiscountIdIn(ids).groupBy { it.discountId }
        val categoryMap = discountCategoryRepository.findByDiscountIdIn(ids).groupBy { it.discountId }
        val customerMap = discountCustomerRepository.findByDiscountIdIn(ids).groupBy { it.discountId }

        return discounts.map { d ->
            d.toResponse(
                outlets = outletMap[d.id]?.map { it.outletId } ?: emptyList(),
                productIds = productMap[d.id]?.map { it.productId } ?: emptyList(),
                categoryIds = categoryMap[d.id]?.map { it.categoryId } ?: emptyList(),
                customerIds = customerMap[d.id]?.map { it.customerId } ?: emptyList()
            )
        }
    }

    fun detail(merchantId: Long, id: Long): DiscountResponse =
        discountRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Discount not found") }
            .toDetailResponse()

    @Transactional
    fun create(merchantId: Long, request: CreateDiscountRequest): DiscountResponse {
        val discount = Discount().apply {
            this.merchantId = merchantId
            name = request.name
            code = request.code
            valueType = request.valueType
            value = request.value
            maxDiscountAmount = request.maxDiscountAmount
            minPurchase = request.minPurchase
            scope = request.scope
            channel = request.channel
            visibility = request.visibility
            usageLimit = request.usageLimit
            usagePerCustomer = request.usagePerCustomer
            startDate = request.startDate
            endDate = request.endDate
            isActive = request.isActive
        }
        discountRepository.save(discount)
        saveBindings(discount.id, merchantId, request.outletIds, request.productIds, request.categoryIds, request.customerIds)
        return discount.toDetailResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdateDiscountRequest): DiscountResponse {
        val discount = discountRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Discount not found") }
        discount.apply {
            name = request.name
            code = request.code
            valueType = request.valueType
            value = request.value
            maxDiscountAmount = request.maxDiscountAmount
            minPurchase = request.minPurchase
            scope = request.scope
            channel = request.channel
            visibility = request.visibility
            usageLimit = request.usageLimit
            usagePerCustomer = request.usagePerCustomer
            startDate = request.startDate
            endDate = request.endDate
            isActive = request.isActive
        }
        discountRepository.save(discount)
        discountOutletRepository.deleteAllByDiscountId(discount.id)
        discountProductRepository.deleteAllByDiscountId(discount.id)
        discountCategoryRepository.deleteAllByDiscountId(discount.id)
        discountCustomerRepository.deleteAllByDiscountId(discount.id)
        saveBindings(discount.id, merchantId, request.outletIds, request.productIds, request.categoryIds, request.customerIds)
        return discount.toDetailResponse()
    }

    @Transactional
    fun delete(merchantId: Long, id: Long) {
        val discount = discountRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Discount not found") }
        discountOutletRepository.deleteAllByDiscountId(id)
        discountProductRepository.deleteAllByDiscountId(id)
        discountCategoryRepository.deleteAllByDiscountId(id)
        discountCustomerRepository.deleteAllByDiscountId(id)
        discountRepository.delete(discount)
    }

    fun validate(merchantId: Long, request: ValidateDiscountRequest): DiscountResponse {
        val discount = discountRepository.findByCodeAndMerchantIdAndIsActiveTrue(request.code, merchantId)
            .orElseThrow { IllegalArgumentException("Discount code not found or inactive") }

        val now = LocalDateTime.now()
        if (discount.startDate != null && now.isBefore(discount.startDate))
            throw IllegalArgumentException("Discount not yet active")
        if (discount.endDate != null && now.isAfter(discount.endDate))
            throw IllegalArgumentException("Discount expired")
        if (discount.minPurchase != null && request.grossSubTotal < discount.minPurchase!!)
            throw IllegalArgumentException("Minimum purchase not met: ${discount.minPurchase}")

        if (discount.visibility == "SPECIFIC_OUTLET" && request.outletId != null) {
            val outletIds = discountOutletRepository.findByDiscountId(discount.id).map { it.outletId }
            if (request.outletId !in outletIds)
                throw IllegalArgumentException("Discount not valid for this outlet")
        }

        if (request.customerId != null) {
            val customerIds = discountCustomerRepository.findByDiscountId(discount.id).map { it.customerId }
            if (customerIds.isNotEmpty() && request.customerId !in customerIds)
                throw IllegalArgumentException("Discount not available for this customer")
        }

        return discount.toDetailResponse()
    }

    private fun saveBindings(
        discountId: Long, merchantId: Long,
        outletIds: List<Long>, productIds: List<Long>,
        categoryIds: List<Long>, customerIds: List<Long>
    ) {
        discountOutletRepository.saveAll(outletIds.map { oid ->
            DiscountOutlet().apply { this.discountId = discountId; this.outletId = oid; this.merchantId = merchantId }
        })
        discountProductRepository.saveAll(productIds.map { pid ->
            DiscountProduct().apply { this.discountId = discountId; this.productId = pid; this.merchantId = merchantId }
        })
        discountCategoryRepository.saveAll(categoryIds.map { cid ->
            DiscountCategory().apply { this.discountId = discountId; this.categoryId = cid; this.merchantId = merchantId }
        })
        discountCustomerRepository.saveAll(customerIds.map { cid ->
            DiscountCustomer().apply { this.discountId = discountId; this.customerId = cid; this.merchantId = merchantId }
        })
    }

    private fun Discount.toDetailResponse(): DiscountResponse = toResponse(
        outlets = discountOutletRepository.findByDiscountId(id).map { it.outletId },
        productIds = discountProductRepository.findByDiscountId(id).map { it.productId },
        categoryIds = discountCategoryRepository.findByDiscountId(id).map { it.categoryId },
        customerIds = discountCustomerRepository.findByDiscountId(id).map { it.customerId }
    )

    private fun Discount.toResponse(
        outlets: List<Long>,
        productIds: List<Long>,
        categoryIds: List<Long>,
        customerIds: List<Long>
    ) = DiscountResponse(
        id = id, name = name, code = code, valueType = valueType, value = value,
        maxDiscountAmount = maxDiscountAmount, minPurchase = minPurchase,
        scope = scope, channel = channel, visibility = visibility,
        usageLimit = usageLimit, usagePerCustomer = usagePerCustomer,
        startDate = startDate, endDate = endDate, isActive = isActive,
        outlets = outlets, productIds = productIds, categoryIds = categoryIds,
        customerIds = customerIds, createdDate = createdDate, modifiedDate = modifiedDate
    )
}
