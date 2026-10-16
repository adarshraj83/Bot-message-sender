package com.ghantas.embedsender

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.ClipData
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

class MainActivity : Activity() {
    private val gold = Color.rgb(255, 212, 59)
    private val dark = Color.rgb(25, 25, 25)
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var tokenInput: EditText
    private lateinit var channelInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var titleInput: EditText
    private lateinit var mode: Spinner
    private lateinit var colorButton: Button
    private var selectedEmbedColor: Int = Color.rgb(255, 212, 59)
    private lateinit var filesLabel: TextView
    private lateinit var status: TextView
    private lateinit var saveToken: CheckBox
    private val pickedUris = mutableListOf<Uri>()
    private val pickedNames = mutableListOf<String>()
    private val prefs by lazy { getSharedPreferences("sender", Context.MODE_PRIVATE) }
    private val alias = "Bot_Message_sender_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = dark
        window.navigationBarColor = dark
        buildUi()
        val saved = prefs.getString("token_enc", null)
        if (saved != null) {
            try { tokenInput.setText(decrypt(saved)) ; saveToken.isChecked = true } catch (_: Exception) { prefs.edit().remove("token_enc").apply() }
        }
        channelInput.setText(prefs.getString("last_channel", "") ?: "")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(245,245,245)); setPadding(dp(18), dp(14), dp(18), dp(18)) }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val heading = TextView(this).apply { text = "BOT MESSAGE SENDER"; textSize = 22f; setTextColor(dark); typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(4)) }
        content.addView(heading)
        val sub = TextView(this).apply { text = "Send Discord messages • Gold embeds • GIFs"; textSize = 13f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; setPadding(0,0,0,dp(16)) }
        content.addView(sub)
        tokenInput = field("Bot token (kept on this phone)", true); content.addView(tokenInput)
        saveToken = CheckBox(this).apply { text = "Remember token securely on this phone"; isChecked = false; setTextColor(dark) }; content.addView(saveToken)
        channelInput = field("Target channel ID"); channelInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER; content.addView(channelInput)
        mode = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Normal message", "Yellow embed")) }
        content.addView(label("Message type")); content.addView(mode)
        titleInput = field("Embed title (optional)"); content.addView(titleInput)
        colorButton = Button(this).apply {
            text = "🎨 Choose embed colour  #FFD43B"
            setOnClickListener { showColorPicker() }
            setBackgroundColor(selectedEmbedColor); setTextColor(Color.BLACK)
        }
        content.addView(colorButton)
        mode.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                titleInput.visibility = if (position == 1) View.VISIBLE else View.GONE
                colorButton.visibility = if (position == 1) View.VISIBLE else View.GONE
            }
        }
        messageInput = field("Message text", false).apply { minLines = 4; gravity = Gravity.TOP or Gravity.START }
        content.addView(messageInput)
        val choose = Button(this).apply { text = "Choose GIF / attachment"; setOnClickListener { pickFiles() } }; content.addView(choose)
        filesLabel = TextView(this).apply { text = "No files selected"; setTextColor(Color.DKGRAY); setPadding(dp(4),dp(4),dp(4),dp(8)) }; content.addView(filesLabel)
        val send = Button(this).apply { text = "SEND TO DISCORD"; setTextColor(dark); setBackgroundColor(gold); typeface = Typeface.DEFAULT_BOLD; setOnClickListener { sendMessage() } }; content.addView(send)
        val clear = Button(this).apply { text = "Clear saved token"; setOnClickListener { prefs.edit().remove("token_enc").apply(); tokenInput.text.clear(); saveToken.isChecked = false; showStatus("Saved token cleared.") } }; content.addView(clear)
        status = TextView(this).apply { text = "Ready. Check the channel ID before sending."; setTextColor(dark); textSize = 14f; setPadding(dp(4),dp(12),dp(4),dp(4)) }; content.addView(status)
        val note = TextView(this).apply { text = "Security: never share your bot token or this app. A timeout after upload starts can leave delivery uncertain; check Discord before trying again to avoid duplicates."; setTextColor(Color.DKGRAY); textSize = 12f; setPadding(dp(4),dp(16),dp(4),dp(8)) }; content.addView(note)
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }


    private fun showColorPicker() {
        val hsv = FloatArray(3)
        Color.colorToHSV(selectedEmbedColor, hsv)
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(4))
        }
        val sample = View(this).apply { setBackgroundColor(selectedEmbedColor) }
        dialogLayout.addView(sample, LinearLayout.LayoutParams(-1, dp(42)))
        val wheelLabel = TextView(this).apply { text = "Hue"; setTextColor(dark); setPadding(0, dp(8), 0, 0) }
        dialogLayout.addView(wheelLabel)
        val hue = SeekBar(this).apply { max = 360; progress = hsv[0].toInt() }
        dialogLayout.addView(hue)
        val svLabel = TextView(this).apply { text = "Choose shade"; setTextColor(dark); setPadding(0, dp(8), 0, 0) }
        dialogLayout.addView(svLabel)
        val shade = ShadeView(this, hsv[0], hsv[1], hsv[2])
        dialogLayout.addView(shade, LinearLayout.LayoutParams(-1, dp(190)))
        val hex = EditText(this).apply {
            hint = "Hex colour, e.g. #5865F2"; setSingleLine(true)
            setText(String.format("#%06X", 0xFFFFFF and selectedEmbedColor))
        }
        dialogLayout.addView(hex)
        val rgb = TextView(this).apply { setTextColor(dark); textSize = 13f }
        dialogLayout.addView(rgb)
        fun update(c: Int) {
            selectedEmbedColor = c
            sample.setBackgroundColor(c)
            val values = FloatArray(3); Color.colorToHSV(c, values)
            shade.setHue(values[0]); shade.setSV(values[1], values[2])
            rgb.text = "R ${Color.red(c)}   G ${Color.green(c)}   B ${Color.blue(c)}"
            colorButton.text = "🎨 Choose embed colour  ${String.format("#%06X", 0xFFFFFF and c)}"
            colorButton.setBackgroundColor(c)
            colorButton.setTextColor(if (Color.luminance(c) < 0.45) Color.WHITE else Color.BLACK)
        }
        hue.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                shade.setHue(progress.toFloat()); update(shade.color())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        shade.onColorChanged = { c -> update(c) }
        hex.setOnEditorActionListener { _, _, _ ->
            try { val raw = hex.text.toString().trim().removePrefix("#"); if (raw.length == 6) update(Color.parseColor("#$raw")) } catch (_: Exception) {}
            false
        }
        update(selectedEmbedColor)
        android.app.AlertDialog.Builder(this)
            .setTitle("Embed colour picker")
            .setView(dialogLayout)
            .setPositiveButton("Use colour") { _, _ ->
                try { val raw = hex.text.toString().trim().removePrefix("#"); if (raw.length == 6) selectedEmbedColor = Color.parseColor("#$raw") } catch (_: Exception) {}
                colorButton.text = "🎨 Choose embed colour  ${String.format("#%06X", 0xFFFFFF and selectedEmbedColor)}"
                colorButton.setBackgroundColor(selectedEmbedColor)
                colorButton.setTextColor(if (Color.luminance(selectedEmbedColor) < 0.45) Color.WHITE else Color.BLACK)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private class ShadeView(context: Context, initialHue: Float, initialSat: Float, initialVal: Float) : View(context) {
        private var hue = initialHue
        private var sat = initialSat
        private var value = initialVal
        var onColorChanged: ((Int) -> Unit)? = null
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val white = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val black = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        private val marker = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; style = android.graphics.Paint.Style.STROKE; strokeWidth =  dp(2).toFloat() }
        private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
        fun setHue(h: Float) { hue = h; invalidate() }
        fun setSV(s: Float, v: Float) { sat = s; value = v; invalidate() }
        fun color(): Int = Color.HSVToColor(floatArrayOf(hue, sat, value))
        override fun onDraw(canvas: android.graphics.Canvas) {
            super.onDraw(canvas)
            val base = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
            paint.shader = android.graphics.LinearGradient(0f, 0f, width.toFloat(), 0f, Color.WHITE, base, android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), Color.TRANSPARENT, Color.BLACK, android.graphics.Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null
            val x = sat * width; val y = (1f - value) * height
            canvas.drawCircle(x, y, dp(8).toFloat(), marker)
        }
        override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
            if (event.action == android.view.MotionEvent.ACTION_DOWN || event.action == android.view.MotionEvent.ACTION_MOVE || event.action == android.view.MotionEvent.ACTION_UP) {
                sat = (event.x / width.coerceAtLeast(1)).coerceIn(0f, 1f)
                value = (1f - event.y / height.coerceAtLeast(1)).coerceIn(0f, 1f)
                invalidate(); onColorChanged?.invoke(color()); return true
            }
            return true
        }
    }

    private fun field(hintText: String, password: Boolean = false): EditText = EditText(this).apply {
        hint = hintText; textSize = 15f; setTextColor(dark); setHintTextColor(Color.GRAY); setPadding(dp(10),dp(8),dp(10),dp(8))
        if (password) { inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(8).toFloat(); setStroke(dp(1), Color.LTGRAY) }
        val lp = LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(5), 0, dp(8)); layoutParams = lp
    }
    private fun label(s: String) = TextView(this).apply { text = s; setTextColor(dark); textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(2),dp(8),0,0) }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun showStatus(s: String) { runOnUiThread { status.text = s } }

    private fun pickFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "image/gif"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) }
        startActivityForResult(intent, 101)
    }
    @Deprecated("Deprecated by Android but compatible with minSdk 26")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 101 || resultCode != RESULT_OK || data == null) return
        pickedUris.clear(); pickedNames.clear()
        val clip = data.clipData
        if (clip != null) { for (i in 0 until clip.itemCount) addUri(clip.getItemAt(i).uri) } else data.data?.let { addUri(it) }
        filesLabel.text = if (pickedNames.isEmpty()) "No files selected" else "Selected: " + pickedNames.joinToString(", ")
    }
    private fun addUri(uri: Uri) { try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}; pickedUris.add(uri); pickedNames.add(queryName(uri)) }
    private fun queryName(uri: Uri): String { return contentResolver.query(uri, null, null, null, null)?.use { c -> val ix = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME); if (c.moveToFirst() && ix >= 0) c.getString(ix) else "attachment.gif" } ?: "attachment.gif" }

    private fun online(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(n) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun sendMessage() {
        val token = tokenInput.text.toString().trim()
        val channel = channelInput.text.toString().trim()
        val body = messageInput.text.toString()
        if (token.isBlank() || channel.isBlank()) { showStatus("Enter bot token and channel ID first."); return }
        if (!channel.matches(Regex("\\d{17,20}"))) { showStatus("Channel ID should be the numeric Discord channel ID."); return }
        if (body.isBlank() && pickedUris.isEmpty()) { showStatus("Enter message text or choose a file."); return }
        if (saveToken.isChecked) { try { prefs.edit().putString("token_enc", encrypt(token)).apply() } catch (_: Exception) { showStatus("Could not securely save token; sending without saving.") } } else prefs.edit().remove("token_enc").apply()
        prefs.edit().putString("last_channel", channel).apply()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(messageInput.windowToken, 0)
        if (!online()) { showStatus("No internet connection. Nothing was sent; reconnect and tap Send again."); return }
        showStatus("Sending… keep the app open until result appears.")
        worker.execute {
            try {
                val result = performRequest(token, channel, body)
                showStatus("✅ Sent successfully. Discord message ID: $result")
            } catch (e: RateLimitException) {
                showStatus("Discord rate-limited this request. Wait ${e.waitSeconds}s, then check the channel before retrying.")
            } catch (e: UncertainDeliveryException) {
                showStatus("⚠️ Delivery uncertain: ${e.message}. Check Discord before retrying to avoid duplicate messages.")
            } catch (e: Exception) {
                showStatus("❌ Not sent: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    private class RateLimitException(val waitSeconds: Int): Exception()
    private class UncertainDeliveryException(message: String): Exception(message)

    private fun performRequest(token: String, channelId: String, body: String): String {
        val conn = (URL("https://discord.com/api/v10/channels/$channelId/messages").openConnection() as HttpURLConnection)
        val boundary = "----Ghantas${UUID.randomUUID()}"
        var uploadStarted = false
        try {
            conn.requestMethod = "POST"; conn.connectTimeout = 12000; conn.readTimeout = 20000
            conn.setRequestProperty("Authorization", "Bot $token")
            conn.setRequestProperty("User-Agent", "GhantasEmbedSender/1.0")
            conn.doOutput = true
            val payload = JSONObject()
            if (mode.selectedItemPosition == 1) {
                val embed = JSONObject().put("description", body).put("color", selectedEmbedColor and 0x00FFFFFF)
                val title = titleInput.text.toString().trim(); if (title.isNotEmpty()) embed.put("title", title)
                if (pickedUris.isNotEmpty()) embed.put("image", JSONObject().put("url", "attachment://${safeFilename(pickedNames.first())}"))
                payload.put("embeds", JSONArray().put(embed))
            } else payload.put("content", body)
            if (pickedUris.isEmpty()) {
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                uploadStarted = true
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            } else {
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                uploadStarted = true
                DataOutputStream(conn.outputStream).use { out ->
                    out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"payload_json\"\r\nContent-Type: application/json\r\n\r\n")
                    out.write(payload.toString().toByteArray(Charsets.UTF_8)); out.writeBytes("\r\n")
                    for (i in pickedUris.indices) {
                        val name = safeFilename(pickedNames[i])
                        out.writeBytes("--$boundary\r\nContent-Disposition: form-data; name=\"files[$i]\"; filename=\"$name\"\r\nContent-Type: ${mimeFor(name)}\r\n\r\n")
                        contentResolver.openInputStream(pickedUris[i]).use { input -> if (input == null) throw Exception("Cannot read $name"); input.copyTo(out) }
                        out.writeBytes("\r\n")
                    }
                    out.writeBytes("--$boundary--\r\n")
                }
            }
            val code = conn.responseCode
            val response = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code == 429) {
                val retry = try { JSONObject(response).optDouble("retry_after", 5.0).toInt().coerceIn(1, 3600) } catch (_: Exception) { 5 }
                throw RateLimitException(retry)
            }
            if (code !in 200..299) {
                val message = try { JSONObject(response).optString("message", "HTTP $code") } catch (_: Exception) { "HTTP $code" }
                throw Exception("Discord returned $code: $message")
            }
            return JSONObject(response).optString("id", "accepted")
        } catch (e: java.net.SocketTimeoutException) {
            if (uploadStarted) throw UncertainDeliveryException("connection timed out after request began") else throw e
        } catch (e: java.io.IOException) {
            if (uploadStarted) throw UncertainDeliveryException("network dropped after request began") else throw e
        } finally { conn.disconnect() }
    }

    private fun safeFilename(s: String): String = s.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "attachment.gif" }.take(90)
    private fun mimeFor(name: String) = if (name.lowercase().endsWith(".gif")) "image/gif" else "application/octet-stream"

    private fun getKey(): SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val all = cipher.iv + encrypted
        return android.util.Base64.encodeToString(all, android.util.Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String {
        val all = android.util.Base64.decode(value, android.util.Base64.NO_WRAP)
        val iv = all.copyOfRange(0, 12); val encrypted = all.copyOfRange(12, all.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
