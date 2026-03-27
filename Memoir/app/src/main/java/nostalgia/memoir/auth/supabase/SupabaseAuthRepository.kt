package nostalgia.memoir.auth.supabase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nostalgia.memoir.auth.AuthRepository
import nostalgia.memoir.auth.User
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SupabaseAuthRepository(
    private val url: String,
    private val key: String
) : AuthRepository {

    private val client = OkHttpClient()
    private var currentUser: User? = null

    override suspend fun signUp(email: String, password: String): Result<User> {
        return authRequest("signup", email, password)
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return authRequest("token?grant_type=password", email, password)
    }

    private suspend fun authRequest(
        path: String,
        email: String,
        password: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
                .put("email", email)
                .put("password", password)

            val requestBody = json.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val req = Request.Builder()
                .url("$url/auth/v1/$path")
                .addHeader("apikey", key)
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: ""

            if (!res.isSuccessful) {
                return@withContext Result.failure(Exception(body))
            }

            val obj = JSONObject(body)
            val userId = obj.optJSONObject("user")?.optString("id") ?: "unknown"

            val user = User(userId, email)
            currentUser = user
            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): User? = currentUser

    override fun signOut() {
        currentUser = null
    }
}