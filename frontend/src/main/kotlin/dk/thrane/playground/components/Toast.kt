package dk.thrane.playground.components

import dk.thrane.playground.*
import org.w3c.dom.Element
import kotlin.browser.window
import kotlin.js.Date

typealias ToastSubscriber = (nextToast: Toast) -> Unit

enum class ToastType {
    INFO
}

data class Toast(
    val type: ToastType,
    val message: String,
    val duration: Long
)

object Toasts {
    private val subscribers = ArrayList<ToastSubscriber>()

    fun subscribe(subscriber: ToastSubscriber): ToastSubscriber {
        subscribers.add(subscriber)
        return subscriber
    }

    fun unsubscribe(subscriber: ToastSubscriber) {
        subscribers.remove(subscriber)
    }

    fun push(toast: Toast) {
        subscribers.forEach { it(toast) }
    }
}

private const val TOAST_ACTIVE = "active"

private val toastStyle = css {
    position = "fixed"
    bottom = (-80).px
    left = 30.px
    minWidth = 300.px
    maxWidth = 500.px
    height = 60.px
    backgroundColor = "black"
    color = "white"
    margin = 10.px
    transition = "opacity 0.5s ease-in, bottom 0.5s ease-in"
    opacity = "0"

    (matchSelf() and byClass(TOAST_ACTIVE)) {
        bottom = 30.px
        opacity = "1"
    }
}

fun Element.toasts() {
    val activeToast = BoundData<Toast?>(null)
    var nextDeadline = 0.0
    val queue = ArrayList<Toast>()

    val subscription = Toasts.subscribe { queue.add(it) }
    val interval = window.setInterval({
        val now = Date.now()
        if (now >= nextDeadline) {
            if (queue.isNotEmpty()) {
                val nextToast = queue.removeAt(0)
                activeToast.currentValue = nextToast
                nextDeadline = nextToast.duration + now
            } else if (activeToast.currentValue != null) {
                activeToast.currentValue = null
            }
        }
    }, 100)

    flex(
        A(
            classes = setOf(
                AlignItems.center,
                JustifyContent.center,
                toastStyle
            )
        )
    ) {
        boundClass(activeToast) { if (it == null) emptySet() else setOf(TOAST_ACTIVE) }

        onDeinit { Toasts.unsubscribe(subscription) }
        onDeinit { window.clearInterval(interval) }

        boundText(activeToast) { toast ->
            toast?.message ?: ""
        }
    }
}
