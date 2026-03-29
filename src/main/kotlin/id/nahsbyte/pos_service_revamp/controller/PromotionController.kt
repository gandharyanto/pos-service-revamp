package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreatePromotionRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePromotionRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.PromotionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/promotion")
class PromotionController(
    private val promotionService: PromotionService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(promotionService.list(merchantId)))
    }

    @GetMapping("/detail/{promotionId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(promotionService.detail(merchantId, promotionId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreatePromotionRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            promotionService.create(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdatePromotionRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            promotionService.update(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @DeleteMapping("/delete/{promotionId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long
    ): ResponseEntity<ApiResponse<*>> {
        promotionService.delete(jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth)), promotionId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Promotion deleted"))
    }

    // --- Buy condition products ---

    @PostMapping("/{promotionId}/buy-products")
    fun addBuyProducts(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.addBuyProducts(merchantId, promotionId, body["productIds"], body["categoryIds"])
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Buy products added"))
    }

    @DeleteMapping("/{promotionId}/buy-products")
    fun removeBuyProduct(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(required = false) categoryId: Long?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.removeBuyProduct(merchantId, promotionId, productId, categoryId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Buy product removed"))
    }

    // --- Reward products ---

    @PostMapping("/{promotionId}/reward-products")
    fun addRewardProducts(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.addRewardProducts(merchantId, promotionId, body["productIds"], body["categoryIds"])
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Reward products added"))
    }

    @DeleteMapping("/{promotionId}/reward-products")
    fun removeRewardProduct(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(required = false) categoryId: Long?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.removeRewardProduct(merchantId, promotionId, productId, categoryId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Reward product removed"))
    }

    // --- Outlets ---

    @PostMapping("/{promotionId}/outlets")
    fun addOutlets(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.addOutlets(merchantId, promotionId, body["outletIds"] ?: emptyList())
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlets added"))
    }

    @DeleteMapping("/{promotionId}/outlets/{outletId}")
    fun removeOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable promotionId: Long,
        @PathVariable outletId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        promotionService.removeOutlet(merchantId, promotionId, outletId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlet removed"))
    }
}
