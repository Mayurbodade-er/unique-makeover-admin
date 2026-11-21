package com.uniquemakeover.admin.feature.settings.data.backup

import android.content.Context
import android.net.Uri
import com.uniquemakeover.admin.core.common.UiText
import com.uniquemakeover.admin.core.di.IoDispatcher
import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot
import com.uniquemakeover.admin.feature.settings.domain.repository.BackupWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupWriterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupWriter {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    override suspend fun writeBackup(targetUriString: String, snapshot: BackupSnapshot): DataResult<Int> {
        return withContext(ioDispatcher) {
            try {
                val uri = Uri.parse(targetUriString)
                val jsonString = json.encodeToString(snapshot)
                val bytes = jsonString.toByteArray(Charsets.UTF_8)
                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext DataResult.Error(
                        UiText.DynamicString("Unable to open output stream for selected file")
                    )

                outputStream.use { stream ->
                    stream.write(bytes)
                    stream.flush()
                }
                DataResult.Success(bytes.size)
            } catch (e: IOException) {
                DataResult.Error(
                    UiText.DynamicString("Backup failed: Storage device was disconnected or write failed"),
                    e
                )
            } catch (e: SecurityException) {
                DataResult.Error(
                    UiText.DynamicString("Permission denied to write backup file"),
                    e
                )
            }
        }
    }
}
