package entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class that combines a Bank with its accounts.
 * Room uses @Relation to automatically join these tables.
 */
data class BankWithDetails(
    @Embedded val recipe: BankEntity,

    @Relation(
        parentColumn = "id", entityColumn = "recipe_id"
    ) val ingredients: List<AccountsEntity>,

    @Relation(
        parentColumn = "id", entityColumn = "recipe_id"
    ) val steps: List<CardEntity>
)



