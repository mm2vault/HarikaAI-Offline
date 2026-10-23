package ai.harika.aura

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.util.Linkify
import android.text.method.LinkMovementMethod
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
    private val bg = Color.rgb(9, 8, 18)
    private val panel = Color.rgb(23, 18, 38)
    private val purple = Color.rgb(180, 108, 255)
    private val cyan = Color.rgb(119, 226, 255)
    private val modelName = "Qwen3-0.6B.litertlm"
    private val modelUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private val releasesApi = "https://api.github.com/repos/mm2vault/HarikaAI-Offline/releases?per_page=30"
    private val releasesPage = "https://github.com/mm2vault/HarikaAI-Offline/releases"
    private val channelId = "aura_notifications"
    private lateinit var messages: LinearLayout
    private lateinit var welcomeArea: LinearLayout
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
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        makeNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 74)
        buildUi()
        restoreChat()
        val savedModel = File(filesDir, modelName)
        if (savedModel.exists() && savedModel.length() > 100_000_000L) {
            status.text = "Kayıtlı model bulundu; başlatılıyor…"
            initializeModel(savedModel)
        }
        checkUpdates(true)
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = 1; setPadding(dp(16), dp(10), dp(16), dp(10)); setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; orientation = 0; setPadding(0, dp(4), 0, dp(14)) }
        header.addView(AuraOrbView(this), LinearLayout.LayoutParams(dp(52), dp(52)))
        val brand = LinearLayout(this).apply { orientation = 1; setPadding(dp(12), 0, 0, 0) }
        brand.addView(label("Aura AI", 23f, Color.WHITE, true))
        brand.addView(label("KİŞİSEL YAPAY ZEKÂ", 10f, cyan, true))
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(button("Ayarlar", false) { settings() })
        root.addView(header)

        welcomeArea = LinearLayout(this).apply { orientation = 1 }
        val hero = LinearLayout(this).apply {
            orientation = 1; gravity = Gravity.CENTER; setPadding(dp(16), dp(17), dp(16), dp(17))
            background = gradient(intArrayOf(Color.rgb(44, 28, 76), Color.rgb(19, 16, 32)), 26)
            elevation = dp(2).toFloat()
        }
        hero.addView(AuraOrbView(this), LinearLayout.LayoutParams(dp(104), dp(104)).apply { bottomMargin = dp(12) })
        hero.addView(label("Nasıl yardımcı olabilirim?", 20f, Color.WHITE, true))
        hero.addView(label("Düşüncelerini keşfet. Fikirlerini gerçeğe dönüştür.", 12f, Color.rgb(194, 184, 215), false))
        hero.addView(label("YEREL MODEL · GİZLİLİK ODAKLI SOHBET", 9f, Color.rgb(190, 154, 255), true))
        status = label("Model henüz yüklenmedi", 12f, Color.rgb(255, 204, 123), true)
        (status.layoutParams as? LinearLayout.LayoutParams)?.gravity = Gravity.CENTER
        hero.addView(status)
        welcomeArea.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        val updatePanel = LinearLayout(this).apply {
            orientation = 1; setPadding(dp(12), dp(10), dp(12), dp(10))
            background = gradient(intArrayOf(Color.rgb(29, 23, 48), Color.rgb(18, 16, 31)), 20)
        }
        val updateRow = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        updateRow.addView(label("GÜNCELLEMELER", 11f, cyan, true), LinearLayout.LayoutParams(0, -2, 1f))
        updateRow.addView(button("Kontrol et", true) { checkUpdates(false) })
        updatePanel.addView(updateRow)
        updatePanel.addView(label("Sürüm denetimi ve güvenli APK kurulumu", 12f, Color.LTGRAY, false))
        welcomeArea.addView(updatePanel, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })

        // Quick actions are lightweight native views; no extra library is needed.
        val quickScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val quickRow = LinearLayout(this).apply {
            orientation = 0
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))
        }
        listOf("Fikir üret", "Kod yaz", "Webde ara: AI haberleri").forEach { suggestion ->
            val chip = button(suggestion, false) {
                if (suggestion.startsWith("Webde ara")) {
                    prompt.setText("Webde ara: AI haberleri")
                    sendMessage()
                } else {
                    prompt.setText(if (suggestion.contains("Kod")) "Bana bir kod örneği hazırla" else "Bana yaratıcı fikirler öner")
                    prompt.setSelection(prompt.text.length)
                    prompt.requestFocus()
                }
            }
            quickRow.addView(chip, LinearLayout.LayoutParams(-2, dp(40)).apply { marginEnd = dp(8) })
        }
        quickScroll.addView(quickRow)
        welcomeArea.addView(quickScroll, LinearLayout.LayoutParams(-1, -2))
        root.addView(welcomeArea, LinearLayout.LayoutParams(-1, -2))

        scroll = ScrollView(this).apply { isFillViewport = true }
        messages = LinearLayout(this).apply { orientation = 1 }
        scroll.addView(messages)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(button("Ücretsiz AI modelini indir / başlat · yaklaşık 586 MB", true) { downloadModel() },
            LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(8); bottomMargin = dp(8) })
        val composer = LinearLayout(this).apply { orientation = 0; gravity = Gravity.CENTER_VERTICAL }
        prompt = EditText(this).apply {
            hint = "Aura AI’ye mesaj yaz…"; setHintTextColor(Color.GRAY); setTextColor(Color.WHITE); textSize = 15f
            maxLines = 4; imeOptions = EditorInfo.IME_ACTION_SEND; setPadding(dp(12), dp(9), dp(12), dp(9))
            background = rounded(Color.rgb(25, 22, 39), 18, Color.rgb(103, 78, 145))
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false }
        }
        composer.addView(prompt, LinearLayout.LayoutParams(0, -2, 1f))
        composer.addView(button("Sesli", false) { voice() })
        composer.addView(button("Gönder", true) { sendMessage() })
        root.addView(composer)
        setContentView(root)
    }

    private data class ChatEntry(val who: String, val text: String)

    /**
     * Store history as JSON so message text can safely contain separators, colons and newlines.
     * Older installs are migrated from the previous delimiter-based format on first read.
     */
    private fun readHistory(): MutableList<ChatEntry> {
        val saved = prefs.getString("history", "").orEmpty()
        if (saved.isBlank()) return mutableListOf()
        val entries = mutableListOf<ChatEntry>()
        try {
            val array = org.json.JSONArray(saved)
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val who = item.optString("who", "")
                val text = item.optString("text", "")
                if (who.isNotBlank()) entries.add(ChatEntry(who, text))
            }
            return entries.takeLast(80).toMutableList()
        } catch (_: Exception) {
            // Continue below and migrate the old history representation.
        }
        saved.split("\\n--J--\\n", "\n--J--\n").forEach { row ->
            val i = row.indexOf("::")
            if (i > 0) entries.add(ChatEntry(row.substring(0, i), row.substring(i + 2)))
        }
        val migrated = entries.takeLast(80).toMutableList()
        writeHistory(migrated)
        return migrated
    }

    private fun writeHistory(entries: List<ChatEntry>) {
        val array = org.json.JSONArray()
        entries.takeLast(80).forEach { entry ->
            array.put(JSONObject().put("who", entry.who).put("text", entry.text))
        }
        prefs.edit().putString("history", array.toString()).apply()
    }

    private fun restoreChat() {
        val history = readHistory()
        if (history.isEmpty()) {
            bubble("AURA AI", "Selam! Ben Aura AI. Modeli indirdiğinde cihazında yerel olarak sohbet edebiliriz.", false, false)
        } else {
            welcomeArea.visibility = View.GONE
            history.forEach { entry ->
                bubble(entry.who, entry.text, entry.who == "Sen", false)
            }
        }
    }

    private fun bubble(who: String, text: String, user: Boolean, save: Boolean) {
        val card = LinearLayout(this).apply {
            orientation = 1; setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(if (user) Color.rgb(66, 42, 105) else Color.rgb(22, 19, 36), 18, if (user) Color.rgb(132, 91, 181) else Color.rgb(78, 60, 112))
            elevation = dp(1).toFloat()
            layoutParams = LinearLayout.LayoutParams(if (user) (resources.displayMetrics.widthPixels * .78f).toInt() else -1, -2).apply {
                gravity = if (user) Gravity.END else Gravity.START; bottomMargin = dp(8)
            }
        }
        card.addView(label(who.uppercase(Locale.ROOT), 10f, if (user) cyan else purple, true))
        card.addView(label(text, 15f, Color.rgb(244, 241, 250), false).apply {
            setPadding(0, dp(4), 0, 0)
            Linkify.addLinks(this, Linkify.WEB_URLS)
            movementMethod = LinkMovementMethod.getInstance()
            linksClickable = true
            setLinkTextColor(cyan)
        })
        messages.addView(card)
        if (save) {
            val history = readHistory()
            history.add(ChatEntry(who, text))
            writeHistory(history)
        }
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun sendMessage() {
        val q = prompt.text.toString().trim()
        if (q.isEmpty() || busy) return
        prompt.setText("")
        welcomeArea.visibility = View.GONE
        bubble("Sen", q, true, true)
        val webQuery = extractWebQuery(q)
        if (webQuery != null) {
            busy = true
            status.text = "İnternette aranıyor…"
            thread {
                try {
                    val results = WebSearch.search(webQuery)
                    val answer = if (results.isEmpty()) {
                        "Bu sorgu için sonuç bulunamadı. Daha farklı kelimelerle tekrar dene."
                    } else {
                        results.mapIndexed { index, item ->
                            "${index + 1}. ${item.title}\n${item.snippet}\n${item.url}"
                        }.joinToString("\n\n")
                    }
                    runOnUiThread {
                        bubble("AURA AI · WEB ARAMA", answer, false, true)
                        status.text = if (results.isEmpty()) "Arama tamamlandı · sonuç yok" else "İnternet araması tamamlandı"
                        busy = false
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        bubble("WEB ARAMA", "İnternette arama yapılamadı. Bağlantını kontrol edip tekrar dene.\nAyrıntı: ${e.message ?: "bağlantı hatası"}", false, true)
                        status.text = "İnternet araması kullanılamıyor"
                        busy = false
                    }
                }
            }
            return
        }
        val conv = conversation
        if (conv == null) {
            busy = true
            status.text = "Hafif çevrimdışı mod · model yok"
            thread {
                val answer = lightweightReply(q)
                runOnUiThread {
                    bubble("AURA AI · HAFİF MOD", answer, false, true)
                    busy = false
                    if (ttsReady) tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "aura-lite")
                }
            }
            return
        }
        busy = true; status.text = "Yanıt hazırlanıyor…"
        thread {
            try {
                val answer = conv.sendMessage(q).toString().trim()
                runOnUiThread {
                    bubble("AURA AI", answer, false, true)
                    status.text = "Yerel yapay zekâ hazır"; status.setTextColor(cyan)
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "aura_ai")
                    busy = false
                }
            } catch (e: Exception) {
                runOnUiThread { bubble("SİSTEM", "Yanıt hatası: " + (e.message ?: "bilinmeyen hata"), false, true); busy = false }
            }
        }
    }

    /**
     * Explicit online-search commands only; normal chat stays local/offline.
     * Examples: "webde ara: Türkiye AI haberleri" or "Türkiye AI haberleri webde ara".
     */
    private fun extractWebQuery(message: String): String? {
        val clean = message.trim()
        val patterns = listOf(
            Regex("(?i)^(?:webde ara|internette ara|internette araştır|web search)\\s*:?\\s*(.+)$"),
            Regex("(?i)^(.+?)\\s+(?:webde ara|internette ara)$")
        )
        for (pattern in patterns) {
            val match = pattern.matchEntire(clean) ?: continue
            val query = match.groupValues[1].trim()
            if (query.isNotEmpty()) return query
        }
        return null
    }

    private fun lightweightReply(input: String): String {
        val q = input.lowercase(Locale.ROOT)
        return when {
            q.contains("selam") || q.contains("merhaba") || q.contains("salam") || q == "hey" -> "Selam! Ben Aura AI. Şu an hafif çevrimdışı moddayım. Daha doğal cevaplar için ücretsiz yerel modeli indirmen gerekiyor."
            q.contains("nasılsın") || q.contains("ne haber") -> "İyiyim, buradayım ve çalışıyorum! Model yüklü olmadığı için şu an sınırlı cevap verebiliyorum."
            q.contains("adın") || q.contains("kimsin") -> "Ben Aura AI, cihazında çalışması için tasarlanan kişisel asistanım."
            q.contains("yardım") || q.contains("ne yap") -> "Şu an hafif modda selamlaşma ve temel yönlendirme yapabiliyorum. Daha doğal sohbet için alttaki ücretsiz yerel modeli indir."
            q.contains("saat") || q.contains("tarih") -> "Cihazındaki tarih ve saat: " + java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(java.util.Date()) + "."
            q.contains("model") || q.contains("indir") -> "Tam sohbet motoru için yaklaşık 586 MB'lık Qwen yerel modelini indirmen gerekiyor."
            else -> "Mesajını aldım: ‘$input’ Ancak şu an yerel dil modeli yüklü değil. Bu hafif mod gerçek üretken AI değildir; ayrıntılı cevaplar için ücretsiz model indirme düğmesini kullan."
        }
    }

    private fun downloadModel() {
        if (busy) return
        status.text = "Model indiriliyor… Wi-Fi önerilir"
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
                                runOnUiThread { status.text = "Model indiriliyor… $percent%" }
                            }
                        }
                    } }
                    c.disconnect()
                    if (part.length() < 100_000_000L) error("İndirilen dosya eksik.")
                    if (!part.renameTo(file)) error("Dosya kaydedilemedi.")
                }
                runOnUiThread { status.text = "Model başlatılıyor…"; initializeModel(file) }
            } catch (e: Exception) {
                runOnUiThread { status.text = "İndirme başarısız"; bubble("SİSTEM", "Model indirilemedi: " + (e.message ?: "bağlantı hatası"), false, true) }
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
                    status.text = "Yerel yapay zekâ hazır"; status.setTextColor(cyan)
                    notifyUser("Aura AI hazır", "Yerel yapay zekâ modeli kullanıma hazır.")
                    bubble("AURA AI", "Model hazır! Artık sohbet edebiliriz.", false, true)
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Model başlatılamadı"; bubble("SİSTEM", "Model hatası: " + (e.message ?: "uyumsuz model"), false, true) }
            }
        }
    }

    private fun checkUpdates(silent: Boolean) {
        thread {
            try {
                val c = URL(releasesApi).openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "AuraAI-Android")
                val code = c.responseCode
                if (code !in 200..299) {
                    c.disconnect()
                    error("GitHub HTTP $code")
                }
                val releaseList = org.json.JSONArray(c.inputStream.bufferedReader().use { it.readText() })
                c.disconnect()

                // Ignore legacy releases; only Aura builds with an APK count.
                val candidates = (0 until releaseList.length()).mapNotNull { index ->
                    val release = releaseList.optJSONObject(index) ?: return@mapNotNull null
                    val tag = release.optString("tag_name", "")
                    val match = Regex("^aura-build-(\\d+)$").matchEntire(tag) ?: return@mapNotNull null
                    val releaseCode = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
                    val assets = release.optJSONArray("assets") ?: return@mapNotNull null
                    var apkUrl: String? = null
                    for (assetIndex in 0 until assets.length()) {
                        val asset = assets.optJSONObject(assetIndex) ?: continue
                        val name = asset.optString("name", "")
                        val url = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://github.com/")) {
                            apkUrl = url
                            break
                        }
                    }
                    if (apkUrl.isNullOrBlank()) null else Triple(releaseCode, release, apkUrl)
                }
                val latest = candidates.maxByOrNull { it.first }
                if (latest == null) error("Aura AI APK sürümü bulunamadı.")
                val (releaseCode, release, apkUrl) = latest
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                val currentCode = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong()
                val current = packageInfo.versionName ?: "bilinmiyor"

                runOnUiThread {
                    if (releaseCode > currentCode) {
                        updateDialog(release.optString("tag_name", "Aura AI"), release.optString("body", "Yeni sürüm hazır."), apkUrl)
                    } else if (!silent) {
                        info("Güncelleme kontrolü", "Yeni Aura AI sürümü bulunamadı. Mevcut sürüm: $current (build $currentCode).")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!silent) info("Güncelleme kontrol edilemedi", "GitHub Release kaynağına ulaşılamadı.\n\n" + (e.message ?: "Bağlantı hatası"))
                }
            }
        }
    }

    private fun updateDialog(version: String, notes: String, apk: String) {
        notifyUser("Yeni güncelleme geldi", "Aura AI $version sürümü hazır.")
        AlertDialog.Builder(this).setTitle("Yeni güncelleme geldi")
            .setMessage("Aura AI $version\n\n" + notes.take(700) + "\n\nGüncellemeyi indirip Android yükleme ekranını açalım mı?")
            .setNegativeButton("Daha sonra", null)
            .setPositiveButton("Güncellemeyi indir") { _, _ -> downloadUpdate(apk) }.show()
    }

    private fun downloadUpdate(url: String) {
        status.text = "Güncelleme indiriliyor…"
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
                            bubble("AURA AI", "Sohbet geçmişi temizlendi.", false, false)
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

    /** Aura artwork is drawn locally with Canvas; no emoji fonts or network image required. */
    private inner class AuraOrbView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val cx = width / 2f
            val cy = height / 2f
            val radius = minOf(width, height) * 0.39f
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(1).toFloat()
            paint.color = Color.argb(130, 207, 164, 255)
            canvas.drawCircle(cx, cy, radius * 1.22f, paint)
            paint.color = Color.argb(90, 119, 226, 255)
            canvas.drawCircle(cx, cy, radius * 1.42f, paint)
            paint.style = Paint.Style.FILL
            paint.shader = RadialGradient(cx - radius * 0.25f, cy - radius * 0.32f, radius * 1.5f,
                intArrayOf(Color.rgb(240, 220, 255), Color.rgb(165, 94, 255), Color.rgb(62, 38, 135), Color.rgb(20, 16, 38)),
                floatArrayOf(0f, 0.34f, 0.72f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(cx, cy, radius, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.color = Color.argb(210, 222, 197, 255)
            canvas.drawArc(cx - radius * 0.72f, cy - radius * 0.42f, cx + radius * 0.72f, cy + radius * 0.62f, 205f, 235f, false, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            canvas.drawCircle(cx - radius * 0.27f, cy - radius * 0.35f, radius * 0.09f, paint)
            paint.color = Color.argb(190, 119, 226, 255)
            canvas.drawCircle(cx + radius * 0.54f, cy - radius * 0.18f, radius * 0.045f, paint)
        }
    }



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