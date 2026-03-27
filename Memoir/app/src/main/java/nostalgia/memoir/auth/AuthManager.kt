package nostalgia.memoir.auth

import nostalgia.memoir.auth.firebase.FirebaseAuthRepository
import nostalgia.memoir.auth.supabase.SupabaseAuthRepository

object AuthManager {

    enum class Provider {
        FIREBASE,
        SUPABASE
    }

    private var provider: Provider = Provider.FIREBASE

    private val firebase = FirebaseAuthRepository()

    private val supabase = SupabaseAuthRepository(
        url = "https://zqexdwjylkxrarzpsegh.supabase.co",
        key = "sb_publishable_rudlth_dfHpG4g84eZyI1w_SZ-te-f7"
    )

    fun setProvider(p: Provider) {
        provider = p
    }

    fun repo(): AuthRepository {
        return when (provider) {
            Provider.FIREBASE -> firebase
            Provider.SUPABASE -> supabase
        }
    }
}