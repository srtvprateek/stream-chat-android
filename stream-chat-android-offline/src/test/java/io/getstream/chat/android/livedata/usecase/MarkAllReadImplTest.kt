package io.getstream.chat.android.livedata.usecase

import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.errors.ChatError
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.ChatDomainImpl
import io.getstream.chat.android.livedata.controller.ChannelControllerImpl
import io.getstream.chat.android.test.TestCall
import io.getstream.chat.android.test.TestCoroutineExtension
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class MarkAllReadImplTest {

    companion object {
        @JvmField
        @RegisterExtension
        val testCoroutines = TestCoroutineExtension()
    }

    private lateinit var chatDomain: ChatDomainImpl
    private lateinit var markAllRead: MarkAllReadImpl
    private lateinit var chatClient: ChatClient
    private lateinit var activeChannels: List<ChannelControllerImpl>

    @BeforeEach
    fun before() {
        activeChannels = listOf(mock(), mock(), mock())

        chatClient = mock()
        chatDomain = mock {
            whenever(mock.scope) doReturn testCoroutines.scope
            whenever(mock.client) doReturn chatClient
            whenever(mock.allActiveChannels()) doReturn activeChannels
        }
        markAllRead = MarkAllReadImpl(chatDomain)
    }

    @Test
    fun `Given successful response When marking all channels as read Should update all the controllers and return true`() {
        return testCoroutines.dispatcher.runBlockingTest {
            whenever(chatClient.markAllRead()) doReturn TestCall(Result(Unit))

            var result = markAllRead.invoke().execute()

            Truth.assertThat(result.isSuccess).isTrue()
            verify(chatClient, times(1)).markAllRead()
            activeChannels.forEach {
                verify(it).markRead()
            }
        }
    }

    @Test
    fun `Given failed response When marking all channels as read Should update all the controllers and return true`() {
        return testCoroutines.dispatcher.runBlockingTest {
            whenever(chatClient.markAllRead()) doReturn TestCall(Result(ChatError()))

            var result = markAllRead.invoke().execute()

            Truth.assertThat(result.isSuccess).isTrue()
            verify(chatClient, times(1)).markAllRead()
            activeChannels.forEach {
                verify(it).markRead()
            }
        }
    }
}
