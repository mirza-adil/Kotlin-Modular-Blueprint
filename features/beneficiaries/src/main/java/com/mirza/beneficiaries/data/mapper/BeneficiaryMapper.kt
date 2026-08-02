package com.mirza.beneficiaries.data.mapper

import com.mirza.beneficiaries.data.datasource.BeneficiaryDto
import com.mirza.beneficiaries.domain.model.Beneficiary
import com.mirza.beneficiaries.domain.model.BeneficiaryTransferType

fun BeneficiaryDto.toDomain(): Beneficiary = Beneficiary(
    id = id,
    name = name,
    nickname = nickname,
    accountNumber = accountNumber,
    bankName = bankName,
    transferType = runCatching { BeneficiaryTransferType.valueOf(transferType) }
        .getOrDefault(BeneficiaryTransferType.LOCAL),
    isFavorite = isFavorite
)

fun List<BeneficiaryDto>.toDomain(): List<Beneficiary> = map { it.toDomain() }

fun Beneficiary.toDto(): BeneficiaryDto = BeneficiaryDto(
    id = id,
    name = name,
    nickname = nickname,
    accountNumber = accountNumber,
    bankName = bankName,
    transferType = transferType.name,
    isFavorite = isFavorite
)