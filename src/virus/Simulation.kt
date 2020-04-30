/**
 *@author Nikolaus Knop
 */

package virus

import javafx.animation.*
import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.stage.Stage
import javafx.util.Duration
import virus.State.*
import kotlin.math.*
import kotlin.random.Random

class Simulation : Application() {
    private var play = false
    private val simulation = Pane()
    private val quarantine = Pane()
    private val statistics = Statistics(1.0, 10.0)
    private val config = Configuration(columns = 3)
    private val people = mutableListOf<Person>()
    private val suspectedPeople = mutableSetOf<Person>()
    private val infectedPeople = mutableSetOf<Person>()
    private val quarantinedPeople = mutableSetOf<Person>()
    private var suspected by statistics("Suspected", 0.0, Display.Plot("blue", PlotType.Line))
    private var infections by statistics("Infections", 0.0, Display.Plot("red", PlotType.Line))
    private var dead by statistics("Dead", 0.0, Display.Plot("gray", PlotType.Line))
    private var recovered by statistics("Recovered", 0.0, Display.Plot("green", PlotType.Line))
    private val infectionRadius by config("Infection Radius", 1.0, 50.0, 15.0)
    private val hospitalCapacity by config("Krankenhauskapazität", 1.0, POPULATION.toDouble(), POPULATION / 10.0)
    private val velocity by config("Velocity", 0.0, 10.0, 3.0)
    private val lethalityWithTreatment by config("Lethality with treatment", 0.0, 1.0, 0.1)
    private val lethalityWithoutTreatment by config("Lethality without treatment", 0.0, 1.0, 0.3)
    private val discoveryTime by config("Discovery time", 0.0, 10.0, 3.0)
    private val pToTravel by config("P(Travel)", 0.0, 1.0, 0.0)
    private val pQuarantine by config("P(Quarantäne)", 0.0, 1.0, 0.0)
    private val pMovementToCentralHub by config("P(Move to Central Hub)", 0.0, 1.0, 0.0)
    private var tick = 0

    init {
        simulation.setPrefSize(WIDTH, HEIGHT)
        quarantine.setPrefSize(WIDTH_Q, HEIGHT_Q)
        quarantine.setBackground(Color.BLACK)
        statistics.view().resize(WIDTH, HEIGHT)
        simulation.setBackground(Color.BLACK)
        simulation.setBorder(Color.WHITE, 5.0)
        statistics.view().setBackground(Color.BLACK)
        createPopulation()
    }

    private fun infect(person: Person) {
        person.state = Infected(ticks = 0)
        person.circle.fill = Color.RED
        suspectedPeople.remove(person)
        infectedPeople.add(person)
    }

    private fun createPopulation() {
        repeat(POPULATION) {
            val cx = Random.nextDouble(BORDER, WIDTH - BORDER)
            val cy = Random.nextDouble(BORDER, HEIGHT - BORDER)
            val c = Circle(cx, cy, RADIUS, Color.BLUE)
            val dir = randomAngle()
            val p = Person(dir, Suspected, c)
            people.add(p)
            simulation.children.add(c)
        }
        suspectedPeople.addAll(people)
    }

    private fun move() {
        for (p in people) {
            val c = p.circle
            c.centerX += sin(p.direction) * velocity
            c.centerY += cos(p.direction) * velocity
            if (c.centerX !in BORDER..WIDTH - BORDER || c.centerY !in BORDER..HEIGHT - BORDER) {
                p.direction += PI
            } else if (Random.nextFloat() <= 0.05) {
                p.direction = randomAngle()
            }
        }
        for (p in quarantinedPeople) {
            val c = p.circle
            c.centerX += sin(p.direction) * velocity
            c.centerY += cos(p.direction) * velocity
            if (c.centerX !in BORDER_Q..WIDTH_Q - BORDER_Q || c.centerY !in BORDER_Q..HEIGHT_Q - BORDER_Q) {
                p.direction += PI
            } else if (Random.nextFloat() <= 0.05) {
                p.direction = randomAngle()
            }
        }
    }

    private fun infect() {
        val infectors = mutableSetOf<Person>()
        for (infector in infectedPeople.toList()) {
            if (Random.nextFloat() <= 0.05) {
                for (victim in suspectedPeople.toList()) {
                    if (victim.state is Infected) continue
                    val x1 = infector.circle.centerX
                    val y1 = infector.circle.centerY
                    val x2 = victim.circle.centerX
                    val y2 = victim.circle.centerY
                    val dist = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
                    if (dist <= infectionRadius) {
                        infectors.add(infector)
                        infect(victim)
                    }
                }
            }
        }
        for (infector in infectors) {
            val c = infector.circle
            val r = Circle(c.centerX, c.centerY, 0.0)
            c.centerXProperty().addListener { _, _, x -> r.centerX = x.toDouble() }
            c.centerYProperty().addListener { _, _, y -> r.centerY = y.toDouble() }
            r.fill = Color.TRANSPARENT
            r.stroke = Color.RED
            simulation.children.add(r)
            val t = object : Transition() {
                init {
                    cycleDuration = DAY.multiply(0.7)
                }

                override fun interpolate(frac: Double) {
                    r.radius = frac * infectionRadius
                }
            }
            t.play()
            t.setOnFinished { simulation.children.remove(r) }
        }
    }

    private fun randomAngle() = Random.nextDouble() * 2 * PI

    private fun repeat(duration: Duration?, action: () -> Unit) {
        val timeline = Timeline(KeyFrame(duration, EventHandler { action() }))
        timeline.cycleCount = Animation.INDEFINITE
        timeline.play()
    }

    private fun remove(person: Person) {
        val dies = dies()
        person.state = Removed(dies)
        if (dies) {
            dead++
            simulation.children.remove(person.circle)
        } else {
            recovered++
            person.circle.fill = Color.GREEN
        }
        infectedPeople.remove(person)
    }

    private fun remove() {
        for (p in infectedPeople.toList()) {
            val s = p.state as Infected
            if (Random.nextFloat() <= log10((++s.ticks) / 300.0)) {
                remove(p)
            }
        }
    }

    private fun removeFromQuarantine() {
        for (p in quarantinedPeople.toList()) {
            val s = p.state as Infected
            if (Random.nextFloat() <= log10((++s.ticks) / 300.0)) {
                removeFromQuarantine(p)
            }
        }
    }

    private fun removeFromQuarantine(p: Person) {
        val dies = dies()
        p.state = Removed(dies)
        quarantinedPeople.remove(p)
        quarantine.children.remove(p.circle)
        if (dies) {
            dead++
        } else {
            recovered++
            p.circle.fill = Color.GREEN
            simulation.children.add(p.circle)
        }
    }

    private fun dies() = Random.nextDouble() <= lethality(infectedPeople.size)

    private fun quarantine() {
        for (p in infectedPeople.toList()) {
            val s = p.state as Infected
            if (s.ticks == (discoveryTime * 20).toInt() && Random.nextFloat() <= pQuarantine) {
                quarantine(p)
            }
        }
    }

    private fun quarantine(p: Person) {
        simulation.children.remove(p.circle)
        p.circle.centerX = Random.nextDouble(BORDER_Q, WIDTH_Q - BORDER_Q)
        p.circle.centerY = Random.nextDouble(BORDER_Q, HEIGHT_Q - BORDER_Q)
        quarantine.children.add(p.circle)
        infectedPeople.remove(p)
        quarantinedPeople.add(p)
    }

    private fun nextTick() {
        move()
        infect()
        remove()
        quarantine()
        removeFromQuarantine()
        infections = infectedPeople.size.toDouble()
        suspected = suspectedPeople.size.toDouble()
        statistics.update()
        tick++
    }

    private fun reset() {
        tick = 0
        people.clear()
        suspectedPeople.clear()
        infectedPeople.clear()
        quarantinedPeople.clear()
        simulation.children.clear()
        quarantine.children.clear()
        statistics.reset()
        createPopulation()
    }

    override fun start(primaryStage: Stage) {
        val startStop = Button("Start")
        val reset = Button("Reset")
        val initDisease = Button("Infect Person")
        startStop.setOnAction {
            play = !play
            startStop.text = if (play) "Stop" else "Start"
        }
        reset.setOnAction {
            reset()
        }
        initDisease.setOnAction {
            infect(suspectedPeople.random())
        }
        primaryStage.scene = Scene(VBox(
            HBox(simulation, VBox(quarantine)),
            HBox(startStop, reset, initDisease),
            config.view()
        ))
        val graph = Stage()
        graph.scene = Scene(statistics.view())
        repeat(Duration.millis(50.0)) {
            if (play) nextTick()
        }
        primaryStage.show()
        graph.show()
    }

    private fun lethality(infections: Int): Double {
        val pTreatment = (hospitalCapacity / infections).coerceAtMost(1.0)
        val pDiesWithTreatment = pTreatment * lethalityWithTreatment
        val pDiesWithoutTreatment = (1.0 - pTreatment) * lethalityWithoutTreatment
        return pDiesWithTreatment + pDiesWithoutTreatment
    }

    private fun Region.setBorder(color: Color, width: Double) {
        border = Border(
            BorderStroke(
                color,
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                BorderWidths(width),
                insets
            )
        )
    }

    companion object {
        private const val WIDTH = 800.0
        private const val HEIGHT = 800.0
        private const val BORDER = 20.0
        private const val RADIUS = 5.0
        const val POPULATION = 100
        private const val WIDTH_Q = 300.0
        private const val HEIGHT_Q = 300.0
        private const val BORDER_Q = 10.0
        private val DAY = Duration.seconds(1.0)

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Simulation::class.java, *args)
        }
    }
}