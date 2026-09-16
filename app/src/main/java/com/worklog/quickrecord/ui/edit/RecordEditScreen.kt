package com.worklog.quickrecord.ui.edit

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.worklog.quickrecord.R
import com.worklog.quickrecord.domain.RecordValidator
import com.worklog.quickrecord.ui.LocalPhoto
import com.worklog.quickrecord.ui.showDateTimePicker
import com.worklog.quickrecord.util.DisplayFormat
import java.util.Locale

@Composable
fun RecordEditScreen(
    viewModel: RecordEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var voiceUnavailable by remember { mutableStateOf(false) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!text.isNullOrBlank()) viewModel.appendVoiceText(text)
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
            // 时间：默认当前时间，点一下可以改，补录时用得上。
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel(stringResource(R.string.label_time))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
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

            // 地点：下面一行是历史常用地点，点一下即填入，这是"十秒记一条"的关键。
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.label_place))
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.label_description))
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
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
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

            if (viewModel.photos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.label_photos))
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
                                    modifier = Modifier.align(Alignment.TopEnd),
                                ) {
                                    Text("×", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            viewModel.validationError?.let { error ->
                Text(
                    text = stringResource(
                        when (error) {
                            RecordValidator.ValidationResult.MissingPlace -> R.string.error_missing_place
                            RecordValidator.ValidationResult.MissingDescription ->
                                R.string.error_missing_description
                            RecordValidator.ValidationResult.Ok -> R.string.action_save
                        },
                    ),
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
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
