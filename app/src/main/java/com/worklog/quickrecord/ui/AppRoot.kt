package com.worklog.quickrecord.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.worklog.quickrecord.AppContainer
import com.worklog.quickrecord.ui.detail.RecordDetailScreen
import com.worklog.quickrecord.ui.detail.RecordDetailViewModel
import com.worklog.quickrecord.ui.edit.RecordEditScreen
import com.worklog.quickrecord.ui.edit.RecordEditViewModel
import com.worklog.quickrecord.ui.export.ExportScreen
import com.worklog.quickrecord.ui.export.ExportViewModel
import com.worklog.quickrecord.ui.list.RecordListScreen
import com.worklog.quickrecord.ui.list.RecordListViewModel

/**
 * 页面只有三个，用状态变量做导航即可，不引入导航库。
 * 以后页面数量增长或有深链接需求时再替换。
 */
sealed interface AppScreen {
    data object List : AppScreen
    data object Export : AppScreen
    data class Detail(val id: Long) : AppScreen
    data class Edit(val id: Long?) : AppScreen
}

@Composable
fun AppRoot(container: AppContainer, modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.List) }

    val listViewModel: RecordListViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RecordListViewModel(container.recordRepository, container.preferences)
            }
        },
    )
    val detailViewModel: RecordDetailViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecordDetailViewModel(container.recordRepository) }
        },
    )
    val editViewModel: RecordEditViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RecordEditViewModel(container.recordRepository, container.photoStore)
            }
        },
    )
    val exportViewModel: ExportViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ExportViewModel(
                    repository = container.recordRepository,
                    photoStore = container.photoStore,
                    exportDir = container.exportDir,
                    preferences = container.preferences,
                    appContext = container.appContext,
                )
            }
        },
    )

    CompositionLocalProvider(LocalPhotoStore provides container.photoStore) {
        when (val current = screen) {
            AppScreen.List -> {
                val state by listViewModel.uiState.collectAsState()
                RecordListScreen(
                    state = state,
                    onQueryChange = listViewModel::onQueryChange,
                    onOpenRecord = { id -> screen = AppScreen.Detail(id) },
                    onCreateRecord = { screen = AppScreen.Edit(null) },
                    onOpenExport = { screen = AppScreen.Export },
                    modifier = modifier,
                )
            }

            AppScreen.Export -> {
                ExportScreen(
                    viewModel = exportViewModel,
                    backupManager = container.backupManager,
                    onBack = { screen = AppScreen.List },
                    modifier = modifier,
                )
            }

            is AppScreen.Detail -> {
                LaunchedEffect(current.id) { detailViewModel.select(current.id) }
                val record by detailViewModel.record.collectAsState()
                RecordDetailScreen(
                    record = record,
                    onBack = { screen = AppScreen.List },
                    onEdit = { screen = AppScreen.Edit(current.id) },
                    onDelete = { detailViewModel.delete { screen = AppScreen.List } },
                    modifier = modifier,
                )
            }

            is AppScreen.Edit -> {
                LaunchedEffect(current.id) { editViewModel.reset(current.id) }
                RecordEditScreen(
                    viewModel = editViewModel,
                    onBack = { screen = AppScreen.List },
                    onSaved = { screen = AppScreen.List },
                    modifier = modifier,
                )
            }
        }
    }

    BackHandler(enabled = screen != AppScreen.List) {
        screen = AppScreen.List
    }
}
