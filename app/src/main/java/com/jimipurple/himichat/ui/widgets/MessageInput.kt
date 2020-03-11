package com.jimipurple.himichat.ui.widgets


/*******************************************************************************
 * Copyright 2016 stfalcon.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.*
import androidx.core.view.ViewCompat
import com.jimipurple.himichat.R
import com.jimipurple.himichat.ui.MessageInputStyle


/**
 * Component for input outcoming messages
 */
class MessageInput : RelativeLayout, View.OnClickListener, TextWatcher,
    OnFocusChangeListener {
    /**
     * Returns EditText for messages input
     *
     * @return EditText
     */
    var inputEditText: EditText? = null
        protected set

    /**
     * Returns `submit` button
     *
     * @return ImageButton
     */
    var button: ImageButton? = null
        protected set
    protected var attachmentButton: ImageButton? = null
    protected var sendButtonSpace: Space? = null
    protected var attachmentButtonSpace: Space? = null
    private var input: CharSequence? = null
    private var inputListener: InputListener? = null
    private var attachmentsListener: AttachmentsListener? = null
    private var isTyping = false
    private var typingListener: TypingListener? = null
    private var delayTypingStatusMillis = 0
    private val typingTimerRunnable = Runnable {
        if (isTyping) {
            isTyping = false
            if (typingListener != null) typingListener!!.onStopTyping()
        }
    }
    private var lastFocus = false

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(
        context,
        attrs
    ) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    /**
     * Sets callback for 'submit' button.
     *
     * @param inputListener input callback
     */
    fun setInputListener(inputListener: InputListener?) {
        this.inputListener = inputListener
    }

    /**
     * Sets callback for 'add' button.
     *
     * @param attachmentsListener input callback
     */
    fun setAttachmentsListener(attachmentsListener: AttachmentsListener?) {
        this.attachmentsListener = attachmentsListener
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.messageSendButton) {
            val isSubmitted = onSubmit()
            if (isSubmitted) {
                inputEditText!!.setText("")
            }
            removeCallbacks(typingTimerRunnable)
            post(typingTimerRunnable)
        } else if (id == R.id.attachmentButton) {
            onAddAttachments()
        }
    }

    /**
     * This method is called to notify you that, within s,
     * the count characters beginning at start have just replaced old text that had length before
     */
    override fun onTextChanged(
        s: CharSequence,
        start: Int,
        count: Int,
        after: Int
    ) {
        input = s
        button!!.isEnabled = input!!.length > 0
        if (s.length > 0) {
            if (!isTyping) {
                isTyping = true
                if (typingListener != null) typingListener!!.onStartTyping()
            }
            removeCallbacks(typingTimerRunnable)
            postDelayed(typingTimerRunnable, delayTypingStatusMillis.toLong())
        }
    }

    /**
     * This method is called to notify you that, within s,
     * the count characters beginning at start are about to be replaced by new text with length after.
     */
    override fun beforeTextChanged(
        charSequence: CharSequence,
        i: Int,
        i1: Int,
        i2: Int
    ) {
        //do nothing
    }

    /**
     * This method is called to notify you that, somewhere within s, the text has been changed.
     */
    override fun afterTextChanged(editable: Editable) {
        //do nothing
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (lastFocus && !hasFocus && typingListener != null) {
            typingListener!!.onStopTyping()
        }
        lastFocus = hasFocus
    }

    private fun onSubmit(): Boolean {
        return inputListener != null && inputListener!!.onSubmit(input)
    }

    private fun onAddAttachments() {
        if (attachmentsListener != null) attachmentsListener!!.onAddAttachments()
    }

    private fun init(
        context: Context,
        attrs: AttributeSet
    ) {
        init(context)
        val style: MessageInputStyle = MessageInputStyle.parse(context, attrs)
        inputEditText!!.maxLines = style.inputMaxLines
        inputEditText!!.hint = style.inputHint
        inputEditText!!.setText(style.inputText)
        inputEditText!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, style.inputTextSize.toFloat())
        inputEditText!!.setTextColor(style.inputTextColor)
        inputEditText!!.setHintTextColor(style.inputHintColor)
        ViewCompat.setBackground(inputEditText!!, style.inputBackground)
        setCursor(style.inputCursorDrawable)
        attachmentButton!!.visibility =
            if (style.showAttachmentButton()) View.VISIBLE else View.GONE
        attachmentButton!!.setImageDrawable(style.getAttachmentButtonIcon())
        attachmentButton!!.layoutParams.width = style.attachmentButtonWidth
        attachmentButton!!.layoutParams.height = style.attachmentButtonHeight
        ViewCompat.setBackground(attachmentButton!!, style.getAttachmentButtonBackground())
        attachmentButtonSpace!!.setVisibility(if (style.showAttachmentButton()) View.VISIBLE else View.GONE)
        attachmentButtonSpace!!.getLayoutParams().width = style.attachmentButtonMargin
        button!!.setImageDrawable(style.getInputButtonIcon())
        button!!.layoutParams.width = style.inputButtonWidth
        button!!.layoutParams.height = style.inputButtonHeight
        ViewCompat.setBackground(button!!, style.getInputButtonBackground())
        sendButtonSpace!!.layoutParams.width = style.inputButtonMargin
        if (paddingLeft == 0 && paddingRight == 0 && paddingTop == 0 && paddingBottom == 0
        ) {
            setPadding(
                style.inputDefaultPaddingLeft,
                style.inputDefaultPaddingTop,
                style.inputDefaultPaddingRight,
                style.inputDefaultPaddingBottom
            )
        }
        delayTypingStatusMillis = style.delayTypingStatus
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.view_message_input, this)
        inputEditText = findViewById<View>(R.id.messageInput) as EditText
        button = findViewById<View>(R.id.messageSendButton) as ImageButton
        attachmentButton = findViewById<View>(R.id.attachmentButton) as ImageButton
        sendButtonSpace = findViewById<View>(R.id.sendButtonSpace) as Space
        attachmentButtonSpace =
            findViewById<View>(R.id.attachmentButtonSpace) as Space
        button!!.setOnClickListener(this)
        attachmentButton!!.setOnClickListener(this)
        inputEditText!!.addTextChangedListener(this)
        inputEditText!!.setText("")
        inputEditText!!.onFocusChangeListener = this
    }

    private fun setCursor(drawable: Drawable?) {
        if (drawable == null) return
        try {
            val drawableResField =
                TextView::class.java.getDeclaredField("mCursorDrawableRes")
            drawableResField.isAccessible = true
            val drawableFieldOwner: Any?
            val drawableFieldClass: Class<*>
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                drawableFieldOwner = inputEditText
                drawableFieldClass = TextView::class.java
            } else {
                val editorField =
                    TextView::class.java.getDeclaredField("mEditor")
                editorField.isAccessible = true
                drawableFieldOwner = editorField[inputEditText]
                drawableFieldClass = drawableFieldOwner.javaClass
            }
            val drawableField =
                drawableFieldClass.getDeclaredField("mCursorDrawable")
            drawableField.isAccessible = true
            drawableField[drawableFieldOwner] = arrayOf(drawable, drawable)
        } catch (ignored: Exception) {
        }
    }

    fun setTypingListener(typingListener: TypingListener?) {
        this.typingListener = typingListener
    }

    /**
     * Interface definition for a callback to be invoked when user pressed 'submit' button
     */
    interface InputListener {
        /**
         * Fires when user presses 'send' button.
         *
         * @param input input entered by user
         * @return if input text is valid, you must return `true` and input will be cleared, otherwise return false.
         */
        fun onSubmit(input: CharSequence?): Boolean
    }

    /**
     * Interface definition for a callback to be invoked when user presses 'add' button
     */
    interface AttachmentsListener {
        /**
         * Fires when user presses 'add' button.
         */
        fun onAddAttachments()
    }

    /**
     * Interface definition for a callback to be invoked when user typing
     */
    interface TypingListener {
        /**
         * Fires when user presses start typing
         */
        fun onStartTyping()

        /**
         * Fires when user presses stop typing
         */
        fun onStopTyping()
    }
}
