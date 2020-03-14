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

package com.jimipurple.himichat.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.DrawableCompat
import com.jimipurple.himichat.R


/**
 * Style for MessageInputStyle customization by xml attributes
 */
internal class MessageInputStyle private constructor(
    context: Context,
    attrs: AttributeSet
) :
    Style(context, attrs) {
    var showAttachmentButton = false
    var attachmentButtonBackground = 0
    var attachmentButtonDefaultBgColor = 0
    var attachmentButtonDefaultBgPressedColor = 0
    var attachmentButtonDefaultBgDisabledColor = 0
    var attachmentButtonIcon = 0
    var attachmentButtonDefaultIconColor = 0
    var attachmentButtonDefaultIconPressedColor = 0
    var attachmentButtonDefaultIconDisabledColor = 0
    var attachmentButtonWidth = 0
        private set
    var attachmentButtonHeight = 0
        private set
    var attachmentButtonMargin = 0
        private set
    private var inputButtonBackground = 0
    var inputButtonDefaultBgColor = 0
    var inputButtonDefaultBgPressedColor = 0
    var inputButtonDefaultBgDisabledColor = 0
    var inputButtonIcon = 0
    var inputButtonDefaultIconColor = 0
    var inputButtonDefaultIconPressedColor = 0
    var inputButtonDefaultIconDisabledColor = 0
    var inputButtonWidth = 0
        private set
    var inputButtonHeight = 0
        private set
    var inputButtonMargin = 0
        private set
    var inputMaxLines = 0
        private set
    var inputHint: String? = null
        private set
    var inputText: String? = null
        private set
    var inputTextSize = 0
        private set
    var inputTextColor = 0
        private set
    var inputHintColor = 0
        private set
    var inputBackground: Drawable? = null
        private set
    var inputCursorDrawable: Drawable? = null
        private set
    var inputDefaultPaddingLeft = 0
        private set
    var inputDefaultPaddingRight = 0
        private set
    var inputDefaultPaddingTop = 0
        private set
    var inputDefaultPaddingBottom = 0
        private set
    var delayTypingStatus = 0
        private set

    private fun getSelector(
        @ColorInt normalColor: Int, @ColorInt pressedColor: Int,
        @ColorInt disabledColor: Int, @DrawableRes shape: Int
    ): Drawable {
        val drawable = DrawableCompat.wrap(getVectorDrawable(shape)!!).mutate()
        DrawableCompat.setTintList(
            drawable,
            ColorStateList(
                arrayOf(
                    intArrayOf(
                        android.R.attr.state_enabled,
                        -android.R.attr.state_pressed
                    ),
                    intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed),
                    intArrayOf(-android.R.attr.state_enabled)
                ), intArrayOf(normalColor, pressedColor, disabledColor)
            )
        )
        return drawable
    }

    fun showAttachmentButton(): Boolean {
        return showAttachmentButton
    }

    fun getAttachmentButtonBackground(): Drawable? {
        return if (attachmentButtonBackground == -1) {
            getSelector(
                attachmentButtonDefaultBgColor, attachmentButtonDefaultBgPressedColor,
                attachmentButtonDefaultBgDisabledColor, R.drawable.mask
            )
        } else {
            getDrawable(attachmentButtonBackground)
        }
    }

    fun getAttachmentButtonIcon(): Drawable? {
        return if (attachmentButtonIcon == -1) {
            getSelector(
                attachmentButtonDefaultIconColor, attachmentButtonDefaultIconPressedColor,
                attachmentButtonDefaultIconDisabledColor, R.drawable.ic_add_attachment
            )
        } else {
            getDrawable(attachmentButtonIcon)
        }
    }

    fun getInputButtonBackground(): Drawable? {
        return if (inputButtonBackground == -1) {
            getSelector(
                inputButtonDefaultBgColor, inputButtonDefaultBgPressedColor,
                inputButtonDefaultBgDisabledColor, R.drawable.mask
            )
        } else {
            getDrawable(inputButtonBackground)
        }
    }

    fun getInputButtonIcon(): Drawable? {
        return if (inputButtonIcon == -1) {
            getSelector(
                inputButtonDefaultIconColor, inputButtonDefaultIconPressedColor,
                inputButtonDefaultIconDisabledColor, R.drawable.ic_send
            )
        } else {
            getDrawable(inputButtonIcon)
        }
    }

    companion object {
        const val DEFAULT_MAX_LINES = 5
        const val DEFAULT_DELAY_TYPING_STATUS = 1500
        fun parse(
            context: Context,
            attrs: AttributeSet
        ): MessageInputStyle {
            val style = MessageInputStyle(context, attrs)
            val typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.MessageInput)
            style.showAttachmentButton =
                typedArray.getBoolean(R.styleable.MessageInput_showAttachmentButton, false)
            style.attachmentButtonBackground =
                typedArray.getResourceId(R.styleable.MessageInput_attachmentButtonBackground, -1)
            style.attachmentButtonDefaultBgColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultBgColor,
                style.getColor(R.color.white_four)
            )
            style.attachmentButtonDefaultBgPressedColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultBgPressedColor,
                style.getColor(R.color.white_five)
            )
            style.attachmentButtonDefaultBgDisabledColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultBgDisabledColor,
                style.getColor(R.color.transparent)
            )
            style.attachmentButtonIcon =
                typedArray.getResourceId(R.styleable.MessageInput_attachmentButtonIcon, -1)
            style.attachmentButtonDefaultIconColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultIconColor,
                style.getColor(R.color.cornflower_blue_two)
            )
            style.attachmentButtonDefaultIconPressedColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultIconPressedColor,
                style.getColor(R.color.cornflower_blue_two_dark)
            )
            style.attachmentButtonDefaultIconDisabledColor = typedArray.getColor(
                R.styleable.MessageInput_attachmentButtonDefaultIconDisabledColor,
                style.getColor(R.color.cornflower_blue_light_40)
            )
            style.attachmentButtonWidth = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_attachmentButtonWidth,
                style.getDimension(R.dimen.input_button_width)
            )
            style.attachmentButtonHeight = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_attachmentButtonHeight,
                style.getDimension(R.dimen.input_button_height)
            )
            style.attachmentButtonMargin = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_attachmentButtonMargin,
                style.getDimension(R.dimen.input_button_margin)
            )
            style.inputButtonBackground =
                typedArray.getResourceId(R.styleable.MessageInput_inputButtonBackground, -1)
            style.inputButtonDefaultBgColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultBgColor,
                style.getColor(R.color.cornflower_blue_two)
            )
            style.inputButtonDefaultBgPressedColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultBgPressedColor,
                style.getColor(R.color.cornflower_blue_two_dark)
            )
            style.inputButtonDefaultBgDisabledColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultBgDisabledColor,
                style.getColor(R.color.white_four)
            )
            style.inputButtonIcon =
                typedArray.getResourceId(R.styleable.MessageInput_inputButtonIcon, -1)
            style.inputButtonDefaultIconColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultIconColor,
                style.getColor(R.color.white)
            )
            style.inputButtonDefaultIconPressedColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultIconPressedColor,
                style.getColor(R.color.white)
            )
            style.inputButtonDefaultIconDisabledColor = typedArray.getColor(
                R.styleable.MessageInput_inputButtonDefaultIconDisabledColor,
                style.getColor(R.color.warm_grey)
            )
            style.inputButtonWidth = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_inputButtonWidth,
                style.getDimension(R.dimen.input_button_width)
            )
            style.inputButtonHeight = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_inputButtonHeight,
                style.getDimension(R.dimen.input_button_height)
            )
            style.inputButtonMargin = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_inputButtonMargin,
                style.getDimension(R.dimen.input_button_margin)
            )
            style.inputMaxLines = typedArray.getInt(
                R.styleable.MessageInput_inputMaxLines,
                DEFAULT_MAX_LINES
            )
            style.inputHint = typedArray.getString(R.styleable.MessageInput_inputHint)
            style.inputText = typedArray.getString(R.styleable.MessageInput_inputText)
            style.inputTextSize = typedArray.getDimensionPixelSize(
                R.styleable.MessageInput_inputTextSize,
                style.getDimension(R.dimen.input_text_size)
            )
            style.inputTextColor = typedArray.getColor(
                R.styleable.MessageInput_inputTextColor,
                style.getColor(R.color.dark_grey_two)
            )
            style.inputHintColor = typedArray.getColor(
                R.styleable.MessageInput_inputHintColor,
                style.getColor(R.color.warm_grey_three)
            )
            style.inputBackground = typedArray.getDrawable(R.styleable.MessageInput_inputBackground)
            style.inputCursorDrawable =
                typedArray.getDrawable(R.styleable.MessageInput_inputCursorDrawable)
            style.delayTypingStatus = typedArray.getInt(
                R.styleable.MessageInput_delayTypingStatus,
                DEFAULT_DELAY_TYPING_STATUS
            )
            typedArray.recycle()
            style.inputDefaultPaddingLeft = style.getDimension(R.dimen.input_padding_left)
            style.inputDefaultPaddingRight = style.getDimension(R.dimen.input_padding_right)
            style.inputDefaultPaddingTop = style.getDimension(R.dimen.input_padding_top)
            style.inputDefaultPaddingBottom = style.getDimension(R.dimen.input_padding_bottom)
            return style
        }
    }
}