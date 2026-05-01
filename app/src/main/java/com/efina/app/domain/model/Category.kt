package com.efina.app.domain.model

data class Category(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val color: String,
    val icon: String?,
    val parentId: Long?,
    val parentName: String? = null
)

enum class CategoryType(val value: String) {
    INCOME("income"),
    EXPENSE("expense")
}

data class CreateCategoryRequest(
    val name: String,
    val type: CategoryType,
    val color: String,
    val icon: String? = null,
    val parentId: Long? = null
)

data class UpdateCategoryRequest(
    val name: String? = null,
    val color: String? = null,
    val icon: String? = null
)
