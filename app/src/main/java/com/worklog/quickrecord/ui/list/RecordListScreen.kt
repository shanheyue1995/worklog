package com.worklog.quickrecord.ui.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import com.worklog.quickrecord.R
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.reminder.ReminderRules
import com.worklog.quickrecord.ui.LocalPhoto
import com.worklog.quickrecord.ui.icons.ExportIcon
import com.worklog.quickrecord.ui.theme.LocalAppColors
import com.worklog.quickrecord.util.DisplayFormat

@Composable
fun RecordListScreen(
    state: RecordListUiState,
    onQueryChange: (String) -> Unit,
    onOpenRecord: (Long) -> Unit,
    onCreateRecord: () -> Unit,
    onEditRecord: (Long) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onOpenExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var searchVisible by remember { mutableStateOf(false) }
    var bannerDismissed by remember(state.reminder) { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }

    Box(modifier = modifier.fillMaxSize().background(colors.page)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏：应用图标 + 标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    // 用前景图而不是 ic_launcher：后者在 API 26+ 是自适应图标 XML，
                    // painterResource 只支持矢量图与位图，加载它会直接崩。
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.height(34.dp),
                )
                Spacer(Modifier.width(11.dp))
                Text(
                    text = stringResource(R.string.screen_records),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                    color = colors.ink,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val reminder = state.reminder
                if (reminder != ReminderRules.State.None && !bannerDismissed) {
                    ReminderBanner(
                        state = reminder,
                        onExport = onOpenExport,
                        onDismiss = { bannerDismissed = true },
                    )
                }

                if (searchVisible) {
                    SearchField(value = state.query, onValueChange = onQueryChange)
                }

                RecordCard(
                    state = state,
                    onOpenRecord = onOpenRecord,
                    onEditRecord = onEditRecord,
                    onRequestDelete = { pendingDelete = it },
                )

                Spacer(Modifier.height(92.dp))
            }
        }

        BottomDock(
            onSearch = { searchVisible = !searchVisible },
            onCreate = onCreateRecord,
            onExport = onOpenExport,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteRecord(record.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReminderBanner(
    state: ReminderRules.State,
    onExport: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.warnContainer)
            .padding(start = 13.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = when (state) {
                    is ReminderRules.State.Stale ->
                        stringResource(R.string.reminder_stale, state.days)
                    ReminderRules.State.NeverExported -> stringResource(R.string.reminder_never)
                    ReminderRules.State.None -> ""
                },
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.warnContent,
            )
            Text(
                text = stringResource(R.string.reminder_body),
                fontSize = 12.sp,
                color = colors.warnSupport,
            )
        }
        TextButton(onClick = onExport) {
            Text(stringResource(R.string.action_go_export), fontSize = 13.sp, color = colors.warnContent)
        }
        TextButton(onClick = onDismiss) {
            Text("×", fontSize = 16.sp, color = colors.warnSupport)
        }
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit) {
    val colors = LocalAppColors.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = colors.brand,
            modifier = Modifier.size(17.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = colors.ink,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        fontSize = 14.sp,
                        color = colors.subSoft,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun RecordCard(
    state: RecordListUiState,
    onOpenRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
    onRequestDelete: (Record) -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card),
    ) {
        // 汇总行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = colors.tileForeground,
                    modifier = Modifier.size(36.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(R.string.records_all),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                )
                Text(
                    text = stringResource(
                        R.string.records_all_meta,
                        state.totalCount,
                        state.records.firstOrNull()?.let { DisplayFormat.short(it.occurredAt) } ?: "-",
                    ),
                    fontSize = 12.5.sp,
                    color = colors.sub,
                )
                Text(
                    text = stringResource(R.string.records_all_desc),
                    fontSize = 12.5.sp,
                    color = colors.subSoft,
                )
            }
        }

        if (state.records.isEmpty()) {
            HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(start = 15.dp))
            Text(
                text = stringResource(
                    if (state.totalCount == 0) R.string.empty_records else R.string.empty_search,
                ),
                fontSize = 13.sp,
                color = colors.sub,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 22.dp),
            )
        } else {
            state.records.forEach { record ->
                HorizontalDivider(
                    color = colors.divider,
                    thickness = 1.dp,
                    modifier = Modifier.padding(start = 15.dp),
                )
                RecordRow(
                    record = record,
                    onClick = { onOpenRecord(record.id) },
                    onEdit = { onEditRecord(record.id) },
                    onDelete = { onRequestDelete(record) },
                )
            }
        }
    }
}

@Composable
private fun RecordRow(
    record: Record,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 15.dp, end = 8.dp, top = 13.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        val cover = record.photos.firstOrNull()
        if (cover != null) {
            LocalPhoto(
                relativePath = cover.relativePath,
                maxSize = 160,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.tile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = null,
                    tint = colors.tileForeground,
                    modifier = Modifier.size(23.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = record.place,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    if (record.photos.isNotEmpty()) {
                        append(
                            stringResource(R.string.records_meta_photos, record.photos.size),
                        )
                        append(stringResource(R.string.records_meta_divider))
                    }
                    append(DisplayFormat.short(record.occurredAt))
                },
                fontSize = 12.5.sp,
                color = colors.sub,
            )
            Text(
                text = record.description,
                fontSize = 12.5.sp,
                color = colors.subSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Box {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.action_more),
                tint = colors.subSoft,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { menuOpen = true }
                    .padding(4.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit)) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_delete), color = colors.danger) },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomDock(
    onSearch: () -> Unit,
    onCreate: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(999.dp)

    Row(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = shape, clip = false)
            .clip(shape)
            .background(colors.card)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        DockButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.action_dock_search),
                tint = colors.ink,
                modifier = Modifier.size(23.dp),
            )
        }
        DockButton(onClick = onCreate) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.action_new_record),
                tint = colors.ink,
                modifier = Modifier.size(25.dp),
            )
        }
        DockButton(onClick = onExport) {
            Icon(
                imageVector = ExportIcon,
                contentDescription = stringResource(R.string.action_export),
                tint = colors.ink,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun DockButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 62.dp, height = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
