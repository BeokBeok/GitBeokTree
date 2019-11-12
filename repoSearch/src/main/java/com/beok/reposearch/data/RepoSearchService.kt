package com.beok.reposearch.data

import com.beok.reposearch.data.response.ReposResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface RepoSearchService {

    @GET("users/{user}/repos")
    suspend fun getRepoList(
        @Path("user")
        user: String
    ): List<ReposResponse>

}