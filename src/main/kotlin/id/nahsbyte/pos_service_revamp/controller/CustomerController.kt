package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.AdjustLoyaltyPointsRequest
import id.nahsbyte.pos_service_revamp.dto.request.CreateCustomerRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCustomerRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.CustomerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/customer")
class CustomerController(
    private val customerService: CustomerService,
    private val jwtUtil: JwtUtil
) {

    /**
     * GET /pos/customer/list           — semua customer
     * GET /pos/customer/list?phone=08x — cari by phone
     * GET /pos/customer/list?email=x@x — cari by email
     */
    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(required = false) phone: String?,
        @RequestParam(required = false) email: String?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(customerService.list(merchantId, phone, email)))
    }

    @GetMapping("/detail/{customerId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable customerId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(customerService.detail(merchantId, customerId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateCustomerRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(customerService.create(merchantId, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateCustomerRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(customerService.update(merchantId, request)))
    }

    @DeleteMapping("/delete/{customerId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable customerId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        customerService.delete(merchantId, customerId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Customer deleted"))
    }

    // ── Loyalty Points ────────────────────────────────────────────────────

    @GetMapping("/{customerId}/loyalty-history")
    fun loyaltyHistory(
        @RequestHeader("Authorization") auth: String,
        @PathVariable customerId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(customerService.loyaltyHistory(merchantId, customerId)))
    }

    @PostMapping("/loyalty/adjust")
    fun adjustPoints(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: AdjustLoyaltyPointsRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            customerService.adjustPoints(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }
}
