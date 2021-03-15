package io.getstream.chat.android.livedata.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.getstream.chat.android.client.api.models.Pagination
import io.getstream.chat.android.livedata.randomMessage
import io.getstream.chat.android.livedata.randomMessageEntity
import io.getstream.chat.android.livedata.randomUser
import io.getstream.chat.android.livedata.repository.domain.message.MessageDao
import io.getstream.chat.android.livedata.repository.domain.message.MessageRepository
import io.getstream.chat.android.livedata.repository.domain.message.MessageRepositoryImpl
import io.getstream.chat.android.livedata.request.AnyChannelPaginationRequest
import io.getstream.chat.android.test.randomString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date

@ExperimentalCoroutinesApi
internal class MessageRepositoryTests {

    private lateinit var messageDao: MessageDao

    private lateinit var sut: MessageRepository

    @BeforeEach
    fun setup() {
        messageDao = mock()
        sut = MessageRepositoryImpl(messageDao, ::randomUser)
    }

    @Test
    fun `Given 2 messages in cache When select message entities Should return message from dao and cache`() = runBlockingTest {
        val cachedMessage1 = randomMessage(id = "id1")
        val cachedMessage2 = randomMessage(id = "id2")
        sut.insertMessages(listOf(cachedMessage1, cachedMessage2), true)
        val messageEntityFromDb1 = randomMessageEntity(id = "id3")
        val messageEntityFromDb2 = randomMessageEntity(id = "id4")
        whenever(messageDao.select(listOf("id3", "id4"))) doReturn listOf(messageEntityFromDb1, messageEntityFromDb2)

        val result = sut.selectMessages(listOf("id1", "id2", "id3", "id4"))

        result.size shouldBeEqualTo 4
        result.any { it.id == "id1" } shouldBeEqualTo true
        result.any { it.id == "id2" } shouldBeEqualTo true
        result.any { it.id == "id3" } shouldBeEqualTo true
        result.any { it.id == "id4" } shouldBeEqualTo true
    }

    @Test
    fun `when selecting messages for channel, correct messages should be requested to DAO`() = runBlockingTest {
        val createdAt = Date()
        val cid = randomString()
        val messageEntity = randomMessageEntity(createdAt = createdAt)

        val requestGreaterOrEqual = AnyChannelPaginationRequest(30).apply {
            messageFilterDirection = Pagination.GREATER_THAN_OR_EQUAL
        }

        val requestGreater = AnyChannelPaginationRequest(30).apply {
            messageFilterDirection = Pagination.GREATER_THAN
        }
        
        val requestLessThan = AnyChannelPaginationRequest(30).apply {
            messageFilterDirection = Pagination.LESS_THAN
        }

        val requestLessOrEqualThan = AnyChannelPaginationRequest(30).apply {
            messageFilterDirection = Pagination.LESS_THAN_OR_EQUAL
        }

        whenever(messageDao.select(requestGreaterOrEqual.messageFilterValue)) doReturn messageEntity
        whenever(messageDao.select(requestGreater.messageFilterValue)) doReturn messageEntity
        whenever(messageDao.select(requestLessThan.messageFilterValue)) doReturn messageEntity
        whenever(messageDao.select(requestLessOrEqualThan.messageFilterValue)) doReturn messageEntity

        whenever(messageDao.messagesForChannelEqualOrNewerThan(any(), any(), any())) doReturn listOf(messageEntity)
        whenever(messageDao.messagesForChannelNewerThan(any(), any(), any())) doReturn listOf(messageEntity)
        whenever(messageDao.messagesForChannelEqualOrOlderThan(any(), any(), any())) doReturn listOf(messageEntity)
        whenever(messageDao.messagesForChannelOlderThan(any(), any(), any())) doReturn listOf(messageEntity)

        sut.selectMessagesForChannel(cid, requestGreaterOrEqual)
        sut.selectMessagesForChannel(cid, requestGreater)
        sut.selectMessagesForChannel(cid, requestLessThan)
        sut.selectMessagesForChannel(cid, requestLessOrEqualThan)

        inOrder(messageDao).run {
            verify(messageDao).messagesForChannelEqualOrNewerThan(cid, requestGreaterOrEqual.messageLimit, createdAt)
            verify(messageDao).messagesForChannelNewerThan(cid, requestGreaterOrEqual.messageLimit, createdAt)
            verify(messageDao).messagesForChannelOlderThan(cid, requestLessOrEqualThan.messageLimit, createdAt)
            verify(messageDao).messagesForChannelEqualOrOlderThan(cid, requestLessThan.messageLimit, createdAt)
        }
    }
}
