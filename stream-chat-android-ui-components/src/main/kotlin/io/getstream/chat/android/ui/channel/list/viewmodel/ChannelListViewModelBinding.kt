@file:JvmName("ChannelListViewModelBinding")

package io.getstream.chat.android.ui.channel.list.viewmodel

import androidx.lifecycle.LifecycleOwner
import io.getstream.chat.android.ui.channel.list.ChannelListView
import io.getstream.chat.android.ui.channel.list.adapter.ChannelListItem

@JvmName("bind")
public fun ChannelListViewModel.bindView(
    view: ChannelListView,
    lifecycle: LifecycleOwner,
) {
    state.observe(lifecycle) { channelState ->
        if (channelState.isLoading) {
            view.showLoadingView()
        } else {
            view.hideLoadingView()
            channelState
                .channels
                .map(ChannelListItem::ChannelItem)
                .let(view::setChannels)
        }
    }

    paginationState.observe(lifecycle) { paginationState ->
        view.setPaginationEnabled(!paginationState.endOfChannels && !paginationState.loadingMore)

        if (paginationState.loadingMore) {
            view.showLoadingMore()
        } else {
            view.hideLoadingMore()
        }
    }

    view.setOnEndReachedListener {
        onAction(ChannelListViewModel.Action.ReachedEndOfList)
    }
}
