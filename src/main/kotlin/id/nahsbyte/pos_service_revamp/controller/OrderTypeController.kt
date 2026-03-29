package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateOrderTypeRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateOrderTypeRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.OrderTypeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/order-type")
class OrderTypeController(
    private val orderTypeService: OrderTypeService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(orderTypeService.list(merchantId)))
    }

    @GetMapping("/detail/{id}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(orderTypeService.detail(merchantId, id)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateOrderTypeRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(orderTypeService.create(merchantId, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateOrderTypeRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(orderTypeService.update(merchantId, request)))
    }

    @DeleteMapping("/delete/{id}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        orderTypeService.delete(merchantId, id)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("OrderType deleted"))
    }
}
