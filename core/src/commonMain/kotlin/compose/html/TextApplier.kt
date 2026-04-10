package compose.html

import androidx.compose.runtime.AbstractApplier

class TextApplier(root: HtmlElementNode) : AbstractApplier<HtmlNode>(root) {

    override fun insertTopDown(index: Int, instance: HtmlNode) {
        // no-op — we use bottom-up like the DomApplier
    }

    override fun insertBottomUp(index: Int, instance: HtmlNode) {
        (current as HtmlElementNode).children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        val children = (current as HtmlElementNode).children
        children.subList(index, index + count).clear()
    }

    override fun move(from: Int, to: Int, count: Int) {
        val children = (current as HtmlElementNode).children
        val moved = children.subList(from, from + count).toList()
        children.subList(from, from + count).clear()
        val insertAt = if (to > from) to - count else to
        children.addAll(insertAt, moved)
    }

    override fun onClear() {
        (current as HtmlElementNode).children.clear()
    }
}
