package ru.pfc.app.debug

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import ru.pfc.app.R
import java.io.File

/** Экран отладочной сборки: доступ к уведомлениям и отправка файла с записями (KTD16). */
class CaptureActivity : ComponentActivity() {

    private var hasAccess by mutableStateOf(false)
    private var recordCount by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.safeDrawingPadding().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(
                                if (hasAccess) R.string.capture_access_on else R.string.capture_access_off,
                            ),
                        )
                        Text(stringResource(R.string.capture_count, recordCount))
                        Button(onClick = ::openAccessSettings) {
                            Text(stringResource(R.string.capture_open_settings))
                        }
                        Button(onClick = ::shareFile, enabled = recordCount > 0) {
                            Text(stringResource(R.string.capture_share))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasAccess = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
        val file = captureFile(this)
        recordCount = if (file.exists()) file.useLines { lines -> lines.count() } else 0
    }

    private fun openAccessSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun shareFile() {
        // FileProvider не отдаёт файлы из noBackupFilesDir, поэтому наружу уходит копия в кэше.
        val copy = File(cacheDir, "captures/notifications.jsonl")
        captureFile(this).copyTo(copy, overwrite = true)
        val uri = FileProvider.getUriForFile(this, "$packageName.captures", copy)
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        startActivity(Intent.createChooser(send, null))
    }
}
