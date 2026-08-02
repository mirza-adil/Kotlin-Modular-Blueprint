package com.mirza.beneficiaries.domain.model

data class Beneficiary(
    val id: String,
    val name: String,
    val nickname: String?,
    val accountNumber: String,
    val bankName: String,
    val transferType: BeneficiaryTransferType,
    val isFavorite: Boolean = false
)

enum class BeneficiaryTransferType {
    OWN_ACCOUNT,
    LOCAL,
    INTERNATIONAL
}