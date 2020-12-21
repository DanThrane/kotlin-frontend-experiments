import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.math.absoluteValue

fun main() {
    document.addEventListener("DOMContentLoaded", {
        try {
            CustomTag.define(App())

            document.head!!.toCursor().apply {
                css {
                    root {
                        setVariable(Theme.primary, "#3860B8")
                        setVariable(Theme.onPrimary, "#FFFFFF")
                        setVariable(Theme.surface, "white")
                        setVariable(Theme.onSurface, "black")
                        setVariable(Theme.hoverOnSurface, "rgba(0, 0, 0, 0.1)")
                        userSelect = "none"
                        fontFamily = "Roboto"
                        fontSize = 11.pt
                    }

                    (byTag("body")) {
                        margin = 0.px
                    }
                }

                title {
                    boundElement(applicationTitle) {
                        text(applicationTitle.current)
                    }
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    })
}

val applicationTitle = stateOf("Title")

object Theme {
    val surface = CSSVar("surface")
    val onSurface = CSSVar("on-surface")
    val primary = CSSVar("primary")
    val onPrimary = CSSVar("on-primary")

    val hoverOnSurface = CSSVar("hover-on-surface")
}

class Card : TemplatedTag(tag, template) {
    companion object {
        const val tag = "dt-card"

        init {
            define(Card())
        }

        private val template = createTemplate {
            css {
                host {
                    backgroundColor = variable(Theme.surface)
                    padding = 16.px
                    color = variable(Theme.onSurface)
                    display = "block"
                    boxShadow = boxShadow(0, 1, 2, 2, "rgba(0, 0, 0, 0.2)")
                }
            }

            div {
                slot()
            }
        }
    }
}

inline fun NodeCursor<*>.card(
    attrs: CommonAttributes<HTMLElement> = CommonAttributes(),
    children: (NodeCursor<HTMLElement>.() -> Unit) = {}
) {
    baseElement(Card.tag, attrs, children)
}

class App : CustomTag("dt-app") {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                display = "flex"
                flexDirection = "row"
            }
        }

        sidebar()
        fileBrowser()
    }
}

class Sidebar : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                height = 100.vh
                width = 300.px
                overflowY = "auto"
                backgroundColor = variable(Theme.primary)
                color = variable(Theme.onPrimary)
                padding = 16.px
                display = "flex"
                flexDirection = "column"
                boxSizing = "border-box"
            }
        }

        h1 { text("Tabs") }

        repeat(5) {
            sidebarTab {
                text("Fie")
            }
        }
    }

    companion object {
        const val tag = "dt-sidebar"

        init {
            define(Sidebar())
        }
    }
}

inline fun NodeCursor<*>.sidebar(
    attrs: CommonAttributes<CustomTagWrapper<Sidebar>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<Sidebar>>.() -> Unit) = {}
) {
    baseElement(Sidebar.tag, attrs, children)
}

class SidebarTab : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                padding = 8.px
                backgroundColor = "white"
                color = "black"
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-sidebar-tab"

        init {
            define(SidebarTab())
        }
    }
}

inline fun NodeCursor<*>.sidebarTab(
    attrs: CommonAttributes<CustomTagWrapper<SidebarTab>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<SidebarTab>>.() -> Unit) = {}
) {
    baseElement(SidebarTab.tag, attrs, children)
}

class FileBrowser : CustomTag(tag) {
    val path = stateOf("/Home/Kotlin")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        val contentRef = Reference<HTMLDivElement>(null)
        val showShadow = stateOf(false)

        css {
            host {
                display = "flex"
                flexDirection = "column"
                width = 100.percent
                height = 100.vh
                boxSizing = "border-box"
            }

            (byClass("content")) {
                height = "calc(100% - 117px)"
                overflowY = "auto"
                flexShrink = "0"
            }
        }

        boundElement(path) {
            fileCrumbs {
                inst.shadow.bindTo(showShadow)
                inst.path.bindTo(path)
            }
        }

        div(A(klass = "content", ref = contentRef)) {
            fileContainer { }
            on("scroll") {
                showShadow.current = contentRef.current.scrollTop != 0.0
            }
        }
    }

    companion object {
        const val tag = "dt-file-browser"

        init {
            define(FileBrowser())
        }
    }
}

inline fun NodeCursor<*>.fileBrowser(
    attrs: CommonAttributes<CustomTagWrapper<FileBrowser>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<FileBrowser>>.() -> Unit) = {}
) {
    baseElement(FileBrowser.tag, attrs, children)
}

class FileCrumbs : CustomTag(tag) {
    val path = attr<String>("path")
    val shadow = attr<Boolean>("shadow")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)
        val pathState = path.asState()

        css {
            host {
                width = 100.percent
                zIndex = "1000"
            }

            (host.attributeEquals(shadow.name, "true")) {
                boxShadow = boxShadow(0, 1, 5, 0, "rgba(0, 0, 0, 0.2)")
            }

            (byClass("wrapper")) {
                paddingLeft = 16.px
                paddingRight = 16.px
                paddingTop = 32.px
                paddingBottom = 32.px
                width = 100.percent
                boxSizing = "border-box"
            }

            (byTag("ul")) {
                listStyle = "none"
                padding = 0.px
                display = "flex"
                flexDirection = "row"
                margin = 0.px
            }

            (byTag("li")) {
                marginRight = 8.px
            }

            (byTag("a")) {
                textDecoration = "none"
                color = "unset"
            }

            (byTag("h1")) {
                margin = 0.px
            }
        }

        div(A(klass = "wrapper")) {
            boundElement(pathState) {
                val path = pathState.current ?: "/"
                val components = path.split("/").filter { it.isNotEmpty() }

                ul {
                    components.dropLast(1).forEach { component ->
                        li {
                            a(href = "#$component") {
                                text(component)
                            }
                        }
                        li { text("/") }
                    }
                }

                h1 { text(components.lastOrNull() ?: "/") }
            }
        }
    }

    companion object {
        const val tag = "dt-file-crumbs"

        init {
            define(FileCrumbs())
        }
    }
}

inline fun NodeCursor<*>.fileCrumbs(
    attrs: CommonAttributes<CustomTagWrapper<FileCrumbs>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<FileCrumbs>>.() -> Unit) = {}
) {
    baseElement(FileCrumbs.tag, attrs, children)
}

class FileContainer : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        val selectionRef = Reference<HTMLDivElement>()
        val containerRef = Reference<HTMLDivElement>()
        val movingRef = Reference<HTMLDivElement>()
        val menuRef = Reference<CustomTagWrapper<ContextMenu>>()
        val containerClass = "container"
        val gridClass = "grid"
        val selectionClass = "selection"
        val movingClass = "moving"
        val menuClass = "menu"
        val menuOpen = stateOf(false)

        css {
            (byClass(containerClass)) {
                width = 100.percent
                height = 100.percent
            }

                (byClass((gridClass))) {
                display = "flex"
                flexWrap = "wrap"
                flexDirection = "row"
            }

            (byClass(gridClass).directChild(matchAny())) {
                margin = 8.px
            }

            (byClass(selectionClass)) {
                backgroundColor = variable(Theme.primary)
                opacity = "0.3"
                position = "fixed"
                display = "none"
            }

            (byClass(movingClass)) {
                opacity = "0.3"
                position = "fixed"
                display = "none"
            }

            (byClass(menuClass)) {
                position = "fixed"
            }
        }

        div(A(klass = containerClass)) {
            var dragStartX = 0
            var dragStartY = 0

            var isDragging = false
            var isMoving = false

            document.body!!.on("contextmenu") { ev ->
                ev.preventDefault()
                ev.stopPropagation()
                menuOpen.current = true
            }.also { onUnmount { document.body!!.removeEventListener("contextmenu", it) } }

            on("mousedown") { ev ->
                ev as MouseEvent
                val x = ev.clientX
                val y = ev.clientY
                dragStartX = x
                dragStartY = y

                when (ev.button.toInt()) {
                    0 -> {
                        menuOpen.current = false

                        // Check if our mouse click lies within the bounds of the old selection. In that case we are
                        // attempting to drag around our existing items.
                        val elems = containerRef.current
                            .findAll<HTMLElement>("[selected=\"true\"]")
                            .map { it.getBoundingClientRect() }
                        val dragY = elems.minByOrNull { it.top }?.top?.toInt() ?: 0
                        val dragX = elems.minByOrNull { it.left }?.left?.toInt() ?: 0
                        val dragHeight = (elems.map { it.top + it.height }.maxOrNull()?.toInt() ?: 0) - dragY
                        val dragWidth = (elems.map { it.left + it.width }.maxOrNull()?.toInt() ?: 0) - dragX
                        if (x >= dragX && x <= dragX + dragWidth && y >= dragY && y <= dragY + dragHeight) {
                            isMoving = true
                        } else {
                            val children = containerRef.current.children
                            for (i in 0 until children.length) {
                                val child = children[i] ?: continue
                                child.attr("selected", "false")
                            }

                            isDragging = true
                            selectionRef.current.style.display = "block"
                            selectionRef.current.style.top = y.px
                            selectionRef.current.style.left = x.px
                        }
                    }

                    2 -> {
                        with (menuRef.current.style) {
                            top = y.px
                            left = x.px
                        }
                    }
                }
            }

            document.body!!.on("mousemove") { ev ->
                ev as MouseEvent

                if (isDragging) {
                    val selection = selectionRef.current
                    val dragWidth = ev.clientX - dragStartX
                    val dragHeight = ev.clientY - dragStartY

                    val dragX = if (dragWidth < 0) ev.clientX else dragStartX
                    val dragY = if (dragHeight < 0) ev.clientY else dragStartY
                    selection.style.width = dragWidth.absoluteValue.px
                    selection.style.height = dragHeight.absoluteValue.px
                    selection.style.top = dragY.px
                    selection.style.left = dragX.px

                    val children = containerRef.current.children
                    for (i in 0 until children.length) {
                        val child = children[i] ?: continue
                        val rect = child.getBoundingClientRect()
                        val centerX = rect.left.toInt() + (rect.width / 2).toInt()
                        val centerY = rect.top.toInt() + (rect.height / 2).toInt()
                        val isSelected = (centerY >= dragY && centerY <= dragY + dragHeight.absoluteValue &&
                                centerX >= dragX && centerX <= dragX + dragWidth.absoluteValue)

                        child.attr("selected", isSelected.toString())
                    }
                } else if (isMoving) {
                    movingRef.current.style.display = "block"
                    movingRef.current.style.top = ev.clientY.px
                    movingRef.current.style.left = ev.clientX.px
                }
            }.also { onUnmount { document.body!!.removeEventListener("mousemove", it) } }

            document.body!!.on("mouseup") { ev ->
                ev as MouseEvent
                if (ev.button == 2.toShort()) {
                    ev.preventDefault()
                    ev.stopPropagation()
                }

                isMoving = false
                isDragging = false

                val moving = movingRef.current
                moving.style.display = "none"

                val selection = selectionRef.current
                selection.style.display = "none"
                selection.style.width = 0.px
                selection.style.height = 0.px
            }.also { onUnmount { document.body!!.removeEventListener("mouseup", it) } }

            div(A(klass = gridClass, ref = containerRef)) {
                repeat(100) {
                    fileIcon { text(it.toString()) }
                }
            }
        }

        div(A(klass = selectionClass, ref = selectionRef))
        div(A(klass = movingClass, ref = movingRef)) {
            fileIcon { text("Moving") }
        }
        contextMenu(A(klass = menuClass, ref = menuRef)) {
            inst.open.bindTo(menuOpen)
            repeat(10) {
                contextMenuItem { text("Item $it") }
            }
        }
    }

    companion object {
        const val tag = "dt-file-container"

        init {
            define(FileContainer())
        }
    }
}

inline fun NodeCursor<*>.fileContainer(
    attrs: CommonAttributes<CustomTagWrapper<FileContainer>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<FileContainer>>.() -> Unit) = {}
) {
    baseElement(FileContainer.tag, attrs, children)
}

class FileIcon : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                display = "block"
                width = 250.px
                height = 75.px
                backgroundColor = "white"
            }

            (host.attributeEquals("selected", "true")) {
                backgroundColor = "cyan"
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-file-icon"

        init {
            define(FileIcon())
        }
    }
}

inline fun NodeCursor<*>.fileIcon(
    attrs: CommonAttributes<CustomTagWrapper<FileIcon>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<FileIcon>>.() -> Unit) = {}
) {
    baseElement(FileIcon.tag, attrs, children)
}

class ContextMenu : CustomTag(tag) {
    val open = attr<Boolean>("open")

    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)

        css {
            host {
                display = "none"
                backgroundColor = variable(Theme.surface)
                color = variable(Theme.onSurface)
                boxShadow = boxShadow(0, 1, 5, 0, "rgba(0, 0, 0, 0.2)")
                minWidth = 200.px
            }

            (host.attributeEquals(open.name, "true")) {
                display = "grid"
                gridTemplateColumns = "1fr"
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-context-menu"

        init {
            define(ContextMenu())
        }
    }
}

inline fun NodeCursor<*>.contextMenu(
    attrs: CommonAttributes<CustomTagWrapper<ContextMenu>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<ContextMenu>>.() -> Unit) = {}
) {
    baseElement(ContextMenu.tag, attrs, children)
}
class ContextMenuItem : CustomTag(tag) {
    override fun init(element: HTMLElement) = element.withShadow {
        super.init(element)
        css {
            host {
                cursor = "pointer"
                padding = 12.px
            }

            (inHost { withPseudoClass("hover") }) {
                backgroundColor = variable(Theme.hoverOnSurface)
            }
        }

        slot()
    }

    companion object {
        const val tag = "dt-context-menu-item"

        init {
            define(ContextMenuItem())
        }
    }
}

inline fun NodeCursor<*>.contextMenuItem(
    attrs: CommonAttributes<CustomTagWrapper<ContextMenuItem>> = CommonAttributes(),
    children: (NodeCursor<CustomTagWrapper<ContextMenuItem>>.() -> Unit) = {}
) {
    baseElement(ContextMenuItem.tag, attrs, children)
}