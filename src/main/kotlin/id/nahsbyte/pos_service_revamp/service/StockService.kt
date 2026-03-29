package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.StockUpdateType
import id.nahsbyte.pos_service_revamp.dto.request.UpdateStockRequest
import id.nahsbyte.pos_service_revamp.dto.response.StockMovementResponse
import id.nahsbyte.pos_service_revamp.entity.Stock
import id.nahsbyte.pos_service_revamp.entity.StockMovement
import id.nahsbyte.pos_service_revamp.exception.BusinessException
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.ProductRepository
import id.nahsbyte.pos_service_revamp.repository.StockMovementRepository
import id.nahsbyte.pos_service_revamp.repository.StockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class StockService(
    private val stockRepository: StockRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productRepository: ProductRepository
) {

    @Transactional
    fun updateStock(merchantId: Long, username: String, request: UpdateStockRequest) {
        val product = productRepository.findByIdAndMerchantIdAndDeletedDateIsNull(request.productId, merchantId)
            .orElseThrow { ResourceNotFoundException("Product not found") }

        val stock = stockRepository.findByProductId(product.id).orElseGet {
            Stock().apply {
                productId = product.id
                qty = 0
                createdBy = username
                createdDate = LocalDateTime.now()
            }
        }

        val newQty = when (request.updateType) {
            StockUpdateType.ADD -> stock.qty + request.qty
            StockUpdateType.SUBTRACT -> {
                if (stock.qty < request.qty) throw BusinessException("Insufficient stock")
                stock.qty - request.qty
            }
            StockUpdateType.SET -> request.qty
        }

        stock.qty = newQty
        stock.modifiedBy = username
        stock.modifiedDate = LocalDateTime.now()
        stockRepository.save(stock)

        val movement = StockMovement().apply {
            productId = product.id
            this.merchantId = merchantId
            qty = request.qty
            movementType = request.updateType.name
            createdBy = username
            createdDate = LocalDateTime.now()
        }
        stockMovementRepository.save(movement)
    }

    fun getMovements(merchantId: Long, productId: Long, startDate: String, endDate: String): List<StockMovementResponse> {
        val start = LocalDate.parse(startDate).atStartOfDay()
        val end = LocalDate.parse(endDate).atTime(23, 59, 59)
        return stockMovementRepository.findAllByProductIdAndCreatedDateBetween(productId, start, end)
            .map {
                StockMovementResponse(
                    id = it.id,
                    productId = it.productId,
                    qty = it.qty,
                    movementType = it.movementType,
                    movementReason = it.movementReason,
                    note = it.note,
                    createdBy = it.createdBy,
                    createdDate = it.createdDate
                )
            }
    }
}
