package nostalgia.memoir.auth

data class User(
    val id: String,
    val email: String?
)

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<User>
    suspend fun signIn(email: String, password: String): Result<User>
    fun getCurrentUser(): User?
    fun signOut()
}