package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Kelompok voucher dalam satu brand.
 * Mendefinisikan aturan dan nilai yang berlaku untuk semua kode dalam grup ini.
 *
 * purchasePrice = harga beli / cost (internal)
 * sellingPrice  = nilai yang bisa ditukarkan saat transaksi (face value)
 *
 * Contoh: Voucher Makan Siang
 *   purchasePrice = 45.000 (cost ke merchant)
 *   sellingPrice  = 50.000 (nilai yang diterima pelanggan)
 */
@Entity
@Table(name = "voucher_group")
class VoucherGroup : BaseAuditEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "brand_id", nullable = false)
    var brandId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0

    var name: String = ""

    /** Harga beli / cost voucher ini */
    @Column(name = "purchase_price", nullable = false)
    var purchasePrice: BigDecimal = BigDecimal.ZERO

    /** Nilai yang bisa ditukarkan saat transaksi */
    @Column(name = "selling_price", nullable = false)
    var sellingPrice: BigDecimal = BigDecimal.ZERO

    /** Tanggal kadaluarsa. Null = tidak ada batas */
    @Column(name = "expired_date")
    var expiredDate: LocalDateTime? = null

    /**
     * Hari valid, comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN.
     * Null = berlaku setiap hari.
     */
    @Column(name = "valid_days")
    var validDays: String? = null

    /** Hanya bisa digunakan oleh customer terdaftar */
    @Column(name = "is_required_customer")
    var isRequiredCustomer: Boolean = false

    /** POS | ONLINE | BOTH */
    var channel: String = "BOTH"

    @Column(name = "is_active")
    var isActive: Boolean = true
}
