package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.LoyaltyService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/loyalty")
class LoyaltyController(
    private val loyaltyService: LoyaltyService,
    private val jwtUtil: JwtUtil
) {

    // ── Program ──────────────────────────────────────────────────────────

    /**
     * GET /pos/loyalty/list              — semua program
     * GET /pos/loyalty/list?isActive=true — hanya program aktif
     */
    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(required = false) isActive: Boolean?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.list(merchantId, isActive)))
    }

    @GetMapping("/detail/{id}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.detail(merchantId, id)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateLoyaltyProgramRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.create(merchantId, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateLoyaltyProgramRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.update(merchantId, request)))
    }

    @DeleteMapping("/delete/{id}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        loyaltyService.delete(merchantId, id)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("LoyaltyProgram deleted"))
    }

    // ── Product Setting ──────────────────────────────────────────────────

    @GetMapping("/product-setting/{productId}")
    fun getProductSetting(
        @RequestHeader("Authorization") auth: String,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.getProductSetting(merchantId, productId)))
    }

    @PutMapping("/product-setting")
    fun setProductSetting(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: SetProductLoyaltyRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(loyaltyService.setProductSetting(merchantId, request)))
    }
}
