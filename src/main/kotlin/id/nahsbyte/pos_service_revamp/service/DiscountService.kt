package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.response.DiscountResponse
import id.nahsbyte.pos_service_revamp.entity.Discount
import id.nahsbyte.pos_service_revamp.entity.DiscountCategory
import id.nahsbyte.pos_service_revamp.entity.DiscountOutlet
import id.nahsbyte.pos_service_revamp.entity.DiscountProduct
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DiscountService(
    private val discountRepository: DiscountRepository,
    private val discountProductRepository: DiscountProductRepository,
    private val discountCategoryRepository: DiscountCategoryRepository,
    private val discountOutletRepository: DiscountOutletRepository
) {

    fun list(merchantId: Long): List<DiscountResponse> =
        discountRepository.findAll()
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }

    fun detail(merchantId: Long, discountId: Long): DiscountResponse =
        discountRepository.findById(discountId)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Discount not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreateDiscountRequest): DiscountResponse {
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
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = discountRepository.save(discount)

        if (request.outletIds.isNotEmpty()) bindOutlets(saved.id, merchantId, request.outletIds)
        if (request.productIds.isNotEmpty()) bindProducts(saved.id, merchantId, request.productIds)
        if (request.categoryIds.isNotEmpty()) bindCategories(saved.id, merchantId, request.categoryIds)

        return saved.toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdateDiscountRequest): DiscountResponse {
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
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return discountRepository.save(discount).toResponse()
    }

    fun delete(merchantId: Long, discountId: Long) {
        val discount = discountRepository.findById(discountId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Discount not found") }
        discountRepository.delete(discount)
    }

    // --- Binding management ---

    @Transactional
    fun addProducts(merchantId: Long, discountId: Long, productIds: List<Long>) {
        validateOwnership(merchantId, discountId)
        val existing = discountProductRepository.findByDiscountId(discountId).map { it.productId }.toSet()
        bindProducts(discountId, merchantId, productIds.filter { it !in existing })
    }

    @Transactional
    fun removeProduct(merchantId: Long, discountId: Long, productId: Long) {
        validateOwnership(merchantId, discountId)
        discountProductRepository.findByDiscountId(discountId)
            .firstOrNull { it.productId == productId }
            ?.let { discountProductRepository.delete(it) }
    }

    @Transactional
    fun addCategories(merchantId: Long, discountId: Long, categoryIds: List<Long>) {
        validateOwnership(merchantId, discountId)
        val existing = discountCategoryRepository.findByDiscountId(discountId).map { it.categoryId }.toSet()
        bindCategories(discountId, merchantId, categoryIds.filter { it !in existing })
    }

    @Transactional
    fun removeCategory(merchantId: Long, discountId: Long, categoryId: Long) {
        validateOwnership(merchantId, discountId)
        discountCategoryRepository.findByDiscountId(discountId)
            .firstOrNull { it.categoryId == categoryId }
            ?.let { discountCategoryRepository.delete(it) }
    }

    @Transactional
    fun addOutlets(merchantId: Long, discountId: Long, outletIds: List<Long>) {
        validateOwnership(merchantId, discountId)
        val existing = discountOutletRepository.findByDiscountId(discountId).map { it.outletId }.toSet()
        bindOutlets(discountId, merchantId, outletIds.filter { it !in existing })
    }

    @Transactional
    fun removeOutlet(merchantId: Long, discountId: Long, outletId: Long) {
        validateOwnership(merchantId, discountId)
        discountOutletRepository.findByDiscountId(discountId)
            .firstOrNull { it.outletId == outletId }
            ?.let { discountOutletRepository.delete(it) }
    }

    // --- Helpers ---

    private fun validateOwnership(merchantId: Long, discountId: Long) {
        discountRepository.findById(discountId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Discount not found") }
    }

    private fun bindOutlets(discountId: Long, merchantId: Long, outletIds: List<Long>) {
        discountOutletRepository.saveAll(outletIds.map { outletId ->
            DiscountOutlet().apply { this.discountId = discountId; this.outletId = outletId; this.merchantId = merchantId }
        })
    }

    private fun bindProducts(discountId: Long, merchantId: Long, productIds: List<Long>) {
        discountProductRepository.saveAll(productIds.map { productId ->
            DiscountProduct().apply { this.discountId = discountId; this.productId = productId; this.merchantId = merchantId }
        })
    }

    private fun bindCategories(discountId: Long, merchantId: Long, categoryIds: List<Long>) {
        discountCategoryRepository.saveAll(categoryIds.map { categoryId ->
            DiscountCategory().apply { this.discountId = discountId; this.categoryId = categoryId; this.merchantId = merchantId }
        })
    }

    private fun Discount.toResponse(): DiscountResponse {
        val outlets = discountOutletRepository.findByDiscountId(id).map { it.outletId }
        val products = discountProductRepository.findByDiscountId(id).map { it.productId }
        val categories = discountCategoryRepository.findByDiscountId(id).map { it.categoryId }
        return DiscountResponse(
            id = id, name = name, code = code, valueType = valueType, value = value,
            maxDiscountAmount = maxDiscountAmount, minPurchase = minPurchase,
            scope = scope, channel = channel, visibility = visibility,
            usageLimit = usageLimit, usagePerCustomer = usagePerCustomer,
            startDate = startDate, endDate = endDate, isActive = isActive,
            outlets = outlets, productIds = products, categoryIds = categories,
            createdDate = createdDate, modifiedDate = modifiedDate
        )
    }
}
