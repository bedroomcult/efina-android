package com.efina.app.domain.model

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "IDR",
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val displayOrder: Int = 0,
    val includeInTotal: Boolean = true,
    val showInDashboard: Boolean = true,
    val createdAt: String? = null
)

enum class AccountType(val displayName: String, val value: String) {
    CHECKING("Checking", "checking"),
    SAVINGS("Savings", "savings"),
    INVESTMENT("Investment", "investment"),
    CREDIT("Credit Card", "credit"),
    CASH("Cash", "cash"),
    OTHER("Other", "other")
}

data class CreateAccountRequest(
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val currency: String = "IDR",
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val includeInTotal: Boolean = true,
    val showInDashboard: Boolean = true
)

data class UpdateAccountRequest(
    val name: String? = null,
    val balance: Double? = null,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    val includeInTotal: Boolean? = null,
    val showInDashboard: Boolean? = null
)
