package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.AddProductRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateProductRequest
import id.nahsbyte.pos_service_revamp.dto.response.CategoryResponse
import id.nahsbyte.pos_service_revamp.dto.response.ProductResponse
import id.nahsbyte.pos_service_revamp.entity.Product
import id.nahsbyte.pos_service_revamp.entity.Stock
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.CategoryRepository
import id.nahsbyte.pos_service_revamp.repository.ProductRepository
import id.nahsbyte.pos_service_revamp.repository.StockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalDate

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val stockRepository: StockRepository
) {

    fun list(
        merchantId: Long,
        page: Int,
        size: Int,
        keyword: String?,
        categoryId: Long?,
        sku: String?,
        upc: String?,
        startDate: String?,
        endDate: String?,
        sortBy: String,
        sortDir: String
    ): Page<ProductResponse> {
        val sort = if (sortDir.uppercase() == "DESC") Sort.by(sortBy).descending() else Sort.by(sortBy).ascending()
        val pageable = PageRequest.of(page, size, sort)
        val start = startDate?.let { LocalDate.parse(it).atStartOfDay() }
        val end = endDate?.let { LocalDate.parse(it).atTime(23, 59, 59) }

        return productRepository.searchProducts(merchantId, keyword, sku, upc, categoryId, start, end, pageable)
            .map { it.toResponse(stockRepository.findByProductId(it.id).map { s -> s.qty }.orElse(0)) }
    }

    fun detail(merchantId: Long, productId: Long): ProductResponse {
        val product = productRepository.findByIdAndMerchantIdAndDeletedDateIsNull(productId, merchantId)
            .orElseThrow { ResourceNotFoundException("Product not found") }
        val qty = stockRepository.findByProductId(productId).map { it.qty }.orElse(0)
        return product.toResponse(qty)
    }

    @Transactional
    fun add(merchantId: Long, username: String, request: AddProductRequest): ProductResponse {
        val categories = if (request.categoryIds.isNotEmpty())
            categoryRepository.findAllById(request.categoryIds).toMutableSet()
        else mutableSetOf()

        val product = Product().apply {
            this.merchantId = merchantId
            name = request.name
            price = request.price
            sku = request.sku
            upc = request.upc
            imageUrl = request.imageUrl
            imageThumbUrl = request.imageThumbUrl
            description = request.description
            this.categories = categories
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = productRepository.save(product)

        val stock = Stock().apply {
            productId = saved.id
            qty = request.qty
            createdBy = username
            createdDate = LocalDateTime.now()
        }
        stockRepository.save(stock)

        return saved.toResponse(request.qty)
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findByIdAndMerchantIdAndDeletedDateIsNull(request.productId, merchantId)
            .orElseThrow { ResourceNotFoundException("Product not found") }

        val categories = if (request.categoryIds.isNotEmpty())
            categoryRepository.findAllById(request.categoryIds).toMutableSet()
        else mutableSetOf()

        product.apply {
            name = request.name
            price = request.price
            sku = request.sku
            upc = request.upc
            imageUrl = request.imageUrl
            imageThumbUrl = request.imageThumbUrl
            description = request.description
            this.categories = categories
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        val saved = productRepository.save(product)
        val qty = stockRepository.findByProductId(saved.id).map { it.qty }.orElse(0)
        return saved.toResponse(qty)
    }

    @Transactional
    fun delete(merchantId: Long, username: String, productId: Long) {
        val product = productRepository.findByIdAndMerchantIdAndDeletedDateIsNull(productId, merchantId)
            .orElseThrow { ResourceNotFoundException("Product not found") }
        product.deletedBy = username
        product.deletedDate = LocalDateTime.now()
        productRepository.save(product)
    }

    private fun Product.toResponse(qty: Int) = ProductResponse(
        id = id,
        name = name,
        price = price,
        sku = sku,
        upc = upc,
        imageUrl = imageUrl,
        imageThumbUrl = imageThumbUrl,
        description = description,
        stockQty = qty,
        isTaxable = isTaxable,
        taxId = taxId,
        categories = categories.map { CategoryResponse(it.id, it.name, it.image, it.description) },
        createdDate = createdDate,
        modifiedDate = modifiedDate
    )
}
