package com.example.data

import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApiService {
    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
        @Query("rating") rating: String = "g"
    ): GiphySearchResponse
}

data class GiphySearchResponse(
    val data: List<GiphyGif>
)

data class GiphyGif(
    val id: String,
    val images: GiphyImages
)

data class GiphyImages(
    val fixed_height: GiphyImageObj,
    val original: GiphyImageObj
)

data class GiphyImageObj(
    val url: String,
    val width: String,
    val height: String
)
