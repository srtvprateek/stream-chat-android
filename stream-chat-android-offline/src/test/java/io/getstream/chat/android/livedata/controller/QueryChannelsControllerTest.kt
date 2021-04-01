package io.getstream.chat.android.livedata.controller

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import io.getstream.chat.android.client.api.models.FilterObject
import io.getstream.chat.android.client.api.models.QuerySort
import io.getstream.chat.android.client.events.ChannelUpdatedEvent
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.EventType
import io.getstream.chat.android.client.models.Filters
import io.getstream.chat.android.client.utils.Result
import io.getstream.chat.android.livedata.BaseDomainTest2
import io.getstream.chat.android.livedata.randomChannel
import io.getstream.chat.android.livedata.randomUser
import io.getstream.chat.android.livedata.request.QueryChannelsPaginationRequest
import io.getstream.chat.android.livedata.utils.filter
import io.getstream.chat.android.test.asCall
import io.getstream.chat.android.test.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
internal class QueryChannelsControllerTest : BaseDomainTest2() {

    @Before
    override fun setup() {
        super.setup()
    }

    @Test
    fun `it should be possible to add new channels`() = runBlocking {
        val request = QueryChannelsPaginationRequest(QuerySort(), 0, 30, 10, 0)
        queryControllerImpl.runQuery(request)
        val channels = queryControllerImpl.channels.getOrAwaitValue()
        val oldSize = channels.size

        // verify that a new channel is added to the list
        val addedEvent = data.notificationAddedToChannel3Event
        queryControllerImpl.handleEvent(addedEvent)
        val newChannels = queryControllerImpl.channels.getOrAwaitValue()
        val newSize = newChannels.size

        Truth.assertThat(newSize - oldSize).isEqualTo(1)
    }

    @Test
    fun `it should be possible to filter out channels`(): Unit = runBlocking {
        val user = randomUser()
        val filter = Filters.and(Filters.eq("type", "livestream"), Filters.`in`("members", listOf(user)))

        val request = QueryChannelsPaginationRequest(QuerySort(), 0, 30, 10, 0)
        val queryChannelsController = chatDomainImpl.queryChannels(filter, QuerySort())

        queryChannelsController.newChannelEventFilter = { channel: Channel, filterObject: FilterObject ->
            filterObject.filter(channel)
        }
        queryChannelsController.runQuery(request)

        val channels = queryChannelsController.channels.getOrAwaitValue()

        // verify that a new channel is NOT added to the list
        val addedEvent = data.notificationAddedToChannel3Event
        queryChannelsController.handleEvent(addedEvent)
        val newChannels = queryChannelsController.channels.getOrAwaitValue()

        Truth.assertThat(newChannels).isEqualTo(channels)
    }

    @Test
    fun `events for channels part of the query should be NOT ignored`() {
        runBlocking {
            val filter = Filters.and(Filters.eq("type", "messaging"), Filters.eq("type", "messaging"))
            val request = QueryChannelsPaginationRequest(QuerySort(), 0, 30, 10, 0)
            val queryChannelsController = chatDomainImpl.queryChannels(filter, QuerySort())

            val randomChannel = randomChannel(type = "messaging", cid = "messaging:123-testing")

            val channelUpdatedEvent = ChannelUpdatedEvent(
                EventType.CHANNEL_UPDATED,
                Date(),
                randomChannel.cid,
                randomChannel.type,
                randomChannel.id,
                null,
                randomChannel
            )

            queryChannelsController.runQuery(request)
            Truth.assertThat(channelUpdatedEvent.channel.cid).isIn(queryChannelsController.queryChannelsSpec.cids)

            queryChannelsController.handleEvent(channelUpdatedEvent)
            val cids = queryChannelsController.channels.getOrAwaitValue().map { it.cid }
            Truth.assertThat(channelUpdatedEvent.channel.cid).isIn(cids)
        }
    }

    @Test
    fun `events for channels not part of the query should be ignored`() {
        runBlocking {
            val request = QueryChannelsPaginationRequest(QuerySort(), 0, 30, 10, 0)
            val queryChannelsController = chatDomainImpl.queryChannels(data.filter2, QuerySort())

            queryChannelsController.runQuery(request)
            val event = data.channelUpdatedEvent2
            Truth.assertThat(event.channel.cid).isNotIn(queryChannelsController.queryChannelsSpec.cids)

            queryChannelsController.handleEvent(event)
            val cids = queryChannelsController.channels.getOrAwaitValue().map { it.cid }
            Truth.assertThat(event.channel.cid).isNotIn(cids)
        }
    }

    @Test
    fun `it should be possible to load more data`() = runBlocking {
        whenever(clientMock.queryChannels(any())) doReturn listOf(randomChannel(), randomChannel()).asCall()
        queryControllerImpl = chatDomainImpl.queryChannels(Filters.eq("type", "messaging"), QuerySort())

        val paginate = QueryChannelsPaginationRequest(QuerySort(), 0, 3, 10, 0)
        val result: Result<List<Channel>> = queryControllerImpl.runQuery(paginate)
        val channels: List<Channel> = queryControllerImpl.channels.getOrAwaitValue()

        assertSuccess(result)
        Truth.assertThat(channels.size).isEqualTo(2)

        whenever(clientMock.queryChannels(any())) doReturn listOf(randomChannel()).asCall()

        val request: QueryChannelsPaginationRequest =
            queryControllerImpl.loadMoreRequest(1, 10, 0)
        val result2: Result<List<Channel>> = queryControllerImpl.runQuery(request)
        val newChannels = queryControllerImpl.channels.getOrAwaitValue()

        assertSuccess(result2)
        Truth.assertThat(request.channelOffset).isEqualTo(2)
        Truth.assertThat(newChannels.size).isEqualTo(3)
    }

    @Test
    fun offlineRunQuery() = runBlocking {
        // insert the query result into offline storage
        val query = QueryChannelsSpec(query.filter, query.sort)
        query.cids = listOf(data.channel1.cid)
        chatDomainImpl.repos.insertQueryChannels(query)
        chatDomainImpl.repos.insertMessage(data.message1)
        chatDomainImpl.storeStateForChannel(data.channel1)
        chatDomainImpl.setOffline()
        val channels = queryControllerImpl.runQueryOffline(QueryChannelsPaginationRequest(query.sort, 0, 2, 10, 0))
        // should return 1 since only 1 is stored in offline storage
        Truth.assertThat(channels?.size).isEqualTo(1)
        // verify we load messages correctly
        Truth.assertThat(channels!!.first().messages.size).isEqualTo(1)
    }

    @Test
    @Ignore("mock me")
    fun onlineRunQuery() = runBlocking {
        // insert the query result into offline storage
        val query = QueryChannelsSpec(query.filter, query.sort)
        query.cids = listOf(data.channel1.cid)
        chatDomainImpl.repos.insertQueryChannels(query)
        chatDomainImpl.storeStateForChannel(data.channel1)
        chatDomainImpl.setOffline()
        queryControllerImpl.runQuery(QueryChannelsPaginationRequest(query.sort, 0, 2, 10, 0))
        val channels = queryControllerImpl.channels.getOrAwaitValue()
        // should return 1 since only 1 is stored in offline storage
        Truth.assertThat(channels.size).isEqualTo(1)
    }
}
