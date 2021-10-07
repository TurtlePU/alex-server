import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Protocol {
    val json = Json {
        allowStructuredMapKeys = true
    }
}

@Serializable
data class Jury(val name: String)

@Serializable
data class JuryToken(val value: String)

@Serializable
data class Participant(
    val name: String,
    val category: String,
    val age: String,
    val residence: String? = null,
    val teacher: String? = null,
)

@Serializable
data class Performance(
    val participant: Participant,
    val repertoire: String,
) {
    val participantName get() = participant.name
}

@Serializable
data class PostAuth(val jury: Jury, val token: JuryToken)

@Serializable
data class PostGrade(val jury: Jury, val token: JuryToken, val performance: Performance, val grade: Double)
