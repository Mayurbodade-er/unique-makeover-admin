package com.uniquemakeover.admin.feature.settings.data.backup

import com.uniquemakeover.admin.feature.settings.domain.model.BackupFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupFileNameTest {

    @Test
    fun generate_formatsDefaultNamingConventionCorrectly() {
        val fileName = BackupFileName.generate("2026-11-14")
        assertEquals("UniqueMakeover_backup_2026-11-14.json", fileName)
    }
}
