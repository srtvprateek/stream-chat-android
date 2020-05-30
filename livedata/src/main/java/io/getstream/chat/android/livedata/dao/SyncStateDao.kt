package io.getstream.chat.android.livedata.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.getstream.chat.android.livedata.entity.SyncStateEntity

@Dao
interface SyncStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncStateEntity: SyncStateEntity)

    @Query(
        "SELECT * FROM stream_sync_state " +
            "WHERE stream_sync_state.userId = :userId"
    )
    suspend fun select(userId: String): SyncStateEntity?
}