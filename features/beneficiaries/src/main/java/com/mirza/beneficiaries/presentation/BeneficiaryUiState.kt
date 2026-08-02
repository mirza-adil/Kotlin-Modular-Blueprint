package com.mirza.beneficiaries.presentation

import com.mirza.beneficiaries.domain.model.Beneficiary

data class BeneficiaryUiState(
    val isLoading: Boolean = false,
    val beneficiaries: List<Beneficiary> = emptyList(),
    val selectedBeneficiary: Beneficiary? = null,
    val errorMessage: String? = null
)