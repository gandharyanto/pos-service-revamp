package id.nahsbyte.pos_service_revamp.dto.request

import java.math.BigDecimal

data class TransactionItemRequest(
    val productId: Long,
    val qty: Int,
    // Fields below are informational only — server overrides with values from DB
    val productName: String? = null,
    val price: BigDecimal? = null,
    val totalPrice: BigDecimal? = null,
    val taxId: Long? = null,
    val taxAmount: BigDecimal? = null,
    val discountAmount: BigDecimal? = null
)

data class CreateTransactionRequest(
    /** Subtotal BERSIH setelah price book + promosi + diskon, sebelum tax/SC/rounding */
    val subTotal: BigDecimal,
    val totalServiceCharge: BigDecimal = BigDecimal.ZERO,
    val totalTax: BigDecimal = BigDecimal.ZERO,
    val totalRounding: BigDecimal = BigDecimal.ZERO,
    val totalAmount: BigDecimal,
    val paymentMethod: String,
    val cashTendered: BigDecimal? = null,
    val cashChange: BigDecimal? = null,
    val priceIncludeTax: Boolean = false,
    val queueNumber: Int? = null,

    /** Order type untuk price book ORDER_TYPE */
    val orderTypeId: Long? = null,

    /** Customer ID untuk eligibilitas promosi/diskon */
    val customerId: Long? = null,

    /** Kode diskon yang diinput kasir/pelanggan */
    val discountCode: String? = null,

    /** Total diskon dari promosi otomatis (untuk validasi) */
    val promoAmount: BigDecimal? = null,

    /** Total diskon dari kode diskon (untuk validasi) */
    val discountAmount: BigDecimal? = null,

    /**
     * Kode voucher yang digunakan sebagai metode pembayaran.
     * Voucher mengurangi totalAmount dari sisi pembayaran (bukan dari subtotal).
     * Tax dan SC tetap dihitung dari total penuh.
     */
    val voucherCode: String? = null,

    /** Nilai voucher yang diklaim (untuk validasi vs server) */
    val voucherAmount: BigDecimal? = null,

    /**
     * Poin loyalty yang ingin diredeem.
     * Mode redeem ditentukan oleh konfigurasi LoyaltyRedemptionRule aktif.
     */
    val loyaltyRedeemPoints: BigDecimal? = null,

    /** Mode redeem: PAYMENT | DISCOUNT. Default server-side dari rule aktif. */
    val loyaltyRedeemMode: String? = null,

    /** Nilai rupiah redeem poin yang diklaim (untuk validasi vs server, hanya mode PAYMENT) */
    val loyaltyRedeemAmount: BigDecimal? = null,

    val transactionItems: List<TransactionItemRequest>
)

data class UpdateTransactionRequest(
    val paymentTrxId: String? = null,
    val paymentMethod: String,
    val amountPaid: BigDecimal,
    val status: String,
    val paymentReference: String? = null,
    val paymentDate: String? = null
)
