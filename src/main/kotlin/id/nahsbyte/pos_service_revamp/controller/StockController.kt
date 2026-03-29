package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.UpdateStockRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.StockService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos")
class StockController(
    private val stockService: StockService,
    private val jwtUtil: JwtUtil
) {

    @PutMapping("/stock/update")
    fun updateStock(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: UpdateStockRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        stockService.updateStock(merchantId, username, request)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Stock updated"))
    }

    @GetMapping("/stock-movement/product/list")
    fun getMovements(
        @RequestHeader("Authorization") auth: String,
        @RequestParam productId: Long,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(stockService.getMovements(merchantId, productId, startDate, endDate)))
    }
}
