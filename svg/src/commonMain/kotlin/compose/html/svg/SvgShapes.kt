package compose.html.svg

import androidx.compose.runtime.Composable
import compose.html.AttrsBuilder

@Composable
fun Circle(cx: Number, cy: Number, r: Number, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("circle", attrs = {
        attr("cx", cx.toString()); attr("cy", cy.toString()); attr("r", r.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Rect(x: Number, y: Number, width: Number, height: Number, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("rect", attrs = {
        attr("x", x.toString()); attr("y", y.toString())
        attr("width", width.toString()); attr("height", height.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Ellipse(cx: Number, cy: Number, rx: Number, ry: Number, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("ellipse", attrs = {
        attr("cx", cx.toString()); attr("cy", cy.toString())
        attr("rx", rx.toString()); attr("ry", ry.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Line(x1: Number, y1: Number, x2: Number, y2: Number, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("line", attrs = {
        attr("x1", x1.toString()); attr("y1", y1.toString())
        attr("x2", x2.toString()); attr("y2", y2.toString())
        attrs?.invoke(this)
    })
}

@Composable
fun Path(d: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("path", attrs = { attr("d", d); attrs?.invoke(this) })
}

@Composable
fun Polygon(points: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("polygon", attrs = { attr("points", points); attrs?.invoke(this) })
}

@Composable
fun Polyline(points: String, attrs: (AttrsBuilder<*>.() -> Unit)? = null) {
    SvgElement("polyline", attrs = { attr("points", points); attrs?.invoke(this) })
}
