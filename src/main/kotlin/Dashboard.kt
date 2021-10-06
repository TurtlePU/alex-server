import tornadofx.*

class Dashboard : View("My View") {
    private val model: ContestModel by inject()
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
        private val model: ContestModel by inject()

        fun enqueueSelection(selected: Performance) {
            model.enqueue(selected)
        }

        fun participantForm(): View {
            val scope = Scope()
            setInScope(PerformanceForm.Model(model.performances::add), scope)
            return find<PerformanceForm>(scope)
        }

        fun previewResults() = find<Results>()
    }
}