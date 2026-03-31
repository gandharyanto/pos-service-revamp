package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreatePaymentSettingRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePaymentSettingRequest
import id.nahsbyte.pos_service_revamp.dto.response.PaymentMethodListResponse
import id.nahsbyte.pos_service_revamp.dto.response.PaymentMethodResponse
import id.nahsbyte.pos_service_revamp.dto.response.PaymentSettingResponse
import id.nahsbyte.pos_service_revamp.entity.PaymentSetting
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.MerchantPaymentMethodRepository
import id.nahsbyte.pos_service_revamp.repository.PaymentMethodRepository
import id.nahsbyte.pos_service_revamp.repository.PaymentSettingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class PaymentSettingService(
    private val paymentSettingRepository: PaymentSettingRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val merchantPaymentMethodRepository: MerchantPaymentMethodRepository
) {

    /** Default setting (outlet_id IS NULL). Untuk outlet-specific, gunakan getByOutlet. */
    fun get(merchantId: Long): PaymentSettingResponse =
        paymentSettingRepository.findByMerchantIdAndOutletIdIsNull(merchantId)
            .orElseThrow { ResourceNotFoundException("Payment setting not found") }
            .toResponse()

    /** Semua setting merchant: default + per-outlet overrides */
    fun list(merchantId: Long): List<PaymentSettingResponse> =
        paymentSettingRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    /** Ambil setting efektif untuk outlet tertentu. Falls back ke default jika override tidak ada. */
    fun getByOutlet(merchantId: Long, outletId: Long): PaymentSettingResponse =
        (paymentSettingRepository.findByMerchantIdAndOutletId(merchantId, outletId)
            .or { paymentSettingRepository.findByMerchantIdAndOutletIdIsNull(merchantId) })
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Payment setting not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreatePaymentSettingRequest): PaymentSettingResponse {
        val existing = if (request.outletId != null)
            paymentSettingRepository.findByMerchantIdAndOutletId(merchantId, request.outletId)
        else
            paymentSettingRepository.findByMerchantIdAndOutletIdIsNull(merchantId)

        if (existing.isPresent)
            throw BusinessException("Payment setting already exists for this scope. Use update endpoint.")

        val setting = PaymentSetting().apply {
            this.merchantId = merchantId
            outletId = request.outletId
            isPriceIncludeTax = request.isPriceIncludeTax
            isRounding = request.isRounding
            roundingTarget = request.roundingTarget
            roundingType = request.roundingType
            isServiceCharge = request.isServiceCharge
            serviceChargePercentage = request.serviceChargePercentage
            serviceChargeAmount = request.serviceChargeAmount
            serviceChargeSource = request.serviceChargeSource
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return paymentSettingRepository.save(setting).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdatePaymentSettingRequest): PaymentSettingResponse {
        val setting = paymentSettingRepository.findById(request.paymentSettingId)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("Payment setting not found") }

        setting.apply {
            isPriceIncludeTax = request.isPriceIncludeTax
            isRounding = request.isRounding
            roundingTarget = request.roundingTarget
            roundingType = request.roundingType
            isServiceCharge = request.isServiceCharge
            serviceChargePercentage = request.serviceChargePercentage
            serviceChargeAmount = request.serviceChargeAmount
            serviceChargeSource = request.serviceChargeSource
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return paymentSettingRepository.save(setting).toResponse()
    }

    fun getPaymentMethods(merchantId: Long): PaymentMethodListResponse {
        val enabledMethodIds = merchantPaymentMethodRepository
            .findAllByMerchantIdAndIsEnabledTrue(merchantId)
            .map { it.paymentMethodId }
            .toSet()

        val allMethods = paymentMethodRepository.findAllByIsActiveTrue()

        val merchantMethods = if (enabledMethodIds.isEmpty()) allMethods
        else allMethods.filter { it.id in enabledMethodIds }

        val (internal, external) = merchantMethods.partition { it.category == "INTERNAL" }

        return PaymentMethodListResponse(
            internalPayments = internal.map { PaymentMethodResponse(it.code, it.name, it.category, it.paymentType, it.provider) },
            externalPayments = external.map { PaymentMethodResponse(it.code, it.name, it.category, it.paymentType, it.provider) }
        )
    }

    private fun PaymentSetting.toResponse() = PaymentSettingResponse(
        paymentSettingId = id,
        outletId = outletId,
        isPriceIncludeTax = isPriceIncludeTax,
        isRounding = isRounding,
        roundingTarget = roundingTarget,
        roundingType = roundingType,
        isServiceCharge = isServiceCharge,
        serviceChargePercentage = serviceChargePercentage,
        serviceChargeAmount = serviceChargeAmount,
        serviceChargeSource = serviceChargeSource
    )
}
