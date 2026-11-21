package com.uniquemakeover.admin.feature.settings.domain.model

object BackupFileName {
    private const val PREFIX = "UniqueMakeover_backup_"
    private const val EXTENSION = ".json"

    fun generate(dateString: String): String {
        return "$PREFIX$dateString$EXTENSION"
    }
}
