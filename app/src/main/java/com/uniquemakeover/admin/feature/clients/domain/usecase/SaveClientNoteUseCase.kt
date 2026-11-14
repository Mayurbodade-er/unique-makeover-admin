package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import com.uniquemakeover.admin.core.domain.result.DataResult
import javax.inject.Inject

class SaveClientNoteUseCase @Inject constructor(
    private val clientNoteRepository: ClientNoteRepository
) {
    companion object {
        private const val PHONE_DIGITS_LENGTH = 10
    }

    suspend operator fun invoke(phone: String, noteText: String): DataResult<Unit> {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "").takeLast(PHONE_DIGITS_LENGTH)
        return clientNoteRepository.saveNote(cleanPhone, noteText)
    }
}
