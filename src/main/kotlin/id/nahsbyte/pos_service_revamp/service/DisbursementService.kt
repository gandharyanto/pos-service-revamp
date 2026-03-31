package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateDisbursementRuleRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateDisbursementRuleRequest
import id.nahsbyte.pos_service_revamp.dto.response.DisbursementLogResponse
import id.nahsbyte.pos_service_revamp.dto.response.DisbursementRuleResponse
import id.nahsbyte.pos_service_revamp.entity.DisbursementRule
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.DisbursementLogRepository
import id.nahsbyte.pos_service_revamp.repository.DisbursementRuleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DisbursementService(
    private val ruleRepository: DisbursementRuleRepository,
    private val logRepository: DisbursementLogRepository
) {

    fun listRules(merchantId: Long, activeOnly: Boolean): List<DisbursementRuleResponse> =
        if (activeOnly) ruleRepository.findAllByMerchantIdAndIsActiveTrue(merchantId).map { it.toResponse() }
        else ruleRepository.findAllByMerchantId(merchantId).map { it.toResponse() }

    fun detailRule(merchantId: Long, ruleId: Long): DisbursementRuleResponse =
        ruleRepository.findByMerchantIdAndId(merchantId, ruleId)
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("Disbursement rule not found") }

    @Transactional
    fun createRule(merchantId: Long, username: String, request: CreateDisbursementRuleRequest): DisbursementRuleResponse {
        val rule = DisbursementRule().apply {
            this.merchantId = merchantId
            name = request.name
            layer = request.layer
            recipientId = request.recipientId
            recipientName = request.recipientName
            percentage = request.percentage
            source = request.source
            productTypeFilter = request.productTypeFilter
            displayOrder = request.displayOrder
            isActive = true
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return ruleRepository.save(rule).toResponse()
    }

    @Transactional
    fun updateRule(merchantId: Long, username: String, request: UpdateDisbursementRuleRequest): DisbursementRuleResponse {
        val rule = ruleRepository.findByMerchantIdAndId(merchantId, request.ruleId)
            .orElseThrow { ResourceNotFoundException("Disbursement rule not found") }

        rule.apply {
            name = request.name
            layer = request.layer
            recipientId = request.recipientId
            recipientName = request.recipientName
            percentage = request.percentage
            source = request.source
            productTypeFilter = request.productTypeFilter
            displayOrder = request.displayOrder
            isActive = request.isActive
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return ruleRepository.save(rule).toResponse()
    }

    @Transactional
    fun deleteRule(merchantId: Long, ruleId: Long) {
        val rule = ruleRepository.findByMerchantIdAndId(merchantId, ruleId)
            .orElseThrow { ResourceNotFoundException("Disbursement rule not found") }
        rule.isActive = false
        ruleRepository.save(rule)
    }

    fun listLogs(merchantId: Long, startDate: String?, endDate: String?): List<DisbursementLogResponse> {
        if (startDate != null && endDate != null) {
            val from = LocalDate.parse(startDate).atStartOfDay()
            val to = LocalDate.parse(endDate).atTime(23, 59, 59)
            return logRepository.findAllByMerchantIdAndCreatedDateBetween(merchantId, from, to)
                .map { it.toResponse() }
        }
        return logRepository.findAllByMerchantId(merchantId).map { it.toResponse() }
    }

    private fun DisbursementRule.toResponse() = DisbursementRuleResponse(
        id = id,
        name = name,
        layer = layer,
        recipientId = recipientId,
        recipientName = recipientName,
        percentage = percentage,
        source = source,
        productTypeFilter = productTypeFilter,
        isActive = isActive,
        displayOrder = displayOrder
    )

    private fun id.nahsbyte.pos_service_revamp.entity.DisbursementLog.toResponse() = DisbursementLogResponse(
        id = id,
        transactionId = transactionId,
        ruleId = ruleId,
        recipientId = recipientId,
        recipientName = recipientName,
        layer = layer,
        baseAmount = baseAmount,
        percentage = percentage,
        amount = amount,
        status = status,
        createdDate = createdDate,
        settledDate = settledDate
    )
}
