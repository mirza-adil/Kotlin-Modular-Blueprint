package com.mirza.beneficiaries.presentation

sealed interface BeneficiaryEvent {
    data object LoadBeneficiaries : BeneficiaryEvent
    data class SelectBeneficiary(val beneficiaryId: String) : BeneficiaryEvent
    data class DeleteBeneficiary(val beneficiaryId: String) : BeneficiaryEvent
    data object DismissError : BeneficiaryEvent
}