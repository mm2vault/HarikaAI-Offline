package ai.harika.jarvis

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.ai.edge.litertlm.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(8, 6, 18)
    private val panel = Color.rgb(24, 17, 43)
    private val purple = Color.rgb(190, 124, 255)
    private val cyan = Color.rgb(111, 220, 255)
    private val pink = Color.rgb(255, 105, 205)
    private val modelName = "Qwen3-0.6B.litertlm"
    private val modelUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private val releasesApi = "https://api.github.com/repos/mm2vault/HarikaAI-Offline/releases/latest"
    private val releasesPage = "https://github.com/mm2vault/HarikaAI-Offline/releases"
    private val channelId = "aura_notifications"
    private lateinit var messages: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var prompt: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var busy = false
    private var ttsReady = false
    private val prefs by lazy { getSharedPreferences("aura_ai", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 7, 25)
        window.navigationBarColor = Color.rgb(8, 6, 18)
        if (Build.VERSION.SDK_INT >= 21) window.decorView.systemUiVisibility = 0
        tts = TextToSpeech(this, this)
        makeNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 74)
        buildUi()
        restoreChat()
        val savedModel = File(filesDir, modelName)
        if (savedModel.exists() && savedModel.length() > 100_000_000L) {
            status.text = "●  Kayıtlı model bulundu; başlatılıyor…"
            initializeModel(savedModel)
        }
        checkUpdates(true)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = 1; setPadding(dp(16), dp(10), dp(16), dp(10)); setBackgroundColor(bg); alpha = 0f }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = 0; setPadding(0, dp(4), 0, dp(14)) }
        header.addView(TextView(this).apply {
            text = "✧"; textSize = 29f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = gradient(intArrayOf(Color.rgb(218, 120, 255), Color.rgb(75, 74, 220), Color.rgb(39, 181, 255)), 19)
        }, LinearLayout.LayoutParams(dp(52), dp(52)))
        val brand = LinearLayout(this).apply { orientation = 1; setPadding(dp(12), 0, 0, 0) }
        brand.addView(label("Aura AI", 22f, Color.WHITE, true))
        brand.addView(label("✦  YOUR AURA COMPANION", 10f, cyan, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("⚙", false) { settings() })
        root.addView(header)

        val hero = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER; setPadding(dp(16), dp(19), dp(16), dp(19))
            background = gradient(intArrayOf(Color.rgb(66, 31, 105), Color.rgb(31, 20, 58), Color.rgb(16, 13, 30)), 25)
        }
        hero.addView(TextView(this).apply {
            text = "◉"; textSize = 42f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = gradient(intArrayOf(Color.rgb(255, 120, 218), Color.rgb(126, 78, 255), Color.rgb(55, 185, 255)), 100)
            elevation = dp(10).toFloat()
        }, LinearLayout.LayoutParams(dp(78), dp(78)).apply { bottomMargin = dp(12) })
        hero.addView(label("Nasıl yardımcı olabilirim?", 20f, Color.WHITE, true))
        hero.addView(label("Sor, düşün, üret. Sohbetlerin cihazında kalır.", 12f, Color.rgb(194, 184, 215), false))
        hero.addView(label("✦  OFFLINE AI  ·  PRIVATE BY DESIGN  ✦", 9f, Color.rgb(208, 170, 255), true))
        status = label("●  Model henüz yüklenmedi", 12f, Color.rgb(255, 204, 123), true)
        (status.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.CENTER
        hero.addView(status)
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        hero.scaleX = 0.98f; hero.scaleY = 0.98f
        hero.animate().scaleX(1f).scaleY(1f).setDuration(650).start()

        val updatePanel = LinearLayout(this).apply {
            orientation = 1; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(25, 19, 43), 18, Color.rgb(104, 67, 151))
        }
        val updateRow = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        updateRow.addView(label("GÜNCELLEMELER", 11f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        updateRow.addView(button("Kontrol et", true) { checkUpdates(false) })
        updatePanel.addView(updateRow)
        updatePanel.addView(label("Yeni sürüm varsa burada haber verilir.", 12f, Color.LTGRAY, false))
        root.addView(updatePanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        scroll = ScrollView(this).apply { isFillViewport = true }
        messages = LinearLayout(this).apply { orientation = 1 }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(button("⬇  Ücretsiz AI modelini indir / başlat  ·  ~586 MB", true) { downloadModel() },
            LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8); bottomMargin = dp(8) })
        val composer = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        prompt = EditText(this).apply {
            hint = "Aura AI'e mesaj yaz…"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); textSize = 15f
            maxLines = 4; imeOptions = EditorInfo.IME_ACTION_SEND; setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(Color.rgb(27, 20, 45), 18, Color.rgb(112, 75, 163))
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false }
        }
        composer.addView(prompt, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(button("🎙", false) { voice() })
        composer.addView(button("➤", true) { sendMessage() })
        root.addView(composer)
        val stage = FrameLayout(this)
        stage.setBackgroundColor(bg)
        stage.addView(root, FrameLayout.LayoutParams(-1, -1))
        val splash = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER; setPadding(dp(28), dp(28), dp(28), dp(28))
            background = gradient(intArrayOf(Color.rgb(15, 8, 31), Color.rgb(40, 17, 68), Color.rgb(8, 6, 18)), 0)
        }
        val mascot = TextView(this).apply {
            text = "◉"; textSize = 68f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
            background = gradient(intArrayOf(Color.rgb(255, 111, 218), Color.rgb(135, 77, 255), Color.rgb(53, 190, 255)), 100)
            elevation = dp(18).toFloat()
        }
        splash.addView(mascot, LinearLayout.LayoutParams(dp(112), dp(112)).apply { bottomMargin = dp(22) })
        splash.addView(label("Aura AI", 34f, Color.WHITE, true))
        splash.addView(label("YOUR OWN LITTLE UNIVERSE", 11f, cyan, true).apply { setPadding(0, dp(8), 0, 0) })
        val shimmer = label("✦  ✧  ✦", 19f, purple, true).apply { setPadding(0, dp(26), 0, 0) }
        splash.addView(shimmer)
        stage.addView(splash, FrameLayout.LayoutParams(-1, -1))
        setContentView(stage)
        mascot.animate().rotationBy(12f).scaleX(1.06f).scaleY(1.06f).setDuration(700).withEndAction {
            mascot.animate().rotationBy(-12f).scaleX(1f).scaleY(1f).setDuration(700).start()
        }.start()
        splash.postDelayed({
            root.animate().alpha(1f).setDuration(450).start()
            splash.animate().alpha(0f).setDuration(400).withEndAction { stage.removeView(splash) }.start()
        }, 1050)
    }

    private fun restoreChat() {
        val saved = prefs.getString("history", "").orEmpty()
        if (saved.isBlank()) bubble("Aura AI", "Selam! Ben Aura AI. Modeli indirdiğinde cihazında yerel olarak sohbet edebiliriz.", false, false)
        else saved.split("\n--J--\n").forEach { row ->
            val i = row.indexOf("::")
            if (i > 0) bubble(row.substring(0, i), row.substring(i + 2), row.substring(0, i) == "Sen", false)
        }
    }

    private fun bubble(who: String, text: String, user: Boolean, save: Boolean) {
        val card = LinearLayout(this).apply {
            orientation = 1; setPadding(dp(13), dp(10), dp(13), dp(10))
            background = rounded(if (user) Color.rgb(62, 40, 96) else panel, 17, if (user) Color.rgb(108, 73, 153) else Color.rgb(52, 43, 72))
            layoutParams = LinearLayout.LayoutParams(if (user) (resources.displayMetrics.widthPixels * .78f).toInt() else -1, -2).apply {
                gravity = if (user) Gravity.END else Gravity.START; bottomMargin = dp(8)
            }
        }
        card.addView(label(who.uppercase(Locale.ROOT), 10f, if (user) cyan else purple, true))
        card.addView(label(text, 15f, Color.rgb(244, 241, 250), false).apply { setPadding(0, dp(4), 0, 0) })
        messages.addView(card)
        if (save) {
            val old = prefs.getString("history", "").orEmpty()
            val all = (old + if (old.isEmpty()) "" else "\n--J--\n") + "$who::$text"
            prefs.edit().putString("history", all.split("\n--J--\n").takeLast(80).joinToString("\n--J--\n")).apply()
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val q = prompt.text.toString().trim()
        if (q.isEmpty() || busy) return
        prompt.setText("")
        bubble("Sen", q, true, true)
        val conv = conversation
        if (conv == null) {
            busy = true
            status.text = "●  Hafif çevrimdışı mod · model yok"
            thread {
                val answer = lightweightReply(q)
                runOnUiThread {
                    bubble("Aura AI · HAFİF MOD", answer, false, true)
                    busy = false
                    if (ttsReady) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "aura-lite")
                }
            }
            return
        }
        busy = true; status.text = "●  Yanıt hazırlanıyor…"
        thread {
            try {
                val answer = conv.sendMessage(q).toString().trim()
                runOnUiThread {
                    bubble("Aura AI", answer, false, true)
                    status.text = "●  Yerel yapay zekâ hazır"; status.setTextColor(cyan)
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "aura_ai")
                    busy = false
                }
            } catch (e: Exception) {
                runOnUiThread { bubble("SİSTEM", "Yanıt hatası: " + (e.message ?: "bilinmeyen hata"), false, true); busy = false }
            }
        }
    }

    private fun lightweightReply(input: String): String {
        val q = input.lowercase(Locale.ROOT)
        return when {
            q.contains("selam") || q.contains("merhaba") || q.contains("salam") || q == "hey" -> "Selam! 💜 Ben Aura AI. Şu an hafif çevrimdışı moddayım. Daha doğal cevaplar için ücretsiz yerel modeli indirmen gerekiyor."
            q.contains("nasılsın") || q.contains("ne haber") -> "İyiyim, buradayım ve çalışıyorum! Model yüklü olmadığı için şu an sınırlı cevap verebiliyorum."
            q.contains("adın") || q.contains("kimsin") -> "Ben Aura AI, cihazında çalışması için tasarlanan kişisel asistanım."
            q.contains("yardım") || q.contains("ne yap") -> "Şu an hafif modda selamlaşma ve temel yönlendirme yapabiliyorum. Daha doğal sohbet için alttaki ücretsiz yerel modeli indir."
            q.contains("saat") || q.contains("tarih") -> "Cihazındaki tarih ve saat: " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(java.util.Date()) + "."
            q.contains("model") || q.contains("indir") -> "Tam sohbet motoru için yaklaşık 586 MB'lık Qwen yerel modelini indirmen gerekiyor."
            else -> "Mesajını aldım: ‘$input’ 💜 Ancak şu an yerel dil modeli yüklü değil. Bu hafif mod gerçek üretken AI değildir; ayrıntılı cevaplar için ücretsiz model indirme düğmesini kullan."
        }
    }

    private fun downloadModel() {
        if (busy) return
        status.text = "●  Model indiriliyor… Wi-Fi önerilir"
        thread {
            try {
                val file = File(filesDir, modelName)
                if (!file.exists() || file.length() < 100_000_000L) {
                    val part = File(filesDir, "$modelName.part")
                    val c = URL(modelUrl).openConnection() as HttpURLConnection
                    c.connectTimeout = 20000; c.readTimeout = 90000; c.instanceFollowRedirects = true
                    if (c.responseCode !in 200..299) error("HTTP " + c.responseCode)
                    val total = c.contentLengthLong
                    c.inputStream.use { src -> part.outputStream().use { dst ->
                        val buf = ByteArray(65536); var count = 0L
                        while (true) {
                            val n = src.read(buf); if (n < 0) break
                            dst.write(buf, 0, n); count += n
                            if (total > 0 && count % (5L * 1024 * 1024) < buf.size) {
                                val percent = count * 100 / total
                                runOnUiThread { status.text = "●  Model indiriliyor… $percent%" }
                            }
                        }
                    } }
                    c.disconnect()
                    if (part.length() < 100_000_000L) error("İndirilen dosya eksik.")
                    if (!part.renameTo(file)) error("Dosya kaydedilemedi.")
                }
                runOnUiThread { status.text = "●  Model başlatılıyor…"; initializeModel(file) }
            } catch (e: Exception) {
                runOnUiThread { status.text = "●  İndirme başarısız"; bubble("SİSTEM", "Model indirilemedi: " + (e.message ?: "bağlantı hatası"), false, true) }
            }
        }
    }

    private fun initializeModel(file: File) {
        thread {
            try {
                val e = Engine(EngineConfig(modelPath = file.absolutePath, backend = Backend.CPU()))
                e.initialize()
                val c = e.createConversation(ConversationConfig(systemInstruction = Contents.of("Sen Aura AI adlı Türkçe kişisel asistansın. Doğru, açık ve güvenli yanıt ver. Bilmediğin bilgiyi uydurma. Türkçe konuş.")))
                engine = e; conversation = c
                runOnUiThread {
                    status.text = "●  Yerel yapay zekâ hazır"; status.setTextColor(cyan)
                    notifyUser("Aura AI hazır", "Yerel yapay zekâ modeli kullanıma hazır.")
                    bubble("Aura AI", "Model hazır! Artık sohbet edebiliriz.", false, true)
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "●  Model başlatılamadı"; bubble("SİSTEM", "Model hatası: " + (e.message ?: "uyumsuz model"), false, true) }
            }
        }
    }

    private fun checkUpdates(silent: Boolean) {
        thread {
            try {
                val c = URL(releasesApi).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "AuraAI-Android")
                if (c.responseCode !in 200..299) error("HTTP " + c.responseCode)
                val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() }); c.disconnect()
                val tag = json.optString("tag_name", "")
                val assets = json.optJSONArray("assets")
                var apk = ""
                if (assets != null) for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    if (a.optString("name").endsWith(".apk", true)) { apk = a.optString("browser_download_url"); break }
                }
                val current = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0"
                runOnUiThread {
                    val publishedVersion = tag.removePrefix("aura-").removePrefix("v").trim()
                    if (publishedVersion.isNotBlank() && publishedVersion != current && apk.isNotBlank()) {
                        updateDialog(publishedVersion, json.optString("body", "Yeni sürüm hazır."), apk)
                    }
                    else if (!silent) info("Güncelleme kontrolü", "Yeni sürüm bulunamadı. Mevcut sürüm: $current")
                }
            } catch (e: Exception) {
                runOnUiThread { if (!silent) info("Güncelleme kontrol edilemedi", "Uygulama güncellemesi için herkese açık GitHub Release kaynağı gerekir.\n\n" + (e.message ?: "Bağlantı hatası")) }
            }
        }
    }

    private fun updateDialog(version: String, notes: String, apk: String) {
        notifyUser("Yeni güncelleme geldi", "Aura AI $version sürümü hazır.")
        AlertDialog.Builder(this).setTitle("✦ Yeni güncelleme geldi")
            .setMessage("Aura AI $version\n\n" + notes.take(700) + "\n\nGüncellemeyi indirip Android yükleme ekranını açalım mı?")
            .setNegativeButton("Daha sonra", null)
            .setPositiveButton("Güncellemeyi indir") { _, _ -> downloadUpdate(apk) }.show()
    }

    private fun downloadUpdate(url: String) {
        status.text = "●  Güncelleme indiriliyor…"
        thread {
            try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 15000; c.readTimeout = 90000; c.instanceFollowRedirects = true
                if (c.responseCode !in 200..299) error("HTTP " + c.responseCode)
                val apk = File(getExternalFilesDir(null), "aura-ai-update.apk")
                c.inputStream.use { src -> apk.outputStream().use { dst -> src.copyTo(dst) } }; c.disconnect()
                runOnUiThread {
                    if (Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
                        AlertDialog.Builder(this).setTitle("Yükleme izni gerekli")
                            .setMessage("Android ayarlarından Aura AI için bu kaynaktan uygulama yükleme iznini aç.")
                            .setPositiveButton("Ayarları aç") { _, _ -> startActivity(Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                            .setNegativeButton("İptal", null).show()
                    } else {
                        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apk)
                        startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            } catch (e: Exception) { runOnUiThread { info("Güncelleme hatası", e.message ?: "İndirme başarısız") } }
        }
    }

    private fun settings() {
        AlertDialog.Builder(this).setTitle("Aura AI · Ayarlar")
            .setItems(arrayOf("Sohbet geçmişini temizle", "Güncellemeleri kontrol et", "GitHub sürümleri")) { _, i ->
                when (i) {
                    0 -> AlertDialog.Builder(this).setTitle("Geçmiş silinsin mi?").setMessage("Bu cihazdaki sohbet geçmişi silinecek.")
                        .setNegativeButton("Vazgeç", null).setPositiveButton("Temizle") { _, _ ->
                            prefs.edit().remove("history").apply(); messages.removeAllViews()
                            bubble("Aura AI", "Sohbet geçmişi temizlendi.", false, false)
                        }.show()
                    1 -> checkUpdates(false)
                    2 -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releasesPage)))
                }
            }.show()
    }

    private fun voice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 72); return
        }
        try {
            startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR"); putExtra(RecognizerIntent.EXTRA_PROMPT, "Aura AI'e Türkçe konuş")
            }, 73)
        } catch (_: Exception) { Toast.makeText(this, "Bu cihazda konuşma tanıma desteklenmiyor.", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 73 && resultCode == RESULT_OK) prompt.setText(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty())
    }

    private fun makeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channelId, "Aura AI bildirimleri", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Model durumu ve uygulama güncellemeleri"
            })
    }

    private fun notifyUser(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val n = NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_aura)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_DEFAULT).build()
        getSystemService(NotificationManager::class.java).notify((title + body).hashCode(), n)
    }

    private fun info(title: String, body: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton("Tamam", null).setNeutralButton("Release sayfası") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releasesPage)))
            }.show()
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(text: String, primary: Boolean, click: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 13f; gravity = Gravity.CENTER
        setPadding(dp(12), dp(10), dp(12), dp(10)); setTextColor(if (primary) Color.WHITE else cyan)
        background = rounded(if (primary) Color.rgb(105, 62, 174) else Color.rgb(32, 26, 47), 14,
            if (primary) Color.rgb(159, 104, 227) else Color.rgb(74, 59, 98))
        setOnClickListener { click() }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), stroke)
    }
    private fun gradient(colors: IntArray, radius: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
        cornerRadius = dp(radius).toFloat(); setStroke(dp(1), Color.rgb(91, 65, 130))
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onInit(code: Int) {
        if (code == TextToSpeech.SUCCESS) {
            tts.language = Locale("tr", "TR")
            ttsReady = true
        }
    }
    override fun onDestroy() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        tts.shutdown()
        super.onDestroy()
    }
}