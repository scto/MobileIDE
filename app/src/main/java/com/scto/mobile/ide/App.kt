package com.scto.mobile.ide

// import com.github.anrwatchdog.ANRWatchDog
// import com.rk.libcommons.application
import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.rk.crashhandler.CrashHandler
import com.rk.resources.Res
import com.rk.update.UpdateManager
import com.scto.mobile.ide.core.icons.pack.IconPackManager
import com.scto.mobile.ide.core.utils.LogCatcher
import com.scto.mobile.ide.utils.application
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class App : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    companion object {
        lateinit var iconPackManager: IconPackManager

        fun getTempDir(): File {
            val tmp = File(application!!.filesDir.parentFile, "tmp")
            if (!tmp.exists()) {
                tmp.mkdir()
            }
            return tmp
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        application = this
        // com.rk.libcommons.application = this
        Res.application = this
        iconPackManager = IconPackManager(this)

        Timber.plant(
            object : Timber.DebugTree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    val finalTag = tag ?: "ApkBuilder"
                    when (priority) {
                        android.util.Log.DEBUG -> LogCatcher.d(finalTag, message)
                        android.util.Log.INFO -> LogCatcher.i(finalTag, message)
                        android.util.Log.WARN -> LogCatcher.w(finalTag, message)
                        android.util.Log.ERROR -> LogCatcher.e(finalTag, message, t as? Exception)
                        else -> LogCatcher.i(finalTag, message)
                    }
                }
            }
        )

        GlobalScope.launch(Dispatchers.IO) {
            getTempDir().apply {
                if (exists() && listFiles().isNullOrEmpty().not()) {
                    deleteRecursively()
                }
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(CrashHandler)
        // ANRWatchDog().start()

        GlobalScope.launch(Dispatchers.IO) { UpdateManager().onUpdate() }

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .apply {
                        detectAll()
                        penaltyLog()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                                println(violation.message)
                                violation.printStackTrace()
                                violation.cause?.let { throw it }
                                println("vm policy error")
                            }
                        }
                    }
                    .build()
            )
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
    }
}
