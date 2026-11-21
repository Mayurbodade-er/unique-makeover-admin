package com.uniquemakeover.admin.feature.settings.domain.repository

import com.uniquemakeover.admin.core.domain.result.DataResult
import com.uniquemakeover.admin.feature.settings.domain.model.BackupSnapshot

interface BackupWriter {
    suspend fun writeBackup(targetUriString: String, snapshot: BackupSnapshot): DataResult<Int>
}
