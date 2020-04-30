package virus

import javafx.scene.shape.Circle

sealed class State {
    object Suspected : State()
    data class Infected(var ticks: Int) : State()
    data class Removed(val dead: Boolean) : State()
    data class Quarantined(var ticks: Int): State()
}

class Person(var direction: Double, var state: State, val circle: Circle)