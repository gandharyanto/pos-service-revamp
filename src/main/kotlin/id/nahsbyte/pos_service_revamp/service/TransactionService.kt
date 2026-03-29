package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.*
import id.nahsbyte.pos_service_revamp.dto.response.MismatchDetail
import id.nahsbyte.pos_service_revamp.exception.AmountMismatchException
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class TransactionService(
    private val transactionRepository: TransactionRepository,
    private val transactionItemRepository: TransactionItemRepository,
    private val paymentRepository: PaymentRepository,
    private val transactionQueueRepository: TransactionQueueRepository,
    private val outletRepository: OutletRepository,
    private val paymentSettingRepository: PaymentSettingRepository,
    private val taxRepository: TaxRepository,
    private val productRepository: ProductRepository,
    // Price Book
    private val priceBookRepository: PriceBookRepository,
    private val priceBookItemRepository: PriceBookItemRepository,
    private val priceBookWholesaleTierRepository: PriceBookWholesaleTierRepository,
    // Promotion
    private val promotionRepository: PromotionRepository,
    private val promotionProductRepository: PromotionProductRepository,
    private val promotionRewardProductRepository: PromotionRewardProductRepository,
    private val promotionOutletRepository: PromotionOutletRepository,
    private val promotionCustomerRepository: PromotionCustomerRepository,
    // Discount
    private val discountRepository: DiscountRepository,
    private val discountProductRepository: DiscountProductRepository,
    private val discountCategoryRepository: DiscountCategoryRepository,
    private val discountOutletRepository: DiscountOutletRepository,
    private val discountCustomerRepository: DiscountCustomerRepository
) {

    fun list(
        merchantId: Long,
        page: Int,
        size: Int,
        startDate: String,
        endDate: String,
        sortBy: String,
        sortType: String
    ): Page<TransactionSummaryResponse> {
        val sort = if (sortType.uppercase() == "DESC") Sort.by(sortBy).descending() else Sort.by(sortBy).ascending()
        val pageable = PageRequest.of(page, size, sort)
        val start = LocalDate.parse(startDate).atStartOfDay()
        val end = LocalDate.parse(endDate).atTime(23, 59, 59)

        return transactionRepository.findAllByMerchantIdAndCreatedDateBetween(merchantId, start, end, pageable)
            .map {
                TransactionSummaryResponse(
                    id = it.id,
                    trxId = it.trxId,
                    status = it.status,
                    paymentMethod = it.paymentMethod,
                    totalAmount = it.totalAmount,
                    createdDate = it.createdDate
                )
            }
    }

    fun detail(merchantId: Long, transactionId: Long): TransactionDetailResponse {
        val trx = transactionRepository.findByIdAndMerchantId(transactionId, merchantId)
            .orElseThrow { ResourceNotFoundException("Transaction not found") }

        val queue = trx.queueId?.let { transactionQueueRepository.findById(it).orElse(null) }
        val items = transactionItemRepository.findAllByTransactionId(trx.id)
        val payments = paymentRepository.findAllByTransactionId(trx.id)

        return trx.toDetailResponse(queue?.queueNumber, items, payments)
    }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreateTransactionRequest): CreateTransactionResponse {
        val outlet = outletRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
            .orElseGet { outletRepository.findAllByMerchantId(merchantId).firstOrNull() }
            ?: throw ResourceNotFoundException("No outlet found for merchant")

        val paymentSetting = paymentSettingRepository.findByMerchantId(merchantId).orElse(null)

        // -----------------------------------------------------------------------
        // LOAD PRODUCTS AND TAXES FROM DB
        // -----------------------------------------------------------------------
        val productIds = request.transactionItems.map { it.productId }.distinct()
        val productMap: Map<Long, Product> = productRepository.findAllById(productIds)
            .also { products ->
                products.forEach { p ->
                    require(p.merchantId == merchantId) { "Product ${p.id} does not belong to this merchant" }
                    require(p.deletedDate == null) { "Product '${p.name}' has been deleted" }
                }
                val missingIds = productIds - products.map { it.id }.toSet()
                if (missingIds.isNotEmpty()) throw ResourceNotFoundException("Products not found: $missingIds")
            }
            .associateBy { it.id }

        val taxIds = productMap.values.filter { it.isTaxable && it.taxId != null }.mapNotNull { it.taxId }.distinct()
        val taxMap: Map<Long, Tax> = if (taxIds.isNotEmpty()) taxRepository.findAllById(taxIds).associateBy { it.id }
        else emptyMap()

        val now = LocalDateTime.now()
        val channel = "POS"

        // -----------------------------------------------------------------------
        // LAYER 1 — PRICE BOOK: resolve effective price per item
        // -----------------------------------------------------------------------
        val activePriceBooks = priceBookRepository.findByMerchantIdAndIsActiveTrue(merchantId)
        val priceBookIds = activePriceBooks.map { it.id }

        // Pre-load price book items and wholesale tiers for all products
        val allPriceBookItems: List<PriceBookItem> =
            if (priceBookIds.isNotEmpty()) priceBookItemRepository.findByPriceBookIdIn(priceBookIds)
            else emptyList()

        val resolvedItems: List<ResolvedItem> = request.transactionItems.map { itemReq ->
            val product = productMap[itemReq.productId]!!
            val tax = if (product.isTaxable) product.taxId?.let { taxMap[it] } else null

            val wholesaleTiers = if (priceBookIds.isNotEmpty())
                priceBookWholesaleTierRepository.findByPriceBookIdInAndProductId(priceBookIds, product.id)
            else emptyList()

            val (effectivePrice, priceBookItemId) = resolveEffectivePrice(
                product = product,
                qty = itemReq.qty,
                orderTypeId = request.orderTypeId,
                activePriceBooks = activePriceBooks,
                allPriceBookItems = allPriceBookItems,
                wholesaleTiers = wholesaleTiers
            )

            ResolvedItem(
                product = product,
                tax = tax,
                qty = itemReq.qty,
                effectivePrice = effectivePrice,
                priceBookItemId = priceBookItemId
            )
        }

        // grossSubTotal = Σ(effectivePrice × qty) — setelah price book, sebelum promo/diskon
        val grossSubTotal = resolvedItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.effectivePrice.multiply(BigDecimal(item.qty)))
        }

        // -----------------------------------------------------------------------
        // LAYER 2 — PROMOTIONS: auto-apply
        // -----------------------------------------------------------------------
        val promoResult = resolvePromotions(
            resolvedItems = resolvedItems,
            grossSubTotal = grossSubTotal,
            merchantId = merchantId,
            outletId = outlet.id,
            customerId = request.customerId,
            channel = channel,
            now = now
        )

        // -----------------------------------------------------------------------
        // LAYER 3 — DISCOUNT CODE: validate and apply
        // -----------------------------------------------------------------------
        val discountCodeResult = resolveDiscountCode(
            code = request.discountCode,
            resolvedItems = resolvedItems,
            grossSubTotal = grossSubTotal,
            merchantId = merchantId,
            outletId = outlet.id,
            customerId = request.customerId,
            channel = channel,
            now = now
        )

        // netSubTotal = base untuk kalkulasi tax, SC, rounding
        val netSubTotal = grossSubTotal
            .subtract(promoResult.totalDiscount)
            .subtract(discountCodeResult.totalDiscount)
            .coerceAtLeast(BigDecimal.ZERO)

        // -----------------------------------------------------------------------
        // CALCULATE TAX, SC, ROUNDING
        // -----------------------------------------------------------------------
        val calculated = calculateAmounts(
            netSubTotal = netSubTotal,
            grossSubTotal = grossSubTotal,
            promoDiscount = promoResult.totalDiscount,
            codeDiscount = discountCodeResult.totalDiscount,
            setting = paymentSetting
        )

        val cashChange = request.cashTendered?.subtract(calculated.totalAmount)
            ?.let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it }

        // -----------------------------------------------------------------------
        // VALIDATE REQUEST vs SERVER-CALCULATED
        // -----------------------------------------------------------------------
        validateAgainstRequest(request, resolvedItems, calculated, cashChange, promoResult, discountCodeResult)

        if (request.paymentMethod.uppercase() == "CASH") {
            val tendered = request.cashTendered
                ?: throw IllegalArgumentException("cashTendered is required for CASH payment")
            require(tendered >= calculated.totalAmount) {
                "Cash tendered ($tendered) is less than total amount (${calculated.totalAmount})"
            }
        }

        // -----------------------------------------------------------------------
        // QUEUE
        // -----------------------------------------------------------------------
        val today = LocalDate.now()
        val queueCount = transactionQueueRepository.countByMerchantIdAndOutletIdAndQueueDate(merchantId, outlet.id, today)
        val queueNumber = request.queueNumber?.let { String.format("%03d", it) }
            ?: String.format("%03d", queueCount + 1)

        val queue = TransactionQueue().apply {
            this.merchantId = merchantId
            outletId = outlet.id
            this.queueNumber = queueNumber
            queueDate = today
            status = "OPEN"
            createdBy = username
            createdDate = now
        }
        val savedQueue = transactionQueueRepository.save(queue)

        val trxId = "TRX-${UUID.randomUUID().toString().take(8).uppercase()}"

        // -----------------------------------------------------------------------
        // SAVE TRANSACTION
        // -----------------------------------------------------------------------
        val transaction = Transaction().apply {
            this.merchantId = merchantId
            outletId = outlet.id
            this.username = username
            this.trxId = trxId
            status = "PAID"
            paymentMethod = request.paymentMethod
            priceIncludeTax = calculated.priceIncludeTax
            grossAmount = calculated.grossSubTotal
            promoAmount = calculated.promoDiscount
            discountAmount = calculated.codeDiscount
            subTotal = calculated.netSubTotal
            totalServiceCharge = calculated.totalServiceCharge
            serviceChargePercentage = paymentSetting?.serviceChargePercentage
            serviceChargeAmount = paymentSetting?.serviceChargeAmount
            totalTax = calculated.totalTax
            taxPercentage = paymentSetting?.taxPercentage
            taxName = paymentSetting?.taxName
            totalRounding = calculated.totalRounding
            roundingType = paymentSetting?.roundingType
            roundingTarget = paymentSetting?.roundingTarget?.toString()
            totalAmount = calculated.totalAmount
            cashTendered = request.cashTendered
            this.cashChange = cashChange
            customerId = request.customerId
            orderTypeId = request.orderTypeId
            discountId = discountCodeResult.discountId
            promoId = promoResult.primaryPromoId
            queueId = savedQueue.id
            createdBy = username
            createdDate = now
            modifiedBy = username
            modifiedDate = now
        }
        val savedTrx = transactionRepository.save(transaction)

        // -----------------------------------------------------------------------
        // SAVE TRANSACTION ITEMS
        // -----------------------------------------------------------------------
        val items = resolvedItems.mapIndexed { index, resolved ->
            val itemTotal = resolved.effectivePrice.multiply(BigDecimal(resolved.qty))
            val itemTaxAmount = resolved.tax?.let {
                itemTotal.multiply(it.percentage).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            }
            val itemPromoDiscount = promoResult.itemDiscounts[index] ?: BigDecimal.ZERO
            val itemCodeDiscount = discountCodeResult.itemDiscounts[index] ?: BigDecimal.ZERO
            val totalItemDiscount = itemPromoDiscount.add(itemCodeDiscount)

            TransactionItem().apply {
                transactionId = savedTrx.id
                productId = resolved.product.id
                productName = resolved.product.name
                originalPrice = resolved.product.price          // harga asal sebelum price book
                price = resolved.effectivePrice                  // harga efektif setelah price book
                qty = resolved.qty
                totalPrice = itemTotal
                discountAmount = if (totalItemDiscount > BigDecimal.ZERO) totalItemDiscount else null
                taxId = resolved.tax?.id
                taxName = resolved.tax?.name
                taxPercentage = resolved.tax?.percentage
                taxAmount = itemTaxAmount
                priceBookItemId = resolved.priceBookItemId
                createdBy = username
                createdDate = now
            }
        }
        transactionItemRepository.saveAll(items)

        return CreateTransactionResponse(
            id = savedTrx.id,
            trxId = savedTrx.trxId,
            queueNumber = savedQueue.queueNumber
        )
    }

    // =========================================================================
    // LAYER 1 — PRICE BOOK
    // =========================================================================

    /**
     * Tentukan harga efektif per item berdasarkan price book aktif.
     *
     * Prioritas:
     *   1. WHOLESALE  — tier qty cocok → override harga satuan
     *   2. ORDER_TYPE — price book untuk order type ini → lihat price_book_item
     *   3. PRODUCT    — price book tipe PRODUCT → lihat price_book_item
     *   4. CATEGORY   — price book tipe CATEGORY → hitung adjustment
     *   5. Default    — product.price
     */
    private fun resolveEffectivePrice(
        product: Product,
        qty: Int,
        orderTypeId: Long?,
        activePriceBooks: List<PriceBook>,
        allPriceBookItems: List<PriceBookItem>,
        wholesaleTiers: List<PriceBookWholesaleTier>
    ): Pair<BigDecimal, Long?> {

        // 1. WHOLESALE
        val matchingTier = wholesaleTiers.firstOrNull { tier ->
            tier.minQty <= qty && (tier.maxQty == null || tier.maxQty!! >= qty)
        }
        if (matchingTier != null) return Pair(matchingTier.price, null)

        // 2. ORDER_TYPE
        if (orderTypeId != null) {
            val orderTypePriceBook = activePriceBooks.firstOrNull { it.type == "ORDER_TYPE" && it.orderTypeId == orderTypeId }
            if (orderTypePriceBook != null) {
                val item = allPriceBookItems.firstOrNull { it.priceBookId == orderTypePriceBook.id && it.productId == product.id }
                if (item != null && item.isActive) return Pair(item.price, item.id)
            }
        }

        // 3. PRODUCT
        val productPriceBook = activePriceBooks.firstOrNull { it.type == "PRODUCT" && it.isDefault }
            ?: activePriceBooks.firstOrNull { it.type == "PRODUCT" }
        if (productPriceBook != null) {
            val item = allPriceBookItems.firstOrNull { it.priceBookId == productPriceBook.id && it.productId == product.id }
            if (item != null && item.isActive) return Pair(item.price, item.id)
        }

        // 4. CATEGORY
        val productCategoryIds = product.categories.map { it.id }.toSet()
        if (productCategoryIds.isNotEmpty()) {
            val categoryPriceBook = activePriceBooks.firstOrNull { it.type == "CATEGORY" && it.categoryId in productCategoryIds }
            if (categoryPriceBook != null) {
                val adjustedPrice = applyCategoryAdjustment(product.price, categoryPriceBook)
                if (adjustedPrice != null) return Pair(adjustedPrice, null)
            }
        }

        // 5. Default
        return Pair(product.price, null)
    }

    private fun applyCategoryAdjustment(basePrice: BigDecimal, priceBook: PriceBook): BigDecimal? {
        val adjType = priceBook.adjustmentType ?: return null
        val adjValue = priceBook.adjustmentValue ?: return null
        return when (adjType) {
            "PERCENTAGE_OFF" -> basePrice.subtract(
                basePrice.multiply(adjValue).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            ).coerceAtLeast(BigDecimal.ZERO)
            "AMOUNT_OFF" -> basePrice.subtract(adjValue).coerceAtLeast(BigDecimal.ZERO)
            "SPECIAL_PRICE" -> adjValue
            else -> null
        }
    }

    // =========================================================================
    // LAYER 2 — PROMOTIONS
    // =========================================================================

    private data class PromotionResult(
        val totalDiscount: BigDecimal,
        val itemDiscounts: Map<Int, BigDecimal>,    // item index → diskon dari promosi
        val primaryPromoId: Long?
    )

    private fun resolvePromotions(
        resolvedItems: List<ResolvedItem>,
        grossSubTotal: BigDecimal,
        merchantId: Long,
        outletId: Long,
        customerId: Long?,
        channel: String,
        now: LocalDateTime
    ): PromotionResult {
        val dayOfWeek = now.dayOfWeek.name.take(3).uppercase() // MON, TUE, ...

        // Load semua promosi aktif untuk merchant ini
        val allPromos = promotionRepository.findByMerchantIdAndIsActiveTrue(merchantId)

        // Filter: date range, valid_days, channel, outlet
        val eligiblePromos = allPromos.filter { promo ->
            val inDateRange = (promo.startDate == null || !now.isBefore(promo.startDate!!)) &&
                              (promo.endDate == null || !now.isAfter(promo.endDate!!))
            val validDay = promo.validDays == null || dayOfWeek in promo.validDays!!.split(",").map { it.trim() }
            val validChannel = promo.channel == "BOTH" || promo.channel == channel
            val validOutlet = promo.visibility == "ALL_OUTLET" || isPromoInOutlet(promo.id, outletId)
            val validCustomer = customerId == null || isPromoEligibleForCustomer(promo.id, customerId)
            inDateRange && validDay && validChannel && validOutlet && validCustomer
        }

        if (eligiblePromos.isEmpty()) return PromotionResult(BigDecimal.ZERO, emptyMap(), null)

        // Load promotion_product untuk semua eligible promos
        val promoIds = eligiblePromos.map { it.id }
        val promoProductsMap = promotionProductRepository.findByPromotionIdIn(promoIds)
            .groupBy { it.promotionId }
        val rewardProductsMap = promotionRewardProductRepository.findByPromotionIdIn(promoIds)
            .groupBy { it.promotionId }

        // Evaluasi kondisi masing-masing promosi
        data class EvaluatedPromo(val promo: Promotion, val discountAmount: BigDecimal, val itemDiscounts: Map<Int, BigDecimal>)

        val evaluated = eligiblePromos.mapNotNull { promo ->
            val buyConditionItems = promoProductsMap[promo.id] ?: emptyList()
            val rewardConditionItems = rewardProductsMap[promo.id] ?: emptyList()

            val result = when (promo.promoType) {
                "DISCOUNT_BY_ORDER" -> evaluateDiscountByOrder(promo, grossSubTotal)
                "DISCOUNT_BY_ITEM_SUBTOTAL" -> evaluateDiscountByItemSubtotal(promo, resolvedItems, buyConditionItems)
                "BUY_X_GET_Y" -> evaluateBuyXGetY(promo, resolvedItems, buyConditionItems, rewardConditionItems)
                else -> null
            } ?: return@mapNotNull null

            EvaluatedPromo(promo, result.first, result.second)
        }

        if (evaluated.isEmpty()) return PromotionResult(BigDecimal.ZERO, emptyMap(), null)

        // Stacking resolution
        val exclusive = evaluated.filter { !it.promo.canCombine }.sortedBy { it.promo.priority }
        val combinable = evaluated.filter { it.promo.canCombine }

        // Dari exclusive ambil satu pemenang (priority terkecil = paling prioritas)
        val winner = exclusive.firstOrNull()
        val finalPromos = listOfNotNull(winner) + combinable

        // Gabungkan semua diskon
        val totalDiscount = finalPromos.fold(BigDecimal.ZERO) { acc, ep -> acc.add(ep.discountAmount) }
        val mergedItemDiscounts = mutableMapOf<Int, BigDecimal>()
        finalPromos.forEach { ep ->
            ep.itemDiscounts.forEach { (idx, amt) ->
                mergedItemDiscounts[idx] = (mergedItemDiscounts[idx] ?: BigDecimal.ZERO).add(amt)
            }
        }
        val primaryPromoId = winner?.promo?.id ?: combinable.firstOrNull()?.promo?.id

        return PromotionResult(totalDiscount, mergedItemDiscounts, primaryPromoId)
    }

    /** DISCOUNT_BY_ORDER: diskon ke total jika grossSubTotal >= minPurchase */
    private fun evaluateDiscountByOrder(
        promo: Promotion,
        grossSubTotal: BigDecimal
    ): Pair<BigDecimal, Map<Int, BigDecimal>>? {
        if (promo.minPurchase != null && grossSubTotal < promo.minPurchase!!) return null
        val discount = computeDiscount(promo.value ?: return null, promo.valueType ?: return null, grossSubTotal)
        return Pair(discount, emptyMap())
    }

    /** DISCOUNT_BY_ITEM_SUBTOTAL: diskon ke item jika subtotal item >= minPurchase */
    private fun evaluateDiscountByItemSubtotal(
        promo: Promotion,
        resolvedItems: List<ResolvedItem>,
        buyConditionItems: List<PromotionProduct>
    ): Pair<BigDecimal, Map<Int, BigDecimal>>? {
        val qualifyingIndices = resolvedItems.indices.filter { idx ->
            isItemMatchingPromoScope(resolvedItems[idx], promo.buyScope, buyConditionItems)
        }
        if (qualifyingIndices.isEmpty()) return null

        val qualifyingSubtotal = qualifyingIndices.fold(BigDecimal.ZERO) { acc, idx ->
            acc.add(resolvedItems[idx].effectivePrice.multiply(BigDecimal(resolvedItems[idx].qty)))
        }

        if (promo.minPurchase != null && qualifyingSubtotal < promo.minPurchase!!) return null

        val totalDiscount = computeDiscount(promo.value ?: return null, promo.valueType ?: return null, qualifyingSubtotal)

        // Distribusikan diskon proporsional ke masing-masing item yang qualify
        val itemDiscounts = mutableMapOf<Int, BigDecimal>()
        qualifyingIndices.forEach { idx ->
            val itemSubtotal = resolvedItems[idx].effectivePrice.multiply(BigDecimal(resolvedItems[idx].qty))
            val proportion = itemSubtotal.divide(qualifyingSubtotal, 10, RoundingMode.HALF_UP)
            itemDiscounts[idx] = totalDiscount.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
        }

        return Pair(totalDiscount, itemDiscounts)
    }

    /** BUY_X_GET_Y: beli X item tertentu, dapatkan reward Y */
    private fun evaluateBuyXGetY(
        promo: Promotion,
        resolvedItems: List<ResolvedItem>,
        buyConditionItems: List<PromotionProduct>,
        rewardConditionItems: List<PromotionRewardProduct>
    ): Pair<BigDecimal, Map<Int, BigDecimal>>? {
        val buyQty = promo.buyQty ?: return null
        val getQty = promo.getQty ?: return null

        // Hitung total qty item yang memenuhi syarat beli
        val qualifyingQty = resolvedItems.indices
            .filter { idx -> isItemMatchingPromoScope(resolvedItems[idx], promo.buyScope, buyConditionItems) }
            .sumOf { resolvedItems[it].qty }

        if (qualifyingQty < buyQty) return null

        // Berapa kali reward berlaku
        val multiples = if (promo.allowMultiple) qualifyingQty / buyQty else 1
        val totalRewardQty = multiples * getQty

        // Cari item reward yang ada di cart (sorted by price DESC — diskon item paling mahal)
        val rewardIndices = resolvedItems.indices
            .filter { idx ->
                val item = resolvedItems[idx]
                val rewardProductIds = rewardConditionItems.mapNotNull { it.productId }.toSet()
                val rewardCategoryIds = rewardConditionItems.mapNotNull { it.categoryId }.toSet()
                when (promo.rewardScope) {
                    "PRODUCT" -> item.product.id in rewardProductIds
                    "CATEGORY" -> item.product.categories.any { it.id in rewardCategoryIds }
                    else -> true // semua item bisa jadi reward
                }
            }
            .sortedByDescending { resolvedItems[it].effectivePrice }

        if (rewardIndices.isEmpty()) return null

        // Terapkan reward ke item-item reward (maks totalRewardQty unit)
        val itemDiscounts = mutableMapOf<Int, BigDecimal>()
        var remainingRewardQty = totalRewardQty

        for (idx in rewardIndices) {
            if (remainingRewardQty <= 0) break
            val item = resolvedItems[idx]
            val applicableQty = minOf(remainingRewardQty, item.qty)
            val rewardValue = promo.rewardValue ?: BigDecimal.ZERO

            val discountPerUnit = when (promo.rewardType) {
                "FREE" -> item.effectivePrice
                "PERCENTAGE" -> item.effectivePrice.multiply(rewardValue).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                "AMOUNT" -> rewardValue.min(item.effectivePrice)
                "FIXED_PRICE" -> item.effectivePrice.subtract(rewardValue).coerceAtLeast(BigDecimal.ZERO)
                else -> BigDecimal.ZERO
            }

            val itemDiscount = discountPerUnit.multiply(BigDecimal(applicableQty))
            itemDiscounts[idx] = (itemDiscounts[idx] ?: BigDecimal.ZERO).add(itemDiscount)
            remainingRewardQty -= applicableQty
        }

        val totalDiscount = itemDiscounts.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        return Pair(totalDiscount, itemDiscounts)
    }

    private fun isItemMatchingPromoScope(
        item: ResolvedItem,
        scope: String?,
        promoProducts: List<PromotionProduct>
    ): Boolean {
        val productIds = promoProducts.mapNotNull { it.productId }.toSet()
        val categoryIds = promoProducts.mapNotNull { it.categoryId }.toSet()
        return when (scope) {
            "PRODUCT" -> item.product.id in productIds
            "CATEGORY" -> item.product.categories.any { it.id in categoryIds }
            else -> true // ALL
        }
    }

    private fun isPromoInOutlet(promotionId: Long, outletId: Long): Boolean {
        val outlets = promotionOutletRepository.findByPromotionIdIn(listOf(promotionId))
        return outlets.any { it.outletId == outletId }
    }

    private fun isPromoEligibleForCustomer(promotionId: Long, customerId: Long): Boolean {
        val customers = promotionCustomerRepository.findByPromotionIdInAndCustomerId(listOf(promotionId), customerId)
        return customers.isNotEmpty()
    }

    // =========================================================================
    // LAYER 3 — DISCOUNT CODE
    // =========================================================================

    private data class DiscountCodeResult(
        val discountId: Long?,
        val totalDiscount: BigDecimal,
        val itemDiscounts: Map<Int, BigDecimal>
    )

    private fun resolveDiscountCode(
        code: String?,
        resolvedItems: List<ResolvedItem>,
        grossSubTotal: BigDecimal,
        merchantId: Long,
        outletId: Long,
        customerId: Long?,
        channel: String,
        now: LocalDateTime
    ): DiscountCodeResult {
        val empty = DiscountCodeResult(null, BigDecimal.ZERO, emptyMap())
        if (code.isNullOrBlank()) return empty

        val discount = discountRepository.findByCodeAndMerchantIdAndIsActiveTrue(code, merchantId)
            .orElseThrow { IllegalArgumentException("Discount code '$code' is not valid or has expired") }

        // Validasi date range
        if (discount.startDate != null && now.isBefore(discount.startDate!!))
            throw IllegalArgumentException("Discount code '$code' is not yet active")
        if (discount.endDate != null && now.isAfter(discount.endDate!!))
            throw IllegalArgumentException("Discount code '$code' has expired")

        // Validasi channel
        if (discount.channel != "BOTH" && discount.channel != channel)
            throw IllegalArgumentException("Discount code '$code' is not valid for this channel")

        // Validasi outlet
        if (discount.visibility == "SPECIFIC_OUTLET") {
            val discountOutlets = discountOutletRepository.findByDiscountId(discount.id)
            if (discountOutlets.none { it.outletId == outletId })
                throw IllegalArgumentException("Discount code '$code' is not valid for this outlet")
        }

        // Validasi customer eligibility (jika ada binding)
        if (customerId != null) {
            val customerCheck = discountCustomerRepository.findByDiscountIdAndCustomerId(discount.id, customerId)
            if (customerCheck.isEmpty) {
                // Cek apakah ada binding untuk discount ini — jika ada, customer ini tidak eligible
                val hasCustomerBinding = discountCustomerRepository.findByDiscountId(discount.id).isNotEmpty()
                if (hasCustomerBinding)
                    throw IllegalArgumentException("Discount code '$code' is not valid for this customer")
            }
        }

        // Validasi min_purchase (dihitung dari grossSubTotal sebelum promosi)
        if (discount.minPurchase != null && grossSubTotal < discount.minPurchase!!)
            throw IllegalArgumentException("Minimum purchase of ${discount.minPurchase} required for discount code '$code'")

        // Tentukan item yang qualify berdasarkan scope
        val qualifyingIndices: List<Int> = when (discount.scope) {
            "PRODUCT" -> {
                val boundProductIds = discountProductRepository.findByDiscountId(discount.id).map { it.productId }.toSet()
                resolvedItems.indices.filter { resolvedItems[it].product.id in boundProductIds }
            }
            "CATEGORY" -> {
                val boundCategoryIds = discountCategoryRepository.findByDiscountId(discount.id).map { it.categoryId }.toSet()
                resolvedItems.indices.filter { idx ->
                    resolvedItems[idx].product.categories.any { it.id in boundCategoryIds }
                }
            }
            else -> resolvedItems.indices.toList() // ALL
        }

        if (qualifyingIndices.isEmpty()) return empty

        // Hitung base untuk diskon
        val discountBase = qualifyingIndices.fold(BigDecimal.ZERO) { acc, idx ->
            acc.add(resolvedItems[idx].effectivePrice.multiply(BigDecimal(resolvedItems[idx].qty)))
        }

        var totalDiscount = computeDiscount(discount.value, discount.valueType, discountBase)

        // Cap untuk tipe PERCENTAGE
        if (discount.valueType == "PERCENTAGE" && discount.maxDiscountAmount != null) {
            totalDiscount = totalDiscount.min(discount.maxDiscountAmount!!)
        }

        // Distribusikan diskon proporsional ke item-item yang qualify
        val itemDiscounts = mutableMapOf<Int, BigDecimal>()
        if (discount.scope != "ALL") {
            qualifyingIndices.forEach { idx ->
                val itemSubtotal = resolvedItems[idx].effectivePrice.multiply(BigDecimal(resolvedItems[idx].qty))
                val proportion = itemSubtotal.divide(discountBase, 10, RoundingMode.HALF_UP)
                itemDiscounts[idx] = totalDiscount.multiply(proportion).setScale(2, RoundingMode.HALF_UP)
            }
        }

        return DiscountCodeResult(discount.id, totalDiscount, itemDiscounts)
    }

    // =========================================================================
    // AMOUNT CALCULATION (Tax, SC, Rounding)
    // =========================================================================

    private data class CalculatedAmounts(
        val grossSubTotal: BigDecimal,
        val promoDiscount: BigDecimal,
        val codeDiscount: BigDecimal,
        val netSubTotal: BigDecimal,
        val totalServiceCharge: BigDecimal,
        val totalTax: BigDecimal,
        val totalRounding: BigDecimal,
        val totalAmount: BigDecimal,
        val priceIncludeTax: Boolean
    )

    private fun calculateAmounts(
        netSubTotal: BigDecimal,
        grossSubTotal: BigDecimal,
        promoDiscount: BigDecimal,
        codeDiscount: BigDecimal,
        setting: PaymentSetting?
    ): CalculatedAmounts {
        val priceIncludeTax = setting?.isPriceIncludeTax ?: false

        // Service charge dihitung dari netSubTotal (setelah diskon)
        val totalServiceCharge: BigDecimal = if (setting?.isServiceCharge == true) {
            when {
                setting.serviceChargePercentage != null ->
                    netSubTotal.multiply(setting.serviceChargePercentage!!)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                setting.serviceChargeAmount != null -> setting.serviceChargeAmount!!
                else -> BigDecimal.ZERO
            }
        } else BigDecimal.ZERO

        val taxBase = netSubTotal.add(totalServiceCharge)
        val totalTax: BigDecimal = if (setting?.isTax == true && setting.taxPercentage != null) {
            val pct = setting.taxPercentage!!
            if (priceIncludeTax)
                taxBase.multiply(pct).divide(BigDecimal(100).add(pct), 2, RoundingMode.HALF_UP)
            else
                taxBase.multiply(pct).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        val amountBeforeRounding = if (priceIncludeTax)
            netSubTotal.add(totalServiceCharge)
        else
            netSubTotal.add(totalServiceCharge).add(totalTax)

        val (totalRounding, totalAmount) = if (setting?.isRounding == true && setting.roundingTarget != null)
            applyRounding(amountBeforeRounding, setting.roundingTarget!!, setting.roundingType)
        else
            Pair(BigDecimal.ZERO, amountBeforeRounding)

        return CalculatedAmounts(
            grossSubTotal = grossSubTotal,
            promoDiscount = promoDiscount,
            codeDiscount = codeDiscount,
            netSubTotal = netSubTotal,
            totalServiceCharge = totalServiceCharge,
            totalTax = totalTax,
            totalRounding = totalRounding,
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP),
            priceIncludeTax = priceIncludeTax
        )
    }

    private fun applyRounding(
        amount: BigDecimal,
        roundingTarget: Int,
        roundingType: String?
    ): Pair<BigDecimal, BigDecimal> {
        val target = BigDecimal(roundingTarget)
        val remainder = amount.remainder(target)
        if (remainder.compareTo(BigDecimal.ZERO) == 0) return Pair(BigDecimal.ZERO, amount)

        val delta = when (roundingType?.uppercase()) {
            "UP" -> target.subtract(remainder)
            "DOWN" -> remainder.negate()
            else -> { val half = target.divide(BigDecimal(2)); if (remainder >= half) target.subtract(remainder) else remainder.negate() }
        }
        return Pair(delta, amount.add(delta))
    }

    // =========================================================================
    // VALIDATION: request vs calculated
    // =========================================================================

    private fun validateAgainstRequest(
        request: CreateTransactionRequest,
        resolvedItems: List<ResolvedItem>,
        calculated: CalculatedAmounts,
        cashChange: BigDecimal?,
        promoResult: PromotionResult,
        discountCodeResult: DiscountCodeResult
    ) {
        val mismatches = mutableListOf<MismatchDetail>()

        fun check(field: String, fromRequest: BigDecimal?, calculatedValue: BigDecimal) {
            if (fromRequest != null && fromRequest.compareTo(calculatedValue) != 0)
                mismatches.add(MismatchDetail(field, fromRequest, calculatedValue))
        }

        check("subTotal",           request.subTotal,           calculated.netSubTotal)
        check("promoAmount",        request.promoAmount,        calculated.promoDiscount)
        check("discountAmount",     request.discountAmount,     calculated.codeDiscount)
        check("totalServiceCharge", request.totalServiceCharge, calculated.totalServiceCharge)
        check("totalTax",           request.totalTax,           calculated.totalTax)
        check("totalRounding",      request.totalRounding,      calculated.totalRounding)
        check("totalAmount",        request.totalAmount,        calculated.totalAmount)
        if (request.cashTendered != null && cashChange != null)
            check("cashChange", request.cashChange, cashChange)

        // Per-item validation
        resolvedItems.forEachIndexed { index, resolved ->
            val itemReq = request.transactionItems[index]
            check("items[$index].price",      itemReq.price,      resolved.effectivePrice)
            check("items[$index].totalPrice", itemReq.totalPrice, resolved.effectivePrice.multiply(BigDecimal(resolved.qty)))
            val itemPromoDiscount = promoResult.itemDiscounts[index] ?: BigDecimal.ZERO
            val itemCodeDiscount  = discountCodeResult.itemDiscounts[index] ?: BigDecimal.ZERO
            val totalItemDiscount = itemPromoDiscount.add(itemCodeDiscount)
            if (totalItemDiscount > BigDecimal.ZERO)
                check("items[$index].discountAmount", itemReq.discountAmount, totalItemDiscount)
            resolved.tax?.let {
                val expectedTax = resolved.effectivePrice.multiply(BigDecimal(resolved.qty))
                    .multiply(it.percentage).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                check("items[$index].taxAmount", itemReq.taxAmount, expectedTax)
            }
        }

        if (mismatches.isNotEmpty()) throw AmountMismatchException(mismatches)
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private fun computeDiscount(value: BigDecimal, valueType: String, base: BigDecimal): BigDecimal =
        when (valueType.uppercase()) {
            "PERCENTAGE" -> base.multiply(value).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            "AMOUNT" -> value.min(base)
            else -> BigDecimal.ZERO
        }

    private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal = if (this < min) min else this

    private fun DiscountCustomerRepository.findByDiscountId(discountId: Long): List<DiscountCustomer> =
        findAll().filter { it.discountId == discountId }

    // =========================================================================
    // INTERNAL MODELS
    // =========================================================================

    private data class ResolvedItem(
        val product: Product,
        val tax: Tax?,
        val qty: Int,
        val effectivePrice: BigDecimal,    // setelah price book
        val priceBookItemId: Long? = null
    )

    // =========================================================================
    // RESPONSE MAPPING
    // =========================================================================

    @Transactional
    fun update(merchantId: Long, username: String, merchantTrxId: String, request: UpdateTransactionRequest) {
        val trx = transactionRepository.findByTrxIdAndMerchantId(merchantTrxId, merchantId)
            .orElseThrow { ResourceNotFoundException("Transaction not found") }

        trx.status = request.status
        trx.paymentMethod = request.paymentMethod
        trx.modifiedBy = username
        trx.modifiedDate = LocalDateTime.now()
        transactionRepository.save(trx)

        val payment = Payment().apply {
            transactionId = trx.id
            paymentTrxId = request.paymentTrxId
            paymentMethod = request.paymentMethod
            amountPaid = request.amountPaid
            status = request.status
            paymentReference = request.paymentReference
            paymentDate = request.paymentDate?.let {
                LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME)
            }
            isEffective = true
            createdBy = username
            createdDate = LocalDateTime.now()
        }
        paymentRepository.save(payment)
    }

    private fun Transaction.toDetailResponse(
        queueNumber: String?,
        items: List<TransactionItem>,
        payments: List<Payment>
    ) = TransactionDetailResponse(
        transactionId = id,
        code = trxId,
        paymentMethod = paymentMethod,
        status = status,
        subTotal = subTotal,
        totalTax = totalTax,
        totalServiceCharge = totalServiceCharge,
        totalRounding = totalRounding,
        totalAmount = totalAmount,
        cashTendered = cashTendered,
        cashChange = cashChange,
        taxName = taxName,
        taxPercentage = taxPercentage,
        serviceChargeAmount = serviceChargeAmount,
        serviceChargePercentage = serviceChargePercentage,
        roundingTarget = roundingTarget,
        roundingType = roundingType,
        transactionDate = createdDate,
        queueNumber = queueNumber,
        transactionItems = items.map {
            TransactionItemResponse(
                id = it.id,
                productId = it.productId,
                productName = it.productName,
                price = it.price,
                qty = it.qty,
                totalPrice = it.totalPrice,
                taxName = it.taxName,
                taxPercentage = it.taxPercentage,
                taxAmount = it.taxAmount
            )
        },
        payments = payments.map {
            PaymentDetailResponse(
                id = it.id,
                paymentMethod = it.paymentMethod,
                amountPaid = it.amountPaid,
                status = it.status,
                paymentReference = it.paymentReference,
                paymentDate = it.paymentDate
            )
        }
    )
}
