import io.ktor.server.engine.*
import io.ktor.server.netty.*
import javafx.collections.ObservableList
import tornadofx.*
import java.net.InetAddress

class Dashboard : View("My View") {
    private val controller: DashboardController by inject()

    override val root = borderpane {
        top = label(controller.serverAddress)
        center = tableview(controller.participants) {
            readonlyColumn("Name", Participant::name)
            for (jury in controller.grading) {
                column<Participant, Double?>(jury.key.name) {
                    jury.value[it.value]!!
                }
            }
            column("Total", controller::countTotal)
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

class DashboardController(val participants: ObservableList<Participant>, jury: List<Jury>) : Controller() {
    val serverAddress: String get() = "${InetAddress.getLocalHost().hostName}:$port"
    val grading = jury.associateWith {
        participants
            .associateWith { objectProperty<Double?>() }
            .toMutableMap()
            .apply { participants.onChange { event ->
                event.removed?.forEach { remove(it) }
                event.addedSubList?.let { putAll(it.associateWith { objectProperty() }) }
            } }
    }

    fun countTotal(participant: Participant): Double? {
        val grades = grading.values.mapNotNull { it[participant]!!.value }
        return if (grades.isEmpty()) null else grades.sum() / grades.size
    }

    fun participantForm(): View {
        val scope = Scope()
        setInScope(ParticipantModel(participants::add), scope)
        return find<ParticipantForm>(scope)
    }

    fun previewResults() = find<Results>()

    fun startServer() = runAsync { server.start() }

    fun stopServer() = runAsync { server.stop(1000, 5000) }

    private val port = 8080
    private val server = embeddedServer(Netty, port = port) {}
}

data class Jury(val name: String)