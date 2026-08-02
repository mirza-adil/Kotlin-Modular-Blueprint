package com.mirza.transfers.data.repository

import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import com.mirza.transfers.data.datasource.TransferLocalDataSource
import com.mirza.transfers.data.datasource.TransferRemoteDataSource
import com.mirza.transfers.data.mapper.toDomain
import com.mirza.transfers.domain.model.Transfer
import com.mirza.transfers.domain.model.TransferType
import com.mirza.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    private val remoteDataSource: TransferRemoteDataSource,
    private val localDataSource: TransferLocalDataSource
) : TransferRepository {

    override fun getTransfers(): Flow<Result<List<Transfer>>> =
        localDataSource.observeTransfers()
            .map { it.toDomain() }
            .asResult()

    override suspend fun getTransferById(transferId: String): Result<Transfer> {
        return try {
            val dto = localDataSource.getTransferById(transferId)
                ?: throw NoSuchElementException("Transfer $transferId not found")
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createTransfer(
        transferType: TransferType,
        sourceAccountId: String,
        recipientName: String,
        recipientAccountNumber: String,
        bankName: String?,
        amount: Double,
        currency: String,
        note: String?
    ): Result<Transfer> {
        return try {
            val dto = remoteDataSource.createTransfer(
                transferType = transferType.name,
                sourceAccountId = sourceAccountId,
                recipientName = recipientName,
                recipientAccountNumber = recipientAccountNumber,
                bankName = bankName,
                amount = amount,
                currency = currency,
                note = note
            )
            localDataSource.addTransfer(dto)
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshTransfers(): Result<Unit> {
        return try {
            localDataSource.cacheTransfers(remoteDataSource.getTransfers())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}