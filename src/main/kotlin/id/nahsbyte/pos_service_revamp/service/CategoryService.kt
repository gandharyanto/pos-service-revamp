package id.nahsbyte.pos_service_revamp.service

import id.nahsbyte.pos_service_revamp.dto.request.AddCategoryRequest
import id.nahsbyte.pos_service_revamp.dto.request.UpdateCategoryRequest
import id.nahsbyte.pos_service_revamp.dto.response.CategoryResponse
import id.nahsbyte.pos_service_revamp.entity.Category
import id.nahsbyte.pos_service_revamp.exception.ResourceNotFoundException
import id.nahsbyte.pos_service_revamp.repository.CategoryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {

    fun list(merchantId: Long, page: Int, size: Int): Page<CategoryResponse> =
        categoryRepository.findAllByMerchantId(merchantId, PageRequest.of(page, size))
            .map { it.toResponse() }

    fun detail(merchantId: Long, categoryId: Long): CategoryResponse =
        categoryRepository.findByIdAndMerchantId(categoryId, merchantId)
            .orElseThrow { ResourceNotFoundException("Category not found") }
            .toResponse()

    @Transactional
    fun add(merchantId: Long, username: String, request: AddCategoryRequest): CategoryResponse {
        val category = Category().apply {
            this.merchantId = merchantId
            name = request.name
            image = request.image
            description = request.description
            createdBy = username
            createdDate = LocalDateTime.now()
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun update(merchantId: Long, username: String, request: UpdateCategoryRequest): CategoryResponse {
        val category = categoryRepository.findByIdAndMerchantId(request.categoryId, merchantId)
            .orElseThrow { ResourceNotFoundException("Category not found") }
        category.apply {
            name = request.name
            image = request.image
            description = request.description
            modifiedBy = username
            modifiedDate = LocalDateTime.now()
        }
        return categoryRepository.save(category).toResponse()
    }

    @Transactional
    fun delete(merchantId: Long, categoryId: Long) {
        val category = categoryRepository.findByIdAndMerchantId(categoryId, merchantId)
            .orElseThrow { ResourceNotFoundException("Category not found") }
        categoryRepository.delete(category)
    }

    private fun Category.toResponse() = CategoryResponse(id, name, image, description)
}
