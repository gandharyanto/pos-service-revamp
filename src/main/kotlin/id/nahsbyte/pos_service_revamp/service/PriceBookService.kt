package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PriceBookService(
    private val priceBookRepository: PriceBookRepository,
    private val priceBookItemRepository: PriceBookItemRepository,
    private val priceBookWholesaleTierRepository: PriceBookWholesaleTierRepository,
    private val priceBookOutletRepository: PriceBookOutletRepository
) {

    fun list(merchantId: Long): List<PriceBookResponse> =
        priceBookRepository.findByMerchantIdAndIsActiveTrue(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, priceBookId: Long): PriceBookResponse =
        priceBookRepository.findById(priceBookId)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Price book not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreatePriceBookRequest): PriceBookResponse {
        val priceBook = PriceBook().apply {
            this.merchantId = merchantId
            name = request.name
            type = request.type
            orderTypeId = request.orderTypeId
            categoryId = request.categoryId
            adjustmentType = request.adjustmentType
            adjustmentValue = request.adjustmentValue
            visibility = request.visibility
            isDefault = request.isDefault
            isActive = request.isActive
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = priceBookRepository.save(priceBook)
        if (request.outletIds.isNotEmpty()) bindOutlets(saved.id, merchantId, request.outletIds)
        return saved.toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdatePriceBookRequest): PriceBookResponse {
        val priceBook = priceBookRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }

        priceBook.apply {
            name = request.name
            type = request.type
            orderTypeId = request.orderTypeId
            categoryId = request.categoryId
            adjustmentType = request.adjustmentType
            adjustmentValue = request.adjustmentValue
            visibility = request.visibility
            isDefault = request.isDefault
            isActive = request.isActive
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return priceBookRepository.save(priceBook).toResponse()
    }

    fun delete(merchantId: Long, priceBookId: Long) {
        val priceBook = priceBookRepository.findById(priceBookId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }
        priceBookRepository.delete(priceBook)
    }

    // --- Price Book Item (PRODUCT / ORDER_TYPE type) ---

    @Transactional
    fun addItem(merchantId: Long, username: String, priceBookId: Long, request: AddPriceBookItemRequest): PriceBookItemResponse {
        validateOwnership(merchantId, priceBookId)
        val existing = priceBookItemRepository.findByPriceBookIdAndProductId(priceBookId, request.productId)
        val item = existing.orElse(PriceBookItem()).apply {
            this.priceBookId = priceBookId
            this.productId = request.productId
            this.merchantId = merchantId
            price = request.price
            isActive = request.isActive
            if (id == 0L) {
                createdBy = username
                createdDate = LocalDateTime.now()
            }
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = priceBookItemRepository.save(item)
        return PriceBookItemResponse(saved.id, saved.productId, saved.price, saved.isActive)
    }

    fun deleteItem(merchantId: Long, priceBookId: Long, productId: Long) {
        validateOwnership(merchantId, priceBookId)
        priceBookItemRepository.findByPriceBookIdAndProductId(priceBookId, productId)
            .ifPresent { priceBookItemRepository.delete(it) }
    }

    // --- Wholesale Tiers (WHOLESALE type) ---

    @Transactional
    fun addWholesaleTier(merchantId: Long, username: String, priceBookId: Long, request: AddWholesaleTierRequest): WholesaleTierResponse {
        validateOwnership(merchantId, priceBookId)
        val tier = PriceBookWholesaleTier().apply {
            this.priceBookId = priceBookId
            this.productId = request.productId
            this.merchantId = merchantId
            minQty = request.minQty
            maxQty = request.maxQty
            price = request.price
            displayOrder = request.displayOrder
        }
        val saved = priceBookWholesaleTierRepository.save(tier)
        return saved.toTierResponse()
    }

    @Transactional
    fun updateWholesaleTier(merchantId: Long, priceBookId: Long, request: UpdateWholesaleTierRequest): WholesaleTierResponse {
        validateOwnership(merchantId, priceBookId)
        val tier = priceBookWholesaleTierRepository.findById(request.tierId)
            .filter { it.priceBookId == priceBookId }
            .orElseThrow { ResourceNotFoundException("Tier not found") }

        tier.apply {
            minQty = request.minQty
            maxQty = request.maxQty
            price = request.price
            displayOrder = request.displayOrder
        }
        return priceBookWholesaleTierRepository.save(tier).toTierResponse()
    }

    fun deleteWholesaleTier(merchantId: Long, priceBookId: Long, tierId: Long) {
        validateOwnership(merchantId, priceBookId)
        priceBookWholesaleTierRepository.findById(tierId)
            .filter { it.priceBookId == priceBookId }
            .ifPresent { priceBookWholesaleTierRepository.delete(it) }
    }

    // --- Outlet binding ---

    @Transactional
    fun addOutlets(merchantId: Long, priceBookId: Long, outletIds: List<Long>) {
        validateOwnership(merchantId, priceBookId)
        val existing = priceBookOutletRepository.findByPriceBookId(priceBookId).map { it.outletId }.toSet()
        bindOutlets(priceBookId, merchantId, outletIds.filter { it !in existing })
    }

    fun removeOutlet(merchantId: Long, priceBookId: Long, outletId: Long) {
        validateOwnership(merchantId, priceBookId)
        priceBookOutletRepository.findByPriceBookId(priceBookId)
            .firstOrNull { it.outletId == outletId }
            ?.let { priceBookOutletRepository.delete(it) }
    }

    // --- Helpers ---

    private fun validateOwnership(merchantId: Long, priceBookId: Long) {
        priceBookRepository.findById(priceBookId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }
    }

    private fun bindOutlets(priceBookId: Long, merchantId: Long, outletIds: List<Long>) {
        priceBookOutletRepository.saveAll(outletIds.map { outletId ->
            PriceBookOutlet().apply { this.priceBookId = priceBookId; this.outletId = outletId; this.merchantId = merchantId }
        })
    }

    private fun PriceBook.toResponse(): PriceBookResponse {
        val pbId = id
        val outlets = priceBookOutletRepository.findByPriceBookId(pbId).map { it.outletId }
        val items = priceBookItemRepository.findByPriceBookIdIn(listOf(pbId))
            .map { PriceBookItemResponse(it.id, it.productId, it.price, it.isActive) }
        val tiers = priceBookWholesaleTierRepository.findByPriceBookIdInAndProductId(listOf(pbId), 0)
            .let { priceBookWholesaleTierRepository.findAll().filter { t -> t.priceBookId == pbId } }
            .map { it.toTierResponse() }
        return PriceBookResponse(
            id = pbId, name = name, type = type, orderTypeId = orderTypeId,
            categoryId = categoryId, adjustmentType = adjustmentType, adjustmentValue = adjustmentValue,
            visibility = visibility, isDefault = isDefault, isActive = isActive,
            outlets = outlets, items = items, wholesaleTiers = tiers,
            createdDate = createdDate, modifiedDate = modifiedDate
        )
    }

    private fun PriceBookWholesaleTier.toTierResponse() =
        WholesaleTierResponse(id, productId, minQty, maxQty, price, displayOrder)
}
