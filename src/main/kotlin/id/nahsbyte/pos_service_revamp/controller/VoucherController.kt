package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.VoucherService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/voucher")
class VoucherController(
    private val voucherService: VoucherService,
    private val jwtUtil: JwtUtil
) {

    // ── Brand ──────────────────────────────────────────────────────────────

    @GetMapping("/brand/list")
    fun listBrands(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.listBrands(merchantId)))
    }

    @GetMapping("/brand/detail/{brandId}")
    fun detailBrand(
        @RequestHeader("Authorization") auth: String,
        @PathVariable brandId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.detailBrand(merchantId, brandId)))
    }

    @PostMapping("/brand/add")
    fun addBrand(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateVoucherBrandRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.createBrand(merchantId, request)))
    }

    @PutMapping("/brand/update")
    fun updateBrand(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateVoucherBrandRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.updateBrand(merchantId, request)))
    }

    @DeleteMapping("/brand/delete/{brandId}")
    fun deleteBrand(
        @RequestHeader("Authorization") auth: String,
        @PathVariable brandId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        voucherService.deleteBrand(merchantId, brandId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("VoucherBrand deleted"))
    }

    // ── Group ──────────────────────────────────────────────────────────────
    // Group list & detail tersedia via brand/list dan brand/detail/{id}

    @PostMapping("/group/add")
    fun addGroup(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreateVoucherGroupRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.createGroup(merchantId, request)))
    }

    @PutMapping("/group/update")
    fun updateGroup(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdateVoucherGroupRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.updateGroup(merchantId, request)))
    }

    @DeleteMapping("/group/delete/{groupId}")
    fun deleteGroup(
        @RequestHeader("Authorization") auth: String,
        @PathVariable groupId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        voucherService.deleteGroup(merchantId, groupId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("VoucherGroup deleted"))
    }

    // ── Code ──────────────────────────────────────────────────────────────

    @GetMapping("/code/list")
    fun listCodes(
        @RequestHeader("Authorization") auth: String,
        @RequestParam groupId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.listCodes(merchantId, groupId)))
    }

    @PostMapping("/code/add")
    fun addCode(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: AddVoucherCodeRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.addCode(merchantId, request)))
    }

    @PostMapping("/code/bulk-import")
    fun bulkImport(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: BulkImportVoucherCodesRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.bulkImport(merchantId, request)))
    }

    @PutMapping("/code/cancel/{voucherId}")
    fun cancelCode(
        @RequestHeader("Authorization") auth: String,
        @PathVariable voucherId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(voucherService.cancelCode(merchantId, voucherId)))
    }
}
