package com.example

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

class ObservableWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    var onScrollChangedCallback: ((Int, Int, Int, Int) -> Unit)? = null
    var isImeVisible: Boolean = false

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollChangedCallback?.invoke(l, t, oldl, oldt)
    }
}
