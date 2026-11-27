package com.aurora.store.compose.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aurora.store.R
import com.aurora.store.data.update.AasRelease
import com.aurora.store.data.update.AasUpdater
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun AasUpdatePanel() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<AasRelease.Candidate?>(null) }
    var downloaded by remember { mutableStateOf<File?>(null) }
    var status by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.aas_update_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.aas_update_explanation), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !busy, onClick = {
            busy = true
            candidate = null
            downloaded = null
            status = R.string.aas_update_checking
            scope.launch {
                try {
                    candidate = AasUpdater.check()
                    status = if (candidate == null) R.string.aas_update_none else null
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { status = R.string.aas_update_error }
                finally { busy = false }
            }
        }) { Text(stringResource(R.string.aas_update_check)) }
        candidate?.let { release ->
            Text(stringResource(R.string.aas_update_found, release.tag, release.version))
            if (downloaded == null) {
                Button(enabled = !busy, onClick = {
                    busy = true
                    status = R.string.aas_update_downloading
                    scope.launch {
                        try {
                            downloaded = AasUpdater.download(context, release)
                            status = R.string.aas_update_verified
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { status = R.string.aas_update_error }
                        finally { busy = false }
                    }
                }) { Text(stringResource(R.string.aas_update_download)) }
            }
        }
        downloaded?.let { file ->
            Button(enabled = !busy, onClick = {
                try {
                    if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:${context.packageName}")))
                        status = R.string.aas_update_permission
                    } else {
                        context.startActivity(AasUpdater.installIntent(context, file))
                    }
                } catch (_: Exception) { status = R.string.aas_update_error }
            }) { Text(stringResource(R.string.aas_update_install)) }
        }
        status?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    }
}
