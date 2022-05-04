import tornadofx.*

class Results : View("Results") {
    private val model: Model by inject()

    override val root = vbox {
        //
    }

    class Model(val leaderboard: List<Pair<Performance, Double>>) : ViewModel()
}