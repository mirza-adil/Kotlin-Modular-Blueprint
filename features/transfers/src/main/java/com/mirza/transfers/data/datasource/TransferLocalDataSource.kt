package com.mirza.transfers.data.datasource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface TransferLocalDataSource {

    fun observeTransfers(): Flow<List<TransferDto>>

    suspend fun getTransferById(transferId: String): TransferDto?

    suspend fun cacheTransfers(transfers: List<TransferDto>)

    suspend fun addTransfer(transfer: TransferDto)
}

@Singleton
class TransferLocalDataSourceImpl @Inject constructor() : TransferLocalDataSource {

    private val cachedTransfers = MutableStateFlow<List<TransferDto>>(emptyList())

    override fun observeTransfers(): Flow<List<TransferDto>> = cachedTransfers.asStateFlow()

    override suspend fun getTransferById(transferId: String): TransferDto? =
        cachedTransfers.value.find { it.id == transferId }

    override suspend fun cacheTransfers(transfers: List<TransferDto>) {
        cachedTransfers.value = transfers
    }

    override suspend fun addTransfer(transfer: TransferDto) {
        cachedTransfers.value = listOf(transfer) + cachedTransfers.value
    }
}