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

    fun get(merchantId: Long): PaymentSettingResponse =
        paymentSettingRepository.findByMerchantId(merchantId)
            .orElseThrow { ResourceNotFoundException("Payment setting not found") }
            .toResponse()

    @Transactional
    fun create(merchantId: Long, username: String, request: CreatePaymentSettingRequest): PaymentSettingResponse {
        if (paymentSettingRepository.findByMerchantId(merchantId).isPresent)
            throw BusinessException("Payment setting already exists. Use update endpoint.")

        val setting = PaymentSetting().apply {
            this.merchantId = merchantId
            isPriceIncludeTax = request.isPriceIncludeTax
            isRounding = request.isRounding
            roundingTarget = request.roundingTarget
            roundingType = request.roundingType
            isServiceCharge = request.isServiceCharge
            serviceChargePercentage = request.serviceChargePercentage
            serviceChargeAmount = request.serviceChargeAmount
            isTax = request.isTax
            taxPercentage = request.taxPercentage
            taxName = request.taxName
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
            isTax = request.isTax
            taxPercentage = request.taxPercentage
            taxName = request.taxName
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
        isPriceIncludeTax = isPriceIncludeTax,
        isRounding = isRounding,
        roundingTarget = roundingTarget,
        roundingType = roundingType,
        isServiceCharge = isServiceCharge,
        serviceChargePercentage = serviceChargePercentage,
        serviceChargeAmount = serviceChargeAmount,
        isTax = isTax,
        taxPercentage = taxPercentage,
        taxName = taxName
    )
}
