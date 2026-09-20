package com.worklog.quickrecord.ui.edit

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.worklog.quickrecord.R
import com.worklog.quickrecord.data.PhotoImport
import com.worklog.quickrecord.domain.RecordValidator
import com.worklog.quickrecord.ui.LocalPhoto
import com.worklog.quickrecord.ui.LocalPhotoStore
import com.worklog.quickrecord.ui.icons.BackIcon
import com.worklog.quickrecord.ui.icons.CameraIcon
import com.worklog.quickrecord.ui.icons.EraserIcon
import com.worklog.quickrecord.ui.showDateTimePicker
import com.worklog.quickrecord.ui.theme.LocalAppColors
import com.worklog.quickrecord.util.DisplayFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun RecordEditScreen(
    viewModel: RecordEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val photoStore = LocalPhotoStore.current
    val scope = rememberCoroutineScope()

    var photoChooserVisible by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                val temp = photoStore.newTempFile()
                val copied = try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                        true
                    } ?: false
                } catch (error: Exception) {
                    false
                }
                if (copied) {
                    PhotoImport.import(photoStore, temp, viewModel.occurredAt)
                } else {
                    photoStore.deleteFile(temp)
                    null
                }
            }
            if (imported == null) {
                photoFailed = true
            } else {
                viewModel.addPhoto(imported.relativePath, imported.width, imported.height)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val source = pendingCapture
        pendingCapture = null
        if (!success || source == null) {
            source?.let(photoStore::deleteFile)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val imported = withContext(Dispatchers.IO) {
                PhotoImport.import(photoStore, source, viewModel.occurredAt)
            }
            if (imported == null) {
                photoFailed = true
            } else {
                viewModel.addPhoto(imported.relativePath, imported.width, imported.height)
            }
        }
    }

    fun startCamera() {
        val temp = photoStore.newTempFile()
        pendingCapture = temp
        try {
            cameraLauncher.launch(
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", temp),
            )
        } catch (error: Exception) {
            photoStore.deleteFile(temp)
            pendingCapture = null
            photoFailed = true
        }
    }

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) onSaved()
    }

    // imePadding：键盘弹出时整页上移，否则下面被键盘挡住的部分（包括「保存记录」）
    // 既看不见也点不到，得先手动收起键盘才能保存。
    Column(modifier = modifier.fillMaxSize().background(colors.page).imePadding()) {
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
                text = stringResource(
                    if (viewModel.isEditing) R.string.screen_edit_record else R.string.screen_new_record,
                ),
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
            FieldCard(label = stringResource(R.string.label_time)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = DisplayFormat.full(viewModel.occurredAt),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                showDateTimePicker(context, viewModel.occurredAt) {
                                    viewModel.onOccurredAtChange(it)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = EraserIcon,
                            contentDescription = stringResource(R.string.action_change_time),
                            tint = colors.brand,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.time_hint),
                    fontSize = 12.sp,
                    color = colors.subSoft,
                )
            }

            FieldCard(label = stringResource(R.string.label_place)) {
                BasicTextField(
                    value = viewModel.place,
                    onValueChange = viewModel::onPlaceChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium, color = colors.ink),
                    cursorBrush = SolidColor(colors.brand),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (viewModel.place.isEmpty()) {
                            Text(
                                text = stringResource(R.string.place_hint),
                                fontSize = 17.sp,
                                color = colors.subSoft,
                            )
                        }
                        inner()
                    },
                )
                if (viewModel.frequentPlaces.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.frequentPlaces.take(3).forEach { place ->
                            Text(
                                text = place,
                                fontSize = 13.sp,
                                color = colors.tileForeground,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.tile)
                                    .clickable { viewModel.onPlaceChange(place) }
                                    .padding(horizontal = 13.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            FieldCard(label = stringResource(R.string.label_description)) {
                BasicTextField(
                    value = viewModel.description,
                    onValueChange = viewModel::onDescriptionChange,
                    textStyle = TextStyle(fontSize = 15.sp, lineHeight = 24.sp, color = colors.ink),
                    cursorBrush = SolidColor(colors.brand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    decorationBox = { inner ->
                        if (viewModel.description.isEmpty()) {
                            Text(
                                text = stringResource(R.string.description_hint),
                                fontSize = 15.sp,
                                color = colors.subSoft,
                            )
                        }
                        inner()
                    },
                )
                Text(
                    text = stringResource(R.string.desc_ime_hint),
                    fontSize = 12.sp,
                    color = colors.subSoft,
                )
            }

            FieldCard(
                label = if (viewModel.photos.isEmpty()) {
                    stringResource(R.string.label_photos)
                } else {
                    stringResource(R.string.label_photos) + " " + viewModel.photos.size + " 张"
                },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    viewModel.photos.forEach { photo ->
                        LocalPhoto(
                            relativePath = photo.relativePath,
                            maxSize = 320,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.removePhoto(photo) },
                        )
                    }
                    if (viewModel.photos.size < RecordValidator.MAX_PHOTOS) {
                        Column(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.page)
                                .clickable { photoChooserVisible = true },
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = CameraIcon,
                                contentDescription = null,
                                tint = colors.sub,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = stringResource(R.string.photo_add),
                                fontSize = 10.5.sp,
                                color = colors.sub,
                            )
                        }
                    }
                }
                if (photoFailed) {
                    Text(
                        text = stringResource(R.string.photo_failed),
                        fontSize = 12.sp,
                        color = colors.danger,
                    )
                }
            }

            viewModel.validationError?.let { error ->
                val message = when (error) {
                    RecordValidator.ValidationResult.MissingPlace -> R.string.error_missing_place
                    RecordValidator.ValidationResult.MissingDescription ->
                        R.string.error_missing_description
                    RecordValidator.ValidationResult.Ok -> R.string.action_save
                }
                Text(
                    text = stringResource(message),
                    fontSize = 13.sp,
                    color = colors.danger,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.brand)
                    .clickable { viewModel.save() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.action_save),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.card,
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }

    if (photoChooserVisible) {
        AlertDialog(
            onDismissRequest = { photoChooserVisible = false },
            title = { Text(stringResource(R.string.photo_add)) },
            text = { Text(stringResource(R.string.photo_source_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        photoChooserVisible = false
                        startCamera()
                    },
                ) { Text(stringResource(R.string.photo_from_camera)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        photoChooserVisible = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) { Text(stringResource(R.string.photo_from_gallery)) }
            },
        )
    }
}

@Composable
private fun FieldCard(label: String, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(text = label, fontSize = 12.5.sp, color = colors.sub)
        content()
    }
}
