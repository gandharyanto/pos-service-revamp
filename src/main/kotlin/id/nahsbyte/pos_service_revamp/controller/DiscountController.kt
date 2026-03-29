package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.request.ValidateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.DiscountService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/discount")
class DiscountController(
    private val discountService: DiscountService,
    private val jwtUtil: JwtUtil
) {

    /**
     * GET /pos/discount/list
     * GET /pos/discount/list?isActive=true&channel=POS&scope=PRODUCT&keyword=lebaran
     */
    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(required = false) channel: String?,
        @RequestParam(required = false) scope: String?,
        @RequestParam(required = false) keyword: String?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.list(merchantId, isActive, channel, scope, keyword)))
    }

    @GetMapping("/detail/{id}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.detail(merchantId, id)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateDiscountRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.create(merchantId, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateDiscountRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.update(merchantId, request)))
    }

    @DeleteMapping("/delete/{id}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.delete(merchantId, id)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Discount deleted"))
    }

    @PostMapping("/validate")
    fun validate(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: ValidateDiscountRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.validate(merchantId, request)))
    }
}
