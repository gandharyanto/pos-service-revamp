package id.nahsbyte.pos_service_revamp.entity

import jakarta.persistence.*

/** Binding price book ke outlet tertentu (untuk visibility=SPECIFIC_OUTLET) */
@Entity
@Table(name = "price_book_outlet")
class PriceBookOutlet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "price_book_id", nullable = false)
    var priceBookId: Long = 0

    @Column(name = "outlet_id", nullable = false)
    var outletId: Long = 0

    @Column(name = "merchant_id", nullable = false)
    var merchantId: Long = 0
}
