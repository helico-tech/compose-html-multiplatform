package compose.html.svg

import compose.html.AttrsBuilder

fun AttrsBuilder<*>.fill(value: String) = attr("fill", value)
fun AttrsBuilder<*>.stroke(value: String) = attr("stroke", value)
fun AttrsBuilder<*>.strokeWidth(value: Number) = attr("stroke-width", value.toString())
fun AttrsBuilder<*>.transform(value: String) = attr("transform", value)
fun AttrsBuilder<*>.opacity(value: Number) = attr("opacity", value.toString())
fun AttrsBuilder<*>.fillOpacity(value: Number) = attr("fill-opacity", value.toString())
fun AttrsBuilder<*>.strokeOpacity(value: Number) = attr("stroke-opacity", value.toString())
fun AttrsBuilder<*>.fillRule(value: String) = attr("fill-rule", value)
fun AttrsBuilder<*>.strokeLinecap(value: String) = attr("stroke-linecap", value)
fun AttrsBuilder<*>.strokeLinejoin(value: String) = attr("stroke-linejoin", value)
fun AttrsBuilder<*>.strokeDasharray(value: String) = attr("stroke-dasharray", value)
fun AttrsBuilder<*>.strokeDashoffset(value: Number) = attr("stroke-dashoffset", value.toString())
