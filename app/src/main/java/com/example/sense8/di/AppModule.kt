package com.example.sense8.di

import android.app.Application
import android.content.Context
import com.example.sense8.data.manager.datastore.LocalUserConfigManagerImpl
import com.example.sense8.data.manager.objectDetection.ObjectDetectionManagerImpl
import com.example.sense8.domain.manager.datastore.LocalUserConfigManager
import com.example.sense8.domain.manager.objectDetection.ObjectDetectionManager
import com.example.sense8.domain.usecases.detection.DetectObjectUseCase
import com.example.sense8.domain.usecases.userconfig.ReadUserConfig
import com.example.sense8.domain.usecases.userconfig.UserConfigUseCases
import com.example.sense8.domain.usecases.userconfig.WriteUserConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.internal.modules.ApplicationContextModule
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module(includes = [ApplicationContextModule::class])
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideLocalUserConfigManager(
        application: Application
    ): LocalUserConfigManager = LocalUserConfigManagerImpl(application)

    @Provides
    @Singleton
    fun provideObjectDetectionManager(
        @ApplicationContext context: Context
    ): ObjectDetectionManager = ObjectDetectionManagerImpl(
        context
    )


    // Use-Cases
    @Provides
    @Singleton
    fun provideUserConfigUseCases(
        localUserConfigManager: LocalUserConfigManager
    ): UserConfigUseCases = UserConfigUseCases(
        readUserConfig = ReadUserConfig(localUserConfigManager),
        writeUserConfig = WriteUserConfig(localUserConfigManager)
    )

    @Provides
    @Singleton
    fun provideDetectObjectUseCase(
        objectDetectionManager: ObjectDetectionManager
    ): DetectObjectUseCase = DetectObjectUseCase(
        objectDetectionManager = objectDetectionManager
    )
}