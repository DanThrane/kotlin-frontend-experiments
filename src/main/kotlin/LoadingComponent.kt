import org.w3c.dom.Element
import kotlin.browser.window
import kotlin.js.Promise

class LoadingState {
    val loading = BoundData(false)
    val error = BoundData<String?>(null)
}

fun Element.loading(state: LoadingState = LoadingState(), children: Element.() -> Unit): LoadingState {
    cardStack(
        CardInStack(
            predicate = { state.error.currentValue != null },
            card = { text("An error! ${state.error.currentValue}") }
        ),

        CardInStack(
            predicate = { state.loading.currentValue },
            card = { loadingIcon() }
        ),

        CardInStack(
            predicate = { true },
            card = children
        ),

        dependencies = listOf(state.loading, state.error)
    )

    return state
}

interface RemoteDataComponent<T> {
    fun fetchData(workFactory: () -> Promise<T>)
}

fun <T : Any> Element.remoteDataWithLoading(children: Element.(data: T) -> Unit): RemoteDataComponent<T> {
    val state = LoadingState()
    var lastLoadedData: T? = null
    val component = object : RemoteDataComponent<T> {
        override fun fetchData(workFactory: () -> Promise<T>) {
            state.loading.currentValue = true
            workFactory()
                .then { data ->
                    lastLoadedData = data
                }
                .catch { ex ->
                    state.error.currentValue = ex.message
                }
                .then {
                    state.loading.currentValue = false
                }
        }
    }

    loading(state) {
        val capturedData = lastLoadedData
        if (capturedData != null) {
            children(capturedData)
        }
    }

    return component
}

private val loadingIconStyle = css {
    (byTag("h2")) {
        fontSize = 30.pt
    }
}

fun Element.loadingIcon() {
    h2(A(klass = loadingIconStyle)) {
        val numberOfDots = BoundData(0)
        val interval = window.setInterval({
            numberOfDots.currentValue = (numberOfDots.currentValue + 1) % 5
        }, 250)
        onDeinit { window.clearInterval(interval) }

        boundText(numberOfDots) { String(CharArray(it + 1) { '.' }) }
    }
}