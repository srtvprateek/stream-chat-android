# Deprecations

This document lists deprecated constructs in the SDK, with their expected time ⌛ of further deprecations and removals.

| API / Feature | Deprecated (warning) | Deprecated (error) | Removed | Notes |
| --- | --- | --- | --- | --- |
| `MessageInputView#setSuggestionListView` <br/>*ui-components* | 2021.04.13 | 2021.04.27 ⌛ | 2021.05.25 ⌛ | Setting external SuggestionListView is no longer necessary |
| `ChatDomain.usecases` <br/>*offline* | 2021.04.6 | 2021.05.6 ⌛ | 2021.06.6 ⌛ | Replace this property call by obtaining a specific use case directly from ChatDomain |
| `MessageInputView#setMembers` <br/>*ui-components* | 2021.04.07 | 2021.04.21<br/>4.9.0 | 2021.05.05 ⌛ | Use MessageInputView::setUserLookupHandler instead of manually passing the list of users |
| `ChannelListView's empty state methods` <br/>*ui-components* | 2021.04.05 | 2021.04.21<br/>4.9.0 | 2021.05.05 ⌛ | These methods no longer need to be called directly, `setChannel` handles empty state changes automatically |
| `MessageListItemStyle#messageTextColorTheirs` <br/>*ui-components* | 2021.03.25 | 2021.04.21<br/>4.9.0 | 2021.05.25 ⌛ | Use MessageListItemStyle::textStyleTheirs::colorOrNull() instead |
| `MessageListItemStyle#messageTextColorMine` <br/>*ui-components* | 2021.03.25 | 2021.04.21<br/>4.9.0 | 2021.05.25 ⌛ | Use MessageListItemStyle::textStyleMine::colorOrNull() instead |
| `com.getstream.sdk.chat.ChatUI`<br/>*ui-components* | 2021.03.19<br/>4.8.0 | 2021.04.21<br/>4.9.0 | 2021.05.19 ⌛ | Use the new ChatUI implementation `io.getstream.chat.android.ui.ChatUI`
| `GetTotalUnreadCount#invoke`<br/> | 2021.03.17<br/>4.7.2  | 2021.04.21<br/>4.9.0 | 2021.05.17 ⌛ | Use ChatDomain::totalUnreadCount instead |
| `GetUnreadChannelCount#invoke`<br/> | 2021.03.17<br/>4.7.2  | 2021.04.21<br/>4.9.0 | 2021.05.17 ⌛ | Use ChatDomain::channelUnreadCount instead |
| `ChatClient#unMuteChannel`<br/>*client* | 2021.03.15<br/>4.7.1 | 2021.04.21<br/>4.9.0 | 2021.05.15 ⌛ | Use the `unmuteChannel` method instead |
| `ChatClient#unBanUser`<br/>*client* | 2021.03.15<br/>4.7.1 | 2021.04.21<br/>4.9.0 | 2021.05.15 ⌛ | Use the `unbanUser` method instead |
| `ChannelClient#unBanUser`<br/>*client* | 2021.03.15<br/>4.7.1 | 2021.04.21<br/>4.9.0 | 2021.05.15 ⌛ | Use the `unbanUser` method instead |
| `ChannelController#unBanUser`<br/>*client* | 2021.03.15<br/>4.7.1 | 2021.04.21<br/>4.9.0 | 2021.04.21<br/>4.9.0 | Use the `unbanUser` method instead |
| `ChatDomain.Builder` constructors with user params | 2021.02.26<br/>4.7.0 | 2021.05.26 ⌛ | 2021.08.26 ⌛ | Use `ChatDomain.Builder(context, chatClient)` instead |
| `ChatDomain#disconnect` | 2021.02.25<br/>4.7.0 | 2021.05.25 ⌛ | 2021.08.25 ⌛ | Use just `ChatClient#disconnect` instead |
| `setUser` (and similar) methods<br/>*client* | 2021.02.03<br/>4.5.3 | 2021.05.03 ⌛ | 2021.08.03 ⌛ | Replaced by `connectUser` style methods that return `Call` objects, see the updated documentation for [Initialization & Users](https://getstream.io/chat/docs/android/init_and_users/?language=kotlin)) |
| `MessageListViewModel.Event.AttachmentDownload`<br/>*ui-common* | 2021.01.29<br/>4.5.2 | 2021.02.29<br/>4.7.0 | 2021.03.29<br/>4.8.0 | Use `DownloadAttachment` instead |
| `subscribe` methods with Kotlin function parameters<br/>*client* | 2021.01.27<br/>4.5.2 | 2021.03.27<br/>4.8.0 | 2021.05.27 ⌛ | Use methods with `ChatEventListener` parameters instead (only affects Java clients) |
| `ChatUI(client, domain, context)`<br/>*ui-common* | 2020.11.09<br/>4.4.2 | 2021.02.22<br/>4.6.0 | 2021.03.22<br/>4.8.0 | Use ctor with just Context param instead |
| `User#unreadCount`<br/>*client* | 2020.11.05<br/>4.4.2 | 2021.02.22<br/>4.6.0 | 2021.03.22<br/>4.8.0 | Use `totalUnreadCount` instead |
| `ChannelController`<br/>*client* | 2020.11.04<br/>4.4.1 | 2021.02.22<br/>4.6.0 | 2021.04.21<br/>4.9.0 | Renamed due to conflicting name with a type in the offline library, use `ChannelClient` instead |
| `Chat` interface<br/>*ui-common* | 2020.10.27<br/>4.4.1 | 2021.02.22<br/>4.6.0 | 2021.04.21<br/>4.9.0 | Use `ChatUI` instead |
| `MessageListView#setViewHolderFactory`<br/>*ui (old)* | 2020.10.15<br/>4.3.1 | 2021.02.22<br/>4.6.0 | 2021.04.21<br/>4.9.0 | Use the more explicit `setMessageViewHolderFactory` method instead |
| `SendMessageWithAttachments` use case <br/>*offline* | 2020.09.30<br/>4.3.0 | 2021.02.22<br/>4.6.0 | 2021.04.21<br/>4.9.0 | - |
| `ChatObservable` based event APIs (`events()`)<br/>*client* | 2020.09.18 | 2021.02.22<br/>4.6.0 | 2021.04.21<br/>4.9.0 | Replace with direct `subscribe` calls on `ChatClient` and `ChannelClient`, see [migration guide](https://github.com/GetStream/stream-chat-android/wiki/Migration-guide:-ChatObserver-and-events()-APIs) |
| `ChatClient#flag(userId)`<br/>*client* | 2020.07.28 | 2021.02.22<br/>4.6.0 | 2021.03.22<br/>4.8.0 | Use the more explicit `flagUser` method instead |
| `ChatDomain.Builder#notificationConfig`<br/>*offline* | - | 2020.12.14<br/>4.4.7 | 2021.03.14<br/>4.8.0 | Configure this on `ChatClient.Builder` instead |
| `Call#enqueue((Result<T>) -> Unit)`<br/>*core* | - | 2020.12.09<br/>4.4.7 | 2021.03.09<br/>4.8.0 | Use `enqueue(Callback<T>)` instead (only affects Java clients) |
| `Pagination#get`<br/>*client* | - | 2020.10.12<br/>4.3.0 | 2021.02.22<br/>4.6.0 | Use `toString` instead |
| `MessageListItemAdapter#replaceEntities`<br/>*ui (old)* | - | 2020.10.05<br/>4.3.0 | 2021.02.22<br/>4.6.0 | Use `submitList` instead |
