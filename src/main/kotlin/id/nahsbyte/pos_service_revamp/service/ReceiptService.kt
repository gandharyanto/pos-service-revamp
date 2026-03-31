package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateReceiptRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateReceiptRequest
import id.nahsbyte.pos_service_revamp.dto.response.ReceiptResponse
import id.nahsbyte.pos_service_revamp.entity.ReceiptTemplate
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.ReceiptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ReceiptService(private val receiptRepository: ReceiptRepository) {

    fun list(merchantId: Long): List<ReceiptResponse> =
        receiptRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, receiptId: Long): ReceiptResponse =
        receiptRepository.findByMerchantIdAndId(merchantId, receiptId)
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Receipt template not found") }

    /** Template efektif untuk outlet tertentu, fallback ke default jika override tidak ada. */
    fun getByOutlet(merchantId: Long, outletId: Long): ReceiptResponse =
        (receiptRepository.findByMerchantIdAndOutletId(merchantId, outletId)
            .or { receiptRepository.findByMerchantIdAndOutletIdIsNull(merchantId) })
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Receipt template not found") }

    @Transactional
    fun create(merchantId: Long, username: String, request: CreateReceiptRequest): ReceiptResponse {
        val existing = if (request.outletId != null)
            receiptRepository.findByMerchantIdAndOutletId(merchantId, request.outletId)
        else
            receiptRepository.findByMerchantIdAndOutletIdIsNull(merchantId)

        if (existing.isPresent)
            throw BusinessException("Receipt template already exists for this scope. Use update endpoint.")

        val template = ReceiptTemplate().apply {
            this.merchantId = merchantId
            outletId = request.outletId
            header = request.header
            footer = request.footer
            showTax = request.showTax
            showServiceCharge = request.showServiceCharge
            showRounding = request.showRounding
            showLogo = request.showLogo
            logoUrl = request.logoUrl
            showQueueNumber = request.showQueueNumber
            paperSize = request.paperSize
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return receiptRepository.save(template).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdateReceiptRequest): ReceiptResponse {
        val template = receiptRepository.findByMerchantIdAndId(merchantId, request.receiptId)
            .orElseThrow { ResourceNotFoundException("Receipt template not found") }

        template.apply {
            header = request.header
            footer = request.footer
            showTax = request.showTax
            showServiceCharge = request.showServiceCharge
            showRounding = request.showRounding
            showLogo = request.showLogo
            logoUrl = request.logoUrl
            showQueueNumber = request.showQueueNumber
            paperSize = request.paperSize
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return receiptRepository.save(template).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, receiptId: Long) {
        val template = receiptRepository.findByMerchantIdAndId(merchantId, receiptId)
            .orElseThrow { ResourceNotFoundException("Receipt template not found") }
        receiptRepository.delete(template)
    }

    private fun ReceiptTemplate.toResponse() = ReceiptResponse(
        id = id,
        outletId = outletId,
        header = header,
        footer = footer,
        showTax = showTax,
        showServiceCharge = showServiceCharge,
        showRounding = showRounding,
        showLogo = showLogo,
        logoUrl = logoUrl,
        showQueueNumber = showQueueNumber,
        paperSize = paperSize
    )
}
