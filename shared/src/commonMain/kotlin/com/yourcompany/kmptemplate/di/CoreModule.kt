package com.yourcompany.kmptemplate.di

import com.yourcompany.kmptemplate.core.data.network.createNetworkClient
import io.ktor.client.HttpClient
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.yourcompany.kmptemplate")
class CoreModule {

    @Single
    fun provideHttpClient(): HttpClient = createNetworkClient()
}
