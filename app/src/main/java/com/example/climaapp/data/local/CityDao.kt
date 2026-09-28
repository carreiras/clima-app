package com.example.climaapp.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {
    @Query("SELECT * FROM favorite_cities")
    fun getAll(): Flow<List<CityEntity>>

    @Query("SELECT * FROM favorite_cities WHERE id = :id")
    suspend fun getById(id: Long): CityEntity?

    @Upsert
    suspend fun upsert(city: CityEntity)

    @Query("DELETE FROM favorite_cities WHERE id = :id")
    suspend fun delete(id: Long)
}
