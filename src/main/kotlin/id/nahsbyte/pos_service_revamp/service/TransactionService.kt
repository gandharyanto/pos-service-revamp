package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.response.*
import id.nahsbyte.pos_service_revamp.entity.Payment
import id.nahsbyte.pos_service_revamp.entity.PaymentSetting
import id.nahsbyte.pos_service_revamp.entity.Product
import id.nahsbyte.pos_service_revamp.entity.Tax
import id.nahsbyte.pos_service_revamp.entity.Transaction
import id.nahsbyte.pos_service_revamp.entity.TransactionItem
import id.nahsbyte.pos_service_revamp.entity.TransactionQueue
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
    private val productRepository: ProductRepository
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

        // --- LOAD PRODUCTS AND TAXES FROM DB ---
        val productIds = request.transactionItems.map { it.productId }.distinct()
        val productMap: Map<Long, Product> = productRepository.findAllById(productIds)
            .also { products ->
                // Validate all products belong to this merchant and are not deleted
                products.forEach { p ->
                    require(p.merchantId == merchantId) {
                        "Product ${p.id} does not belong to this merchant"
                    }
                    require(p.deletedDate == null) {
                        "Product '${p.name}' has been deleted and cannot be added to a transaction"
                    }
                }
                val foundIds = products.map { it.id }.toSet()
                val missingIds = productIds - foundIds
                if (missingIds.isNotEmpty()) {
                    throw ResourceNotFoundException("Products not found: $missingIds")
                }
            }
            .associateBy { it.id }

        // Load taxes for taxable products (taxId from product, not from request)
        val taxIds = productMap.values.filter { it.isTaxable && it.taxId != null }
            .mapNotNull { it.taxId }.distinct()
        val taxMap: Map<Long, Tax> = if (taxIds.isNotEmpty()) {
            taxRepository.findAllById(taxIds).associateBy { it.id }
        } else emptyMap()

        // Resolve items: pair each request line with its product + tax from DB
        val resolvedItems: List<ResolvedItem> = request.transactionItems.map { itemReq ->
            val product = productMap[itemReq.productId]!!
            val tax = if (product.isTaxable) product.taxId?.let { taxMap[it] } else null
            ResolvedItem(product = product, tax = tax, qty = itemReq.qty)
        }

        // --- CALCULATE ---
        val calculated = calculateAmounts(resolvedItems, paymentSetting)
        val cashChange = request.cashTendered?.subtract(calculated.totalAmount)
            ?.let { if (it < BigDecimal.ZERO) BigDecimal.ZERO else it }

        // --- COMPARE REQUEST vs CALCULATED ---
        validateAgainstRequest(request, resolvedItems, calculated, cashChange)

        // Validate cash payment
        if (request.paymentMethod.uppercase() == "CASH") {
            val tendered = request.cashTendered
                ?: throw IllegalArgumentException("cashTendered is required for CASH payment")
            require(tendered >= calculated.totalAmount) {
                "Cash tendered ($tendered) is less than total amount (${calculated.totalAmount})"
            }
        }

        // --- QUEUE ---
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
            createdDate = LocalDateTime.now()
        }
        val savedQueue = transactionQueueRepository.save(queue)

        val trxId = "TRX-${UUID.randomUUID().toString().take(8).uppercase()}"

        // --- SAVE TRANSACTION ---
        val transaction = Transaction().apply {
            this.merchantId = merchantId
            outletId = outlet.id
            this.username = username
            this.trxId = trxId
            status = "PAID"
            paymentMethod = request.paymentMethod
            priceIncludeTax = calculated.priceIncludeTax
            subTotal = calculated.subTotal
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
            queueId = savedQueue.id
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val savedTrx = transactionRepository.save(transaction)

        // --- SAVE ITEMS (price and tax from DB product) ---
        val items = resolvedItems.map { resolved ->
            val itemTotal = resolved.product.price.multiply(BigDecimal(resolved.qty))
            val itemTaxAmount = resolved.tax?.let {
                itemTotal.multiply(it.percentage).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            }
            TransactionItem().apply {
                transactionId = savedTrx.id
                productId = resolved.product.id
                productName = resolved.product.name          // snapshot from DB
                price = resolved.product.price               // price from DB
                qty = resolved.qty
                totalPrice = itemTotal
                taxId = resolved.tax?.id
                taxName = resolved.tax?.name                 // tax snapshot from DB
                taxPercentage = resolved.tax?.percentage     // tax % snapshot from DB
                taxAmount = itemTaxAmount
                createdBy = username
                createdDate = LocalDateTime.now()
            }
        }
        transactionItemRepository.saveAll(items)

        return CreateTransactionResponse(
            id = savedTrx.id,
            trxId = savedTrx.trxId,
            queueNumber = savedQueue.queueNumber
        )
    }

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

    // -------------------------------------------------------------------------
    // Validation: compare request values against server-calculated values
    // -------------------------------------------------------------------------

    private fun validateAgainstRequest(
        request: CreateTransactionRequest,
        resolvedItems: List<ResolvedItem>,
        calculated: CalculatedAmounts,
        cashChange: BigDecimal?
    ) {
        val mismatches = mutableListOf<MismatchDetail>()

        fun check(field: String, fromRequest: BigDecimal?, calculatedValue: BigDecimal) {
            if (fromRequest != null && fromRequest.compareTo(calculatedValue) != 0) {
                mismatches.add(MismatchDetail(field, fromRequest, calculatedValue))
            }
        }

        // Transaction-level fields
        check("subTotal",           request.subTotal,           calculated.subTotal)
        check("totalServiceCharge", request.totalServiceCharge, calculated.totalServiceCharge)
        check("totalTax",           request.totalTax,           calculated.totalTax)
        check("totalRounding",      request.totalRounding,      calculated.totalRounding)
        check("totalAmount",        request.totalAmount,        calculated.totalAmount)
        if (request.cashTendered != null && cashChange != null) {
            check("cashChange", request.cashChange, cashChange)
        }

        // Per-item fields (only validate when client provides the optional fields)
        resolvedItems.forEachIndexed { index, resolved ->
            val itemReq = request.transactionItems[index]
            val expectedPrice    = resolved.product.price
            val expectedTotal    = resolved.product.price.multiply(BigDecimal(resolved.qty))
            val expectedTaxAmt   = resolved.tax?.let {
                expectedTotal.multiply(it.percentage).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            }

            check("items[$index].price",      itemReq.price,      expectedPrice)
            check("items[$index].totalPrice", itemReq.totalPrice, expectedTotal)
            if (expectedTaxAmt != null) {
                check("items[$index].taxAmount", itemReq.taxAmount, expectedTaxAmt)
            }
        }

        if (mismatches.isNotEmpty()) throw AmountMismatchException(mismatches)
    }

    // -------------------------------------------------------------------------
    // Internal models & calculation logic
    // -------------------------------------------------------------------------

    private data class ResolvedItem(
        val product: Product,
        val tax: Tax?,
        val qty: Int
    )

    private data class CalculatedAmounts(
        val subTotal: BigDecimal,
        val totalServiceCharge: BigDecimal,
        val totalTax: BigDecimal,
        val totalRounding: BigDecimal,
        val totalAmount: BigDecimal,
        val priceIncludeTax: Boolean
    )

    private fun calculateAmounts(
        items: List<ResolvedItem>,
        setting: PaymentSetting?
    ): CalculatedAmounts {
        val priceIncludeTax = setting?.isPriceIncludeTax ?: false

        // 1. subTotal: product.price (from DB) × qty
        val subTotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.product.price.multiply(BigDecimal(item.qty)))
        }

        // 2. Service charge
        val totalServiceCharge: BigDecimal = if (setting?.isServiceCharge == true) {
            when {
                setting.serviceChargePercentage != null ->
                    subTotal.multiply(setting.serviceChargePercentage!!)
                        .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                setting.serviceChargeAmount != null -> setting.serviceChargeAmount!!
                else -> BigDecimal.ZERO
            }
        } else BigDecimal.ZERO

        // 3. Tax (global PaymentSetting tax — applied on subTotal + serviceCharge)
        val taxBase = subTotal.add(totalServiceCharge)
        val totalTax: BigDecimal = if (setting?.isTax == true && setting.taxPercentage != null) {
            val pct = setting.taxPercentage!!
            if (priceIncludeTax) {
                // Tax already included in price — extract: tax = base × rate / (100 + rate)
                taxBase.multiply(pct)
                    .divide(BigDecimal(100).add(pct), 2, RoundingMode.HALF_UP)
            } else {
                // Tax on top: tax = base × rate / 100
                taxBase.multiply(pct)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            }
        } else BigDecimal.ZERO

        // 4. Amount before rounding
        val amountBeforeRounding = if (priceIncludeTax) {
            subTotal.add(totalServiceCharge)
        } else {
            subTotal.add(totalServiceCharge).add(totalTax)
        }

        // 5. Rounding
        val (totalRounding, totalAmount) = if (setting?.isRounding == true && setting.roundingTarget != null) {
            applyRounding(amountBeforeRounding, setting.roundingTarget!!, setting.roundingType)
        } else {
            Pair(BigDecimal.ZERO, amountBeforeRounding)
        }

        return CalculatedAmounts(
            subTotal = subTotal,
            totalServiceCharge = totalServiceCharge,
            totalTax = totalTax,
            totalRounding = totalRounding,
            totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP),
            priceIncludeTax = priceIncludeTax
        )
    }

    /**
     * Computes the rounding delta and final total.
     * roundingTarget: granularity (e.g. 100 → round to nearest 100)
     * roundingType: "UP", "DOWN", "NEAREST"
     * Returns Pair(roundingDelta, roundedTotal)
     */
    private fun applyRounding(
        amount: BigDecimal,
        roundingTarget: Int,
        roundingType: String?
    ): Pair<BigDecimal, BigDecimal> {
        val target = BigDecimal(roundingTarget)
        val remainder = amount.remainder(target)

        if (remainder.compareTo(BigDecimal.ZERO) == 0) {
            return Pair(BigDecimal.ZERO, amount)
        }

        val roundingDelta = when (roundingType?.uppercase()) {
            "UP" -> target.subtract(remainder)
            "DOWN" -> remainder.negate()
            else -> { // NEAREST
                val half = target.divide(BigDecimal(2))
                if (remainder >= half) target.subtract(remainder) else remainder.negate()
            }
        }

        return Pair(roundingDelta, amount.add(roundingDelta))
    }

    // -------------------------------------------------------------------------

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
