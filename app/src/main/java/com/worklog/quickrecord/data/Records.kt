package com.worklog.quickrecord.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val occurredAt: LocalDateTime,
    val place: String,
    val description: String,
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = RecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recordId")],
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val recordId: Long,
    val relativePath: String,
    val width: Int,
    val height: Int,
    val orderIndex: Int,
)

data class RecordWithPhotos(
    @Embedded val record: RecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val photos: List<PhotoEntity>,
)

@Dao
interface RecordDao {

    @Transaction
    @Query("SELECT * FROM records ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<RecordWithPhotos>>

    @Transaction
    @Query("SELECT * FROM records WHERE id = :id")
    fun observeById(id: Long): Flow<RecordWithPhotos?>

    /** 导出用：闭区间查询，按时间正序，报告里就是从上到下。 */
    @Transaction
    @Query("SELECT * FROM records WHERE occurredAt >= :from AND occurredAt <= :to ORDER BY occurredAt ASC")
    suspend fun listInRange(from: LocalDateTime, to: LocalDateTime): List<RecordWithPhotos>

    @Insert
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecord(id: Long)

    @Transaction
    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): RecordWithPhotos?

    /** 最近一条记录，供桌面小组件显示。 */
    @Transaction
    @Query("SELECT * FROM records ORDER BY occurredAt DESC LIMIT 1")
    suspend fun latest(): RecordWithPhotos?

    @Query("SELECT relativePath FROM photos WHERE recordId = :recordId")
    suspend fun photoPathsOf(recordId: Long): List<String>

    @Insert
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("DELETE FROM photos WHERE recordId = :recordId")
    suspend fun deletePhotosOf(recordId: Long)

    @Query("SELECT COUNT(*) FROM records")
    suspend fun countRecords(): Int

    @Transaction
    @Query("SELECT * FROM records ORDER BY occurredAt ASC")
    suspend fun listAll(): List<RecordWithPhotos>

    @Query("DELETE FROM records")
    suspend fun deleteAllRecords()
}

@Database(
    entities = [RecordEntity::class, PhotoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}
