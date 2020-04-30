/**
 *@author Nikolaus Knop
 */

package virus

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MapDelegate<K, V>(private val map: MutableMap<K, V>, private val key: K): ReadWriteProperty<Any?, V> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): V = map.getValue(key)

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        map[key] = value
    }
}