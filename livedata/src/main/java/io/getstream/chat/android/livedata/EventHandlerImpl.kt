package io.getstream.chat.android.livedata

import exhaustive
import io.getstream.chat.android.client.events.ChannelCreatedEvent
import io.getstream.chat.android.client.events.ChannelDeletedEvent
import io.getstream.chat.android.client.events.ChannelHiddenEvent
import io.getstream.chat.android.client.events.ChannelMuteEvent
import io.getstream.chat.android.client.events.ChannelTruncatedEvent
import io.getstream.chat.android.client.events.ChannelUnmuteEvent
import io.getstream.chat.android.client.events.ChannelUpdatedEvent
import io.getstream.chat.android.client.events.ChannelUserBannedEvent
import io.getstream.chat.android.client.events.ChannelUserUnbannedEvent
import io.getstream.chat.android.client.events.ChannelVisibleEvent
import io.getstream.chat.android.client.events.ChannelsMuteEvent
import io.getstream.chat.android.client.events.ChannelsUnmuteEvent
import io.getstream.chat.android.client.events.ChatEvent
import io.getstream.chat.android.client.events.CidEvent
import io.getstream.chat.android.client.events.ConnectedEvent
import io.getstream.chat.android.client.events.ConnectingEvent
import io.getstream.chat.android.client.events.DisconnectedEvent
import io.getstream.chat.android.client.events.ErrorEvent
import io.getstream.chat.android.client.events.GlobalUserBannedEvent
import io.getstream.chat.android.client.events.GlobalUserUnbannedEvent
import io.getstream.chat.android.client.events.HealthEvent
import io.getstream.chat.android.client.events.MemberAddedEvent
import io.getstream.chat.android.client.events.MemberRemovedEvent
import io.getstream.chat.android.client.events.MemberUpdatedEvent
import io.getstream.chat.android.client.events.MessageDeletedEvent
import io.getstream.chat.android.client.events.MessageReadEvent
import io.getstream.chat.android.client.events.MessageUpdatedEvent
import io.getstream.chat.android.client.events.NewMessageEvent
import io.getstream.chat.android.client.events.NotificationAddedToChannelEvent
import io.getstream.chat.android.client.events.NotificationChannelDeletedEvent
import io.getstream.chat.android.client.events.NotificationChannelMutesUpdatedEvent
import io.getstream.chat.android.client.events.NotificationChannelTruncatedEvent
import io.getstream.chat.android.client.events.NotificationInviteAcceptedEvent
import io.getstream.chat.android.client.events.NotificationInvitedEvent
import io.getstream.chat.android.client.events.NotificationMarkReadEvent
import io.getstream.chat.android.client.events.NotificationMessageNewEvent
import io.getstream.chat.android.client.events.NotificationMutesUpdatedEvent
import io.getstream.chat.android.client.events.NotificationRemovedFromChannelEvent
import io.getstream.chat.android.client.events.ReactionDeletedEvent
import io.getstream.chat.android.client.events.ReactionNewEvent
import io.getstream.chat.android.client.events.ReactionUpdateEvent
import io.getstream.chat.android.client.events.TypingStartEvent
import io.getstream.chat.android.client.events.TypingStopEvent
import io.getstream.chat.android.client.events.UnknownEvent
import io.getstream.chat.android.client.events.UserDeletedEvent
import io.getstream.chat.android.client.events.UserMutedEvent
import io.getstream.chat.android.client.events.UserPresenceChangedEvent
import io.getstream.chat.android.client.events.UserStartWatchingEvent
import io.getstream.chat.android.client.events.UserStopWatchingEvent
import io.getstream.chat.android.client.events.UserUnmutedEvent
import io.getstream.chat.android.client.events.UserUpdatedEvent
import io.getstream.chat.android.client.events.UsersMutedEvent
import io.getstream.chat.android.client.events.UsersUnmutedEvent
import io.getstream.chat.android.client.models.Channel
import io.getstream.chat.android.client.models.ChannelUserRead
import io.getstream.chat.android.livedata.entity.ChannelEntity
import io.getstream.chat.android.livedata.entity.MessageEntity
import io.getstream.chat.android.livedata.entity.UserEntity
import io.getstream.chat.android.livedata.extensions.users
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class EventHandlerImpl(var domainImpl: ChatDomainImpl, var runAsync: Boolean = true) {

    fun handleEvents(events: List<ChatEvent>) {
        if (runAsync) {
            domainImpl.scope.launch(domainImpl.scope.coroutineContext) {
                handleEventsInternal(events)
            }
        } else {
            runBlocking(domainImpl.scope.coroutineContext) { handleEventsInternal(events) }
        }
    }

    internal suspend fun handleEvent(event: ChatEvent) {
        handleEventsInternal(listOf(event))
    }

    internal suspend fun updateOfflineStorageFromEvents(events: List<ChatEvent>) {
        events.sortedBy { it.createdAt }

        val users: MutableMap<String, UserEntity> = mutableMapOf()
        val channels: MutableMap<String, ChannelEntity> = mutableMapOf()

        val messages: MutableMap<String, MessageEntity> = mutableMapOf()
        val channelsToFetch = mutableSetOf<String>()
        val messagesToFetch = mutableSetOf<String>()

        // step 1. see which data we need to retrieve from offline storage
        for (event in events) {
            when (event) {
                is MessageReadEvent -> channelsToFetch += event.cid
                is MemberAddedEvent -> channelsToFetch += event.cid
                is MemberRemovedEvent -> channelsToFetch += event.cid
                is NotificationRemovedFromChannelEvent -> channelsToFetch += event.cid
                is MemberUpdatedEvent -> channelsToFetch += event.cid
                is ChannelUpdatedEvent -> channelsToFetch += event.cid
                is ChannelDeletedEvent -> channelsToFetch += event.cid
                is ChannelHiddenEvent -> channelsToFetch += event.cid
                is ChannelVisibleEvent -> channelsToFetch += event.cid
                is NotificationAddedToChannelEvent -> channelsToFetch += event.cid
                is NotificationInvitedEvent -> channelsToFetch += event.cid
                is NotificationInviteAcceptedEvent -> channelsToFetch += event.cid
                is ChannelTruncatedEvent -> channelsToFetch += event.cid
                is ReactionNewEvent -> messagesToFetch += event.reaction.messageId
                is ReactionDeletedEvent -> messagesToFetch += event.reaction.messageId
                is ChannelCreatedEvent -> channelsToFetch += event.cid
                is ChannelMuteEvent -> channelsToFetch += event.channelMute.channel.cid
                is ChannelsMuteEvent -> {
                    event.channelsMute.forEach { channelsToFetch.add(it.channel.cid) }
                }
                is ChannelUnmuteEvent -> channelsToFetch += event.channelMute.channel.cid
                is ChannelsUnmuteEvent -> {
                    event.channelsMute.forEach { channelsToFetch.add(it.channel.cid) }
                }
                is HealthEvent -> { }
                is MessageDeletedEvent -> messagesToFetch += event.message.id
                is MessageUpdatedEvent -> messagesToFetch += event.message.id
                is NewMessageEvent -> messagesToFetch += event.message.id
                is NotificationMessageNewEvent -> messagesToFetch += event.message.id
                is ReactionUpdateEvent -> messagesToFetch += event.message.id
                is NotificationChannelDeletedEvent -> channelsToFetch += event.cid
                is NotificationChannelTruncatedEvent -> channelsToFetch += event.cid
                is NotificationMarkReadEvent -> channelsToFetch += event.cid
                is TypingStartEvent -> channelsToFetch += event.cid
                is TypingStopEvent -> channelsToFetch += event.cid
                is ChannelUserBannedEvent -> channelsToFetch += event.cid
                is UserStartWatchingEvent -> channelsToFetch += event.cid
                is UserStopWatchingEvent -> channelsToFetch += event.cid
                is ChannelUserUnbannedEvent -> channelsToFetch += event.cid
                is NotificationMutesUpdatedEvent -> { }
                is GlobalUserBannedEvent -> { }
                is UserDeletedEvent -> { }
                is UserMutedEvent -> { }
                is UsersMutedEvent -> { }
                is UserPresenceChangedEvent -> { }
                is GlobalUserUnbannedEvent -> { }
                is UserUnmutedEvent -> { }
                is UsersUnmutedEvent -> { }
                is UserUpdatedEvent -> { }
                is NotificationChannelMutesUpdatedEvent -> { }
                is ConnectedEvent -> { }
                is ConnectingEvent -> { }
                is DisconnectedEvent -> { }
                is ErrorEvent -> { }
                is UnknownEvent -> { }
            }.exhaustive
        }
        // actually fetch the data
        val channelMap = domainImpl.repos.channels.select(channelsToFetch.toList()).associateBy { it.cid }
        val messageMap = domainImpl.repos.messages.select(messagesToFetch.toList()).associateBy { it.id }

        // step 2. second pass through the events, make a list of what we need to update
        loop@ for (event in events) {
            when (event) {
                // keep the data in Room updated based on the various events..
                // note that many of these events should also update user information
                is NewMessageEvent -> {
                    event.message.cid = event.cid
                    event.totalUnreadCount?.let { domainImpl.setTotalUnreadCount(it) }
                    users.putAll(event.message.users().map { UserEntity(it) }.associateBy { it.id })
                    messages[event.message.id] = MessageEntity(event.message)
                }
                is MessageDeletedEvent -> {
                    event.message.cid = event.cid
                    users.putAll(event.message.users().map { UserEntity(it) }.associateBy { it.id })
                    messages[event.message.id] = MessageEntity(event.message)
                }
                is MessageUpdatedEvent -> {
                    event.message.cid = event.cid
                    users.putAll(event.message.users().map { UserEntity(it) }.associateBy { it.id })
                    messages[event.message.id] = MessageEntity(event.message)
                }
                is NotificationMessageNewEvent -> {
                    event.message.cid = event.cid
                    event.totalUnreadCount?.let { domainImpl.setTotalUnreadCount(it) }
                    users.putAll(event.message.users().map { UserEntity(it) }.associateBy { it.id })
                    messages[event.message.id] = MessageEntity(event.message)
                }
                is NotificationAddedToChannelEvent -> {
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                    channels[event.cid] = ChannelEntity(event.channel)
                }
                is NotificationInvitedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is NotificationInviteAcceptedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is ChannelHiddenEvent -> {
                    channels[event.cid] = ChannelEntity(Channel(cid = event.cid)).apply {
                        hidden = true
                        hideMessagesBefore = event.createdAt.takeIf { event.clearHistory }
                    }
                }
                is ChannelVisibleEvent -> {
                    channels[event.cid] = ChannelEntity(Channel(cid = event.cid)).apply {
                        hidden = false
                    }
                }
                is ChannelUserBannedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is GlobalUserBannedEvent -> {
                    users[event.user.id] = UserEntity(event.user).apply { banned = true }
                }
                is ChannelUserUnbannedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is GlobalUserUnbannedEvent -> {
                    users[event.user.id] = UserEntity(event.user).apply { banned = false }
                }
                is NotificationMutesUpdatedEvent -> {
                    domainImpl.updateCurrentUser(event.me)
                }
                is ConnectedEvent -> {
                    domainImpl.updateCurrentUser(event.me)
                }
                is MessageReadEvent -> {
                    // get the channel, update reads, write the channel
                    channelMap[event.cid]?.let {
                        channels[it.cid] = it.apply {
                            updateReads(ChannelUserRead(user = event.user, lastRead = event.createdAt))
                        }
                    }
                }
                is UserUpdatedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is ReactionNewEvent -> {
                    // get the message, update the reaction data, update the message
                    // note that we need to use event.reaction and not event.message
                    // event.message only has a subset of reactions
                    messageMap[event.reaction.messageId]?.let {
                        messages[it.id] = it.apply {
                            addReaction(event.reaction, domainImpl.currentUser.id == event.user.id)
                        }
                    } ?: Unit
                }
                is ReactionDeletedEvent -> {
                    // get the message, update the reaction data, update the message
                    messageMap[event.reaction.messageId]?.let {
                        messages[it.id] = it.apply {
                            removeReaction(event.reaction, false)
                            reactionCounts = event.message.reactionCounts
                        }
                    } ?: Unit
                }
                is ReactionUpdateEvent -> {
                    messageMap[event.reaction.messageId]?.let {
                        messages[it.id] = it.apply {
                            addReaction(event.reaction, domainImpl.currentUser.id == event.user.id)
                        }
                    }
                }
                is MemberAddedEvent -> {
                    channelMap[event.cid]?.let {
                        it.setMember(event.member.user.id, event.member)
                        channels[it.cid] = it
                        users.put(event.member.user.id, UserEntity(event.member.user.id))
                    }
                }
                is MemberUpdatedEvent -> {
                    channelMap[event.cid]?.let {
                        it.setMember(event.member.user.id, event.member)
                        channels[it.cid] = it
                        users.put(event.member.user.id, UserEntity(event.member.user.id))
                    }
                }
                is MemberRemovedEvent -> {
                    channelMap[event.cid]?.let {
                        it.setMember(event.user.id, null)
                        channels[it.cid] = it
                    }
                }
                is NotificationRemovedFromChannelEvent -> {
                    channelMap[event.cid]?.let {
                        it.setMember(event.user.id, null)
                        channels[it.cid] = it
                    }
                }
                is ChannelUpdatedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is ChannelDeletedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is ChannelCreatedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is ChannelMuteEvent -> {
                    channels[event.channelMute.channel.cid] = ChannelEntity(event.channelMute.channel)
                    users.putAll(event.channelMute.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is ChannelsMuteEvent -> {
                    event.channelsMute.forEach {
                        channels[it.channel.cid] = ChannelEntity(it.channel)
                        users.putAll(it.channel.users().map { UserEntity(it) }.associateBy { it.id })
                    }
                }
                is ChannelUnmuteEvent -> {
                    channels[event.channelMute.channel.cid] = ChannelEntity(event.channelMute.channel)
                    users.putAll(event.channelMute.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is ChannelsUnmuteEvent -> {
                    event.channelsMute.forEach {
                        channels[it.channel.cid] = ChannelEntity(it.channel)
                        users.putAll(it.channel.users().map { UserEntity(it) }.associateBy { it.id })
                    }
                }
                is ChannelTruncatedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is NotificationChannelDeletedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is NotificationChannelMutesUpdatedEvent -> {
                    domainImpl.updateCurrentUser(event.me)
                }
                is NotificationChannelTruncatedEvent -> {
                    channels[event.cid] = ChannelEntity(event.channel)
                    users.putAll(event.channel.users().map { UserEntity(it) }.associateBy { it.id })
                }
                is NotificationMarkReadEvent -> {
                    event.totalUnreadCount?.let { domainImpl.setTotalUnreadCount(it) }
                    channelMap[event.cid]?.let {
                        channels[it.cid] = it.apply {
                            updateReads(ChannelUserRead(user = event.user, lastRead = event.createdAt))
                        }
                    }
                }
                is UserDeletedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is UserMutedEvent -> {
                    users[event.targetUser.id] = UserEntity(event.targetUser)
                    users[event.user.id] = UserEntity(event.user)
                }
                is UsersMutedEvent -> {
                    event.targetUsers.forEach { users[it.id] = UserEntity(it) }
                    users[event.user.id] = UserEntity(event.user)
                }
                is UserPresenceChangedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is UserStartWatchingEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is UserStopWatchingEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                }
                is UserUnmutedEvent -> {
                    users[event.user.id] = UserEntity(event.user)
                    users[event.targetUser.id] = UserEntity(event.targetUser)
                }
                is UsersUnmutedEvent -> {
                    event.targetUsers.forEach { users[it.id] = UserEntity(it) }
                    users[event.user.id] = UserEntity(event.user)
                }
                is TypingStartEvent -> { }
                is TypingStopEvent -> { }
                is HealthEvent -> { }
                is ConnectingEvent -> { }
                is DisconnectedEvent -> { }
                is ErrorEvent -> { }
                is UnknownEvent -> { }
            }.exhaustive
        }
        // actually insert the data
        users.remove(domainImpl.currentUser.id)?.let { domainImpl.updateCurrentUser(it.toUser()) }
        domainImpl.repos.users.insert(users.values.toList())
        domainImpl.repos.channels.insert(channels.values.toList())
        // we only cache messages for which we're receiving events
        domainImpl.repos.messages.insert(messages.values.toList(), true)

        // handle delete and truncate events
        for (event in events) {
            when (event) {
                is NotificationChannelTruncatedEvent -> {
                    domainImpl.repos.messages.deleteChannelMessagesBefore(event.cid, event.createdAt)
                }
                is ChannelTruncatedEvent -> {
                    domainImpl.repos.messages.deleteChannelMessagesBefore(event.cid, event.createdAt)
                }
                is ChannelDeletedEvent -> {
                    domainImpl.repos.messages.deleteChannelMessagesBefore(event.cid, event.createdAt)
                    domainImpl.repos.channels.select(event.cid)?.let {
                        domainImpl.repos.channels.insert(it.apply { deletedAt = event.createdAt })
                    }
                }
            }
        }
    }

    private suspend fun handleEventsInternal(events: List<ChatEvent>) {
        events.sortedBy { it.createdAt }
        updateOfflineStorageFromEvents(events)

        // step 3 - forward the events to the active chanenls

        events.filterIsInstance<CidEvent>()
            .groupBy { it.cid }
            .filterNot { it.key.isBlank() }
            .forEach {
                val (cid, eventList) = it
                if (domainImpl.isActiveChannel(cid)) {
                    domainImpl.channel(cid).handleEvents(eventList)
                }
            }

        // only afterwards forward to the queryRepo since it borrows some data from the channel
        // queryRepo mainly monitors for the notification added to channel event
        for (queryRepo in domainImpl.getActiveQueries()) {
            queryRepo.handleEvents(events)
        }

        // send out the connect events
        for (event in events) {

            // connection events are never send on the recovery endpoint, so handle them 1 by 1
            when (event) {
                is DisconnectedEvent -> {
                    domainImpl.postOffline()
                }
                is ConnectedEvent -> {
                    val recovered = domainImpl.isInitialized()

                    domainImpl.postOnline()
                    domainImpl.postInitialized()
                    if (recovered && domainImpl.recoveryEnabled) {
                        domainImpl.connectionRecovered(true)
                    } else {
                        domainImpl.connectionRecovered(false)
                    }
                }
            }
        }
    }
}