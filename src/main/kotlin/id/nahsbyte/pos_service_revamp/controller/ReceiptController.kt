package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateReceiptRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateReceiptRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.ReceiptService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/receipt-template")
class ReceiptController(
    private val receiptService: ReceiptService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(receiptService.list(merchantId)))
    }

    @GetMapping("/detail/{receiptId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable receiptId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(receiptService.detail(merchantId, receiptId)))
    }

    @GetMapping("/outlet/{outletId}")
    fun getByOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable outletId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(receiptService.getByOutlet(merchantId, outletId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateReceiptRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(receiptService.create(merchantId, username, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateReceiptRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(receiptService.update(merchantId, username, request)))
    }

    @DeleteMapping("/delete/{receiptId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable receiptId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        receiptService.delete(merchantId, receiptId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Receipt template deleted"))
    }
}
