package com.beok.reposearch.domain.usecase

import com.beok.common.Result
import com.beok.common.succeeded
import com.beok.reposearch.data.RepoSearchDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepoSearchUsecase(
    private val repoSearchRepository: RepoSearchDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend operator fun invoke(user: String) = withContext(ioDispatcher) {
        repoSearchRepository.getRepoList(user).let {
            if (it.succeeded) {
                Result.Success((it as Result.Success).data.value)
            } else {
                it as Result.Error
            }
        }
    }
}