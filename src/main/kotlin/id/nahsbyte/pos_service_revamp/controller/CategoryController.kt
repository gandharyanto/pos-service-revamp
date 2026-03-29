package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.AddCategoryRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCategoryRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/category")
class CategoryController(
    private val categoryService: CategoryService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "100") size: Int
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(categoryService.list(merchantId, page, size)))
    }

    @GetMapping("/detail/{categoryId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable categoryId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(categoryService.detail(merchantId, categoryId)))
    }

    @PostMapping("/single/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: AddCategoryRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(categoryService.add(merchantId, username, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @Valid @RequestBody request: UpdateCategoryRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(merchantId, username, request)))
    }

    @DeleteMapping("/delete/{categoryId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable categoryId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        categoryService.delete(merchantId, categoryId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Category deleted"))
    }
}
