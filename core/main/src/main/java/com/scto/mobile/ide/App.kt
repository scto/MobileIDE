package com.scto.mobile.ide





import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.os.LocaleListCompat
import com.github.anrwatchdog.ANRWatchDog
import com.scto.mobile.ide.activities.main.session.SessionManager
import com.scto.mobile.ide.commands.CommandProvider
import com.scto.mobile.ide.commands.KeybindingsManager
import com.scto.mobile.ide.crashhandler.CrashHandler
import com.scto.mobile.ide.editor.CodeHighlighter
import com.scto.mobile.ide.editor.FontCache
import com.scto.mobile.ide.editor.KeywordManager
import com.scto.mobile.ide.editor.LanguageManager
import com.scto.mobile.ide.icons.pack.IconPackManager
import com.scto.mobile.ide.lsp.FileIconProvider
import com.scto.mobile.ide.lsp.LspPersistence
import com.scto.mobile.ide.lsp.MarkdownImageProvider
import com.scto.mobile.ide.settings.Preference
import com.scto.mobile.ide.settings.Settings
import com.scto.mobile.ide.settings.debugOptions.LogcatService
import com.scto.mobile.ide.settings.debugOptions.startThemeFlipperIfNotRunning
import com.scto.mobile.ide.settings.editor.DEFAULT_APP_FONT_PATH
import com.scto.mobile.ide.settings.editor.DEFAULT_EDITOR_FONT_PATH
import com.scto.mobile.ide.settings.editor.DEFAULT_TERMINAL_FONT_PATH
import com.scto.mobile.ide.theme.ThemeManager
import com.scto.mobile.ide.utils.application
import com.scto.mobile.ide.utils.getTempDir
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch











@OptIn(DelicateCoroutinesApi::class)
open class App : Application() {
    companion object {
        val versionCode: Long by lazy {
            val app = application ?: throw IllegalStateException("Application is not initialized yet")
            PackageInfoCompat.getLongVersionCode(app.packageManager.getPackageInfo(app.packageName, 0))
        }

        private var _iconPackManager: IconPackManager? = null
        val iconPackManager: IconPackManager
            get() {
                if (_iconPackManager == null) {
                    _iconPackManager = IconPackManager(application!!)
                }

                return _iconPackManager!!
            }

        private var _themeManager: ThemeManager? = null
        val themeManager: ThemeManager
            get() {
                if (_themeManager == null) {
                    _themeManager = ThemeManager(application!!)
                }

                return _themeManager!!
            }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        application = this
        Res.application = this

        LspPersistence.restoreServers()

        MarkdownImageProvider.register()
        FileIconProvider.register()

        CommandProvider.buildCommands()
        KeybindingsManager.loadKeybindings()

        val currentLocale = Locale.forLanguageTag(Settings.current_lang)
        val appLocale = LocaleListCompat.create(currentLocale)
        AppCompatDelegate.setApplicationLocales(appLocale)

        GlobalScope.launch(Dispatchers.IO) {
            launch(Dispatchers.IO) {
                iconPackManager.indexLocalPacks()
                iconPackManager.indexStoreIconPacks()
            }
            launch(Dispatchers.IO) {
                themeManager.indexLocalThemes()
                themeManager.indexStoreThemes()
            }

            launch { LanguageManager.initGrammarRegistry() }

            launch { KeywordManager.initKeywordRegistry(this@App) }

            launch { CodeHighlighter.registerMarkdownCodeHighlighter(this@App) }

            launch(Dispatchers.IO) { SessionManager.preloadSession() }

            launch(Dispatchers.IO) {
                val editorFontPath = Settings.editor_font_path.ifEmpty { DEFAULT_EDITOR_FONT_PATH }
                val isEditorAsset = if (editorFontPath.isNotEmpty()) Settings.is_editor_font_asset else true

                val appFontPath = Settings.app_font_path.ifEmpty { DEFAULT_APP_FONT_PATH }
                val isAppAsset = if (editorFontPath.isNotEmpty()) Settings.is_app_font_asset else true

                val terminalFontPath = Settings.terminal_font_path.ifEmpty { DEFAULT_TERMINAL_FONT_PATH }
                val isTerminalAsset = if (terminalFontPath.isNotEmpty()) Settings.is_terminal_font_asset else true

                FontCache.loadFont(this@App, editorFontPath, isEditorAsset)
                FontCache.loadFont(this@App, appFontPath, isAppAsset)
                FontCache.loadFont(this@App, terminalFontPath, isTerminalAsset)
            }

            launch(Dispatchers.IO) { Preference.preloadAllSettings() }

            launch { DocumentProvider.setDocumentProviderEnabled(this@App, Settings.expose_home_dir) }

            launch(Dispatchers.IO) {
                getTempDir().apply {
                    if (exists() && listFiles().isNullOrEmpty().not()) {
                        deleteRecursively()
                    }
                }
            }

            launch { runCatching { UpdateChecker.checkForUpdates("main") } }

            // wait until UpdateManager is done, it should only take few milliseconds
            UpdateManager.inspect()

            // debug options
            startThemeFlipperIfNotRunning()
            if (Settings.enable_logcat) {
                LogcatService.start(this@App)
            }
        }

        if (Settings.anr_watchdog) {
            ANRWatchDog().start()
        }

        if (Settings.strict_mode) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .apply {
                        detectAll()
                        penaltyLog()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                                violation.printStackTrace()
                                violation.cause?.let { throw it }
                            }
                        }
                    }
                    .build()
            )
        }
    }
}
