package com.mirza.payments.data.repository

import com.mirza.common.extensions.asResult
import com.mirza.common.result.Result
import com.mirza.payments.data.datasource.PaymentLocalDataSource
import com.mirza.payments.data.datasource.PaymentRemoteDataSource
import com.mirza.payments.data.mapper.toDomain
import com.mirza.payments.data.mapper.toDomainScheduled
import com.mirza.payments.data.mapper.toDto
import com.mirza.payments.domain.model.Payment
import com.mirza.payments.domain.model.PaymentCategory
import com.mirza.payments.domain.model.ScheduledPayment
import com.mirza.payments.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepositoryImpl @Inject constructor(
    private val remoteDataSource: PaymentRemoteDataSource,
    private val localDataSource: PaymentLocalDataSource
) : PaymentRepository {

    override fun getPayments(): Flow<Result<List<Payment>>> =
        localDataSource.observePayments()
            .map { it.toDomain() }
            .asResult()

    override fun getScheduledPayments(): Flow<Result<List<ScheduledPayment>>> =
        localDataSource.observeScheduledPayments()
            .map { it.toDomainScheduled() }
            .asResult()

    override suspend fun payBill(
        billerName: String,
        category: PaymentCategory,
        amount: Double,
        currency: String
    ): Result<Payment> {
        return try {
            val dto = remoteDataSource.payBill(billerName, category.name, amount, currency)
            localDataSource.addPayment(dto)
            Result.Success(dto.toDomain())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun scheduleBillPayment(scheduledPayment: ScheduledPayment): Result<Unit> {
        return try {
            val dto = scheduledPayment.toDto()
            remoteDataSource.scheduleBillPayment(dto)
            localDataSource.upsertScheduledPayment(dto)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelScheduledPayment(scheduledPaymentId: String): Result<Unit> {
        return try {
            remoteDataSource.cancelScheduledPayment(scheduledPaymentId)
            localDataSource.removeScheduledPayment(scheduledPaymentId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun refreshPayments(): Result<Unit> {
        return try {
            localDataSource.cachePayments(remoteDataSource.getPayments())
            localDataSource.cacheScheduledPayments(remoteDataSource.getScheduledPayments())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}