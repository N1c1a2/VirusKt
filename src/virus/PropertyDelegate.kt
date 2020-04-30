package virus

import javafx.beans.property.DoubleProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class PropertyDelegate(private val p: DoubleProperty) : ReadOnlyProperty<Any?, Double> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Double = p.value
}