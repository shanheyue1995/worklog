package com.worklog.quickrecord.ui.edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.worklog.quickrecord.data.PhotoStore
import com.worklog.quickrecord.data.RecordRepository
import com.worklog.quickrecord.domain.Photo
import com.worklog.quickrecord.domain.Record
import com.worklog.quickrecord.domain.RecordSearch
import com.worklog.quickrecord.domain.RecordValidator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class RecordEditViewModel(
    private val repository: RecordRepository,
    private val photoStore: PhotoStore,
) : ViewModel() {

    private var recordId: Long? = null

    var occurredAt by mutableStateOf(LocalDateTime.now().withSecond(0).withNano(0))
        private set

    var place by mutableStateOf("")
        private set

    var description by mutableStateOf("")
        private set

    val photos = mutableStateListOf<Photo>()

    /** 历史常用地点，用于一键填入。 */
    val frequentPlaces = mutableStateListOf<String>()

    var validationError by mutableStateOf<RecordValidator.ValidationResult?>(null)
        private set

    var saved by mutableStateOf(false)
        private set

    var isEditing by mutableStateOf(false)
        private set

    /** 进入新建或编辑时调用，重置上一轮的草稿。 */
    fun reset(id: Long?) {
        val targetId = id
        recordId = targetId
        isEditing = targetId != null
        occurredAt = LocalDateTime.now().withSecond(0).withNano(0)
        place = ""
        description = ""
        photos.clear()
        validationError = null
        saved = false
        frequentPlaces.clear()

        viewModelScope.launch {
            val all = repository.observeRecords().first()
            frequentPlaces.addAll(RecordSearch.frequentPlaces(all))
        }

        if (targetId != null) {
            viewModelScope.launch {
                repository.findById(targetId)?.let { record ->
                    occurredAt = record.occurredAt
                    place = record.place
                    description = record.description
                    photos.clear()
                    photos.addAll(record.photos)
                }
            }
        }
    }

    fun onOccurredAtChange(value: LocalDateTime) {
        occurredAt = value.withSecond(0).withNano(0)
    }

    fun onPlaceChange(value: String) {
        place = value
        validationError = null
    }

    fun onDescriptionChange(value: String) {
        description = value
        validationError = null
    }

    fun appendVoiceText(text: String) {
        description = if (description.isBlank()) text else "$description$text"
        validationError = null
    }

    fun addPhoto(relativePath: String, width: Int, height: Int) {
        if (photos.size >= RecordValidator.MAX_PHOTOS) return
        photos.add(0, Photo(relativePath = relativePath, width = width, height = height))
    }

    fun removePhoto(photo: Photo) {
        photos.remove(photo)
        photoStore.delete(photo.relativePath)
    }

    fun save() {
        val result = RecordValidator.validate(place, description)
        if (result != RecordValidator.ValidationResult.Ok) {
            validationError = result
            return
        }
        viewModelScope.launch {
            repository.save(
                Record(
                    id = recordId ?: 0L,
                    occurredAt = occurredAt,
                    place = place,
                    description = description,
                    photos = photos.toList(),
                ),
            )
            saved = true
        }
    }
}
