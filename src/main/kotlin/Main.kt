import tornadofx.*

fun main(args: Array<String>) {
    launch<AlexApp>(args)
}

class AlexApp : App(ChooseSpreadsheet::class)

data class Jury(val name: String)

data class Participant(
    val name: String,
    val category: String,
    val age: String,
    val residence: String? = null,
)

data class Performance(
    val participant: Participant,
    val repertoire: String,
) {
    val participantName get() = participant.name
}