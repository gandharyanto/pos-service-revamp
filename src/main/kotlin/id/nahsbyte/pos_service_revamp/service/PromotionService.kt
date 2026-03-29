package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.PromotionResponse
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PromotionService(
    private val promotionRepository: PromotionRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val promotionRewardProductRepository: PromotionRewardProductRepository,
    private val promotionOutletRepository: PromotionOutletRepository
) {

    fun list(
        merchantId: Long,
        isActive: Boolean? = null,
        promoType: String? = null,
        channel: String? = null,
        canCombine: Boolean? = null
    ): List<PromotionResponse> {
        var promotions = promotionRepository.findByMerchantId(merchantId)
        if (isActive != null) promotions = promotions.filter { it.isActive == isActive }
        if (promoType != null) promotions = promotions.filter { it.promoType == promoType }
        if (channel != null) promotions = promotions.filter { it.channel == channel }
        if (canCombine != null) promotions = promotions.filter { it.canCombine == canCombine }

        if (promotions.isEmpty()) return emptyList()

        val ids = promotions.map { it.id }
        val outletMap = promotionOutletRepository.findByPromotionIdIn(ids).groupBy { it.promotionId }
        val buyProductMap = promotionProductRepository.findByPromotionIdIn(ids).groupBy { it.promotionId }
        val rewardProductMap = promotionRewardProductRepository.findByPromotionIdIn(ids).groupBy { it.promotionId }

        return promotions.map { p ->
            val buyProducts = buyProductMap[p.id] ?: emptyList()
            val rewardProducts = rewardProductMap[p.id] ?: emptyList()
            p.toResponse(
                outlets = outletMap[p.id]?.map { it.outletId } ?: emptyList(),
                buyProducts = buyProducts.mapNotNull { it.productId },
                buyCategories = buyProducts.mapNotNull { it.categoryId },
                rewardProducts = rewardProducts.mapNotNull { it.productId },
                rewardCategories = rewardProducts.mapNotNull { it.categoryId }
            )
        }
    }

    fun detail(merchantId: Long, id: Long): PromotionResponse =
        promotionRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }
            .toDetailResponse()

    @Transactional
    fun create(merchantId: Long, request: CreatePromotionRequest): PromotionResponse {
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
        }
        promotionRepository.save(promotion)
        saveBindings(
            promotion.id, merchantId,
            request.outletIds, request.buyProductIds, request.buyCategoryIds,
            request.rewardProductIds, request.rewardCategoryIds
        )
        return promotion.toDetailResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdatePromotionRequest): PromotionResponse {
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
        }
        promotionRepository.save(promotion)
        promotionOutletRepository.deleteAllByPromotionId(promotion.id)
        promotionProductRepository.deleteAllByPromotionId(promotion.id)
        promotionRewardProductRepository.deleteAllByPromotionId(promotion.id)
        saveBindings(
            promotion.id, merchantId,
            request.outletIds, request.buyProductIds, request.buyCategoryIds,
            request.rewardProductIds, request.rewardCategoryIds
        )
        return promotion.toDetailResponse()
    }

    @Transactional
    fun delete(merchantId: Long, id: Long) {
        val promotion = promotionRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Promotion not found") }
        promotionOutletRepository.deleteAllByPromotionId(id)
        promotionProductRepository.deleteAllByPromotionId(id)
        promotionRewardProductRepository.deleteAllByPromotionId(id)
        promotionRepository.delete(promotion)
    }

    private fun saveBindings(
        promotionId: Long, merchantId: Long,
        outletIds: List<Long>,
        buyProductIds: List<Long>, buyCategoryIds: List<Long>,
        rewardProductIds: List<Long>, rewardCategoryIds: List<Long>
    ) {
        promotionOutletRepository.saveAll(outletIds.map { oid ->
            PromotionOutlet().apply { this.promotionId = promotionId; this.outletId = oid; this.merchantId = merchantId }
        })
        promotionProductRepository.saveAll(
            buyProductIds.map { pid ->
                PromotionProduct().apply { this.promotionId = promotionId; this.productId = pid; this.merchantId = merchantId }
            } + buyCategoryIds.map { cid ->
                PromotionProduct().apply { this.promotionId = promotionId; this.categoryId = cid; this.merchantId = merchantId }
            }
        )
        promotionRewardProductRepository.saveAll(
            rewardProductIds.map { pid ->
                PromotionRewardProduct().apply { this.promotionId = promotionId; this.productId = pid; this.merchantId = merchantId }
            } + rewardCategoryIds.map { cid ->
                PromotionRewardProduct().apply { this.promotionId = promotionId; this.categoryId = cid; this.merchantId = merchantId }
            }
        )
    }

    private fun Promotion.toDetailResponse(): PromotionResponse {
        val buyProducts = promotionProductRepository.findByPromotionId(id)
        val rewardProducts = promotionRewardProductRepository.findByPromotionId(id)
        return toResponse(
            outlets = promotionOutletRepository.findByPromotionId(id).map { it.outletId },
            buyProducts = buyProducts.mapNotNull { it.productId },
            buyCategories = buyProducts.mapNotNull { it.categoryId },
            rewardProducts = rewardProducts.mapNotNull { it.productId },
            rewardCategories = rewardProducts.mapNotNull { it.categoryId }
        )
    }

    private fun Promotion.toResponse(
        outlets: List<Long>,
        buyProducts: List<Long>, buyCategories: List<Long>,
        rewardProducts: List<Long>, rewardCategories: List<Long>
    ) = PromotionResponse(
        id = id, name = name, promoType = promoType, priority = priority,
        canCombine = canCombine, valueType = valueType, value = value,
        minPurchase = minPurchase, buyQty = buyQty, getQty = getQty,
        allowMultiple = allowMultiple, rewardType = rewardType, rewardValue = rewardValue,
        buyScope = buyScope, rewardScope = rewardScope, validDays = validDays,
        channel = channel, visibility = visibility, startDate = startDate, endDate = endDate,
        isActive = isActive, outlets = outlets,
        buyProducts = buyProducts, buyCategories = buyCategories,
        rewardProducts = rewardProducts, rewardCategories = rewardCategories,
        createdDate = createdDate, modifiedDate = modifiedDate
    )
}
