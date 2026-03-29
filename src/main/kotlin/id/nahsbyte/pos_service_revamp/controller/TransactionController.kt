package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateTransactionRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.TransactionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/transaction")
class TransactionController(
    private val transactionService: TransactionService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam startDate: String,
        @RequestParam endDate: String,
        @RequestParam(defaultValue = "createdDate") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortType: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(transactionService.list(merchantId, page, size, startDate, endDate, sortBy, sortType)))
    }

    @GetMapping("/detail/{transactionId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable transactionId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(transactionService.detail(merchantId, transactionId)))
    }

    @PostMapping("/create")
    fun create(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateTransactionRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(transactionService.create(merchantId, username, request)))
    }

    @PutMapping("/update/{merchantTrxId}")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @PathVariable merchantTrxId: String,
        @RequestBody request: UpdateTransactionRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        transactionService.update(merchantId, username, merchantTrxId, request)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Transaction updated"))
    }
}
