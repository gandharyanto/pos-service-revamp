package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateCashierRequest
import id.nahsbyte.pos_service_revamp.dto.request.ResetCashierPasswordRequest
import id.nahsbyte.pos_service_revamp.dto.request.SetCashierPinRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCashierRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.CashierService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/cashier")
class CashierManagementController(
    private val cashierService: CashierService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(cashierService.list(merchantId)))
    }

    @GetMapping("/detail/{cashierId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable cashierId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(cashierService.detail(merchantId, cashierId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateCashierRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(cashierService.create(merchantId, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateCashierRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(cashierService.update(merchantId, request)))
    }

    @DeleteMapping("/delete/{cashierId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable cashierId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        cashierService.delete(merchantId, cashierId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Cashier deactivated"))
    }

    @PutMapping("/set-pin")
    fun setPin(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: SetCashierPinRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        cashierService.setPin(merchantId, request)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("PIN set successfully"))
    }

    @PutMapping("/reset-password")
    fun resetPassword(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: ResetCashierPasswordRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        cashierService.resetPassword(merchantId, request)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Password reset successfully"))
    }
}
