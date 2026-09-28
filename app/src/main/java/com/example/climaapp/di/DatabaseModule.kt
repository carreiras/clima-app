package com.example.climaapp.di

import android.content.Context
import androidx.room.Room
import com.example.climaapp.data.local.AppDatabase
import com.example.climaapp.data.local.CityDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "clima-app.db").build()

    @Provides
    @Singleton
    fun provideCityDao(database: AppDatabase): CityDao = database.cityDao()
}
