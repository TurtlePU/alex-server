import kotlinx.serialization.encodeToString
import tornadofx.*
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter

class Dashboard : View("My View") {
    private val model: ContestModel by inject()
    private val server: Server by inject()
    private val controller: Control by inject()

    override val root = borderpane {
        top = borderpane {
            center = label("Server is running at ${server.address}")
        }
        center = tableview(model.performances) {
            readonlyColumn("№", Performance::id)
            readonlyColumn("Name", Performance::participantName)
            readonlyColumn("Repertoire", Performance::repertoire)
            for (jury in model.jury) {
                column<Performance, Double?>(jury) {
                    model.viewGrade(jury, it.value)
                }
            }
            column<Performance, Double?>("Total") {
                model.countTotal(it.value)
            }
            column<Performance, Boolean>("Enqueued") {
                model.isEnqueued(it.value)
            }.cellFormat {
                if (it) {
                    text = "YES"
                    tableRow?.removeClass(Style.dequeued)
                    tableRow?.addClass(Style.enqueued)
                } else {
                    text = "NO"
                    tableRow?.removeClass(Style.enqueued)
                    tableRow?.addClass(Style.dequeued)
                }
            }
            onDoubleClick { controller.enqueueSelection(selectedItem!!) }
        }
        bottom = borderpane {
            left = button("+") {
                action { openInternalWindow(controller.participantForm()) }
            }
            center = button(messages["preview.results"]) {
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
        runAsync {
            controller.dumpSnapshot()
            server.stop()
        }
    }

    class Control : Controller() {
        private val model: ContestModel by inject()

        fun enqueueSelection(selected: Performance) {
            model.enqueue(selected)
        }

        fun participantForm(): View {
            val scope = Scope()
            setInScope(PerformanceForm.Model(model::addSorted), scope)
            return find<PerformanceForm>(scope)
        }

        fun previewResults(): View {
            val scope = Scope()
            setInScope(Results.Model(model.leaderboard()), scope)
            return find<Results>(scope)
        }

        fun dumpSnapshot() {
            val snapshot = model.snapshot()
            val str = Protocol.json.encodeToString(snapshot)
            File(snapshot.sourcePath).dumpFile().bufferedWriter().use {
                it.write(str)
            }
        }

        companion object {
            fun File.dumpFile(instant: Instant = Instant.now(), pid: Long = ProcessHandle.current().pid()): File {
                val timestamp = DateTimeFormatter.ISO_INSTANT.format(instant)
                return File("$parent/$nameWithoutExtension-$timestamp-$pid.json")
            }
        }
    }
}