package com.worklog.quickrecord.ui.export

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.worklog.quickrecord.R
import com.worklog.quickrecord.data.BackupManager
import com.worklog.quickrecord.domain.ExportRange
import com.worklog.quickrecord.ui.icons.BackIcon
import com.worklog.quickrecord.ui.showDatePicker
import com.worklog.quickrecord.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ExportScreen(
    viewModel: ExportViewModel,
    backupManager: BackupManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        val done = viewModel.state as? ExportState.Done ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        done.file.inputStream().use { input -> input.copyTo(output) }
                        true
                    } ?: false
                } catch (error: Exception) {
                    false
                }
            }
            message = context.getString(
                if (ok) R.string.export_saved else R.string.export_save_failed,
            )
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    val temp = File(context.cacheDir, "quickrecord-backup.zip")
                    val written = backupManager.export(temp)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        temp.inputStream().use { input -> input.copyTo(output) }
                    }
                    temp.delete()
                    written
                } catch (error: Exception) {
                    -1
                }
            }
            message = if (count >= 0) {
                context.getString(R.string.backup_done, count)
            } else {
                context.getString(R.string.backup_failed)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val count = withContext(Dispatchers.IO) {
                try {
                    val temp = File(context.cacheDir, "quickrecord-restore.zip")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    backupManager.import(temp).also { temp.delete() }
                } catch (error: Exception) {
                    null
                }
            }
            message = if (count != null) {
                context.getString(R.string.restore_done, count)
            } else {
                context.getString(R.string.restore_failed)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(colors.page)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = BackIcon,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.ink,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.screen_export),
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = colors.ink,
            )
        }

        when (val state = viewModel.state) {
            ExportState.Choosing -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FieldCard(stringResource(R.string.label_range)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.page)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ExportRange.entries.forEach { range ->
                            val active = range == viewModel.range
                            val label = stringResource(
                                when (range) {
                                    ExportRange.ThisWeek -> R.string.range_week
                                    ExportRange.ThisMonth -> R.string.range_month
                                    ExportRange.LastMonth -> R.string.range_last_month
                                    ExportRange.Custom -> R.string.range_custom
                                },
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (active) colors.card else colors.page)
                                    .clickable { viewModel.selectRange(range) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active) colors.ink else colors.sub,
                                )
                            }
                        }
                    }

                    // 选「自定义」时才出现，给两个日期入口
                    if (viewModel.range == ExportRange.Custom) {
                        DateRow(
                            label = stringResource(R.string.custom_start),
                            value = viewModel.customStart.toString(),
                        ) {
                            showDatePicker(context, viewModel.customStart, viewModel::updateCustomStart)
                        }
                        DateRow(
                            label = stringResource(R.string.custom_end),
                            value = viewModel.customEnd.toString(),
                        ) {
                            showDatePicker(context, viewModel.customEnd, viewModel::updateCustomEnd)
                        }
                    }
                }

                FieldCard(null) {
                    KeyValueRow(stringResource(R.string.export_range), "${viewModel.summary.start} ~ ${viewModel.summary.end}")
                    KeyValueRow(stringResource(R.string.export_records), context.getString(R.string.export_count_records, viewModel.summary.recordCount))
                    KeyValueRow(stringResource(R.string.export_photos), context.getString(R.string.export_count_photos, viewModel.summary.photoCount))
                }

                FieldCard(null) {
                    Text(
                        text = stringResource(R.string.export_note),
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp,
                        color = colors.sub,
                    )
                }

                FieldCard(stringResource(R.string.backup_title)) {
                    Text(
                        text = stringResource(R.string.backup_note),
                        fontSize = 12.5.sp,
                        lineHeight = 20.sp,
                        color = colors.sub,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            text = stringResource(R.string.action_backup),
                            filled = true,
                            modifier = Modifier.weight(1f),
                        ) { backupLauncher.launch("工作快录备份.zip") }
                        PillButton(
                            text = stringResource(R.string.action_restore),
                            filled = false,
                            modifier = Modifier.weight(1f),
                        ) { confirmRestore = true }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.brand)
                        .clickable { viewModel.generate() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.action_generate),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.card,
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            ExportState.Working -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = colors.brand)
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.export_working), color = colors.sub, fontSize = 13.sp)
                }
            }

            is ExportState.Done -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = context.getString(
                            R.string.export_summary,
                            state.recordCount,
                            state.photoCount,
                        ) + " · " + stringResource(R.string.export_pages, state.pageCount),
                        fontSize = 12.5.sp,
                        color = colors.sub,
                    )
                    PdfFirstPagePreview(file = state.file, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PillButton(
                        text = stringResource(R.string.action_save_file),
                        filled = false,
                        modifier = Modifier.weight(1f),
                    ) { saveLauncher.launch(state.file.nameWithoutExtension + ".pdf") }
                    PillButton(
                        text = stringResource(R.string.action_share),
                        filled = true,
                        modifier = Modifier.weight(1f),
                    ) { sharePdf(context, state.file) }
                }
            }

            ExportState.Empty, ExportState.Failed -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        if (state == ExportState.Empty) R.string.export_empty else R.string.export_failed,
                    ),
                    fontSize = 14.sp,
                    color = colors.sub,
                )
                Spacer(Modifier.height(16.dp))
                PillButton(
                    text = stringResource(R.string.action_back),
                    filled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) { viewModel.backToChoosing() }
            }
        }

        message?.let {
            Text(
                text = it,
                fontSize = 13.sp,
                color = colors.brand,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        restoreLauncher.launch(arrayOf("*/*"))
                    },
                ) { Text(stringResource(R.string.action_confirm), color = colors.brand) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun FieldCard(label: String?, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        label?.let { Text(text = it, fontSize = 12.5.sp, color = colors.sub) }
        content()
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = key, fontSize = 14.sp, color = colors.sub)
        Text(text = value, fontSize = 14.sp, color = colors.ink)
    }
}

@Composable
private fun DateRow(label: String, value: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.page)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 14.sp, color = colors.sub)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.ink,
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (filled) colors.brandSoft else colors.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (filled) colors.brand else colors.ink,
        )
    }
}

private fun sharePdf(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_chooser)))
}
