package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.LoyaltyProgram
import id.nahsbyte.pos_service_revamp.entity.LoyaltyRedemptionRule
import id.nahsbyte.pos_service_revamp.entity.LoyaltyTransaction
import id.nahsbyte.pos_service_revamp.entity.ProductLoyaltySetting
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class LoyaltyService(
    private val loyaltyProgramRepository: LoyaltyProgramRepository,
    private val loyaltyRedemptionRuleRepository: LoyaltyRedemptionRuleRepository,
    private val productLoyaltySettingRepository: ProductLoyaltySettingRepository,
    private val loyaltyTransactionRepository: LoyaltyTransactionRepository,
    private val customerRepository: CustomerRepository
) {

    // ── Program ────────────────────────────────────────────────────────────

    fun list(merchantId: Long, isActive: Boolean? = null): List<LoyaltyProgramResponse> {
        if (isActive == true)
            return loyaltyProgramRepository.findByMerchantIdAndIsActiveTrue(merchantId)
                .map { listOf(it.toResponse()) }.orElse(emptyList())
        return loyaltyProgramRepository.findByMerchantId(merchantId).map { it.toResponse() }
    }

    fun detail(merchantId: Long, id: Long): LoyaltyProgramResponse =
        loyaltyProgramRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("LoyaltyProgram not found") }

    @Transactional
    fun create(merchantId: Long, request: CreateLoyaltyProgramRequest): LoyaltyProgramResponse {
        val program = LoyaltyProgram().apply {
            this.merchantId = merchantId
            name = request.name
            earnMode = request.earnMode
            pointsPerAmount = request.pointsPerAmount
            earnMultiplier = request.earnMultiplier
            expiryMode = request.expiryMode
            expiryDays = request.expiryDays
            expiryDate = request.expiryDate
            isActive = request.isActive
        }
        loyaltyProgramRepository.save(program)
        saveRules(program.id, merchantId, request.rules)
        return program.toResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdateLoyaltyProgramRequest): LoyaltyProgramResponse {
        val program = loyaltyProgramRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("LoyaltyProgram not found") }
        program.apply {
            name = request.name
            earnMode = request.earnMode
            pointsPerAmount = request.pointsPerAmount
            earnMultiplier = request.earnMultiplier
            expiryMode = request.expiryMode
            expiryDays = request.expiryDays
            expiryDate = request.expiryDate
            isActive = request.isActive
        }
        loyaltyProgramRepository.save(program)
        loyaltyRedemptionRuleRepository.deleteAllByLoyaltyProgramId(program.id)
        saveRules(program.id, merchantId, request.rules)
        return program.toResponse()
    }

    private fun saveRules(programId: Long, merchantId: Long, rules: List<CreateRedemptionRuleRequest>) {
        rules.forEach { r ->
            loyaltyRedemptionRuleRepository.save(LoyaltyRedemptionRule().apply {
                loyaltyProgramId = programId
                this.merchantId = merchantId
                type = r.type
                redeemRate = r.redeemRate
                minRedeemPoints = r.minRedeemPoints
                maxRedeemPoints = r.maxRedeemPoints
                requiredPoints = r.requiredPoints
                discountType = r.discountType
                discountValue = r.discountValue
                maxDiscountAmount = r.maxDiscountAmount
                minPurchase = r.minPurchase
                rewardProductId = r.rewardProductId
                rewardQty = r.rewardQty
                isActive = r.isActive
                createdDate = LocalDateTime.now()
            })
        }
    }

    @Transactional
    fun delete(merchantId: Long, id: Long) {
        val program = loyaltyProgramRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("LoyaltyProgram not found") }
        loyaltyProgramRepository.delete(program)
    }

    // ── Product Setting ────────────────────────────────────────────────────

    fun getProductSetting(merchantId: Long, productId: Long): ProductLoyaltySettingResponse =
        productLoyaltySettingRepository.findByProductIdAndMerchantId(productId, merchantId)
            .map { ProductLoyaltySettingResponse(it.productId, it.isLoyaltyEnabled, it.fixedPoints) }
            .orElse(ProductLoyaltySettingResponse(productId, true, null))

    @Transactional
    fun setProductSetting(merchantId: Long, request: SetProductLoyaltyRequest): ProductLoyaltySettingResponse {
        val setting = productLoyaltySettingRepository.findByProductIdAndMerchantId(request.productId, merchantId)
            .orElse(ProductLoyaltySetting().apply {
                productId = request.productId
                this.merchantId = merchantId
            })
        setting.apply {
            isLoyaltyEnabled = request.isLoyaltyEnabled
            fixedPoints = request.fixedPoints
            modifiedDate = LocalDateTime.now()
            if (createdDate == null) createdDate = LocalDateTime.now()
        }
        val saved = productLoyaltySettingRepository.save(setting)
        return ProductLoyaltySettingResponse(saved.productId, saved.isLoyaltyEnabled, saved.fixedPoints)
    }

    // ── Core Logic (dipakai TransactionService) ────────────────────────────

    /**
     * Hitung poin yang diperoleh dari transaksi.
     * Dipanggil setelah transaksi PAID, basis = netSubTotal.
     *
     * Product-level override diapply per item sebelum dijumlahkan.
     */
    fun calculateEarnedPoints(
        merchantId: Long,
        netSubTotal: BigDecimal,
        itemContributions: List<Pair<Long, BigDecimal>>  // (productId, itemNetAmount)
    ): BigDecimal {
        val program = loyaltyProgramRepository.findByMerchantIdAndIsActiveTrue(merchantId)
            .orElse(null) ?: return BigDecimal.ZERO

        val productSettings = productLoyaltySettingRepository
            .findByMerchantIdAndProductIdIn(merchantId, itemContributions.map { it.first })
            .associateBy { it.productId }

        var totalPoints = BigDecimal.ZERO

        for ((productId, itemAmount) in itemContributions) {
            val setting = productSettings[productId]

            // Produk dinonaktifkan dari loyalty
            if (setting?.isLoyaltyEnabled == false) continue

            val points = when {
                // Fixed points override
                setting?.fixedPoints != null -> setting.fixedPoints!!

                // Global RATIO mode
                program.earnMode == "RATIO" && program.pointsPerAmount > BigDecimal.ZERO ->
                    itemAmount.divide(program.pointsPerAmount, 0, RoundingMode.FLOOR)

                // Global MULTIPLY mode
                program.earnMode == "MULTIPLY" && program.earnMultiplier != null ->
                    itemAmount.multiply(program.earnMultiplier!!).setScale(0, RoundingMode.FLOOR)

                else -> BigDecimal.ZERO
            }
            totalPoints = totalPoints.add(points)
        }

        return totalPoints.max(BigDecimal.ZERO)
    }

    /**
     * Hitung nilai redeem poin sebagai payment.
     * Return: Pair(pointsToDeduct, rupiahValue)
     */
    fun calculateRedeemAsPayment(
        merchantId: Long,
        programId: Long,
        requestedPoints: BigDecimal,
        totalAmount: BigDecimal,
        customerPoints: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val rule = loyaltyRedemptionRuleRepository
            .findByLoyaltyProgramIdAndTypeAndIsActiveTrue(programId, "PAYMENT")
            .orElseThrow { IllegalArgumentException("No active PAYMENT redemption rule") }

        val rate = rule.redeemRate ?: throw IllegalArgumentException("redeemRate not configured")
        val minPts = rule.minRedeemPoints ?: BigDecimal.ZERO
        val maxPts = rule.maxRedeemPoints

        if (requestedPoints < minPts)
            throw IllegalArgumentException("Minimum redeem points: $minPts")
        if (requestedPoints > customerPoints)
            throw IllegalArgumentException("Insufficient loyalty points (have: $customerPoints, want: $requestedPoints)")
        if (maxPts != null && requestedPoints > maxPts)
            throw IllegalArgumentException("Maximum redeem points per transaction: $maxPts")

        val rupiahValue = requestedPoints.multiply(rate).min(totalAmount)
        return Pair(requestedPoints, rupiahValue)
    }

    /**
     * Hitung nilai redeem poin sebagai diskon.
     * Return: Pair(pointsToDeduct, discountAmount)
     */
    fun calculateRedeemAsDiscount(
        merchantId: Long,
        programId: Long,
        netSubTotal: BigDecimal
    ): Pair<BigDecimal, BigDecimal> {
        val rule = loyaltyRedemptionRuleRepository
            .findByLoyaltyProgramIdAndTypeAndIsActiveTrue(programId, "DISCOUNT")
            .orElseThrow { IllegalArgumentException("No active DISCOUNT redemption rule") }

        val requiredPoints = rule.requiredPoints ?: throw IllegalArgumentException("requiredPoints not configured")
        val minPurchase = rule.minPurchase ?: BigDecimal.ZERO

        if (netSubTotal < minPurchase)
            throw IllegalArgumentException("Minimum purchase for loyalty discount: $minPurchase")

        val discountAmount = when (rule.discountType) {
            "PERCENTAGE" -> {
                val raw = netSubTotal.multiply(rule.discountValue ?: BigDecimal.ZERO)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                if (rule.maxDiscountAmount != null) raw.min(rule.maxDiscountAmount!!) else raw
            }
            "AMOUNT" -> (rule.discountValue ?: BigDecimal.ZERO).min(netSubTotal)
            else -> BigDecimal.ZERO
        }

        return Pair(requiredPoints, discountAmount)
    }

    /**
     * Kredit poin ke customer setelah transaksi selesai.
     * Dipanggil dari TransactionService setelah save transaction.
     */
    @Transactional
    fun creditEarnedPoints(
        merchantId: Long,
        customerId: Long,
        transactionId: Long,
        points: BigDecimal,
        programId: Long,
        username: String
    ) {
        if (points <= BigDecimal.ZERO) return

        val customer = customerRepository.findById(customerId).orElse(null) ?: return
        val program = loyaltyProgramRepository.findById(programId).orElse(null) ?: return

        customer.loyaltyPoints = customer.loyaltyPoints.add(points)
        customerRepository.save(customer)

        val expiryDate: LocalDateTime? = when (program.expiryMode) {
            "ROLLING_DAYS" -> program.expiryDays?.let { LocalDateTime.now().plusDays(it.toLong()) }
            "FIXED_DATE" -> program.expiryDate
            else -> null
        }

        loyaltyTransactionRepository.save(LoyaltyTransaction().apply {
            this.merchantId = merchantId
            this.customerId = customerId
            this.transactionId = transactionId
            this.points = points
            type = "EARN"
            note = "Earned from transaction #$transactionId"
            this.expiryDate = expiryDate
            createdBy = username
            createdDate = LocalDateTime.now()
        })
    }

    /**
     * Debit poin dari customer (saat redeem).
     * Dipanggil dari TransactionService setelah save transaction.
     */
    @Transactional
    fun debitRedeemedPoints(
        merchantId: Long,
        customerId: Long,
        transactionId: Long,
        points: BigDecimal,
        redeemType: String,   // REDEEM_PAYMENT | REDEEM_DISCOUNT
        username: String
    ) {
        if (points <= BigDecimal.ZERO) return

        val customer = customerRepository.findById(customerId).orElse(null) ?: return
        customer.loyaltyPoints = (customer.loyaltyPoints.subtract(points)).max(BigDecimal.ZERO)
        customerRepository.save(customer)

        loyaltyTransactionRepository.save(LoyaltyTransaction().apply {
            this.merchantId = merchantId
            this.customerId = customerId
            this.transactionId = transactionId
            this.points = points.negate()
            type = redeemType
            note = "Redeemed in transaction #$transactionId"
            createdBy = username
            createdDate = LocalDateTime.now()
        })
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private fun LoyaltyProgram.toResponse(): LoyaltyProgramResponse {
        val rules = loyaltyRedemptionRuleRepository.findByLoyaltyProgramId(id).map { it.toRuleResponse() }
        return LoyaltyProgramResponse(
            id = id,
            name = name,
            earnMode = earnMode,
            pointsPerAmount = pointsPerAmount,
            earnMultiplier = earnMultiplier,
            expiryMode = expiryMode,
            expiryDays = expiryDays,
            expiryDate = expiryDate,
            isActive = isActive,
            redemptionRules = rules,
            createdDate = createdDate,
            modifiedDate = modifiedDate
        )
    }

    private fun LoyaltyRedemptionRule.toRuleResponse() = RedemptionRuleResponse(
        id = id,
        type = type,
        redeemRate = redeemRate,
        minRedeemPoints = minRedeemPoints,
        maxRedeemPoints = maxRedeemPoints,
        requiredPoints = requiredPoints,
        discountType = discountType,
        discountValue = discountValue,
        maxDiscountAmount = maxDiscountAmount,
        minPurchase = minPurchase,
        rewardProductId = rewardProductId,
        rewardQty = rewardQty,
        isActive = isActive
    )
}
