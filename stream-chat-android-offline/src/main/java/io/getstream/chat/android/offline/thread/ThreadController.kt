package io.getstream.chat.android.offline.thread

import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.logger.ChatLogger
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.extensions.wasCreatedAfterOrAt
import io.getstream.chat.android.offline.channel.ChannelController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class ThreadController(
    val threadId: String,
    private val channelControllerImpl: ChannelController,
    domain: ChatDomainImpl,
) {
    private val _loadingOlderMessages = MutableStateFlow(false)
    private val _endOfOlderMessages = MutableStateFlow(false)
    private var firstMessage: Message? = null
    private val logger = ChatLogger.get("ThreadController")

    private val threadMessages: Flow<List<Message>> =
        channelControllerImpl.unfilteredMessages.map { messageList -> messageList.filter { it.id == threadId || it.parentId == threadId } }

    private val sortedVisibleMessages: StateFlow<List<Message>> = threadMessages.map {
        it.sortedBy { m -> m.createdAt ?: m.createdLocallyAt }
            .filter { channelControllerImpl.hideMessagesBefore == null || it.wasCreatedAfterOrAt(channelControllerImpl.hideMessagesBefore) }
    }.stateIn(domain.scope, SharingStarted.Eagerly, emptyList())

    /** the sorted list of messages for this thread */
    val messages: StateFlow<List<Message>> = sortedVisibleMessages

    /** if we are currently loading older messages */
    val loadingOlderMessages: StateFlow<Boolean> = _loadingOlderMessages

    /** if we've reached the earliest point in this thread */
    val endOfOlderMessages: StateFlow<Boolean> = _endOfOlderMessages

    fun getMessagesSorted(): List<Message> = messages.value

    suspend fun loadOlderMessages(limit: Int = 30): Result<List<Message>> {
        // TODO: offline storage for thread load more
        if (_loadingOlderMessages.value) {
            val errorMsg = "already loading messages for this thread, ignoring the load more requests."
            logger.logI(errorMsg)
            return Result(ChatError(errorMsg))
        }
        _loadingOlderMessages.value = true
        val result = channelControllerImpl.loadOlderThreadMessages(threadId, limit, firstMessage)
        if (result.isSuccess) {
            _endOfOlderMessages.value = result.data().size < limit
            firstMessage = result.data().sortedBy { it.createdAt }.firstOrNull() ?: firstMessage
        }

        _loadingOlderMessages.value = false
        return result
    }
}
