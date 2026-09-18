package bo.gob.gamc.catastro.digid.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenManager.getAccessToken()

        return if (!token.isNullOrEmpty() && original.header("Authorization") == null) {
            val authenticated = original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticated)
        } else {
            chain.proceed(original)
        }
    }
}
