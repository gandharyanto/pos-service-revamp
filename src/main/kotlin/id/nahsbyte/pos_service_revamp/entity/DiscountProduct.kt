package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/**
 * Binding produk ke diskon (many-to-many).
 * Digunakan untuk tipe ITEM_QTY dan ITEM_SUBTOTAL:
 * - ITEM_QTY      — produk yang qty-nya dihitung untuk menentukan tier
 * - ITEM_SUBTOTAL — produk yang subtotal-nya dihitung untuk menentukan tier
 *
 * Satu diskon bisa berlaku untuk satu atau lebih produk.
 * Jika tidak ada entry di tabel ini, diskon berlaku untuk semua produk (null = semua).
 */
@Entity
@Table(name = "discount_product")
class DiscountProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "discount_id", nullable = false)
    var discountId: Long = 0

    @Column(name = "product_id", nullable = false)
    var productId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
