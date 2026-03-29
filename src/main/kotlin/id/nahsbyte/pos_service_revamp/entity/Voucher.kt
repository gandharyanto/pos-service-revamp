package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Kode voucher individual — satu baris = satu kode yang bisa ditukarkan.
 *
 * Hierarki: VoucherBrand → VoucherGroup → VoucherCode (tabel ini)
 *
 * Status:
 *   AVAILABLE  — belum digunakan, bisa ditukarkan
 *   USED       — sudah ditukarkan, tidak bisa digunakan lagi
 *   EXPIRED    — melewati expired_date group
 *   CANCELLED  — dibatalkan oleh merchant
 *
 * Voucher berfungsi sebagai PAYMENT METHOD, bukan diskon.
 * Nilai yang ditukarkan = VoucherGroup.sellingPrice.
 * Tax dan SC tetap dihitung dari total penuh (sebelum voucher).
 */
@Entity
@Table(name = "voucher")
class Voucher : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    @Column(unique = true)
    var code: String = ""

    // --- Kolom revamp (nullable — aman untuk production) ---

    /** FK ke voucher_group. Null = data lama sebelum revamp */
    @Column(name = "group_id")
    var groupId: Long? = null

    /**
     * AVAILABLE | USED | EXPIRED | CANCELLED
     * Null = data lama (gunakan isActive untuk status lama)
     */
    var status: String? = null

    /** Tanggal dan waktu kode ini ditukarkan */
    @Column(name = "used_date")
    var usedDate: LocalDateTime? = null

    /** Transaction ID yang menggunakan kode ini */
    @Column(name = "transaction_id")
    var transactionId: Long? = null

    // --- Kolom lama (dipertahankan untuk backward compatibility) ---

    /** @deprecated Gunakan VoucherGroup.sellingPrice. Dipertahankan untuk data lama. */
    var type: String = "AMOUNT"

    /** @deprecated Gunakan VoucherGroup.sellingPrice. Dipertahankan untuk data lama. */
    var value: BigDecimal = BigDecimal.ZERO

    /** @deprecated Gunakan VoucherGroup.expiredDate. */
    @Column(name = "min_purchase")
    var minPurchase: BigDecimal? = null

    @Column(name = "max_use")
    var maxUse: Int? = null

    @Column(name = "used_count")
    var usedCount: Int = 0

    @Column(name = "expired_date")
    var expiredDate: LocalDateTime? = null

    @Column(name = "is_active")
    var isActive: Boolean = true
}
