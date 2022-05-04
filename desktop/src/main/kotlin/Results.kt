import tornadofx.*

class Results : View("Results") {
    private val model: Model by inject()

    override val root = vbox {
        tableview(model.leaderboard.toObservable()) {
            readonlyColumn("Name", PerformanceResult::participant)
            readonlyColumn("Repertoire", PerformanceResult::repertoire)
            readonlyColumn("Total", PerformanceResult::total)
        }
    }

    class Model(val leaderboard: List<PerformanceResult>) : ViewModel()

    data class PerformanceResult(val performance: Performance, val total: Double) {
        val participant: String get() = performance.participantName
        val repertoire: String get() = performance.repertoire
    }
}