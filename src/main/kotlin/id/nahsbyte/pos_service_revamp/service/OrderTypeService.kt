package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.CreateOrderTypeRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateOrderTypeRequest
import id.nahsbyte.pos_service_revamp.dto.response.OrderTypeResponse
import id.nahsbyte.pos_service_revamp.entity.OrderType
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.OrderTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OrderTypeService(
    private val orderTypeRepository: OrderTypeRepository
) {

    fun list(merchantId: Long): List<OrderTypeResponse> =
        orderTypeRepository.findByMerchantId(merchantId).map { it.toResponse() }

    fun detail(merchantId: Long, id: Long): OrderTypeResponse =
        orderTypeRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .map { it.toResponse() }
            .orElseThrow { ResourceNotFoundException("OrderType not found") }

    @Transactional
    fun create(merchantId: Long, request: CreateOrderTypeRequest): OrderTypeResponse {
        // Jika isDefault = true, reset default lainnya
        if (request.isDefault) clearDefault(merchantId)

        val orderType = OrderType().apply {
            this.merchantId = merchantId
            name = request.name
            code = request.code
            isDefault = request.isDefault
            isActive = request.isActive
        }
        return orderTypeRepository.save(orderType).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, request: UpdateOrderTypeRequest): OrderTypeResponse {
        val orderType = orderTypeRepository.findById(request.id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("OrderType not found") }

        if (request.isDefault && !orderType.isDefault) clearDefault(merchantId)

        orderType.apply {
            name = request.name
            code = request.code
            isDefault = request.isDefault
            isActive = request.isActive
        }
        return orderTypeRepository.save(orderType).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, id: Long) {
        val orderType = orderTypeRepository.findById(id)
            .filter { it.merchantId == merchantId }
            .orElseThrow { ResourceNotFoundException("OrderType not found") }
        orderTypeRepository.delete(orderType)
    }

    private fun clearDefault(merchantId: Long) {
        orderTypeRepository.findByMerchantId(merchantId)
            .filter { it.isDefault }
            .forEach { it.isDefault = false; orderTypeRepository.save(it) }
    }

    private fun OrderType.toResponse() = OrderTypeResponse(
        id = id,
        name = name,
        code = code,
        isDefault = isDefault,
        isActive = isActive,
        createdDate = createdDate,
        modifiedDate = modifiedDate
    )
}
