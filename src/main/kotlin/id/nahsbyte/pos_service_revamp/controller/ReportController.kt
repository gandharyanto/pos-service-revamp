package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/summary-report")
class ReportController(
    private val reportService: ReportService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun summary(
        @RequestHeader("Authorization") auth: String,
        @RequestParam startDate: String,
        @RequestParam endDate: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(reportService.getSummary(merchantId, startDate, endDate)))
    }
}
