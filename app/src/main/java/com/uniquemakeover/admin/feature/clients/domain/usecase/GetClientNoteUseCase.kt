package com.uniquemakeover.admin.feature.clients.domain.usecase

import com.uniquemakeover.admin.core.domain.model.ClientNote
import com.uniquemakeover.admin.core.domain.repository.ClientNoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetClientNoteUseCase @Inject constructor(
    private val clientNoteRepository: ClientNoteRepository
) {
    companion object {
        private const val PHONE_DIGITS_LENGTH = 10
    }

    operator fun invoke(phone: String): Flow<ClientNote?> {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "").takeLast(PHONE_DIGITS_LENGTH)
        return clientNoteRepository.getNote(cleanPhone)
    }
}
