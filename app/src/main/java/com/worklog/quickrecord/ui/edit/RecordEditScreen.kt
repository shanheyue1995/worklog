package com.worklog.quickrecord.ui.edit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.core.content.FileProvider
import com.worklog.quickrecord.R
import com.worklog.quickrecord.data.PhotoImport
import com.worklog.quickrecord.domain.RecordValidator
import com.worklog.quickrecord.ui.LocalPhoto
import com.worklog.quickrecord.ui.LocalPhotoStore
import com.worklog.quickrecord.ui.showDateTimePicker
import com.worklog.quickrecord.util.DisplayFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun RecordEditScreen(
    viewModel: RecordEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val photoStore = LocalPhotoStore.current
    val scope = rememberCoroutineScope()

    var voiceUnavailable by remember { mutableStateOf(false) }
    var photoChooserVisible by remember { mutableStateOf(false) }
    var photoFailed by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<File?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) viewModel.appendVoiceText(text)
    }

    // 相册选择走系统照片选择器，不需要读取相册的权限。
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

    // 系统相机把原图写进我们指定的临时位置，返回后再压缩入库。
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
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                temp,
            )
            cameraLauncher.launch(uri)
        } catch (error: Exception) {
            photoStore.deleteFile(temp)
            pendingCapture = null
            photoFailed = true
        }
    }

    LaunchedEffect(viewModel.saved) {
        if (viewModel.saved) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
            Text(
                text = stringResource(
                    if (viewModel.isEditing) R.string.screen_edit_record else R.string.screen_new_record,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SectionBlock(stringResource(R.string.label_time)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(DisplayFormat.full(viewModel.occurredAt))
                        TextButton(
                            onClick = {
                                showDateTimePicker(context, viewModel.occurredAt) {
                                    viewModel.onOccurredAtChange(it)
                                }
                            },
                        ) {
                            Text(stringResource(R.string.action_change_time))
                        }
                    }
                }
            }

            SectionBlock(stringResource(R.string.label_place)) {
                OutlinedTextField(
                    value = viewModel.place,
                    onValueChange = viewModel::onPlaceChange,
                    placeholder = { Text(stringResource(R.string.place_hint)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (viewModel.frequentPlaces.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.frequentPlaces.take(3).forEach { place ->
                            OutlinedButton(
                                onClick = { viewModel.onPlaceChange(place) },
                                shape = RoundedCornerShape(999.dp),
                            ) {
                                Text(place, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            SectionBlock(stringResource(R.string.label_description)) {
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = viewModel::onDescriptionChange,
                    placeholder = { Text(stringResource(R.string.description_hint)) },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE,
                                Locale.getDefault().toLanguageTag(),
                            )
                            putExtra(
                                RecognizerIntent.EXTRA_PROMPT,
                                context.getString(R.string.description_hint),
                            )
                        }
                        try {
                            voiceLauncher.launch(intent)
                        } catch (error: ActivityNotFoundException) {
                            voiceUnavailable = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.voice_hint))
                }
                if (voiceUnavailable) {
                    Text(
                        text = stringResource(R.string.voice_unavailable),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            SectionBlock(stringResource(R.string.label_photos)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.photos.forEach { photo ->
                        Box {
                            LocalPhoto(
                                relativePath = photo.relativePath,
                                maxSize = 320,
                                modifier = Modifier
                                    .size(78.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                            TextButton(
                                onClick = { viewModel.removePhoto(photo) },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp),
                            ) {
                                Text("×", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (viewModel.photos.size < RecordValidator.MAX_PHOTOS) {
                        OutlinedButton(
                            onClick = { photoChooserVisible = true },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.size(78.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.photo_add),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
                if (photoFailed) {
                    Text(
                        text = stringResource(R.string.photo_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(8.dp))
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = viewModel::save,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.action_save))
            }
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
private fun SectionBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
