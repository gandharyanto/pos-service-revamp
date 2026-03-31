package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateTaxRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateTaxRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.TaxService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/tax")
class TaxController(
    private val taxService: TaxService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(taxService.list(merchantId)))
    }

    @GetMapping("/detail/{taxId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable taxId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(taxService.detail(merchantId, taxId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateTaxRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(taxService.create(merchantId, username, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateTaxRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(taxService.update(merchantId, username, request)))
    }

    @DeleteMapping("/delete/{taxId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable taxId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        taxService.delete(merchantId, taxId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Tax deleted"))
    }
}
