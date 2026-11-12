package dev.revilodev.onewidget.widgets.clock

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.revilodev.onewidget.WidgetConfigActivity

class DigitalClockConfigActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, WidgetConfigActivity::class.java).putExtras(intent).addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT))
        finish()
    }
}
