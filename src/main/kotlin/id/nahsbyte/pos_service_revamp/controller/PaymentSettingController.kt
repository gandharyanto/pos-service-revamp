package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreatePaymentSettingRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePaymentSettingRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.PaymentSettingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos")
class PaymentSettingController(
    private val paymentSettingService: PaymentSettingService,
    private val jwtUtil: JwtUtil
) {

    /** Default setting (tidak terikat outlet). */
    @GetMapping("/payment-setting")
    fun get(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.get(merchantId)))
    }

    /** Semua setting: default + per-outlet overrides. */
    @GetMapping("/payment-setting/list")
    fun list(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.list(merchantId)))
    }

    /** Setting efektif untuk outlet tertentu (fallback ke default jika override tidak ada). */
    @GetMapping("/payment-setting/outlet/{outletId}")
    fun getByOutlet(
        @RequestHeader("Authorization") auth: String,
        @PathVariable outletId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.getByOutlet(merchantId, outletId)))
    }

    @PostMapping("/payment-setting/create")
    fun create(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreatePaymentSettingRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.create(merchantId, username, request)))
    }

    @PutMapping("/payment-setting/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdatePaymentSettingRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.update(merchantId, username, request)))
    }

    @GetMapping("/payment-method/merchant/list")
    fun getPaymentMethods(@RequestHeader("Authorization") auth: String): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(paymentSettingService.getPaymentMethods(merchantId)))
    }
}
