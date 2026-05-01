package com.efina.app.domain.model

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "IDR",
    val description: String? = null,
    val displayOrder: Int = 0,
    val initialBalance: Double = 0.0,
    val createdAt: String? = null
)

enum class AccountType(val displayName: String, val value: String) {
    BANK("Bank", "bank"),
    EWALLET("E-Wallet", "ewallet"),
    CASH("Cash", "cash"),
    CREDIT("Credit Card", "credit"),
    INVESTMENT("Investment", "investment")
}

data class CreateAccountRequest(
    val name: String,
    val type: AccountType,
    val balance: Double = 0.0,
    val currency: String = "IDR",
    val description: String? = null
)

data class UpdateAccountRequest(
    val name: String? = null,
    val balance: Double? = null,
    val description: String? = null
)
