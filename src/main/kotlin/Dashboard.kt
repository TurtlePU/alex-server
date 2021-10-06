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
    private val model: Model by inject()
    private val server: Server by inject()
    private val controller: Control by inject()

    override val root = borderpane {
        top = label(server.address)
        center = tableview(model.performances) {
            readonlyColumn("Name", Performance::participantName)
            readonlyColumn("Repertoire", Performance::repertoire)
            for (jury in model.jury) {
                column<Performance, Double?>(jury.name) {
                    controller.grade(jury, it.value)
                }
            }
            column<Performance, Double?>("Total") {
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
        runAsync {
            server.start()
            currentWindow!!.setOnCloseRequest {
                onUndock()
            }
        }
    }

    override fun onUndock() {
        runAsync { server.stop() }
    }

    class Control : Controller() {
        private val model: Model by inject()

        fun participantForm(): View {
            val scope = Scope()
            setInScope(PerformanceForm.Model(model.performances::add), scope)
            return find<PerformanceForm>(scope)
        }

        fun previewResults() = find<Results>()

        fun total(performance: Performance) = model.getOrCompute(performance).mean

        fun grade(jury: Jury, performance: Performance): ObjectBinding<Double?> =
            Bindings.valueAt(model.getOrCompute(performance).grades, jury)
    }

    class Model(val performances: ObservableList<Performance>, val jury: List<Jury>): ViewModel() {
        private val grades = WeakHashMap<Performance, Data>()

        constructor(performances: Sequence<Performance>, jury: Sequence<Jury>) : this(
            performances.toMutableList().toObservable(), jury.toList()
        )

        fun getOrCompute(performance: Performance): Data = grades.computeIfAbsent(performance) {
            val grades = jury.associateWith<Jury, Double?> { null }.toObservable()
            val mean = objectBinding(grades, grades) {
                val values = values.filterNotNull()
                if (values.isEmpty()) null else values.sum() / values.size
            }
            Data(mean, grades)
        }

        class Data(val mean: ObjectBinding<Double?>, val grades: ObservableMap<Jury, Double?>)
    }
}