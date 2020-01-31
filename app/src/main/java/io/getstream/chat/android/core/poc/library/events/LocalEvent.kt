package io.getstream.chat.android.core.poc.library.events

import io.getstream.chat.android.core.poc.library.EventType

open class LocalEvent(type: EventType) : ChatEvent() {
    init {
        this.type = type.label
    }
}