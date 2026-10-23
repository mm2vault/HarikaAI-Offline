package ai.harika.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val purple = Color.rgb(180, 90, 255)
    private val modelName = "Qwen3-0.6B.litertlm"
    private val modelUrl = "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm"
    private lateinit var chat: TextView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val prefs by lazy { getSharedPreferences("jarvis", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 8, 18)
        window.navigationBarColor = Color.rgb(10, 8, 18)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 24, 18, 16)
            setBackgroundColor(Color.rgb(10, 8, 18))
        }
        root.addView(TextView(this).apply {
            text = "✦ JARVİS AI"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(purple)
        })
        status = TextView(this).apply {
            text = "Model henüz yüklenmedi"
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, 8, 0, 10)
        }
        root.addView(status)
        chat = TextView(this).apply {
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(27, 20, 42))
            text = prefs.getString("chat", "JARVİS: Merhaba! Önce ücretsiz yerel modeli indirebilirsin.") ?: ""
        }
        root.addView(ScrollView(this).apply { addView(chat) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "Ücretsiz yapay zekâ modelini indir (~586 MB)"
            setOnClickListener { downloadModel() }
        })
        input = EditText(this).apply {
            hint = "JARVİS'e bir şey sor..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 25, 49))
        }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Button(this).apply { text = "Gönder"; setOnClickListener { sendMessage() } })
        row.addView(Button(this).apply { text = "🎙"; setOnClickListener { startVoice() } })
        root.addView(row)
        root.addView(Button(this).apply {
            text = "İnternette ara"
            setOnClickListener {
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + java.net.URLEncoder.encode(q, "UTF-8"))))
            }
        })
        setContentView(root)
    }

    private fun append(line: String) {
        chat.append("\n\n" + line)
        prefs.edit().putString("chat", chat.text.toString()).apply()
    }

    private fun downloadModel() {
        status.text = "Model indiriliyor. Wi-Fi önerilir."
        thread {
            try {
                val file = File(filesDir, modelName)
                if (!file.exists() || file.length() < 100_000_000L) {
                    val conn = URL(modelUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 20000
                    conn.readTimeout = 60000
                    conn.instanceFollowRedirects = true
                    conn.connect()
                    if (conn.responseCode !in 200..299) error("HTTP " + conn.responseCode)
                    conn.inputStream.use { src -> file.outputStream().use { dst -> src.copyTo(dst) } }
                    conn.disconnect()
                }
                if (file.length() < 100_000_000L) error("Model dosyası eksik görünüyor.")
                runOnUiThread { initializeModel(file) }
            } catch (e: Exception) {
                runOnUiThread { status.text = "İndirme hatası: " + e.message }
            }
        }
    }

    private fun initializeModel(file: File) {
        status.text = "Yerel model başlatılıyor…"
        thread {
            try {
                val newEngine = Engine(EngineConfig(modelPath = file.absolutePath, backend = Backend.CPU()))
                newEngine.initialize()
                val conv = newEngine.createConversation(ConversationConfig(
                    systemInstruction = Contents.of("Sen JARVİS adlı Türkçe kişisel asistansın. Açık, doğru ve güvenli yanıt ver. Bilmediğin bilgiyi uydurma.")
                ))
                engine = newEngine
                conversation = conv
                runOnUiThread { status.text = "Yerel yapay zekâ hazır" }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Model başlatma hatası: " + e.message }
            }
        }
    }

    private fun sendMessage() {
        val prompt = input.text.toString().trim()
        if (prompt.isEmpty()) return
        input.setText("")
        append("Sen: " + prompt)
        if (prompt.equals("saat kaç", true) || prompt.equals("tarih ne", true)) {
            val pattern = if (prompt.contains("saat")) "HH:mm" else "dd MMMM yyyy"
            val answer = java.text.SimpleDateFormat(pattern, Locale("tr", "TR")).format(java.util.Date())
            append("JARVİS: " + answer)
            tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
            return
        }
        val conv = conversation
        if (conv == null) {
            append("JARVİS: Önce model indirme düğmesine bas.")
            return
        }
        status.text = "Yanıt hazırlanıyor…"
        thread {
            try {
                val answer = conv.sendMessage(prompt)
                runOnUiThread {
                    append("JARVİS: " + answer)
                    status.text = "Yerel yapay zekâ hazır"
                    tts.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Yanıt hatası: " + e.message }
            }
        }
    }

    private fun startVoice() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 72)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVİS'e Türkçe konuş")
            }
            startActivityForResult(intent, 73)
        } catch (_: Exception) {
            Toast.makeText(this, "Bu cihazda konuşma tanıma desteklenmiyor.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 73 && resultCode == RESULT_OK) {
            input.setText(data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty())
        }
    }

    override fun onInit(code: Int) {
        if (code == TextToSpeech.SUCCESS) tts.language = Locale("tr", "TR")
    }

    override fun onDestroy() {
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        tts.shutdown()
        super.onDestroy()
    }
}
