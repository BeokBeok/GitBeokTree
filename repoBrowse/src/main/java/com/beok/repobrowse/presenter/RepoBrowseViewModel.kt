package com.beok.repobrowse.presenter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.beok.common.Result
import com.beok.common.base.BaseViewModel
import com.beok.common.succeeded
import com.beok.repobrowse.domain.usecase.UserRepoBrowseUsecase
import com.beok.repobrowse.presenter.model.RepoFileTreeModel
import com.beok.repobrowse.presenter.model.RepoUserModel
import kotlinx.coroutines.launch

class RepoBrowseViewModel(
    private val userRepoBrowseUsecase: UserRepoBrowseUsecase,
    private val repoUser: RepoUserModel
) : BaseViewModel() {

    private val _repoFileTree = MutableLiveData<List<RepoFileTreeModel>>()
    private val _errMsg = MutableLiveData<Throwable>()
    private val _isLoading = MutableLiveData<Boolean>()
    private val _branch = MutableLiveData<List<String>>()
    private var currentBranchName: String = ""

    val repoFileTree: LiveData<List<RepoFileTreeModel>> get() = _repoFileTree
    val errMsg: LiveData<Throwable> get() = _errMsg
    val isLoading: LiveData<Boolean> get() = _isLoading
    val branch: LiveData<List<String>> get() = _branch
    var currentPos = -1
        private set

    fun setBranchList() = viewModelScope.launch {
        _branch.value?.let { return@launch }

        val branchList = (userRepoBrowseUsecase.getRepoBranches(
            repoUser.userName,
            repoUser.repoName
        ) as Result.Success).data
        _branch.value = getListWithDefaultBranchToTop(branchList)
    }

    fun moveToBranch(branchName: String) = viewModelScope.launch {
        showProgressBar()
        removeFileTreeAt(null)
        setFileTree(
            fileTree = userRepoBrowseUsecase.getRepoFileTree(
                userName = repoUser.userName,
                repoName = repoUser.repoName,
                branchName = branchName
            )
        )
        this@RepoBrowseViewModel.currentBranchName = branchName
        currentPos = _branch.value?.indexOf(branchName) ?: -1
        hideProgressBar()
    }

    fun clickSpecificItem(selectedItem: RepoFileTreeModel) = viewModelScope.launch {
        if (selectedItem.type != "dir") {
            clickFileItem(selectedItemUrl = selectedItem.downloadUrl)
            return@launch
        }
        if (selectedItem.expandable) {
            setFileTree(
                fileTree = userRepoBrowseUsecase.getRepoFileTree(
                    repoUser.userName,
                    repoUser.repoName,
                    selectedItem.path,
                    currentBranchName
                )
            )
        } else {
            removeFileTreeAt(selectedItem)
        }
    }

    private fun clickFileItem(selectedItemUrl: String) =
        navigate(RepoBrowseFragmentDirections.actionRepobrowseToFileviewer(selectedItemUrl))

    private fun setFileTree(fileTree: Result<List<RepoFileTreeModel>>) {
        if (!fileTree.succeeded) {
            _errMsg.value = (fileTree as? Result.Error)?.exception
            return
        }
        val sortedFileTree = getUpdatedFileTree(
            itemToAdd = (fileTree as Result.Success).data
                .sortedWith(compareBy(RepoFileTreeModel::type, RepoFileTreeModel::path))
        )
        _repoFileTree.value = sortedFileTree
    }

    private fun getUpdatedFileTree(itemToAdd: List<RepoFileTreeModel>): List<RepoFileTreeModel> {
        val updatedFileTree = mutableListOf<RepoFileTreeModel>()
        _repoFileTree.value?.let {
            updatedFileTree.addAll(it)
        } ?: run {
            return itemToAdd
        }

        val parentIndex = getParentIndex(
            allFileTree = updatedFileTree,
            childFileTree = itemToAdd
        )

        addFileTree(
            toIndex = parentIndex,
            wholeTree = updatedFileTree,
            item = itemToAdd
        )
        return updatedFileTree
    }

    private fun getParentIndex(
        allFileTree: List<RepoFileTreeModel>,
        childFileTree: List<RepoFileTreeModel>
    ): Int {
        val parentElement = allFileTree.asSequence()
            .find { it.path.plus("/${childFileTree[0].name}") == childFileTree[0].path }
        return if (parentElement != null) {
            allFileTree.indexOf(parentElement)
        } else {
            -1
        }
    }

    private fun addFileTree(
        toIndex: Int,
        wholeTree: MutableList<RepoFileTreeModel>,
        item: List<RepoFileTreeModel>
    ) {
        if (toIndex >= 0) {
            wholeTree.addAll(toIndex + 1, item)
            wholeTree[toIndex].expandable = false
        }
    }

    private fun removeFileTreeAt(parentItem: RepoFileTreeModel?) {
        if (parentItem == null) {
            _repoFileTree.value = null
            return
        }

        val removedFileTree = mutableListOf<RepoFileTreeModel>()
        _repoFileTree.value?.let {
            removedFileTree.addAll(it)
        } ?: return

        removedFileTree[removedFileTree.indexOf(parentItem)].expandable = true
        _repoFileTree.value = removedFileTree.filterNot {
            it.path.startsWith(parentItem.path.plus("/"))
        }
    }

    private fun getListWithDefaultBranchToTop(branches: List<String>): List<String> {
        val listToMutable = branches.toMutableList()
        val index = listToMutable.indexOf(repoUser.defaultBranch)
        listToMutable.removeAt(index)
        listToMutable.add(0, repoUser.defaultBranch)
        return listToMutable
    }

    private fun showProgressBar() {
        _isLoading.value = true
    }

    private fun hideProgressBar() {
        _isLoading.value = false
    }

}