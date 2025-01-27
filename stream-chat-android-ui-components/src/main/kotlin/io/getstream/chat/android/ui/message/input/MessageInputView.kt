package io.getstream.chat.android.ui.message.input

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.view.updatePadding
import com.getstream.sdk.chat.model.AttachmentMetaData
import com.getstream.sdk.chat.utils.extensions.focusAndShowKeyboard
import com.getstream.sdk.chat.utils.extensions.inflater
import io.getstream.chat.android.client.models.Command
import io.getstream.chat.android.client.models.Member
import io.getstream.chat.android.client.models.Message
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.models.name
import io.getstream.chat.android.ui.R
import io.getstream.chat.android.ui.common.Debouncer
import io.getstream.chat.android.ui.common.extensions.internal.getFragmentManager
import io.getstream.chat.android.ui.databinding.StreamUiMessageInputBinding
import io.getstream.chat.android.ui.message.input.attachment.internal.AttachmentDialogFragment
import io.getstream.chat.android.ui.message.input.attachment.internal.AttachmentSelectionListener
import io.getstream.chat.android.ui.message.input.attachment.internal.AttachmentSource
import io.getstream.chat.android.ui.message.input.internal.MessageInputFieldView
import io.getstream.chat.android.ui.suggestion.internal.SuggestionListController
import io.getstream.chat.android.ui.suggestion.internal.SuggestionListPopupWindow
import io.getstream.chat.android.ui.suggestion.list.SuggestionListView
import kotlinx.coroutines.flow.collect
import java.io.File
import kotlin.properties.Delegates

public class MessageInputView : ConstraintLayout {

    public var inputMode: InputMode by Delegates.observable(InputMode.Normal) { _, previousValue, newValue ->
        configSendAlsoToChannelCheckbox()
        configInputMode(previousValue, newValue)
    }

    public var chatMode: ChatMode by Delegates.observable(ChatMode.GROUP_CHAT) { _, _, _ ->
        configSendAlsoToChannelCheckbox()
    }

    private lateinit var binding: StreamUiMessageInputBinding
    private lateinit var style: MessageInputViewStyle

    private var currentAnimatorSet: AnimatorSet? = null
    private var sendMessageHandler: MessageSendHandler = EMPTY_MESSAGE_SEND_HANDLER
    private var suggestionListController: SuggestionListController? = null
    private var isSendButtonEnabled: Boolean = true
    private var mentionsEnabled: Boolean = true
    private var commandsEnabled: Boolean = true

    private var onSendButtonClickListener: OnMessageSendButtonClickListener? = null
    private var typingListener: TypingListener? = null

    private val attachmentSelectionListener = object : AttachmentSelectionListener {
        override fun onAttachmentsSelected(attachments: Set<AttachmentMetaData>, attachmentSource: AttachmentSource) {
            if (attachments.isNotEmpty()) {
                when (attachmentSource) {
                    AttachmentSource.MEDIA,
                    AttachmentSource.CAMERA,
                    -> {
                        binding.messageInputFieldView.mode =
                            MessageInputFieldView.Mode.MediaAttachmentMode(attachments.toList())
                    }
                    AttachmentSource.FILE -> {
                        binding.messageInputFieldView.mode =
                            MessageInputFieldView.Mode.FileAttachmentMode(attachments.toList())
                    }
                }
            }
        }
    }

    private var userLookupHandler: UserLookupHandler = DefaultUserLookupHandler(emptyList())
    private var messageInputDebouncer: Debouncer? = null

    public constructor(context: Context) : super(context) {
        init(context)
    }

    public constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    public constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun configInputMode(previousValue: InputMode, newValue: InputMode) {
        when (newValue) {
            is InputMode.Reply -> {
                binding.inputModeHeader.isVisible = true
                binding.headerLabel.text = context.getString(R.string.stream_ui_reply_to_message)
                binding.inputModeIcon.setImageResource(R.drawable.stream_ui_ic_arrow_curve_left)
                binding.messageInputFieldView.onReply(newValue.repliedMessage)
                binding.messageInputFieldView.binding.messageEditText.focusAndShowKeyboard()
            }

            is InputMode.Edit -> {
                binding.inputModeHeader.isVisible = true
                binding.headerLabel.text = context.getString(R.string.stream_ui_message_option_edit)
                binding.inputModeIcon.setImageResource(R.drawable.stream_ui_ic_edit)
                binding.messageInputFieldView.onEdit(newValue.oldMessage)
                binding.messageInputFieldView.binding.messageEditText.focusAndShowKeyboard()
            }

            else -> {
                binding.inputModeHeader.isVisible = false
                if (previousValue is InputMode.Reply) {
                    binding.messageInputFieldView.onReplyDismissed()
                }
            }
        }
    }

    public fun setOnSendButtonClickListener(listener: OnMessageSendButtonClickListener?) {
        this.onSendButtonClickListener = listener
    }

    public fun setTypingListener(listener: TypingListener?) {
        this.typingListener = listener
    }

    /**
     * Sets up [MessageSendHandler] implementation. [MessageInputView] will delegate all the message sending operations
     * to this object.
     */
    public fun setSendMessageHandler(handler: MessageSendHandler) {
        this.sendMessageHandler = handler
    }

    @Deprecated(
        message = "Use setUserLookupHandler instead of manually passing the list of users",
        ReplaceWith(
            "setUserLookupHandler(DefaultUserLookupHandler(members.map(Member::user)))",
            "io.getstream.chat.android.ui.message.input.MessageInputView.DefaultUserLookupHandler",
            "io.getstream.chat.android.client.models.Member"
        ),
        level = DeprecationLevel.ERROR,
    )
    public fun setMembers(members: List<Member>) {
        setUserLookupHandler(DefaultUserLookupHandler(members.map(Member::user)))
    }

    public fun setCommands(commands: List<Command>) {
        suggestionListController?.commands = commands
        refreshControlsState()
    }

    public fun enableSendButton() {
        isSendButtonEnabled = true
        refreshControlsState()
    }

    public fun disableSendButton() {
        isSendButtonEnabled = false
        refreshControlsState()
    }

    /**
     * Enables or disables the handling of mentions in the message input view.
     *
     * @param enabled True if handling of mentions in the message input view is enabled, false otherwise.
     */
    public fun setMentionsEnabled(enabled: Boolean) {
        this.mentionsEnabled = enabled
        suggestionListController?.mentionsEnabled = mentionsEnabled
    }

    /**
     * Enables or disables the commands feature.
     *
     * @param enabled True if commands are enabled, false otherwise.
     */
    public fun setCommandsEnabled(enabled: Boolean) {
        commandsEnabled = enabled
        suggestionListController?.commandsEnabled = commandsEnabled
        refreshControlsState()
    }

    @Deprecated(
        message = "Setting external SuggestionListView is no longer necessary to display suggestions popup",
        level = DeprecationLevel.WARNING,
    )
    public fun setSuggestionListView(suggestionListView: SuggestionListView) {
        setSuggestionListViewInternal(suggestionListView, popupWindow = false)
    }

    private fun setSuggestionListViewInternal(suggestionListView: SuggestionListView, popupWindow: Boolean = true) {
        suggestionListView.configStyle(style)

        suggestionListView.setOnSuggestionClickListener(
            object : SuggestionListView.OnSuggestionClickListener {
                override fun onMentionClick(user: User) {
                    binding.messageInputFieldView.autoCompleteUser(user)
                }

                override fun onCommandClick(command: Command) {
                    binding.messageInputFieldView.autoCompleteCommand(command)
                }
            }
        )

        suggestionListView.binding.suggestionsCardView.setCardBackgroundColor(style.suggestionsBackground)

        val dismissListener = PopupWindow.OnDismissListener {
            binding.commandsButton.postDelayed(CLICK_DELAY) { binding.commandsButton.isSelected = false }
        }
        val suggestionListUi = if (popupWindow) {
            SuggestionListPopupWindow(suggestionListView, this, dismissListener)
        } else {
            suggestionListView
        }
        suggestionListController = SuggestionListController(suggestionListUi).also {
            it.mentionsEnabled = mentionsEnabled
            it.commandsEnabled = commandsEnabled
            it.userLookupHandler = userLookupHandler
        }
        refreshControlsState()
    }

    public fun setUserLookupHandler(handler: UserLookupHandler) {
        this.userLookupHandler = handler
        suggestionListController?.userLookupHandler = handler
    }

    private fun SuggestionListView.configStyle(style: MessageInputViewStyle) {
        style.commandsTitleTextStyle.apply(binding.commandsTitleTextView)
        styleCommandsName(style.commandsNameTextStyle)
        styleCommandsDescription(style.commandsDescriptionTextStyle)
        styleMentionsUsername(style.mentionsUsernameTextStyle)
        styleMentionsName(style.mentionsNameTextStyle)
        styleMentionsIcon(style.mentionsIcon)
    }

    public fun setMaxMessageLength(maxMessageLength: Int) {
        binding.messageInputFieldView.setMaxMessageLength(maxMessageLength)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        messageInputDebouncer = Debouncer(TYPING_DEBOUNCE_MS)
    }

    override fun onDetachedFromWindow() {
        messageInputDebouncer?.shutdown()
        messageInputDebouncer = null
        suggestionListController?.hideSuggestionList()
        super.onDetachedFromWindow()
    }

    @SuppressLint("CustomViewStyleable")
    private fun init(context: Context, attr: AttributeSet? = null) {
        binding = StreamUiMessageInputBinding.inflate(context.inflater, this)
        style = MessageInputViewStyle(context, attr)

        setBackgroundColor(style.backgroundColor)
        configAttachmentButton()
        configLightningButton()
        configTextInput()
        configSendButton()
        configSendAlsoToChannelCheckbox()
        configSendButtonListener()
        binding.dismissInputMode.setOnClickListener { dismissInputMode(inputMode) }
        setMentionsEnabled(style.mentionsEnabled)
        setCommandsEnabled(style.commandsEnabled)
        setSuggestionListViewInternal(SuggestionListView(context))
        binding.messageInputFieldView.setAttachmentMaxFileMb(style.attachmentMaxFileSize)
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.stream_ui_spacing_tiny)
        updatePadding(left = horizontalPadding, right = horizontalPadding)

        refreshControlsState()
    }

    public suspend fun listenForBigAttachments(listener: BigFileSelectionListener) {
        binding.messageInputFieldView.hasBigAttachment.collect(listener::handleBigFileSelected)
    }

    private fun dismissInputMode(inputMode: InputMode) {
        if (inputMode is InputMode.Reply) {
            sendMessageHandler.dismissReply()
        }

        this.inputMode = InputMode.Normal
    }

    private fun configSendButtonListener() {
        binding.sendMessageButtonEnabled.setOnClickListener {
            onSendButtonClickListener?.onClick()
            inputMode.let {
                when (it) {
                    is InputMode.Normal -> sendMessage()
                    is InputMode.Thread -> sendThreadMessage(it.parentMessage)
                    is InputMode.Edit -> editMessage(it.oldMessage)
                    is InputMode.Reply -> sendMessage(it.repliedMessage)
                }
            }
            binding.messageInputFieldView.clearContent()
        }
    }

    private fun configSendAlsoToChannelCheckbox() {
        val isThreadModeActive = inputMode is InputMode.Thread
        val shouldShowCheckbox = style.showSendAlsoToChannelCheckbox && isThreadModeActive
        if (shouldShowCheckbox) {
            val text = when (chatMode) {
                ChatMode.GROUP_CHAT -> {
                    context.getString(R.string.stream_ui_send_also_to_channel)
                }
                ChatMode.DIRECT_CHAT -> {
                    context.getString(R.string.stream_ui_send_also_as_direct_message)
                }
            }
            binding.sendAlsoToChannel.text = text
        }
        binding.sendAlsoToChannel.isVisible = shouldShowCheckbox
    }

    private fun configAttachmentButton() {
        binding.attachmentsButton.run {
            style.attachButtonIcon.let(this::setImageDrawable)
            setOnClickListener {
                context.getFragmentManager()?.let {
                    AttachmentDialogFragment.newInstance()
                        .apply { setAttachmentSelectionListener(attachmentSelectionListener) }
                        .show(it, AttachmentDialogFragment.TAG)
                }
            }
        }
    }

    private fun configLightningButton() {
        binding.commandsButton.run {
            style.commandsButtonIcon.let(this::setImageDrawable)
            setOnClickListener {
                suggestionListController?.let {
                    if (isSelected || it.isSuggestionListVisible()) {
                        it.hideSuggestionList()
                    } else {
                        isSelected = true
                        it.showAvailableCommands()
                    }
                }
            }
        }
    }

    private fun setSendMessageButtonEnabled(hasValidContent: Boolean) {
        val isSendButtonEnabled = hasValidContent && this.isSendButtonEnabled
        if (binding.sendMessageButtonEnabled.isEnabled == isSendButtonEnabled) return

        currentAnimatorSet?.cancel()

        val (fadeInView, fadeOutView) = if (isSendButtonEnabled) {
            binding.sendMessageButtonEnabled to binding.sendMessageButtonDisabled
        } else {
            binding.sendMessageButtonDisabled to binding.sendMessageButtonEnabled
        }
        currentAnimatorSet = AnimatorSet().apply {
            duration = 300
            playTogether(
                ObjectAnimator.ofFloat(fadeOutView, "alpha", fadeOutView.alpha, 0F),
                ObjectAnimator.ofFloat(fadeInView, "alpha", fadeInView.alpha, 1F)
            )
            start()
        }

        binding.sendMessageButtonEnabled.isEnabled = isSendButtonEnabled
    }

    private fun configTextInput() {
        binding.messageInputFieldView.setContentChangeListener(
            object : MessageInputFieldView.ContentChangeListener {
                override fun onMessageTextChanged(messageText: String) {
                    refreshControlsState()
                    handleKeyStroke()
                    messageInputDebouncer?.submitSuspendable { suggestionListController?.onNewMessageText(messageText) }
                }

                override fun onSelectedAttachmentsChanged(selectedAttachments: List<AttachmentMetaData>) {
                    refreshControlsState()
                }

                override fun onModeChanged(mode: MessageInputFieldView.Mode) {
                    refreshControlsState()
                }
            }
        )

        binding.messageInputFieldView.run {
            setTextColor(style.messageInputTextColor)
            setHintTextColor(style.messageInputHintTextColor)
            setTextSizePx(style.messageInputTextSize)
            setInputFieldScrollBarEnabled(style.messageInputScrollbarEnabled)
            setInputFieldScrollbarFadingEnabled(style.messageInputScrollbarFadingEnabled)
            setCustomBackgroundDrawable(style.editTextBackgroundDrawable)

            style.messageInputTextStyle.apply(binding.messageEditText)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                style.customCursorDrawable?.let(::setCustomCursor)
            }
        }

        binding.separator.background = style.dividerBackground
    }

    private fun handleKeyStroke() {
        if (binding.messageInputFieldView.messageText.isNotEmpty()) {
            typingListener?.onKeystroke()
        } else {
            typingListener?.onStopTyping()
        }
    }

    private fun configSendButton() {
        isSendButtonEnabled = style.sendButtonEnabled

        binding.sendMessageButtonDisabled.run {
            style.sendButtonDisabledIcon.let(this::setImageDrawable)
            alpha = 1F
            isEnabled = false
        }

        binding.sendMessageButtonEnabled.run {
            style.sendButtonEnabledIcon.let(this::setImageDrawable)
            alpha = 0F
            isEnabled = false
        }
    }

    private fun refreshControlsState() {
        with(binding) {
            val commandMode = messageInputFieldView.mode is MessageInputFieldView.Mode.CommandMode
            val hasContent = messageInputFieldView.hasContent()
            val messageLengthExceeded = messageInputFieldView.isMaxMessageLengthExceeded()
            val hasValidContent = hasContent && !messageLengthExceeded

            attachmentsButton.isVisible = style.attachButtonEnabled && !commandMode
            commandsButton.isVisible = shouldShowCommandsButton() && !commandMode
            commandsButton.isEnabled = !hasContent
            setSendMessageButtonEnabled(hasValidContent)
        }
    }

    private fun shouldShowCommandsButton(): Boolean {
        val hasCommands = suggestionListController?.commands?.isNotEmpty() ?: false
        return hasCommands && style.commandsButtonEnabled && commandsEnabled
    }

    private fun sendMessage(messageReplyTo: Message? = null) {
        if (binding.messageInputFieldView.hasValidAttachments()) {
            sendMessageHandler.sendMessageWithAttachments(
                binding.messageInputFieldView.messageText,
                binding.messageInputFieldView.getAttachedFiles(),
                messageReplyTo
            )
        } else {
            sendMessageHandler.sendMessage(binding.messageInputFieldView.messageText, messageReplyTo)
        }
    }

    private fun sendThreadMessage(parentMessage: Message) {
        val sendAlsoToChannel = binding.sendAlsoToChannel.isChecked
        if (binding.messageInputFieldView.hasValidAttachments()) {
            sendMessageHandler.sendToThreadWithAttachments(
                parentMessage,
                binding.messageInputFieldView.messageText,
                sendAlsoToChannel,
                binding.messageInputFieldView.getAttachedFiles()
            )
        } else {
            sendMessageHandler.sendToThread(
                parentMessage,
                binding.messageInputFieldView.messageText,
                sendAlsoToChannel
            )
        }
    }

    private fun editMessage(oldMessage: Message) {
        sendMessageHandler.editMessage(oldMessage, binding.messageInputFieldView.messageText)
        inputMode = InputMode.Normal
    }

    private companion object {
        private const val CLICK_DELAY = 100L
        private const val TYPING_DEBOUNCE_MS = 300L
        val EMPTY_MESSAGE_SEND_HANDLER = object : MessageSendHandler {
            override fun sendMessage(messageText: String, messageReplyTo: Message?) {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }

            override fun sendMessageWithAttachments(
                message: String,
                attachmentsFiles: List<File>,
                messageReplyTo: Message?,
            ) {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }

            override fun sendToThread(
                parentMessage: Message,
                messageText: String,
                alsoSendToChannel: Boolean,
            ) {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }

            override fun sendToThreadWithAttachments(
                parentMessage: Message,
                message: String,
                alsoSendToChannel: Boolean,
                attachmentsFiles: List<File>,
            ) {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }

            override fun editMessage(oldMessage: Message, newMessageText: String) {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }

            override fun dismissReply() {
                throw IllegalStateException("MessageInputView#messageSendHandler needs to be configured to send messages")
            }
        }
    }

    public sealed class InputMode {
        public object Normal : InputMode()
        public data class Thread(val parentMessage: Message) : InputMode()
        public data class Edit(val oldMessage: Message) : InputMode()
        public data class Reply(val repliedMessage: Message) : InputMode()
    }

    public enum class ChatMode {
        DIRECT_CHAT,
        GROUP_CHAT,
    }

    public interface MessageSendHandler {
        public fun sendMessage(
            messageText: String,
            messageReplyTo: Message? = null,
        )

        public fun sendMessageWithAttachments(
            message: String,
            attachmentsFiles: List<File>,
            messageReplyTo: Message? = null,
        )

        public fun sendToThread(
            parentMessage: Message,
            messageText: String,
            alsoSendToChannel: Boolean,
        )

        public fun sendToThreadWithAttachments(
            parentMessage: Message,
            message: String,
            alsoSendToChannel: Boolean,
            attachmentsFiles: List<File>,
        )

        public fun editMessage(oldMessage: Message, newMessageText: String)
        public fun dismissReply()
    }

    public fun interface OnMessageSendButtonClickListener {
        public fun onClick()
    }

    public interface TypingListener {
        public fun onKeystroke()
        public fun onStopTyping()
    }

    @FunctionalInterface
    public interface BigFileSelectionListener {
        public fun handleBigFileSelected(hasBigFile: Boolean)
    }

    /**
     * Users lookup functional interface. Used to create custom users lookup algorithm.
     */
    public interface UserLookupHandler {
        /**
         * Performs users lookup by given [query] in suspend way. It's executed on background, so it can perform heavy operations.
         *
         * @param query String as user input for lookup algorithm.
         * @return List of users as result of lookup.
         */
        public suspend fun handleUserLookup(query: String): List<User>
    }

    public class DefaultUserLookupHandler(public var users: List<User>) : UserLookupHandler {
        override suspend fun handleUserLookup(query: String): List<User> {
            return users.filter { it.name.contains(query, true) }
        }
    }
}
