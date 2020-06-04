/**
 *@author Nikolaus Knop and Niclas Thiebach
 */

package virus

import javafx.beans.property.*
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import kotlin.math.*
import kotlin.properties.ReadOnlyProperty

class Configuration(private val columns: Int) {
    private val properties = mutableMapOf<String, DoubleProperty>()

    private var column = VBox()
    private val node = HBox()

    operator fun invoke(name: String, min: Double, max: Double, default: Double): ReadOnlyProperty<Any?, Double> {
        check(name !in properties) { "Configurable property with name '$name' already present" }
        val l = Label("$name: ")
        val v = Label(format(default, max))
        val s = Slider(min, max, default)
        val b = HBox(l, v)
        b.prefWidth = 150.0
        column.children.add(HBox(b, s))
        if (column.children.size == 1) node.children.add(column)
        if (column.children.size ==  columns) column = VBox()
        s.valueProperty().addListener { _, _, new ->
            v.text = format(new.toDouble(), max)
        }
        return PropertyDelegate(s.valueProperty())
    }

    fun view(): Node {
        return node
    }

    companion object {
        fun format(value: Double, max: Double): String = buildString {
            val v = (value * 100.0).roundToInt() / 100.0
            append(v)
            val size = ceil(log10(max)).toInt() + 3
            while (length < size) append(0)
        }
    }
}