package com.ghantas.embedsender

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(12, 16, 28)
        window.navigationBarColor = Color.rgb(12, 16, 28)

        val background = Color.rgb(12, 16, 28)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(background)
            setPadding(32, 32, 32, 32)
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f
            scaleX = 0.88f
            scaleY = 0.88f
        }

        root.addView(
            logo,
            LinearLayout.LayoutParams(dp(124), dp(124))
        )

        val heading = TextView(this).apply {
            text = "BOT MESSAGE SENDER"
            textSize = 19f
            letterSpacing = 0.12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = 0f
        }

        val headingParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        headingParams.topMargin = dp(24)
        root.addView(heading, headingParams)

        val subtitle = TextView(this).apply {
            text = "Simple. Fast. Reliable."
            textSize = 13f
            letterSpacing = 0.05f
            setTextColor(Color.rgb(165, 177, 200))
            gravity = Gravity.CENTER
            alpha = 0f
        }

        val subtitleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        subtitleParams.topMargin = dp(9)
        root.addView(subtitle, subtitleParams)

        setContentView(root)

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(650)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        heading.animate()
            .alpha(1f)
            .setStartDelay(180)
            .setDuration(550)
            .start()

        subtitle.animate()
            .alpha(1f)
            .setStartDelay(300)
            .setDuration(450)
            .start()

        root.postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 1500)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
