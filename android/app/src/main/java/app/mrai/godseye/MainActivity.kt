package app.mrai.godseye

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import app.mrai.godseye.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * God's Eye — light Android heart.
 *
 * The heavy 3D globe lives on a server (GitHub Pages by default, or any URL
 * the user sets in Settings). This app is only a glass shell around it:
 * splash, glass top pill with a live UTC clock, and a glass bottom dock.
 * No Cesium, no globe engine, no map data is bundled in the APK.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("gods-eye", Context.MODE_PRIVATE) }
    private val clockHandler = Handler(Looper.getMainLooper())
    private var chromeHidden = false
    private var firstLoad = true

    companion object {
        // Change this if the repo owner changes. Users can override it in Settings.
        const val DEFAULT_SERVER = "https://mrai-tech.github.io/mrai-gods-eye/"
        const val PREF_SERVER = "server_url"
    }

    private fun serverUrl(): String =
        prefs.getString(PREF_SERVER, DEFAULT_SERVER)!!.trimEnd('/')

    /** Clean embed view: the globe, without the web app's own chrome. */
    private fun embedUrl(): String = serverUrl() + "/?embed=1"

    /** Full web UI: the globe plus its own layer panel, search and settings. */
    private fun fullUrl(): String = serverUrl() + "/"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- WebView: the globe itself -------------------------------------
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            // Dark from the first frame: a WebView defaults to white, so a
            // slow or failed globe load showed a white page behind the shell.
            setBackgroundColor(android.graphics.Color.parseColor("#05070D"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String,
                    callback: GeolocationPermissions.Callback
                ) = callback.invoke(origin, true, false)
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false // stay inside the app

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if (firstLoad) binding.splashProgress.visibility = View.VISIBLE
                }

                override fun onPageFinished(view: WebView, url: String) {
                    if (firstLoad) {
                        firstLoad = false
                        // Splash fades out, globe fades in — iOS-style.
                        binding.splash.animate().alpha(0f).setDuration(600).withEndAction {
                            binding.splash.visibility = View.GONE
                        }
                        binding.chrome.animate().alpha(1f).setDuration(600).start()
                    }
                    binding.splashProgress.visibility = View.GONE
                }
            }
            loadUrl(embedUrl())
        }

        // --- Top pill: live UTC clock --------------------------------------
        binding.chrome.alpha = 0f
        val clockTick = object : Runnable {
            override fun run() {
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                binding.clockText.text = fmt.format(Date()) + " UTC"
                clockHandler.postDelayed(this, 1000)
            }
        }
        clockHandler.post(clockTick)

        // --- Bottom glass dock ---------------------------------------------
        binding.btnHome.setOnClickListener { tap(it) { binding.webView.loadUrl(embedUrl()) } }
        binding.btnLayers.setOnClickListener { tap(it) { binding.webView.loadUrl(fullUrl()) } }
        binding.btnFullscreen.setOnClickListener { tap(it) { toggleChrome() } }
        binding.btnShare.setOnClickListener { tap(it) { shareCurrent() } }
        binding.btnSettings.setOnClickListener { tap(it) { showServerDialog() } }
    }

    /** Little press motion + haptic, then the real action. */
    private fun tap(view: View, action: () -> Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(90).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
        }.start()
        action()
    }

    private fun toggleChrome() {
        chromeHidden = !chromeHidden
        val target = if (chromeHidden) 0f else 1f
        binding.chrome.animate().alpha(target).setDuration(300).start()
        binding.dock.animate().alpha(target).setDuration(300).start()
        Toast.makeText(
            this,
            if (chromeHidden) "Fullscreen — tap the globe to bring the glass back"
            else "Glass chrome back",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun shareCurrent() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, binding.webView.url ?: embedUrl())
            putExtra(Intent.EXTRA_SUBJECT, "God's Eye — live globe")
        }
        startActivity(Intent.createChooser(send, "Share this view"))
    }

    private fun showServerDialog() {
        val input = EditText(this).apply {
            setText(serverUrl())
            hint = "https://your-server.example/"
            setSingleLine(true)
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Server — where the globe lives")
            .setMessage("The heavy globe stays on a server. Paste its URL here and the app will load it instead.")
            .setView(input)
            .setPositiveButton("Save & reload") { _, _ ->
                val url = input.text.toString().trim().trimEnd('/')
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    prefs.edit().putString(PREF_SERVER, url).apply()
                    firstLoad = true
                    binding.splash.visibility = View.VISIBLE
                    binding.splash.alpha = 1f
                    binding.webView.loadUrl(embedUrl())
                    Toast.makeText(this, "Server saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "That needs to start with http:// or https://", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        clockHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
