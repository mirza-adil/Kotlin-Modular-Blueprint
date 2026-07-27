package api

import model.BankDto
import model.RecipeListResponse
import model.NetworkResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for bank-related endpoints.
 */
interface AccountApi {

    @GET("...")
    fun getRecipes(
        @Query("..") page: Int = 1,
        @Query("..") limit: Int = 20
    ): Call<NetworkResponse<RecipeListResponse>>

    @GET("../{id}")
    fun getRecipeById(
        @Path("..") recipeId: String
    ): Call<NetworkResponse<BankDto>>

    @GET("../search")
    fun searchRecipes(
        @Query("query") query: String,
        @Query("..") page: Int = 1,
        @Query("..") limit: Int = 20
    ): Call<NetworkResponse<RecipeListResponse>>

    @GET("../category/{category}")
    fun getRecipesByCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Call<NetworkResponse<RecipeListResponse>>
}



