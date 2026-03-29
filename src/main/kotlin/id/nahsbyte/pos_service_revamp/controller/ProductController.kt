package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.AddProductRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateProductRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.ProductService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/product")
class ProductController(
    private val productService: ProductService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) sku: String?,
        @RequestParam(required = false) upc: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(defaultValue = "createdDate") sortBy: String,
        @RequestParam(defaultValue = "DESC") sortDir: String
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        val result = productService.list(merchantId, page, size, keyword, categoryId, sku, upc, startDate, endDate, sortBy, sortDir)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping("/detail/{productId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(productService.detail(merchantId, productId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: AddProductRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(productService.add(merchantId, username, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: UpdateProductRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(productService.update(merchantId, username, request)))
    }

    @DeleteMapping("/delete/{productId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        productService.delete(merchantId, username, productId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Product deleted"))
    }
}
