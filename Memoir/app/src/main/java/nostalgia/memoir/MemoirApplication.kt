package nostalgia.memoir

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nostalgia.memoir.data.startup.AppStartupInitializer

class MemoirApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            AppStartupInitializer(this@MemoirApplication).run()
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        appScope.cancel()
    }
}
