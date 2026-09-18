package bo.gob.gamc.catastro.digid.di

import bo.gob.gamc.catastro.digid.BuildConfig
import bo.gob.gamc.catastro.digid.core.network.AuthInterceptor
import bo.gob.gamc.catastro.digid.core.network.KeycloakAuthenticator
import bo.gob.gamc.catastro.digid.data.remote.api.AiApi
import bo.gob.gamc.catastro.digid.data.remote.api.GeoextractApi
import bo.gob.gamc.catastro.digid.data.remote.api.KeycloakApi
import bo.gob.gamc.catastro.digid.data.remote.api.ResolucionesApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val contentType = "application/json".toMediaType()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        authenticator: KeycloakAuthenticator,
        logging: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideKeycloakApi(logging: HttpLoggingInterceptor): KeycloakApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val baseUrl = if (BuildConfig.KEYCLOAK_BASE_URL.endsWith("/")) BuildConfig.KEYCLOAK_BASE_URL else "${BuildConfig.KEYCLOAK_BASE_URL}/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(KeycloakApi::class.java)
    }

    @Provides
    @Singleton
    fun provideResolucionesApi(okHttpClient: OkHttpClient): ResolucionesApi {
        val baseUrl = if (BuildConfig.ERP_BACKEND_URL.endsWith("/")) BuildConfig.ERP_BACKEND_URL else "${BuildConfig.ERP_BACKEND_URL}/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ResolucionesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAiApi(logging: HttpLoggingInterceptor): AiApi {
        // Timeout de 180s para permitir inferencia local en Ollama
        val aiClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()

        val baseUrl = if (BuildConfig.AI_BACKEND_URL.endsWith("/")) BuildConfig.AI_BACKEND_URL else "${BuildConfig.AI_BACKEND_URL}/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(aiClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeoextractApi(okHttpClient: OkHttpClient): GeoextractApi {
        val baseUrl = if (BuildConfig.IDEC_ERP_BACKEND_URL.endsWith("/")) BuildConfig.IDEC_ERP_BACKEND_URL else "${BuildConfig.IDEC_ERP_BACKEND_URL}/"

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GeoextractApi::class.java)
    }
}
