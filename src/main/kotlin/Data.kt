data class Jury(val name: String)

data class Participant(
    val name: String,
    val category: String,
    val age: String,
    val residence: String? = null,
)