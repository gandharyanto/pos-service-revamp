package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class ReportController(
    private val reportService: ReportService,
    private val jwtUtil: JwtUtil
) {

    // --- Legacy summary endpoint (product list + payment split) ---

    @GetMapping("/pos/summary-report/list")
    fun legacySummary(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSummary(merchantId, startDate, endDate)))
    }

    // --- Financial Report endpoints ---

    @GetMapping("/pos/report/summary")
    fun financialSummary(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(required = false) outletId: Long?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getFinancialSummary(merchantId, startDate, endDate, outletId)))
    }

    @GetMapping("/pos/report/payment-method")
    fun paymentMethodBreakdown(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(required = false) outletId: Long?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPaymentMethodBreakdown(merchantId, startDate, endDate, outletId)))
    }

    @GetMapping("/pos/report/top-products")
    fun topProducts(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTopProducts(merchantId, startDate, endDate, limit)))
    }

    @GetMapping("/pos/report/outlet")
    fun outletBreakdown(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getOutletBreakdown(merchantId, startDate, endDate)))
    }

    @GetMapping("/pos/report/disbursement")
    fun disbursementSummary(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDisbursementSummary(merchantId, startDate, endDate)))
    }
}
