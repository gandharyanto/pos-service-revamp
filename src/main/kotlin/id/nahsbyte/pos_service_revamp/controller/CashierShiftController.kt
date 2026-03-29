package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CloseShiftRequest
import id.nahsbyte.pos_service_revamp.dto.request.OpenShiftRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.CashierShiftService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/shift")
class CashierShiftController(
    private val cashierShiftService: CashierShiftService,
    private val jwtUtil: JwtUtil
) {

    /**
     * GET /pos/shift/list/{outletId}              — semua shift
     * GET /pos/shift/list/{outletId}?status=OPEN  — shift yang sedang aktif
     */
    @GetMapping("/list/{outletId}")
    fun listByOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable outletId: Long,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(cashierShiftService.listByOutlet(merchantId, outletId, status)))
    }

    @PostMapping("/open")
    fun openShift(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: OpenShiftRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            cashierShiftService.openShift(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @PutMapping("/close")
    fun closeShift(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CloseShiftRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            cashierShiftService.closeShift(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }
}
