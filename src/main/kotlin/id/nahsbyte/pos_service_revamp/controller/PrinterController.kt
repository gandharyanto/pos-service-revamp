package id.nahsbyte.pos_service_revamp.controller

import id.nahsbyte.pos_service_revamp.dto.request.CreatePrinterRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdatePrinterRequest
import id.nahsbyte.pos_service_revamp.dto.response.ApiResponse
import id.nahsbyte.pos_service_revamp.security.JwtUtil
import id.nahsbyte.pos_service_revamp.service.PrinterService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/pos/printer")
class PrinterController(
    private val printerService: PrinterService,
    private val jwtUtil: JwtUtil
) {

    @GetMapping("/list")
    fun list(
        @RequestHeader("Authorization") auth: String,
        @RequestParam(required = false) outletId: Long?
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(printerService.list(merchantId, outletId)))
    }

    @GetMapping("/detail/{printerId}")
    fun detail(
        @RequestHeader("Authorization") auth: String,
        @PathVariable printerId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        return ResponseEntity.ok(ApiResponse.ok(printerService.detail(merchantId, printerId)))
    }

    @PostMapping("/add")
    fun add(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: CreatePrinterRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(printerService.create(merchantId, username, request)))
    }

    @PutMapping("/update")
    fun update(
        @RequestHeader("Authorization") auth: String,
        @RequestBody request: UpdatePrinterRequest
    ): ResponseEntity<ApiResponse<*>> {
        val token = jwtUtil.resolveToken(auth)
        val merchantId = jwtUtil.extractMerchantId(token)
        val username = jwtUtil.extractUsername(token)
        return ResponseEntity.ok(ApiResponse.ok(printerService.update(merchantId, username, request)))
    }

    @DeleteMapping("/delete/{printerId}")
    fun delete(
        @RequestHeader("Authorization") auth: String,
        @PathVariable printerId: Long
    ): ResponseEntity<ApiResponse<*>> {
        val merchantId = jwtUtil.extractMerchantId(jwtUtil.resolveToken(auth))
        printerService.delete(merchantId, printerId)
        return ResponseEntity.ok(ApiResponse.ok<Nothing>("Printer deleted"))
    }
}
