package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PriceBookService(
    private val priceBookRepository: PriceBookRepository,
    private val priceBookItemRepository: PriceBookItemRepository,
    private val priceBookWholesaleTierRepository: PriceBookWholesaleTierRepository,
    private val priceBookOutletRepository: PriceBookOutletRepository
) {

    fun list(
        merchantId: Long,
        isActive: Boolean? = null,
        type: String? = null,
        isDefault: Boolean? = null
    ): List<PriceBookResponse> {
        var books = priceBookRepository.findByMerchantId(merchantId)
        if (isActive != null) books = books.filter { it.isActive == isActive }
        if (type != null) books = books.filter { it.type == type }
        if (isDefault != null) books = books.filter { it.isDefault == isDefault }

        if (books.isEmpty()) return emptyList()

        val ids = books.map { it.id }
        val outletMap = priceBookOutletRepository.findByPriceBookIdIn(ids).groupBy { it.priceBookId }
        val itemMap = priceBookItemRepository.findByPriceBookIdIn(ids).groupBy { it.priceBookId }
        val tierMap = priceBookWholesaleTierRepository.findByPriceBookIdIn(ids).groupBy { it.priceBookId }

        return books.map { pb ->
            pb.toResponse(
                outlets = outletMap[pb.id]?.map { it.outletId } ?: emptyList(),
                items = itemMap[pb.id]?.map { PriceBookItemResponse(it.id, it.productId, it.price, it.isActive) } ?: emptyList(),
                tiers = tierMap[pb.id]?.map { it.toTierResponse() } ?: emptyList()
            )
        }
    }

    fun detail(merchantId: Long, id: Long): PriceBookResponse =
        priceBookRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }
            .toDetailResponse()

    @Transactional
    fun create(merchantId: Long, request: CreatePriceBookRequest): PriceBookResponse {
        if (request.isDefault) clearDefault(merchantId, request.type)
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
        }
        priceBookRepository.save(priceBook)
        saveBindings(priceBook.id, merchantId, request.outletIds, request.items, request.wholesaleTiers)
        return priceBook.toDetailResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdatePriceBookRequest): PriceBookResponse {
        val priceBook = priceBookRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }
        if (request.isDefault && !priceBook.isDefault) clearDefault(merchantId, request.type)
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
        }
        priceBookRepository.save(priceBook)
        priceBookOutletRepository.deleteAllByPriceBookId(priceBook.id)
        priceBookItemRepository.deleteAllByPriceBookId(priceBook.id)
        priceBookWholesaleTierRepository.deleteAllByPriceBookId(priceBook.id)
        saveBindings(priceBook.id, merchantId, request.outletIds, request.items, request.wholesaleTiers)
        return priceBook.toDetailResponse()
    }

    @Transactional
    fun delete(merchantId: Long, id: Long) {
        val priceBook = priceBookRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Price book not found") }
        priceBookOutletRepository.deleteAllByPriceBookId(id)
        priceBookItemRepository.deleteAllByPriceBookId(id)
        priceBookWholesaleTierRepository.deleteAllByPriceBookId(id)
        priceBookRepository.delete(priceBook)
    }

    private fun clearDefault(merchantId: Long, type: String) {
        priceBookRepository.findByMerchantIdAndTypeAndIsDefaultTrue(merchantId, type).forEach { pb ->
            pb.isDefault = false
            priceBookRepository.save(pb)
        }
    }

    private fun saveBindings(
        priceBookId: Long, merchantId: Long,
        outletIds: List<Long>,
        items: List<PriceBookItemData>,
        tiers: List<PriceBookTierData>
    ) {
        priceBookOutletRepository.saveAll(outletIds.map { oid ->
            PriceBookOutlet().apply { this.priceBookId = priceBookId; this.outletId = oid; this.merchantId = merchantId }
        })
        priceBookItemRepository.saveAll(items.map { item ->
            PriceBookItem().apply {
                this.priceBookId = priceBookId; this.productId = item.productId
                this.merchantId = merchantId; price = item.price; isActive = item.isActive
            }
        })
        priceBookWholesaleTierRepository.saveAll(tiers.map { tier ->
            PriceBookWholesaleTier().apply {
                this.priceBookId = priceBookId; this.productId = tier.productId
                this.merchantId = merchantId; minQty = tier.minQty; maxQty = tier.maxQty
                price = tier.price; displayOrder = tier.displayOrder
            }
        })
    }

    private fun PriceBook.toDetailResponse(): PriceBookResponse = toResponse(
        outlets = priceBookOutletRepository.findByPriceBookId(id).map { it.outletId },
        items = priceBookItemRepository.findByPriceBookId(id).map { PriceBookItemResponse(it.id, it.productId, it.price, it.isActive) },
        tiers = priceBookWholesaleTierRepository.findByPriceBookId(id).map { it.toTierResponse() }
    )

    private fun PriceBook.toResponse(
        outlets: List<Long>,
        items: List<PriceBookItemResponse>,
        tiers: List<WholesaleTierResponse>
    ) = PriceBookResponse(
        id = id, name = name, type = type, orderTypeId = orderTypeId,
        categoryId = categoryId, adjustmentType = adjustmentType, adjustmentValue = adjustmentValue,
        visibility = visibility, isDefault = isDefault, isActive = isActive,
        outlets = outlets, items = items, wholesaleTiers = tiers,
        createdDate = createdDate, modifiedDate = modifiedDate
    )

    private fun PriceBookWholesaleTier.toTierResponse() =
        WholesaleTierResponse(id, productId, minQty, maxQty, price, displayOrder)
}
