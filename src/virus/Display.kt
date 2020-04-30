/**
 *@author Nikolaus Knop
 */

package virus

import javafx.scene.paint.Color

sealed class Display {
    data class Plot(val color: Color, val type: PlotType) : Display() {
        constructor(color: String, type: PlotType): this(Color.web(color), type)
    }

    object Textual: Display()
}

enum class PlotType {
    Line, Dot, Region
}