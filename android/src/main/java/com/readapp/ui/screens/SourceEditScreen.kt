package com.readapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.readapp.viewmodel.SourceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceEditScreen(
    sourceId: String? = null,
    onNavigateBack: () -> Unit,
    sourceViewModel: SourceViewModel = viewModel(factory = SourceViewModel.Factory)
) {
    var jsonContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(sourceId) {
        if (sourceId != null) {
            isLoading = true
            val content = sourceViewModel.getSourceDetail(sourceId)
            if (content != null) {
                jsonContent = content
            } else {
                errorMessage = "加载书源失败"
                snackbarHostState.showSnackbar("加载书源失败")
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (sourceId == null) "新建书源" else "编辑书源") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isLoading = true
                                val result = sourceViewModel.saveSource(jsonContent)
                                isLoading = false
                                if (result.isSuccess) {
                                    // Refresh logic might be needed in parent or viewmodel auto refresh?
                                    // SourceViewModel.fetchSources() is not automatically called but maybe we should trigger it.
                                    // Actually SourceViewModel instance here is same if Factory works right with activity scope, 
                                    // but we usually want to trigger a refresh on the list screen.
                                    // For simplicity, we just go back and let the user pull to refresh or auto refresh.
                                    sourceViewModel.fetchSources()
                                    onNavigateBack()
                                } else {
                                    errorMessage = result.exceptionOrNull()?.message ?: "保存失败"
                                    snackbarHostState.showSnackbar(errorMessage ?: "保存失败")
                                }
                            }
                        },
                        enabled = jsonContent.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
            }
            
            TextField(
                value = jsonContent,
                onValueChange = { jsonContent = it },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                placeholder = { Text("在此输入书源JSON...") },
                supportingText = { Text("请确保JSON格式正确") }
            )
        }
    }
}
