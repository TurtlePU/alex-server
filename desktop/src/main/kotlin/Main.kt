import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    launch<AlexApp>(args)
}

class AlexApp : App(ChooseSpreadsheet::class, Style::class)