package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateTaxRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateTaxRequest
import id.nahsbyte.pos_service_revamp.dto.response.TaxResponse
import id.nahsbyte.pos_service_revamp.entity.Tax
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.TaxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class TaxService(private val taxRepository: TaxRepository) {

    fun list(merchantId: Long): List<TaxResponse> =
        taxRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, taxId: Long): TaxResponse =
        taxRepository.findByMerchantIdAndId(merchantId, taxId)
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Tax not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreateTaxRequest): TaxResponse {
        if (request.isDefault) clearDefault(merchantId)

        val tax = Tax().apply {
            this.merchantId = merchantId
            name = request.name
            percentage = request.percentage
            isDefault = request.isDefault
            isActive = true
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return taxRepository.save(tax).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdateTaxRequest): TaxResponse {
        val tax = taxRepository.findByMerchantIdAndId(merchantId, request.taxId)
            .orElseThrow { ResourceNotFoundException("Tax not found") }

        if (request.isDefault && !tax.isDefault) clearDefault(merchantId)

        tax.apply {
            name = request.name
            percentage = request.percentage
            isDefault = request.isDefault
            isActive = request.isActive
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return taxRepository.save(tax).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, taxId: Long) {
        val tax = taxRepository.findByMerchantIdAndId(merchantId, taxId)
            .orElseThrow { ResourceNotFoundException("Tax not found") }
        tax.isActive = false
        tax.isDefault = false
        taxRepository.save(tax)
    }

    private fun clearDefault(merchantId: Long) {
        taxRepository.findByMerchantIdAndIsDefaultTrue(merchantId)
            .ifPresent { it.isDefault = false; taxRepository.save(it) }
    }

    private fun Tax.toResponse() = TaxResponse(
        id = id,
        name = name,
        percentage = percentage,
        isActive = isActive,
        isDefault = isDefault
    )
}
