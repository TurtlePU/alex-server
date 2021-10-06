import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tornadofx.App
import tornadofx.launch

fun main(args: Array<String>) {
    launch<AlexApp>(args)
}

class AlexApp : App(ChooseSpreadsheet::class, Style::class) {
    companion object {
        val json = Json {
            allowStructuredMapKeys = true
        }
    }
}

@Serializable
data class Jury(val name: String)

@Serializable
data class Participant(
    val name: String,
    val category: String,
    val age: String,
    val residence: String? = null,
)

@Serializable
data class Performance(
    val participant: Participant,
    val repertoire: String,
) {
    val participantName get() = participant.name
}

@Serializable
data class Snapshot(
    val sourcePath: String,
    val performances: List<Performance>,
    val jury: List<Jury>,
    val grades: Map<Performance, Map<Jury, Double?>>,
    val queue: List<Performance>,
)