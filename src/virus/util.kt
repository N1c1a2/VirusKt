/**
 * @author Nikolaus Knop
 */

package virus

import javafx.scene.canvas.Canvas
import javafx.scene.layout.*
import javafx.scene.paint.Color

internal fun Region.setBackground(color: Color) {
    background = Background(BackgroundFill(color, CornerRadii.EMPTY, insets))
}