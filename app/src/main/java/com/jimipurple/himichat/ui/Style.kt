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
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.jimipurple.himichat.R


/**
 * Base class for chat component styles
 */
abstract class Style protected constructor(context: Context, attrs: AttributeSet) {
    protected var context: Context
    protected var resources: Resources
    protected var attrs: AttributeSet
    val systemAccentColor: Int
        get() = getSystemColor(R.attr.colorAccent)

   val systemPrimaryColor: Int
        get() = getSystemColor(R.attr.colorPrimary)

    val systemPrimaryDarkColor: Int
        get() = getSystemColor(R.attr.colorPrimaryDark)

    val systemPrimaryTextColor: Int
        get() = getSystemColor(android.R.attr.textColorPrimary)

    protected val systemHintColor: Int
        get() = getSystemColor(android.R.attr.textColorHint)

    fun getSystemColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        val a: TypedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(attr))
        val color = a.getColor(0, 0)
        a.recycle()
        return color
    }

    fun getDimension(@DimenRes dimen: Int): Int {
        return resources.getDimensionPixelSize(dimen)
    }

    fun getColor(@ColorRes color: Int): Int {
        return ContextCompat.getColor(context, color)
    }

    fun getDrawable(@DrawableRes drawable: Int): Drawable? {
        return ContextCompat.getDrawable(context, drawable)
    }

    fun getVectorDrawable(@DrawableRes drawable: Int): Drawable? {
        return ContextCompat.getDrawable(context, drawable)
    }

    init {
        this.context = context
        resources = context.getResources()
        this.attrs = attrs
    }
}