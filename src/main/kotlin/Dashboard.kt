import io.ktor.server.engine.*
import io.ktor.server.netty.*
import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import tornadofx.*
import java.net.InetAddress
import java.util.*

class Dashboard : View("My View") {
    private val controller: DashboardController by inject()

    override val root = borderpane {
        top = label(controller.serverAddress)
        center = tableview(controller.participants) {
            readonlyColumn("Name", Participant::name)
            for (jury in controller.jury) {
                column<Participant, Double?>(jury.name) {
                    controller.grade(jury, it.value)
                }
            }
            column<Participant, Double?>("Total") {
                controller.total(it.value)
            }
        }
        bottom = hbox {
            button("+") {
                action { openInternalWindow(controller.participantForm()) }
            }
            button(messages["preview.results"]) {
                action { openInternalWindow(controller.previewResults()) }
            }
        }
    }

    override fun onDock() {
        controller.startServer()
        currentWindow!!.setOnCloseRequest {
            onUndock()
        }
    }

    override fun onUndock() {
        controller.stopServer()
    }
}

class DashboardController(val participants: ObservableList<Participant>, val jury: List<Jury>) : Controller() {
    val serverAddress: String get() = "${InetAddress.getLocalHost().hostName}:$port"

    fun participantForm(): View {
        val scope = Scope()
        setInScope(ParticipantModel(participants::add), scope)
        return find<ParticipantForm>(scope)
    }

    fun previewResults() = find<Results>()

    fun startServer() = runAsync { server.start() }

    fun stopServer() = runAsync { server.stop(1000, 5000) }

    fun total(participant: Participant) = observables[participant].mean

    fun grade(jury: Jury, participant: Participant): ObjectBinding<Double?> =
        Bindings.valueAt(observables[participant].grades, jury)

    private val observables = Observables(jury)

    private val port = 8080
    private val server = embeddedServer(Netty, port = port) {}
}

class Observables(private val jury: List<Jury>) {
    operator fun get(participant: Participant): Data {
        return values.computeIfAbsent(participant) {
            val grades = jury.associateWith<Jury, Double?> { null }.toObservable()
            val mean = objectBinding(grades, grades) {
                val values = values.filterNotNull()
                if (values.isEmpty()) null else values.sum() / values.size
            }
            Data(mean, grades)
        }
    }

    class Data(val mean: ObjectBinding<Double?>, val grades: ObservableMap<Jury, Double?>)

    private val values = WeakHashMap<Participant, Data>()
}

data class Jury(val name: String)