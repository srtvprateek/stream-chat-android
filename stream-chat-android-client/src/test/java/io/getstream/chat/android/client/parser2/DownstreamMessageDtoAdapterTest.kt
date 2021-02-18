package io.getstream.chat.android.client.parser2

import com.google.common.truth.Truth
import io.getstream.chat.android.client.api2.model.dto.DownstreamMessageDto
import io.getstream.chat.android.client.parser2.DtoTestData.downstreamJson
import io.getstream.chat.android.client.parser2.DtoTestData.downstreamMessage
import org.amshove.kluent.shouldBeEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DownstreamMessageDtoAdapterTest {

    private val parser = MoshiChatParser()

    @Test
    fun `AssertJ - Deserialize JSON message with custom fields`() {
        val message = parser.fromJson(downstreamJson, DownstreamMessageDto::class.java)
        assertThat(message).isEqualTo(downstreamMessage)
    }

    @Test
    fun `Kluent - Deserialize JSON message with custom fields`() {
        val message = parser.fromJson(downstreamJson, DownstreamMessageDto::class.java)
        message shouldBeEqualTo downstreamMessage
    }

    @Test
    fun `Truth - Deserialize JSON message with custom fields`() {
        val message = parser.fromJson(downstreamJson, DownstreamMessageDto::class.java)
        Truth.assertThat(message).isEqualTo(downstreamMessage)
    }
}
