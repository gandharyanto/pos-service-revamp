package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreateDiscountRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateDiscountRequest
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

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.list(merchantId)))
    }

    @GetMapping("/detail/{discountId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(discountService.detail(merchantId, discountId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateDiscountRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            discountService.create(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateDiscountRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            discountService.update(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @DeleteMapping("/delete/{discountId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        discountService.delete(jwtUtil.extractMerchantId(token), discountId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Discount deleted"))
    }

    // --- Product binding ---

    @PostMapping("/{discountId}/products")
    fun addProducts(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.addProducts(merchantId, discountId, body["productIds"] ?: emptyList())
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Products added"))
    }

    @DeleteMapping("/{discountId}/products/{productId}")
    fun removeProduct(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.removeProduct(merchantId, discountId, productId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Product removed"))
    }

    // --- Category binding ---

    @PostMapping("/{discountId}/categories")
    fun addCategories(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.addCategories(merchantId, discountId, body["categoryIds"] ?: emptyList())
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Categories added"))
    }

    @DeleteMapping("/{discountId}/categories/{categoryId}")
    fun removeCategory(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @PathVariable categoryId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.removeCategory(merchantId, discountId, categoryId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Category removed"))
    }

    // --- Outlet binding ---

    @PostMapping("/{discountId}/outlets")
    fun addOutlets(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.addOutlets(merchantId, discountId, body["outletIds"] ?: emptyList())
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlets added"))
    }

    @DeleteMapping("/{discountId}/outlets/{outletId}")
    fun removeOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable discountId: Long,
        @PathVariable outletId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        discountService.removeOutlet(merchantId, discountId, outletId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlet removed"))
    }
}
