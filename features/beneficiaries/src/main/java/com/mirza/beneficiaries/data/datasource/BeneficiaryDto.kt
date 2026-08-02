package com.mirza.beneficiaries.data.datasource

data class BeneficiaryDto(
    val id: String,
    val name: String,
    val nickname: String?,
    val accountNumber: String,
    val bankName: String,
    val transferType: String,
    val isFavorite: Boolean
)