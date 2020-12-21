import kotlinx.browser.document
import org.w3c.dom.Comment
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.get

private var fragmentCounter = 0
private const val fragmentStart = "-start"
private const val fragmentEnd = "-end"
private const val fragmentItem = "-item"

fun <T> Attribute<T>.bindTo(state: State<T>) {
    val listener = state.listen { current = it }

    tag.onUnmount {
        state.removeListener(listener)
    }
}

fun <T> NodeCursor<*>.boundClass(state: State<T>, classGenerator: (T) -> Array<String>) {
    val element = (node as? Element) ?: error("Cannot bind a class to something which is not an element")
    var addedByUs = emptyArray<String>()
    state.listen { newState ->
        element.classList.remove(*addedByUs)
        addedByUs = classGenerator(newState)
        element.classList.add(*addedByUs)
    }
}

fun <T, R : Node> NodeCursor<R>.boundElement(state: State<T>, nodeCreator: NodeCursor<R>.(T) -> Unit) {
    val parent = node
    val fragmentId = "dtfragment-${fragmentCounter++}"
    add(document.createComment(fragmentId + fragmentStart))

    var initialCreation = true
    state.listen {
        if (initialCreation) {
            initialCreation = false
            nodeCreator(it)
        } else {
            // Clean up between the two comments
            var startIdx = -1
            var i = 0
            while (i < parent.childNodes.length) {
                val child = parent.childNodes[i++] ?: continue
                if (startIdx == -1) {
                    if (child is Comment) {
                        if (child.data == fragmentId + fragmentStart) {
                            startIdx = i
                        }
                    }
                } else {
                    if (child is Comment && child.data == fragmentId + fragmentEnd) {
                        break
                    } else {
                        parent.removeChild(child)
                        i--
                    }
                }
            }

            NodeCursor(node, startIdx).nodeCreator(it)
        }
    }

    add(document.createComment(fragmentId + fragmentEnd))
}

fun <T, R : Node> NodeCursor<R>.boundElement(state: StateList<T>, nodeCreator: NodeCursor<R>.(T) -> Unit) {
    val parent = node
    val fragmentId = "dtfragment-${fragmentCounter++}"
    add(document.createComment(fragmentId + fragmentStart))
    add(document.createComment(fragmentId + fragmentEnd))

    state.addListener(object : StateListListener<T> {
        override fun onAdd(item: T, idx: Int) {
            var itemIdx = 0
            var startIdx = -1
            var childIdx = 0
            while (childIdx < parent.childNodes.length) {
                val child = parent.childNodes[childIdx++] ?: continue
                if (startIdx == -1) {
                    if (child is Comment) {
                        if (child.data == fragmentId + fragmentStart) {
                            startIdx = childIdx
                        }
                    }
                } else {
                    if (child !is Comment) continue

                    var eof = false
                    if (child.data == fragmentId + fragmentEnd) {
                        eof = true
                    } else if (child.data == fragmentId + fragmentItem) {
                        itemIdx++
                    }

                    if (itemIdx - 1 == idx || eof) {
                        val nodeCursor = NodeCursor(node, childIdx - 1)
                        nodeCursor.add(document.createComment(fragmentId + fragmentItem))
                        nodeCursor.nodeCreator(item)
                        break
                    }
                }
            }
        }

        override fun onRemove(item: T, idx: Int) {
            var itemIdx = 0
            var startIdx = -1
            var childIdx = 0
            var startIdxOfChild = -1
            while (childIdx < parent.childNodes.length) {
                val child = parent.childNodes[childIdx++] ?: continue
                if (startIdxOfChild != -1) {
                    if (child is Comment) {
                        if (child.data == fragmentId + fragmentEnd) break
                        if (child.data == fragmentId + fragmentItem) break
                    }

                    parent.removeChild(child)
                    childIdx--
                } else if (startIdx == -1) {
                    if (child is Comment) {
                        if (child.data == fragmentId + fragmentStart) {
                            startIdx = childIdx
                        }
                    }
                } else {
                    if (child !is Comment) continue

                    if (child.data == fragmentId + fragmentEnd) {
                        break
                    } else if (child.data == fragmentId + fragmentItem) {
                        if (itemIdx == idx) {
                            parent.removeChild(child)
                            childIdx--
                            startIdxOfChild = childIdx
                        }
                        itemIdx++
                    }
                }
            }
        }

        override fun onClear() {
            var startIdx = -1
            var i = 0
            while (i < parent.childNodes.length) {
                val child = parent.childNodes[i++] ?: continue
                if (startIdx == -1) {
                    if (child is Comment) {
                        if (child.data == fragmentId + fragmentStart) {
                            console.log("Found the start", child, i)
                            startIdx = i
                        }
                    }
                } else {
                    if (child is Comment && child.data == fragmentId + fragmentEnd) {
                        break
                    } else {
                        parent.removeChild(child)
                        i--
                    }
                }
            }
        }
    })
}
