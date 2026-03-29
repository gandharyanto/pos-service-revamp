package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.*
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.PriceBookService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/price-book")
class PriceBookController(
    private val priceBookService: PriceBookService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(priceBookService.list(merchantId)))
    }

    @GetMapping("/detail/{priceBookId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(priceBookService.detail(merchantId, priceBookId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreatePriceBookRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            priceBookService.create(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdatePriceBookRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            priceBookService.update(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), request)
        ))
    }

    @DeleteMapping("/delete/{priceBookId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long
    ): ResponseEntity<ApiResponse<*>> {
        priceBookService.delete(jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth)), priceBookId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Price book deleted"))
    }

    // --- Items (PRODUCT / ORDER_TYPE) ---

    @PostMapping("/{priceBookId}/items")
    fun addItem(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @RequestBody request: AddPriceBookItemRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            priceBookService.addItem(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), priceBookId, request)
        ))
    }

    @DeleteMapping("/{priceBookId}/items/{productId}")
    fun deleteItem(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @PathVariable productId: Long
    ): ResponseEntity<ApiResponse<*>> {
        priceBookService.deleteItem(jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth)), priceBookId, productId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Item removed"))
    }

    // --- Wholesale Tiers (WHOLESALE) ---

    @PostMapping("/{priceBookId}/tiers")
    fun addTier(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @RequestBody request: AddWholesaleTierRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        return ResponseEntity.ok(ApiResponse.ok(
            priceBookService.addWholesaleTier(jwtUtil.extractMerchantId(token), jwtUtil.extractUsername(token), priceBookId, request)
        ))
    }

    @PutMapping("/{priceBookId}/tiers")
    fun updateTier(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @RequestBody request: UpdateWholesaleTierRequest
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(
            priceBookService.updateWholesaleTier(merchantId, priceBookId, request)
        ))
    }

    @DeleteMapping("/{priceBookId}/tiers/{tierId}")
    fun deleteTier(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @PathVariable tierId: Long
    ): ResponseEntity<ApiResponse<*>> {
        priceBookService.deleteWholesaleTier(jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth)), priceBookId, tierId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Tier deleted"))
    }

    // --- Outlets ---

    @PostMapping("/{priceBookId}/outlets")
    fun addOutlets(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @RequestBody body: Map<String, List<Long>>
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        priceBookService.addOutlets(merchantId, priceBookId, body["outletIds"] ?: emptyList())
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlets added"))
    }

    @DeleteMapping("/{priceBookId}/outlets/{outletId}")
    fun removeOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable priceBookId: Long,
        @PathVariable outletId: Long
    ): ResponseEntity<ApiResponse<*>> {
        priceBookService.removeOutlet(jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth)), priceBookId, outletId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Outlet removed"))
    }
}
