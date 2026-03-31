package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateDisbursementRuleRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateDisbursementRuleRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.DisbursementService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/disbursement")
class DisbursementController(
    private val disbursementService: DisbursementService,
    private val jwtUtil: JwtUtil
) {

    // --- Rules ---

    @GetMapping("/rule/list")
    fun listRules(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(defaultValue = "false") activeOnly: Boolean
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(disbursementService.listRules(merchantId, activeOnly)))
    }

    @GetMapping("/rule/detail/{ruleId}")
    fun detailRule(
        @RequestHeader("Authorization") auth: String,
        @PathVariable ruleId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(disbursementService.detailRule(merchantId, ruleId)))
    }

    @PostMapping("/rule/add")
    fun addRule(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateDisbursementRuleRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(disbursementService.createRule(merchantId, username, request)))
    }

    @PutMapping("/rule/update")
    fun updateRule(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateDisbursementRuleRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(disbursementService.updateRule(merchantId, username, request)))
    }

    @DeleteMapping("/rule/delete/{ruleId}")
    fun deleteRule(
        @RequestHeader("Authorization") auth: String,
        @PathVariable ruleId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        disbursementService.deleteRule(merchantId, ruleId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Disbursement rule deactivated"))
    }

    // --- Logs ---

    @GetMapping("/log/list")
    fun listLogs(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(disbursementService.listLogs(merchantId, startDate, endDate)))
    }
}
