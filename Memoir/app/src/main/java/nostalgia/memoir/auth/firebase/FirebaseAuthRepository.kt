package nostalgia.memoir.auth.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import nostalgia.memoir.auth.AuthRepository
import nostalgia.memoir.auth.User

class FirebaseAuthRepository : AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    override suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            val res = auth.createUserWithEmailAndPassword(email, password).await()
            val user = res.user!!
            Result.success(User(user.uid, user.email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val res = auth.signInWithEmailAndPassword(email, password).await()
            val user = res.user!!
            Result.success(User(user.uid, user.email))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): User? {
        val u = auth.currentUser ?: return null
        return User(u.uid, u.email)
    }

    override fun signOut() {
        auth.signOut()
    }
}