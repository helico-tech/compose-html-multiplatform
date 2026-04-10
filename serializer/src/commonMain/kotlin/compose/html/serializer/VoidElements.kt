package compose.html.serializer

import compose.html.HtmlElementNode

private val HTML_VOID_ELEMENTS = setOf(
    "area", "base", "br", "col", "embed", "hr", "img",
    "input", "link", "meta", "param", "source", "track", "wbr",
)

private val SVG_CONTAINER_ELEMENTS = setOf(
    "svg", "g", "defs", "symbol", "marker", "clipPath", "mask",
    "pattern", "filter", "linearGradient", "radialGradient",
    "text", "tspan", "textPath", "a", "switch", "view",
)

fun isVoidElement(node: HtmlElementNode): Boolean {
    if (node.namespace == null && node.tagName in HTML_VOID_ELEMENTS) return true
    if (node.namespace != null && node.tagName !in SVG_CONTAINER_ELEMENTS) return true
    return false
}
