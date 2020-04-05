package virus

import virus.Virus.State.*
import javafx.animation.*
import javafx.application.Application
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.stage.Stage
import javafx.util.Duration
import kotlin.math.*
import kotlin.random.Random

class Virus : Application() {
    private val simulation = Pane()
    private val graph = Pane()
    private val people = mutableListOf<Person>()
    private val suspected = mutableSetOf<Person>()
    private val infected = mutableSetOf<Person>()
    private var day = 0
    private var suspectedLast = POPULATION
    private var infectionsLast = 0
    private var deadLast = 0
    private var dead = 0
    private var recoveredLast = 0
    private var recovered = 0

    private fun Region.setBackground(color: Color) {
        background = Background(BackgroundFill(color, CornerRadii.EMPTY, simulation.insets))
    }

    private fun infect(person: Person) {
        person.state = Infected(days = 0)
        person.circle.fill = Color.RED
        suspected.remove(person)
        infected.add(person)
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
        suspected.addAll(people)
    }

    private fun move() {
        for (p in people) {
            val c = p.circle
            c.centerX += sin(p.direction) * VELOCITY
            c.centerY += cos(p.direction) * VELOCITY
            if (c.centerX !in BORDER..WIDTH - BORDER || c.centerY !in BORDER..HEIGHT - BORDER) {
                p.direction += PI
            } else if (Random.nextFloat() <= 0.05) {
                p.direction = randomAngle()
            }
        }
    }

    private fun infect() {
        val infectors = mutableSetOf<Person>()
        for (infector in infected.toList()) {
            for (victim in suspected.toList()) {
                if (victim.state is Infected) continue
                val x1 = infector.circle.centerX
                val y1 = infector.circle.centerY
                val x2 = victim.circle.centerX
                val y2 = victim.circle.centerY
                val dist = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
                if (dist <= INFECTION_RADIUS) {
                    infectors.add(infector)
                    infect(victim)
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
                    r.radius = frac * INFECTION_RADIUS
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

    private fun updateGraph(last: Double, now: Double, color: Color, yScale: Double = HEIGHT / POPULATION) {
        val fromY = last * yScale
        val toY = now * yScale
        val l = Line(10.0 * day, HEIGHT - fromY, 10.0 * (day + 1), HEIGHT - toY)
        l.stroke = color
        graph.children.add(l)
    }

    private fun updateGraph(last: Int, now: Int, color: Color, yScale: Double = HEIGHT / POPULATION) {
        updateGraph(last.toDouble(), now.toDouble(), color, yScale)
    }

    private fun remove(person: Person) {
        val dies = Random.nextDouble() <= lethality(infected.size)
        person.state = Removed(dies)
        if (dies) {
            dead++
            simulation.children.remove(person.circle)
        } else {
            recovered++
            person.circle.fill = Color.GREEN
        }
        infected.remove(person)
    }

    private fun remove() {
        for (p in infected.toList()) {
            val s = p.state as Infected
            if (s.days++ == 14) {
                remove(p)
            }
        }
    }

    override fun start(stage: Stage) {
        simulation.setPrefSize(WIDTH, HEIGHT)
        graph.setPrefSize(WIDTH, HEIGHT)
        simulation.setBackground(Color.BLACK)
        graph.setBackground(Color.BLACK)
        createPopulation()
        val y = HEIGHT - HOSPITAL_CAPACITY * (HEIGHT / POPULATION)
        println(y)
        val capacity = Line(0.0, y, WIDTH, y)
        capacity.stroke = Color.WHITE
        graph.children.add(capacity)
        infect(people.first())
        repeat(Duration.millis(50.0)) { move() }
        repeat(DAY) {
            remove()
            updateGraph(recoveredLast, recovered, Color.GREEN)
            recoveredLast = recovered
            updateGraph(deadLast, dead, Color.GRAY)
            deadLast = dead
            infect()
            updateGraph(infectionsLast, infected.size, Color.RED)
            println(lethality(infected.size) * HEIGHT)
            updateGraph(lethality(infectionsLast), lethality(infected.size), Color.ORANGE, yScale = HEIGHT)
            infectionsLast = infected.size
            updateGraph(suspectedLast, suspected.size, Color.BLUE)
            suspectedLast = suspected.size
            day++
        }
        stage.scene = Scene(HBox(simulation, graph))
        stage.show()
    }

    private fun lethality(infections: Int): Double {
        val pTreatment = (HOSPITAL_CAPACITY.toDouble() / infections).coerceAtMost(1.0)
        val pDiesWithTreatment = pTreatment * LETHALITY_WITH_TREATMENT
        val pDiesWithoutTreatment = (1.0 - pTreatment) * LETHALITY_WITHOUT_TREATMENT
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
        private const val WIDTH = 1000.0
        private const val HEIGHT = 1000.0
        private const val BORDER = 20.0
        private const val RADIUS = 5.0
        private const val INFECTION_RADIUS = 15.0
        private const val POPULATION = 100
        private const val VELOCITY = 3
        private const val LETHALITY_WITH_TREATMENT = 0.01
        private const val LETHALITY_WITHOUT_TREATMENT = 0.5
        private const val HOSPITAL_CAPACITY = 20
        private val DAY = Duration.seconds(1.0)

        @JvmStatic
        fun main(args: Array<String>) {
            launch(Virus::class.java, *args)
        }
    }

    class Person(var direction: Double, var state: State, val circle: Circle)

    sealed class State {
        object Suspected : State()
        data class Infected(var days: Int) : State()
        data class Removed(val dead: Boolean) : State()
    }
}
