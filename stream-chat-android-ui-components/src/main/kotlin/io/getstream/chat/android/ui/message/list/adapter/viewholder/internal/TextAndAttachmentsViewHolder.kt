package io.getstream.chat.android.ui.message.list.adapter.viewholder.internal

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.getstream.sdk.chat.ChatMarkdown
import com.getstream.sdk.chat.adapter.MessageListItem
import com.getstream.sdk.chat.utils.extensions.inflater
import io.getstream.chat.android.client.models.Attachment
import io.getstream.chat.android.ui.common.extensions.internal.dpToPx
import io.getstream.chat.android.ui.common.extensions.internal.hasLink
import io.getstream.chat.android.ui.common.internal.LongClickFriendlyLinkMovementMethod
import io.getstream.chat.android.ui.databinding.StreamUiItemTextAndAttachmentsBinding
import io.getstream.chat.android.ui.message.list.adapter.MessageListItemPayloadDiff
import io.getstream.chat.android.ui.message.list.adapter.MessageListListenerContainer
import io.getstream.chat.android.ui.message.list.adapter.internal.DecoratedBaseMessageItemViewHolder
import io.getstream.chat.android.ui.message.list.adapter.view.internal.AttachmentClickListener
import io.getstream.chat.android.ui.message.list.adapter.view.internal.AttachmentDownloadClickListener
import io.getstream.chat.android.ui.message.list.adapter.view.internal.AttachmentLongClickListener
import io.getstream.chat.android.ui.message.list.adapter.view.internal.FileAttachmentsView
import io.getstream.chat.android.ui.message.list.adapter.view.internal.LinkAttachmentView
import io.getstream.chat.android.ui.message.list.adapter.view.internal.MediaAttachmentsGroupView
import io.getstream.chat.android.ui.message.list.adapter.viewholder.attachment.AttachmentViewFactory
import io.getstream.chat.android.ui.message.list.adapter.viewholder.decorator.internal.Decorator
import io.getstream.chat.android.ui.message.list.internal.MessageListItemStyle

internal class TextAndAttachmentsViewHolder(
    parent: ViewGroup,
    decorators: List<Decorator>,
    private val listeners: MessageListListenerContainer,
    private val markdown: ChatMarkdown,
    private val attachmentViewFactory: AttachmentViewFactory,
    private val style: MessageListItemStyle,
    internal val binding: StreamUiItemTextAndAttachmentsBinding = StreamUiItemTextAndAttachmentsBinding.inflate(
        parent.inflater,
        parent,
        false
    ),
) : DecoratedBaseMessageItemViewHolder<MessageListItem.MessageItem>(binding.root, decorators) {

    init {
        binding.run {
            root.setOnClickListener {
                listeners.messageClickListener.onMessageClick(data.message)
            }
            reactionsView.setReactionClickListener {
                listeners.reactionViewClickListener.onReactionViewClick(data.message)
            }
            footnote.setOnThreadClickListener {
                listeners.threadClickListener.onThreadClick(data.message)
            }
            root.setOnLongClickListener {
                listeners.messageLongClickListener.onMessageLongClick(data.message)
                true
            }
            LongClickFriendlyLinkMovementMethod.set(
                textView = messageText,
                longClickTarget = root,
                onLinkClicked = { url -> listeners.linkClickListener.onLinkClick(url) }
            )
        }
    }

    override fun bindData(data: MessageListItem.MessageItem, diff: MessageListItemPayloadDiff?) {
        super.bindData(data, diff)

        binding.messageText.isVisible = data.message.text.isNotEmpty()
        markdown.setText(binding.messageText, data.message.text)

        setupAttachment(data)
    }

    private fun setupAttachment(data: MessageListItem.MessageItem) {
        binding.attachmentsContainer.removeAllViews()

        val (links, attachments) = data.message.attachments.partition(Attachment::hasLink)

        if (attachments.isNotEmpty()) {
            val attachmentView = attachmentViewFactory.createAttachmentsView(attachments, context)
            binding.attachmentsContainer.addView(attachmentView)
            when (attachmentView) {
                is MediaAttachmentsGroupView -> setupMediaAttachmentView(attachmentView, attachments)
                is FileAttachmentsView -> setupFileAttachmentsView(attachmentView, attachments)
            }
        }

        links.firstOrNull()?.also(::setupLinkView)
    }

    private fun setupMediaAttachmentView(
        mediaAttachmentsGroupView: MediaAttachmentsGroupView,
        attachments: List<Attachment>,
    ) = mediaAttachmentsGroupView.run {
        setPadding(MEDIA_ATTACHMENT_VIEW_PADDING)
        setupBackground(data)
        attachmentClickListener = AttachmentClickListener {
            listeners.attachmentClickListener.onAttachmentClick(data.message, it)
        }
        attachmentLongClickListener = AttachmentLongClickListener {
            listeners.messageLongClickListener.onMessageLongClick(data.message)
        }
        showAttachments(attachments)
    }

    private fun setupFileAttachmentsView(fileAttachmentsView: FileAttachmentsView, attachments: List<Attachment>) =
        fileAttachmentsView.run {
            setPadding(FILE_ATTACHMENT_VIEW_PADDING)
            attachmentLongClickListener = AttachmentLongClickListener {
                listeners.messageLongClickListener.onMessageLongClick(data.message)
            }
            attachmentClickListener = AttachmentClickListener {
                listeners.attachmentClickListener.onAttachmentClick(data.message, it)
            }
            attachmentDownloadClickListener = AttachmentDownloadClickListener {
                listeners.attachmentDownloadClickListener.onAttachmentDownloadClick(it)
            }
            setAttachments(attachments)
        }

    private fun setupLinkView(linkAttachment: Attachment) {
        val linkAttachmentView = attachmentViewFactory.createLinkAttachmentView(linkAttachment, context)
        binding.attachmentsContainer.addView(linkAttachmentView)
        (linkAttachmentView as? LinkAttachmentView)?.run {
            setPadding(LINK_VIEW_PADDING)
            setLinkPreviewClickListener { url ->
                listeners.linkClickListener.onLinkClick(url)
            }
            setLongClickTarget(binding.root)
            style.getStyleTextColor(data.isMine)?.also(::setTextColor)
            showLinkAttachment(linkAttachment)
        }
    }

    private companion object {
        private val MEDIA_ATTACHMENT_VIEW_PADDING = 1.dpToPx()
        private val LINK_VIEW_PADDING = 8.dpToPx()
        private val FILE_ATTACHMENT_VIEW_PADDING = 4.dpToPx()
    }
}
