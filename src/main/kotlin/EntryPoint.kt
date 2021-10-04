import tornadofx.*

fun main(args: Array<String>) {
    launch<AlexApp>(args)
}

class AlexApp : App(ChooseSpreadsheet::class)