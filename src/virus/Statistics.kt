/**
 *@author Nikolaus Knop and Niclas Thiebach
 */

package virus

import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.shape.Line
import virus.Display.Plot
import virus.Display.Textual
import kotlin.properties.ReadWriteProperty

class Statistics(private val scaleX: Double, private val scaleY: Double) {
    private val graph = Pane()
    private val info = HBox(30.0)
    private val default = mutableMapOf<String, Double>()
    private var properties = mutableMapOf<String, Double>()
    private var previous = mutableMapOf<String, Double>()
    private val textualInfo = mutableMapOf<String, Label>()
    private val plottedProperties = mutableMapOf<String, Plot>()
    private var tick = 0
    private val node = VBox(graph, info)

    init {
        graph.setPrefSize(1000.0, Simulation.POPULATION.toDouble() * scaleY)
    }

    operator fun invoke(name: String, initial: Double, display: Display): ReadWriteProperty<Any?, Double> {
        check(name !in properties) { "Statistic with name '$name' already present" }
        previous[name] = initial
        default[name] = initial
        properties[name] = initial
        when (display) {
            is Plot -> plottedProperties[name] = display
            Textual -> {
                val l = Label("$name: $initial")
                textualInfo[name] = l
                info.children.add(l)
            }
        }
        return MapDelegate(properties, name)
    }

    fun reset() {
        tick = 0
        graph.children.clear()
        for ((name, def) in default) {
            properties[name] = def
        }
    }

    fun update() {
        for ((name, l) in textualInfo) {
            val v = properties.getValue(name)
            l.text = "$name: $v"
        }
        var cumulativeY = graph.height
        for ((name, plot) in plottedProperties) {
            val v = properties.getValue(name)
            val p = previous.getValue(name)
            val l = when (plot.type) {
                PlotType.Line   -> Line(tick * scaleX, graph.prefHeight - p * scaleY, (tick + 1) * scaleX, graph.prefHeight - v * scaleY)
                PlotType.Dot    -> {
                    val x = (tick + 1) * scaleX
                    val y = p * scaleY
                    Line(x, y, x, y)
                }
                PlotType.Region -> {
                    val x = (tick + 1) * scaleX
                    Line(x, cumulativeY, x, cumulativeY - p * scaleY).also {
                        cumulativeY -= p * scaleY
                    }
                }
            }
            graph.children.add(l)
            l.stroke = plot.color
        }
        previous = properties.toMutableMap()
        tick++
    }

    fun view(): VBox = node
}