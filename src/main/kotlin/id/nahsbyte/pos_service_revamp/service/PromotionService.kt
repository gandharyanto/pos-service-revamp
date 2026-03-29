package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreatePromotionRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePromotionRequest
import id.nahsbyte.pos_service_revamp.dto.response.PromotionResponse
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PromotionService(
    private val promotionRepository: PromotionRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val promotionRewardProductRepository: PromotionRewardProductRepository,
    private val promotionOutletRepository: PromotionOutletRepository
) {

    fun list(merchantId: Long): List<PromotionResponse> =
        promotionRepository.findByMerchantIdAndIsActiveTrue(merchantId).map { it.toResponse() } +
        promotionRepository.findAll().filter { it.merchantId == merchantId && !it.isActive }.map { it.toResponse() }

    fun detail(merchantId: Long, promotionId: Long): PromotionResponse =
        promotionRepository.findById(promotionId)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreatePromotionRequest): PromotionResponse {
        val promotion = Promotion().apply {
            this.merchantId = merchantId
            name = request.name
            promoType = request.promoType
            priority = request.priority
            canCombine = request.canCombine
            valueType = request.valueType
            value = request.value
            minPurchase = request.minPurchase
            buyQty = request.buyQty
            getQty = request.getQty
            allowMultiple = request.allowMultiple
            rewardType = request.rewardType
            rewardValue = request.rewardValue
            buyScope = request.buyScope
            rewardScope = request.rewardScope
            validDays = request.validDays
            channel = request.channel
            visibility = request.visibility
            startDate = request.startDate
            endDate = request.endDate
            isActive = request.isActive
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = promotionRepository.save(promotion)

        if (request.outletIds.isNotEmpty()) bindOutlets(saved.id, merchantId, request.outletIds)
        if (request.buyProductIds.isNotEmpty()) bindBuyProducts(saved.id, merchantId, request.buyProductIds, null)
        if (request.buyCategoryIds.isNotEmpty()) bindBuyProducts(saved.id, merchantId, null, request.buyCategoryIds)
        if (request.rewardProductIds.isNotEmpty()) bindRewardProducts(saved.id, merchantId, request.rewardProductIds, null)
        if (request.rewardCategoryIds.isNotEmpty()) bindRewardProducts(saved.id, merchantId, null, request.rewardCategoryIds)

        return saved.toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdatePromotionRequest): PromotionResponse {
        val promotion = promotionRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }

        promotion.apply {
            name = request.name
            promoType = request.promoType
            priority = request.priority
            canCombine = request.canCombine
            valueType = request.valueType
            value = request.value
            minPurchase = request.minPurchase
            buyQty = request.buyQty
            getQty = request.getQty
            allowMultiple = request.allowMultiple
            rewardType = request.rewardType
            rewardValue = request.rewardValue
            buyScope = request.buyScope
            rewardScope = request.rewardScope
            validDays = request.validDays
            channel = request.channel
            visibility = request.visibility
            startDate = request.startDate
            endDate = request.endDate
            isActive = request.isActive
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return promotionRepository.save(promotion).toResponse()
    }

    fun delete(merchantId: Long, promotionId: Long) {
        val promotion = promotionRepository.findById(promotionId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }
        promotionRepository.delete(promotion)
    }

    // --- Binding management ---

    @Transactional
    fun addBuyProducts(merchantId: Long, promotionId: Long, productIds: List<Long>?, categoryIds: List<Long>?) {
        validateOwnership(merchantId, promotionId)
        if (!productIds.isNullOrEmpty()) bindBuyProducts(promotionId, merchantId, productIds, null)
        if (!categoryIds.isNullOrEmpty()) bindBuyProducts(promotionId, merchantId, null, categoryIds)
    }

    @Transactional
    fun removeBuyProduct(merchantId: Long, promotionId: Long, productId: Long?, categoryId: Long?) {
        validateOwnership(merchantId, promotionId)
        promotionProductRepository.findByPromotionIdIn(listOf(promotionId)).forEach { pp ->
            if ((productId != null && pp.productId == productId) ||
                (categoryId != null && pp.categoryId == categoryId))
                promotionProductRepository.delete(pp)
        }
    }

    @Transactional
    fun addRewardProducts(merchantId: Long, promotionId: Long, productIds: List<Long>?, categoryIds: List<Long>?) {
        validateOwnership(merchantId, promotionId)
        if (!productIds.isNullOrEmpty()) bindRewardProducts(promotionId, merchantId, productIds, null)
        if (!categoryIds.isNullOrEmpty()) bindRewardProducts(promotionId, merchantId, null, categoryIds)
    }

    @Transactional
    fun removeRewardProduct(merchantId: Long, promotionId: Long, productId: Long?, categoryId: Long?) {
        validateOwnership(merchantId, promotionId)
        promotionRewardProductRepository.findByPromotionIdIn(listOf(promotionId)).forEach { rp ->
            if ((productId != null && rp.productId == productId) ||
                (categoryId != null && rp.categoryId == categoryId))
                promotionRewardProductRepository.delete(rp)
        }
    }

    @Transactional
    fun addOutlets(merchantId: Long, promotionId: Long, outletIds: List<Long>) {
        validateOwnership(merchantId, promotionId)
        val existing = promotionOutletRepository.findByPromotionIdIn(listOf(promotionId)).map { it.outletId }.toSet()
        bindOutlets(promotionId, merchantId, outletIds.filter { it !in existing })
    }

    @Transactional
    fun removeOutlet(merchantId: Long, promotionId: Long, outletId: Long) {
        validateOwnership(merchantId, promotionId)
        promotionOutletRepository.findByPromotionIdIn(listOf(promotionId))
            .firstOrNull { it.outletId == outletId }
            ?.let { promotionOutletRepository.delete(it) }
    }

    // --- Helpers ---

    private fun validateOwnership(merchantId: Long, promotionId: Long) {
        promotionRepository.findById(promotionId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }
    }

    private fun bindOutlets(promotionId: Long, merchantId: Long, outletIds: List<Long>) {
        promotionOutletRepository.saveAll(outletIds.map { outletId ->
            PromotionOutlet().apply { this.promotionId = promotionId; this.outletId = outletId; this.merchantId = merchantId }
        })
    }

    private fun bindBuyProducts(promotionId: Long, merchantId: Long, productIds: List<Long>?, categoryIds: List<Long>?) {
        val entries = mutableListOf<PromotionProduct>()
        productIds?.forEach { productId ->
            entries.add(PromotionProduct().apply { this.promotionId = promotionId; this.productId = productId; this.merchantId = merchantId })
        }
        categoryIds?.forEach { categoryId ->
            entries.add(PromotionProduct().apply { this.promotionId = promotionId; this.categoryId = categoryId; this.merchantId = merchantId })
        }
        if (entries.isNotEmpty()) promotionProductRepository.saveAll(entries)
    }

    private fun bindRewardProducts(promotionId: Long, merchantId: Long, productIds: List<Long>?, categoryIds: List<Long>?) {
        val entries = mutableListOf<PromotionRewardProduct>()
        productIds?.forEach { productId ->
            entries.add(PromotionRewardProduct().apply { this.promotionId = promotionId; this.productId = productId; this.merchantId = merchantId })
        }
        categoryIds?.forEach { categoryId ->
            entries.add(PromotionRewardProduct().apply { this.promotionId = promotionId; this.categoryId = categoryId; this.merchantId = merchantId })
        }
        if (entries.isNotEmpty()) promotionRewardProductRepository.saveAll(entries)
    }

    private fun Promotion.toResponse(): PromotionResponse {
        val promoId = id
        val outlets = promotionOutletRepository.findByPromotionIdIn(listOf(promoId)).map { it.outletId }
        val buyProducts = promotionProductRepository.findByPromotionIdIn(listOf(promoId))
        val rewardProducts = promotionRewardProductRepository.findByPromotionIdIn(listOf(promoId))
        return PromotionResponse(
            id = promoId, name = name, promoType = promoType, priority = priority,
            canCombine = canCombine, valueType = valueType, value = value,
            minPurchase = minPurchase, buyQty = buyQty, getQty = getQty,
            allowMultiple = allowMultiple, rewardType = rewardType, rewardValue = rewardValue,
            buyScope = buyScope, rewardScope = rewardScope, validDays = validDays,
            channel = channel, visibility = visibility, startDate = startDate, endDate = endDate,
            isActive = isActive, outlets = outlets,
            buyProducts = buyProducts.mapNotNull { it.productId },
            buyCategories = buyProducts.mapNotNull { it.categoryId },
            rewardProducts = rewardProducts.mapNotNull { it.productId },
            rewardCategories = rewardProducts.mapNotNull { it.categoryId },
            createdDate = createdDate, modifiedDate = modifiedDate
        )
    }
}
